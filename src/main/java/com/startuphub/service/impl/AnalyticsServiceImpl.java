package com.startuphub.service.impl;

import com.startuphub.dto.HealthDto;
import com.startuphub.dto.HealthDto.HealthFactorDto;
import com.startuphub.model.*;
import com.startuphub.repository.*;
import com.startuphub.service.AnalyticsService;
import com.startuphub.service.StartupService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private final StartupService startupService;
    private final EmployeeRepository employeeRepository;
    private final InvestmentRepository investmentRepository;
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final CompetitorRepository competitorRepository;
    private final CampaignRepository campaignRepository;
    private final RoadmapRepository roadmapRepository;
    private final MilestoneRepository milestoneRepository;
    private final MvpRepository mvpRepository;

    public AnalyticsServiceImpl(StartupService startupService, EmployeeRepository employeeRepository,
                                 InvestmentRepository investmentRepository, RevenueRepository revenueRepository,
                                 ExpenseRepository expenseRepository, CompetitorRepository competitorRepository,
                                 CampaignRepository campaignRepository, RoadmapRepository roadmapRepository,
                                 MilestoneRepository milestoneRepository,
                                 MvpRepository mvpRepository) {
        this.startupService = startupService;
        this.employeeRepository = employeeRepository;
        this.investmentRepository = investmentRepository;
        this.revenueRepository = revenueRepository;
        this.expenseRepository = expenseRepository;
        this.competitorRepository = competitorRepository;
        this.campaignRepository = campaignRepository;
        this.roadmapRepository = roadmapRepository;
        this.milestoneRepository = milestoneRepository;
        this.mvpRepository = mvpRepository;
    }

    @Override
    public Map<String, Object> summarize(Long startupId) {
        Startup startup = startupService.findById(startupId);
        List<Employee> employees = employeeRepository.findByStartupId(startupId);
        List<Campaign> campaigns = campaignRepository.findByStartupId(startupId);
        List<Investment> investments = investmentRepository.findByStartupId(startupId);

        double totalRevenue = revenueRepository.findByStartupId(startupId).stream().mapToDouble(Revenue::getAmount).sum();
        double totalExpense = expenseRepository.findByStartupId(startupId).stream().mapToDouble(Expense::getAmount).sum();
        double totalInvested = investments.stream().mapToDouble(Investment::getAmount).sum();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("startup", startup.getName());
        summary.put("stage", startup.getStage());
        summary.put("valuation", startup.getValuation());
        summary.put("employeeCount", employees.size());
        summary.put("payroll", employees.stream().mapToDouble(Employee::getSalary).sum());
        summary.put("avgPerformance", employees.isEmpty() ? 0
                : Math.round(employees.stream().mapToInt(Employee::getPerformanceScore).average().orElse(0)));
        summary.put("totalInvested", totalInvested);
        summary.put("investorCount", investments.stream().map(i -> i.getInvestor().getId()).distinct().count());
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalExpense", totalExpense);
        summary.put("profit", totalRevenue - totalExpense);
        summary.put("campaignCount", campaigns.size());
        summary.put("competitorCount", competitorRepository.findByStartupId(startupId).size());

        List<Milestone> milestones = milestonesOf(startupId);
        summary.put("milestoneCount", milestones.size());
        summary.put("milestonesDone", milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.DONE).count());
        summary.put("milestonesOverdue", milestones.stream().filter(Milestone::isOverdue).count());

        // MVP snapshot for the Overview tab (null-safe: startups without an MVP report "none").
        Mvp mvp = mvpRepository.findByStartupId(startupId).orElse(null);
        summary.put("mvpExists", mvp != null);
        summary.put("mvpVersion", mvp == null ? null : mvp.getVersion());
        summary.put("mvpStatus", mvp == null ? null : mvp.getStatus());
        summary.put("mvpProgress", mvp == null ? 0 : mvp.progressPercent());
        summary.put("mvpFeatureCount", mvp == null ? 0 : mvp.totalFeatures());
        summary.put("mvpFeaturesDone", mvp == null ? 0 : mvp.completedFeatures());
        return summary;
    }

    /**
     * Health score = six weighted factors, every one of them derived from rows that
     * actually exist in the database. Nothing here is random or hard-coded; a section
     * with no data scores low and says so rather than inventing a number.
     *
     * Finance 25 + Team 15 + Roadmap 15 + Investment 15 + Growth 15 + MVP 15 = 100
     */
    @Override
    public HealthDto health(Long startupId) {
        Startup startup = startupService.findById(startupId);
        List<HealthFactorDto> factors = new ArrayList<>();

        // ---- Finance: does revenue cover the expenses? ----------------------------
        double revenue = revenueRepository.findByStartupId(startupId).stream().mapToDouble(Revenue::getAmount).sum();
        double expense = expenseRepository.findByStartupId(startupId).stream().mapToDouble(Expense::getAmount).sum();
        double coverage;
        String financeDetail;
        if (revenue == 0 && expense == 0) {
            coverage = 0.3;
            financeDetail = "No finance records yet";
        } else if (expense == 0) {
            coverage = 1.0;
            financeDetail = "Revenue recorded, no expenses logged";
        } else {
            coverage = Math.min(1.0, revenue / expense);
            financeDetail = Math.round(coverage * 100) + "% of expenses covered by revenue";
        }
        int financeScore = (int) Math.round(25 * coverage);
        factors.add(new HealthFactorDto("finance", "Finance",
                coverage >= 1 ? "GOOD" : coverage >= 0.6 ? "WARN" : "BAD",
                financeDetail, financeScore, 25));

        // ---- Team: headcount and average performance ------------------------------
        List<Employee> employees = employeeRepository.findByStartupId(startupId);
        int teamScore;
        String teamDetail;
        if (employees.isEmpty()) {
            teamScore = 0;
            teamDetail = "No team members yet";
        } else {
            double avg = employees.stream().mapToInt(Employee::getPerformanceScore).average().orElse(0);
            teamScore = (int) Math.round(Math.min(1.0, employees.size() / 5.0) * 7.5 + (avg / 100.0) * 7.5);
            teamDetail = employees.size() + " member" + (employees.size() == 1 ? "" : "s")
                    + ", avg performance " + Math.round(avg);
        }
        factors.add(new HealthFactorDto("team", "Team",
                teamScore >= 11 ? "GOOD" : teamScore >= 6 ? "WARN" : "BAD", teamDetail, teamScore, 15));

        // ---- Roadmap: completion rate, penalised for overdue milestones -----------
        List<Milestone> milestones = milestonesOf(startupId);
        int roadmapScore;
        String roadmapDetail;
        String roadmapStatus;
        if (milestones.isEmpty()) {
            roadmapScore = 0;
            roadmapDetail = "No milestones planned";
            roadmapStatus = "BAD";
        } else {
            long done = milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.DONE).count();
            long overdue = milestones.stream().filter(Milestone::isOverdue).count();
            double ratio = (double) done / milestones.size();
            roadmapScore = (int) Math.max(0, Math.min(15, Math.round(15 * ratio) - Math.min(8, overdue * 2)));
            roadmapDetail = overdue > 0
                    ? overdue + " overdue, " + done + "/" + milestones.size() + " done"
                    : done + " of " + milestones.size() + " milestones done";
            roadmapStatus = overdue >= 3 ? "BAD" : overdue > 0 ? "WARN" : (ratio >= 0.5 ? "GOOD" : "WARN");
        }
        factors.add(new HealthFactorDto("roadmap", "Roadmap", roadmapStatus, roadmapDetail, roadmapScore, 15));

        // ---- Investment: funding measured against what this stage usually needs ---
        double raised = investmentRepository.findByStartupId(startupId).stream().mapToDouble(Investment::getAmount).sum();
        double expected = expectedFundingFor(startup.getStage());
        double fundingRatio = Math.min(1.0, raised / expected);
        int investmentScore = (int) Math.round(15 * fundingRatio);
        factors.add(new HealthFactorDto("investment", "Investment",
                fundingRatio >= 0.7 ? "GOOD" : fundingRatio >= 0.3 ? "WARN" : "BAD",
                raised == 0 ? "No funding raised yet"
                        : Math.round(fundingRatio * 100) + "% of a typical " + startup.getStage() + " round",
                investmentScore, 15));

        // ---- Growth: stage progress plus campaign conversion ----------------------
        int stageIndex = startup.getStage().ordinal();
        List<Campaign> campaigns = campaignRepository.findByStartupId(startupId);
        double avgConversion = campaigns.stream().filter(c -> c.getReach() > 0)
                .mapToDouble(Campaign::conversionRate).average().orElse(0);
        int growthScore = (int) Math.round((stageIndex / 3.0) * 8 + Math.min(1.0, avgConversion / 10.0) * 7);
        factors.add(new HealthFactorDto("growth", "Growth",
                growthScore >= 10 ? "GOOD" : growthScore >= 5 ? "WARN" : "BAD",
                "Stage " + startup.getStage()
                        + (campaigns.isEmpty() ? ", no campaigns"
                           : ", avg conversion " + String.format("%.1f", avgConversion) + "%"),
                growthScore, 15));

        // ---- MVP: how much of the product is actually built ------------------------
        // 12 points scale with feature completion, up to 3 more for how far the MVP has moved
        // (planning 0, in development 1, testing 2, launched 3). No MVP scores 0 and says so.
        Mvp mvp = mvpRepository.findByStartupId(startupId).orElse(null);
        int mvpScore;
        String mvpDetail;
        String mvpStatus;
        if (mvp == null) {
            mvpScore = 0;
            mvpDetail = "No MVP defined yet";
            mvpStatus = "BAD";
        } else {
            int progress = mvp.progressPercent();
            int statusPoints = switch (mvp.getStatus()) {
                case PLANNING -> 0;
                case IN_DEVELOPMENT -> 1;
                case TESTING -> 2;
                case LAUNCHED -> 3;
            };
            mvpScore = Math.min(15, (int) Math.round(12 * progress / 100.0) + statusPoints);
            mvpDetail = "Version " + mvp.getVersion() + " " + mvp.getStatus().name().toLowerCase().replace('_', ' ')
                    + ", " + progress + "% of features done";
            mvpStatus = mvpScore >= 11 ? "GOOD" : mvpScore >= 5 ? "WARN" : "BAD";
        }
        factors.add(new HealthFactorDto("mvp", "MVP", mvpStatus, mvpDetail, mvpScore, 15));

        int total = factors.stream().mapToInt(HealthFactorDto::score).sum();
        String grade = total >= 80 ? "Strong" : total >= 60 ? "Healthy" : total >= 40 ? "Needs attention" : "At risk";
        return new HealthDto(total, grade, factors);
    }

    private List<Milestone> milestonesOf(Long startupId) {
        List<Milestone> all = new ArrayList<>();
        for (Roadmap r : roadmapRepository.findByStartupId(startupId)) {
            all.addAll(milestoneRepository.findByRoadmapId(r.getId()));
        }
        return all;
    }

    /** Funding benchmark per stage - written down here so the score stays explainable. */
    private double expectedFundingFor(StartupStage stage) {
        return switch (stage) {
            case IDEA -> 5000d;
            case MVP -> 25000d;
            case GROWTH -> 100000d;
            case SCALE -> 500000d;
        };
    }
}
