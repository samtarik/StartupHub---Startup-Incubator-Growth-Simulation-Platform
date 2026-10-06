package com.startuphub.service.impl;

import com.startuphub.dto.ActivityDto;
import com.startuphub.model.*;
import com.startuphub.repository.*;
import com.startuphub.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Every number on the dashboard comes from here, and every number comes from a real row.
 *
 * "Scope" is the set of startups the logged-in user is allowed to see totals for:
 *   FOUNDER  -> the startups they founded
 *   INVESTOR -> the startups they have actually invested in
 *   MENTOR   -> every startup on the platform (they coach across the portfolio)
 *   ADMIN    -> every startup on the platform
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM");

    private final StartupRepository startupRepository;
    private final InvestmentRepository investmentRepository;
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationRepository notificationRepository;
    private final MvpRepository mvpRepository;

    public DashboardServiceImpl(StartupRepository startupRepository, InvestmentRepository investmentRepository,
                                RevenueRepository revenueRepository, ExpenseRepository expenseRepository,
                                EmployeeRepository employeeRepository, NotificationRepository notificationRepository,
                                MvpRepository mvpRepository) {
        this.startupRepository = startupRepository;
        this.investmentRepository = investmentRepository;
        this.revenueRepository = revenueRepository;
        this.expenseRepository = expenseRepository;
        this.employeeRepository = employeeRepository;
        this.notificationRepository = notificationRepository;
        this.mvpRepository = mvpRepository;
    }

    private List<Startup> scopeFor(User user) {
        if (user.getRole() == Role.FOUNDER) {
            return startupRepository.findByFounderId(user.getId());
        }
        if (user.getRole() == Role.INVESTOR) {
            Set<Long> ids = investmentRepository.findByInvestorId(user.getId()).stream()
                    .map(i -> i.getStartup().getId()).collect(Collectors.toSet());
            return startupRepository.findAllById(ids);
        }
        return startupRepository.findAll();
    }

    @Override
    public Map<String, Object> summaryFor(User user) {
        List<Startup> scope = scopeFor(user);
        List<Long> ids = scope.stream().map(Startup::getId).toList();

        List<Revenue> revenues = new ArrayList<>();
        List<Expense> expenses = new ArrayList<>();
        List<Investment> investments = new ArrayList<>();
        int teamMembers = 0;
        double payroll = 0;
        double performanceSum = 0;

        for (Long id : ids) {
            revenues.addAll(revenueRepository.findByStartupId(id));
            expenses.addAll(expenseRepository.findByStartupId(id));
            investments.addAll(investmentRepository.findByStartupId(id));
            List<Employee> staff = employeeRepository.findByStartupId(id);
            teamMembers += staff.size();
            payroll += staff.stream().mapToDouble(Employee::getSalary).sum();
            performanceSum += staff.stream().mapToInt(Employee::getPerformanceScore).sum();
        }

        double totalValuation = scope.stream().mapToDouble(Startup::getValuation).sum();
        double totalRevenue = revenues.stream().mapToDouble(Revenue::getAmount).sum();
        double totalExpense = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalRaised = investments.stream().mapToDouble(Investment::getAmount).sum();

        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);

        double revThis = sumInMonth(revenues, Revenue::getRecordedOn, Revenue::getAmount, thisMonth);
        double revLast = sumInMonth(revenues, Revenue::getRecordedOn, Revenue::getAmount, lastMonth);
        double expThis = sumInMonth(expenses, Expense::getRecordedOn, Expense::getAmount, thisMonth);
        double expLast = sumInMonth(expenses, Expense::getRecordedOn, Expense::getAmount, lastMonth);
        double invThisMonth = sumInMonth(investments, Investment::getInvestedOn, Investment::getAmount, thisMonth);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", user.getRole());
        out.put("userName", user.getName());
        out.put("startupCount", scope.size());
        out.put("totalValuation", totalValuation);
        out.put("totalRevenue", totalRevenue);
        out.put("totalExpense", totalExpense);
        out.put("netPosition", totalRevenue - totalExpense);
        out.put("burnRate", totalExpense - totalRevenue);
        out.put("teamMembers", teamMembers);
        out.put("payroll", payroll);
        out.put("avgPerformance", teamMembers == 0 ? 0 : Math.round(performanceSum / teamMembers));
        out.put("totalRaised", totalRaised);
        out.put("investorCount", investments.stream().map(i -> i.getInvestor().getId()).distinct().count());

        // Month-over-month deltas. null means "not enough history to compare" -
        // the frontend then shows no arrow instead of a made-up percentage.
        out.put("revenueChange", percentChange(revLast, revThis));
        out.put("expenseChange", percentChange(expLast, expThis));
        out.put("valuationChange", percentChange(totalValuation - invThisMonth, totalValuation));
        out.put("newFundingThisMonth", invThisMonth);

        // Investor-specific: what this user personally put in.
        if (user.getRole() == Role.INVESTOR) {
            List<Investment> mine = investmentRepository.findByInvestorId(user.getId());
            out.put("myInvested", mine.stream().mapToDouble(Investment::getAmount).sum());
            out.put("myInvestmentCount", mine.size());
        }

        out.put("trend", monthlyTrend(revenues, expenses, 6));
        out.put("stageBreakdown", stageBreakdown(scope));
        return out;
    }

    /** Last n months of revenue vs expense, ready for the Chart.js line chart. */
    private List<Map<String, Object>> monthlyTrend(List<Revenue> revenues, List<Expense> expenses, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            YearMonth ym = cursor.plusMonths(i);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", ym.atDay(1).format(MONTH_LABEL));
            point.put("revenue", sumInMonth(revenues, Revenue::getRecordedOn, Revenue::getAmount, ym));
            point.put("expense", sumInMonth(expenses, Expense::getRecordedOn, Expense::getAmount, ym));
            trend.add(point);
        }
        return trend;
    }

    private Map<String, Long> stageBreakdown(List<Startup> scope) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (StartupStage stage : StartupStage.values()) {
            counts.put(stage.name(), scope.stream().filter(s -> s.getStage() == stage).count());
        }
        return counts;
    }

    private <T> double sumInMonth(List<T> rows, java.util.function.Function<T, LocalDate> dateOf,
                                  java.util.function.ToDoubleFunction<T> amountOf, YearMonth month) {
        return rows.stream()
                .filter(r -> dateOf.apply(r) != null && YearMonth.from(dateOf.apply(r)).equals(month))
                .mapToDouble(amountOf).sum();
    }

    /** null when there is no baseline to compare against - never a fabricated 0%. */
    private Double percentChange(double previous, double current) {
        if (previous <= 0) return null;
        return Math.round(((current - previous) / previous) * 1000.0) / 10.0;
    }

    @Override
    public List<ActivityDto> activityFor(User user, int limit) {
        List<Startup> scope = scopeFor(user);
        Map<Long, String> names = scope.stream()
                .collect(Collectors.toMap(Startup::getId, Startup::getName, (a, b) -> a, LinkedHashMap::new));
        List<ActivityDto> items = new ArrayList<>();

        for (Startup s : scope) {
            for (Investment i : investmentRepository.findByStartupId(s.getId())) {
                items.add(new ActivityDto("investment",
                        i.getInvestor().getName() + " invested " + money(i.getAmount()) + " in " + s.getName(),
                        i.getInvestedOn().atStartOfDay(), s.getId(), s.getName()));
            }
            for (Revenue r : revenueRepository.findByStartupId(s.getId())) {
                items.add(new ActivityDto("revenue",
                        "Revenue of " + money(r.getAmount()) + " recorded from " + r.getSource(),
                        r.getRecordedOn().atStartOfDay(), s.getId(), s.getName()));
            }
            for (Expense e : expenseRepository.findByStartupId(s.getId())) {
                items.add(new ActivityDto("expense",
                        "Expense of " + money(e.getAmount()) + " logged for " + e.getCategory(),
                        e.getRecordedOn().atStartOfDay(), s.getId(), s.getName()));
            }
            mvpRepository.findByStartupId(s.getId()).ifPresent(mvp -> addMvpActivity(items, s, mvp));
            items.add(new ActivityDto("startup", s.getName() + " was created", s.getCreatedAt(), s.getId(), s.getName()));
        }

        // Notifications carry a real timestamp, so milestone/funding events show up
        // with the minute they happened rather than just a date.
        for (Notification n : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            items.add(new ActivityDto("notification", n.getMessage(), n.getCreatedAt(), null, null));
        }

        items.sort(Comparator.comparing(ActivityDto::at,
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));
        return items.size() > limit ? items.subList(0, limit) : items;
    }

    /** MVP created / feature completed / launched, all taken from real timestamps on the MVP rows. */
    private void addMvpActivity(List<ActivityDto> items, Startup s, Mvp mvp) {
        items.add(new ActivityDto("mvp", "MVP " + mvp.getVersion() + " \"" + mvp.getName() + "\" was created",
                mvp.getCreatedAt(), s.getId(), s.getName()));
        for (MvpFeature f : mvp.getFeatures()) {
            if (f.isCompleted() && f.getCompletedAt() != null) {
                items.add(new ActivityDto("mvp", "MVP feature completed: " + f.getTitle(),
                        f.getCompletedAt(), s.getId(), s.getName()));
            }
        }
        if (mvp.getStatus() == MvpStatus.LAUNCHED) {
            items.add(new ActivityDto("mvp", "MVP " + mvp.getVersion() + " launched",
                    mvp.getUpdatedAt(), s.getId(), s.getName()));
        } else if (mvp.getUpdatedAt() != null && mvp.getCreatedAt() != null
                && mvp.getUpdatedAt().isAfter(mvp.getCreatedAt().plusMinutes(1))) {
            items.add(new ActivityDto("mvp", "MVP updated (" + mvp.getStatus().name().toLowerCase().replace('_', ' ') + ")",
                    mvp.getUpdatedAt(), s.getId(), s.getName()));
        }
    }

    private String money(double amount) {
        return "$" + String.format("%,.0f", amount);
    }
}
