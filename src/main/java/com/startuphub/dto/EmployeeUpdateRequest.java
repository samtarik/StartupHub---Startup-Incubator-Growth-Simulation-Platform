package com.startuphub.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record EmployeeUpdateRequest(@NotBlank(message = "Name is required") String name,
                                    @NotBlank(message = "Job title is required") String jobTitle,
                                    @PositiveOrZero(message = "Salary cannot be negative") double salary,
                                    @Min(value = 0, message = "Performance must be between 0 and 100")
                                    @Max(value = 100, message = "Performance must be between 0 and 100")
                                    Integer performanceScore) {}
