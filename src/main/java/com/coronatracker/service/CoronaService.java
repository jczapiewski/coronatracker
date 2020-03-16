package com.coronatracker.service;

import com.coronatracker.entity.CoronaStats;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CoronaService {

    @Value("${corona.data.url}")
    private String VIRUS_DATA_URL;
    private List<CoronaStats> stats = new ArrayList<>();

    public List<CoronaStats> getStats() {
        return stats;
    }

    public int getTotalCases() {
        return stats.stream()
                .mapToInt(CoronaStats::getLatestTotal)
                .sum();
    }

    public int getNewCases() {
        return stats.stream()
                .filter(stat -> stat.getDailyDifference() > 0)
                .mapToInt(CoronaStats::getDailyDifference)
                .sum();
    }

    @PostConstruct
    @Scheduled(cron = "* * 1 * * *")
    private void fetchData() throws IOException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(VIRUS_DATA_URL))
                .build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        CSVParser records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(httpResponse.body()));
        this.stats = getStatsMap(records);
    }

    private List<CoronaStats> getStatsMap(CSVParser parser) throws IOException {
        return parser.getRecords().stream()
                .filter(stat -> dailyCases(stat, 1) > 0)
                .map(this::createCoronaStat)
                .sorted(Comparator.comparingInt(CoronaStats::getLatestTotal).reversed())
                .collect(Collectors.toList());
    }

    private CoronaStats createCoronaStat(CSVRecord reader) {
        int latestCases = dailyCases(reader, 1);
        return CoronaStats.builder()
                .country(reader.get("Country/Region"))
                .state(reader.get("Province/State"))
                .latestTotal(latestCases)
                .dailyDifference(latestCases - dailyCases(reader, 2))
                .build();
    }

    private int dailyCases(CSVRecord reader, int i) {
        return Integer.parseInt(reader.get(reader.size() - i));
    }
}
