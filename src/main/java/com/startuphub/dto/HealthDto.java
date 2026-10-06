package com.startuphub.dto;

import java.util.List;

/** Startup Health Score: a score out of 100 plus the factors that produced it. */
public record HealthDto(int score, String grade, List<HealthFactorDto> factors) {

    /** status is GOOD / WARN / BAD - the frontend maps these onto badge colours. */
    public record HealthFactorDto(String key, String label, String status, String detail, int score, int max) {}
}
