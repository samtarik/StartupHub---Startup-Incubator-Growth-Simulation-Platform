package com.startuphub.service;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.model.Expense;
import com.startuphub.model.Revenue;
import com.startuphub.model.Startup;
import java.time.LocalDate;
import java.util.List;

public interface FinanceService {
    Revenue addRevenue(double amount, String source, Startup startup);
    Expense addExpense(double amount, String category, Startup startup);
    List<Revenue> findRevenues(Long startupId);
    List<Expense> findExpenses(Long startupId);
    double totalRevenue(Long startupId);
    double totalExpense(Long startupId);
    double burnRate(Long startupId);
    double projectRevenue(String strategyName, double baseAmount, long customers);

    // ---- edit / delete ----
    Revenue findRevenue(Long revenueId);
    Expense findExpense(Long expenseId);
    Revenue updateRevenue(Long revenueId, double amount, String source, LocalDate recordedOn);
    Expense updateExpense(Long expenseId, double amount, String category, LocalDate recordedOn);
    void deleteRevenue(Long revenueId);
    void deleteExpense(Long expenseId);

    // ---- automatic revenue calculator (Strategy pattern) ----
    /** Pure calculation, nothing is stored. */
    RevenueCalculationResponse calculateRevenue(RevenueCalculationRequest request);

    /** Re-runs the calculation on the server and stores exactly one Revenue record for it. */
    Revenue saveCalculatedRevenue(Startup startup, RevenueCalculationRequest request);
}
