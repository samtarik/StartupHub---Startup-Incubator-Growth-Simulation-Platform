package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record EmployeeRequest(@NotBlank(message = "Name is required") String name,
                              @NotBlank(message = "Job title is required") String jobTitle,
                              @PositiveOrZero(message = "Salary cannot be negative") double salary) {}
