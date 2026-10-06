package com.startuphub.controller;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Startup;
import com.startuphub.model.Expense;
import com.startuphub.model.Revenue;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.ExpenseDto;
import com.startuphub.dto.ExpenseRequest;
import com.startuphub.dto.RevenueDto;
import com.startuphub.dto.RevenueRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.FinanceService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/startups/{startupId}/finance")
public class FinanceController {
    private final FinanceService financeService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public FinanceController(FinanceService financeService, StartupService startupService, AccessGuard accessGuard) {
        this.financeService = financeService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping
    public Map<String, Object> view(@PathVariable Long startupId) {
        double revenue = financeService.totalRevenue(startupId);
        double expense = financeService.totalExpense(startupId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("revenues", financeService.findRevenues(startupId).stream().map(RevenueDto::from).toList());
        out.put("expenses", financeService.findExpenses(startupId).stream().map(ExpenseDto::from).toList());
        out.put("totalRevenue", revenue);
        out.put("totalExpense", expense);
        out.put("netPosition", revenue - expense);
        out.put("burnRate", financeService.burnRate(startupId));
        return out;
    }

    @PostMapping("/revenue")
    public RevenueDto addRevenue(@PathVariable Long startupId, @Valid @RequestBody RevenueRequest req,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        var revenue = financeService.addRevenue(req.amount(), req.source(), startup);
        if (req.recordedOn() != null) revenue = financeService.updateRevenue(revenue.getId(), req.amount(), req.source(), req.recordedOn());
        return RevenueDto.from(revenue);
    }

    @PostMapping("/expense")
    public ExpenseDto addExpense(@PathVariable Long startupId, @Valid @RequestBody ExpenseRequest req,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        var expense = financeService.addExpense(req.amount(), req.category(), startup);
        if (req.recordedOn() != null) expense = financeService.updateExpense(expense.getId(), req.amount(), req.category(), req.recordedOn());
        return ExpenseDto.from(expense);
    }

    // ---- Automatic revenue calculator -------------------------------------------------

    /** Preview: the server calculates with the Strategy for the chosen model. Nothing is stored. */
    @PostMapping("/revenue/calculate")
    public RevenueCalculationResponse calculate(@PathVariable Long startupId,
                                                @Valid @RequestBody RevenueCalculationRequest req,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return financeService.calculateRevenue(req);
    }

    /** Save: the server calculates again from the inputs and stores one Revenue record. */
    @PostMapping("/revenue/calculated")
    public RevenueDto saveCalculated(@PathVariable Long startupId,
                                     @Valid @RequestBody RevenueCalculationRequest req,
                                     @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return RevenueDto.from(financeService.saveCalculatedRevenue(startup, req));
    }

    // ---- Edit / delete ---------------------------------------------------------------

    @PutMapping("/revenue/{revenueId}")
    public RevenueDto updateRevenue(@PathVariable Long startupId, @PathVariable Long revenueId,
                                    @Valid @RequestBody RevenueRequest req,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        Revenue revenue = financeService.findRevenue(revenueId);
        requireBelongsAndOwner(startupId, revenue.getStartup(), principal, "Revenue record not found: " + revenueId);
        return RevenueDto.from(financeService.updateRevenue(revenueId, req.amount(), req.source(), req.recordedOn()));
    }

    @DeleteMapping("/revenue/{revenueId}")
    public void deleteRevenue(@PathVariable Long startupId, @PathVariable Long revenueId,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        Revenue revenue = financeService.findRevenue(revenueId);
        requireBelongsAndOwner(startupId, revenue.getStartup(), principal, "Revenue record not found: " + revenueId);
        financeService.deleteRevenue(revenueId);
    }

    @PutMapping("/expense/{expenseId}")
    public ExpenseDto updateExpense(@PathVariable Long startupId, @PathVariable Long expenseId,
                                    @Valid @RequestBody ExpenseRequest req,
                                    @AuthenticationPrincipal CustomUserDetails principal) {
        Expense expense = financeService.findExpense(expenseId);
        requireBelongsAndOwner(startupId, expense.getStartup(), principal, "Expense record not found: " + expenseId);
        return ExpenseDto.from(financeService.updateExpense(expenseId, req.amount(), req.category(), req.recordedOn()));
    }

    @DeleteMapping("/expense/{expenseId}")
    public void deleteExpense(@PathVariable Long startupId, @PathVariable Long expenseId,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        Expense expense = financeService.findExpense(expenseId);
        requireBelongsAndOwner(startupId, expense.getStartup(), principal, "Expense record not found: " + expenseId);
        financeService.deleteExpense(expenseId);
    }

    /** The record must belong to the startup in the URL, and the caller must own that startup. */
    private void requireBelongsAndOwner(Long startupId, Startup owner, CustomUserDetails principal, String notFound) {
        if (!owner.getId().equals(startupId)) throw new ResourceNotFoundException(notFound);
        accessGuard.requireOwner(principal.getUser(), owner);
    }
}
