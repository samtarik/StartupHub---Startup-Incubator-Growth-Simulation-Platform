package com.startuphub.service.impl;

import com.startuphub.dto.RevenueCalculationRequest;
import com.startuphub.dto.RevenueCalculationResponse;
import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Expense;
import com.startuphub.model.Revenue;
import com.startuphub.model.Startup;
import com.startuphub.pattern.strategy.RevenueStrategy;
import com.startuphub.pattern.strategy.RevenueStrategyResolver;
import com.startuphub.repository.ExpenseRepository;
import com.startuphub.repository.RevenueRepository;
import com.startuphub.service.FinanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class FinanceServiceImpl implements FinanceService {
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final Map<String, RevenueStrategy> strategies;
    private final RevenueStrategyResolver strategyResolver;

    public FinanceServiceImpl(RevenueRepository revenueRepository, ExpenseRepository expenseRepository,
                               Map<String, RevenueStrategy> strategies, RevenueStrategyResolver strategyResolver) {
        this.revenueRepository = revenueRepository;
        this.expenseRepository = expenseRepository;
        this.strategies = strategies;
        this.strategyResolver = strategyResolver;
    }

    @Override
    public Revenue addRevenue(double amount, String source, Startup startup) {
        return revenueRepository.save(new Revenue(amount, source, startup));
    }

    @Override
    public Expense addExpense(double amount, String category, Startup startup) {
        return expenseRepository.save(new Expense(amount, category, startup));
    }

    @Override
    public List<Revenue> findRevenues(Long startupId) { return revenueRepository.findByStartupId(startupId); }

    @Override
    public List<Expense> findExpenses(Long startupId) { return expenseRepository.findByStartupId(startupId); }

    @Override
    public double totalRevenue(Long startupId) {
        return revenueRepository.findByStartupId(startupId).stream().mapToDouble(Revenue::getAmount).sum();
    }

    @Override
    public double totalExpense(Long startupId) {
        return expenseRepository.findByStartupId(startupId).stream().mapToDouble(Expense::getAmount).sum();
    }

    @Override
    public double burnRate(Long startupId) { return totalExpense(startupId) - totalRevenue(startupId); }

    @Override
    public double projectRevenue(String strategyName, double baseAmount, long customers) {
        RevenueStrategy strategy = strategies.get(strategyName);
        if (strategy == null) throw new IllegalArgumentException("Unknown revenue strategy: " + strategyName);
        return strategy.projectMonthlyRevenue(baseAmount, customers);
    }

    // ------------------------------------------------------------------ edit / delete

    @Override
    public Revenue findRevenue(Long revenueId) {
        return revenueRepository.findById(revenueId)
                .orElseThrow(() -> new ResourceNotFoundException("Revenue record not found: " + revenueId));
    }

    @Override
    public Expense findExpense(Long expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense record not found: " + expenseId));
    }

    @Override
    public Revenue updateRevenue(Long revenueId, double amount, String source, LocalDate recordedOn) {
        Revenue r = findRevenue(revenueId);
        boolean amountChanged = Math.abs(r.getAmount() - amount) > 0.004;
        r.setAmount(amount);
        r.setSource(source);
        if (recordedOn != null) r.setRecordedOn(recordedOn);
        // A calculator record whose amount is overridden by hand no longer matches its formula;
        // say so instead of leaving a misleading explanation behind.
        if (amountChanged && r.getRevenueModel() != null && r.getCalculationDetails() != null
                && !r.getCalculationDetails().endsWith("(amount manually adjusted)")) {
            r.setCalculationDetails(r.getCalculationDetails() + " (amount manually adjusted)");
        }
        return revenueRepository.save(r);
    }

    @Override
    public Expense updateExpense(Long expenseId, double amount, String category, LocalDate recordedOn) {
        Expense e = findExpense(expenseId);
        e.setAmount(amount);
        e.setCategory(category);
        if (recordedOn != null) e.setRecordedOn(recordedOn);
        return expenseRepository.save(e);
    }

    @Override
    public void deleteRevenue(Long revenueId) { revenueRepository.delete(findRevenue(revenueId)); }

    @Override
    public void deleteExpense(Long expenseId) { expenseRepository.delete(findExpense(expenseId)); }

    // ------------------------------------------------------------------ automatic revenue calculator

    /** Strategy pattern: the resolver picks the strategy for the chosen model, the strategy does the maths. */
    @Override
    public RevenueCalculationResponse calculateRevenue(RevenueCalculationRequest request) {
        return strategyResolver.resolve(request.model()).calculate(request);
    }

    /**
     * The client never sends an amount, only the inputs. The amount that gets stored is always
     * the one the server just calculated, so a tampered request cannot save a made-up figure.
     * The result is stored as a normal Revenue row, which is why every existing total, chart,
     * dashboard number and health score picks it up with no extra wiring.
     */
    @Override
    @Transactional
    public Revenue saveCalculatedRevenue(Startup startup, RevenueCalculationRequest request) {
        RevenueCalculationResponse result = calculateRevenue(request);

        String source = request.source() == null || request.source().isBlank()
                ? result.modelLabel() + " revenue"
                : request.source().trim();

        Revenue revenue = new Revenue(result.amount().doubleValue(), source, startup);
        revenue.setRevenueModel(result.model());
        revenue.setCalculationDetails(result.formula());
        return revenueRepository.save(revenue);
    }
}
