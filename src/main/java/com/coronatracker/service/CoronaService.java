package com.coronatracker.service;

import com.coronatracker.entity.CoronaStats;
import com.coronatracker.entity.Point;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;
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

    @Value("${corona.confirmed.data.url}")
    private String VIRUS_DATA_CONFIRMED_URL;

    @Value("${corona.deaths.data.url}")
    private String virusDataDeathsUrl;

    @Value("${corona.recovered.data.url}")
    private String virusDataRecoversUrl;

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
                .uri(URI.create(VIRUS_DATA_CONFIRMED_URL))
                .build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        HttpRequest deathRequest = getHttpRequest();
        HttpResponse<String> deathResponse = httpClient.send(deathRequest, HttpResponse.BodyHandlers.ofString());
        CSVParser confirmedRecords = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(httpResponse.body()));
        CSVParser csvDeathRecords = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(new StringReader(deathResponse.body()));
        this.stats = getStatsMap(confirmedRecords, this::createCoronaStat, CoronaStats::getLatestTotal);
    }

    private HttpRequest getHttpRequest() {
        return HttpRequest.newBuilder()
                    .uri(URI.create(virusDataDeathsUrl))
                    .build();
    }

    private List<CoronaStats> getStatsMap(CSVParser parser, Function<? super CSVRecord, ? extends CoronaStats> function, ToIntFunction<? super CoronaStats> toIntFunction) throws IOException {
        return parser.getRecords().stream()
                .filter(stat -> dailyCases(stat, 1) > 0)
                .map(function)
                .sorted(Comparator.comparingInt(toIntFunction).reversed())
                .collect(Collectors.toList());
    }

    private CoronaStats createCoronaStat(CSVRecord reader) {
        int latestCases = dailyCases(reader, 1);
        CoronaStats coronaStats = CoronaStats.builder()
                .country(reader.get("Country/Region"))
                .state(reader.get("Province/State"))
                .latestTotal(latestCases)
                .dailyDifference(latestCases - dailyCases(reader, 2))
                .build();
        coronaStats.setPoint(getPointForStat(reader, coronaStats));
        return coronaStats;
    }

    private Point getPointForStat(CSVRecord reader, CoronaStats stats) {
        return Point.builder()
                .lat(Double.parseDouble(reader.get("Lat")))
                .lon(Double.parseDouble(reader.get("Long")))
                .text("<b>" + stats.getCountry() + "<br>" + stats.getState() + "</b>" + "<br>" + "Cases of Coronavirus: " + stats.getLatestTotal())
                .build();
    }

    private int dailyCases(CSVRecord reader, int i) {
        String value = reader.get(reader.size() - i);
        if (value.equals("")) {
            return 0;
        }
        return Integer.parseInt(value);
    }
}
