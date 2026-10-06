package com.startuphub.dto;

import com.startuphub.model.Expense;
import java.time.LocalDate;

public record ExpenseDto(Long id, double amount, String category, LocalDate recordedOn) {
    public static ExpenseDto from(Expense e) {
        return new ExpenseDto(e.getId(), e.getAmount(), e.getCategory(), e.getRecordedOn());
    }
}
