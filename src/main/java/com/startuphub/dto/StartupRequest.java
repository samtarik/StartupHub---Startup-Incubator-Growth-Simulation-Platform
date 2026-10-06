package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;

public record StartupRequest(@NotBlank String name, String description) {}
