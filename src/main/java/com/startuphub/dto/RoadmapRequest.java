package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;

public record RoadmapRequest(@NotBlank String title) {}
