package com.coronatracker.entity;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class CoronaStats {

    private String state;
    private String country;
    private Point point;
    private int latestTotal;
    private int deathsTotal;
    private int recoversTotal;
    private int dailyDifference;
    private LocalDate currentDate;
}
