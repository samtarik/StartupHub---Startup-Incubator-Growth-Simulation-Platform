package com.startuphub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** recordedOn is optional: omitted on create means "today", omitted on edit means "unchanged". */
public record ExpenseRequest(@Positive(message = "Amount must be greater than zero") double amount,
                             @NotBlank(message = "Category is required") String category,
                             LocalDate recordedOn) {}
