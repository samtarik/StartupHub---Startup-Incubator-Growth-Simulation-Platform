package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Startup;
import com.startuphub.model.User;
import com.startuphub.repository.*;
import com.startuphub.service.StartupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class StartupServiceImpl implements StartupService {
    private final StartupRepository startupRepository;
    private final RevenueRepository revenueRepository;
    private final ExpenseRepository expenseRepository;
    private final InvestmentRepository investmentRepository;
    private final CampaignRepository campaignRepository;
    private final CompetitorRepository competitorRepository;
    private final ProductRepository productRepository;
    private final RoadmapRepository roadmapRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final MvpRepository mvpRepository;

    public StartupServiceImpl(StartupRepository startupRepository, RevenueRepository revenueRepository,
                              ExpenseRepository expenseRepository, InvestmentRepository investmentRepository,
                              CampaignRepository campaignRepository, CompetitorRepository competitorRepository,
                              ProductRepository productRepository, RoadmapRepository roadmapRepository,
                              EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                              MvpRepository mvpRepository) {
        this.startupRepository = startupRepository;
        this.revenueRepository = revenueRepository;
        this.expenseRepository = expenseRepository;
        this.investmentRepository = investmentRepository;
        this.campaignRepository = campaignRepository;
        this.competitorRepository = competitorRepository;
        this.productRepository = productRepository;
        this.roadmapRepository = roadmapRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.mvpRepository = mvpRepository;
    }

    @Override
    public Startup create(String name, String description, User founder) {
        return startupRepository.save(new Startup(name, description, founder));
    }

    @Override
    public List<Startup> findAll() { return startupRepository.findAll(); }

    @Override
    public List<Startup> findByFounder(Long founderId) { return startupRepository.findByFounderId(founderId); }

    @Override
    public Startup findById(Long id) {
        return startupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Startup not found: " + id));
    }

    @Override
    public Startup advanceStage(Long id) {
        Startup s = findById(id);
        s.advanceStage();
        return startupRepository.save(s);
    }

    @Override
    public Startup update(Long id, String name, String description, Double valuation) {
        Startup s = findById(id);
        s.setName(name.trim());
        s.setDescription(description == null || description.isBlank() ? null : description.trim());
        if (valuation != null) s.setValuation(valuation);
        return startupRepository.save(s);
    }

    /**
     * Children hold non-null foreign keys to the startup, so they have to go first -
     * deleting the startup row on its own fails with a constraint violation in MySQL.
     * Roadmaps cascade to their milestones (Roadmap.milestones is cascade = ALL).
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Startup startup = findById(id);

        mvpRepository.findByStartupId(id).ifPresent(mvpRepository::delete); // cascades to its features
        revenueRepository.deleteAll(revenueRepository.findByStartupId(id));
        expenseRepository.deleteAll(expenseRepository.findByStartupId(id));
        investmentRepository.deleteAll(investmentRepository.findByStartupId(id));
        campaignRepository.deleteAll(campaignRepository.findByStartupId(id));
        competitorRepository.deleteAll(competitorRepository.findByStartupId(id));
        productRepository.deleteAll(productRepository.findByStartupId(id));
        roadmapRepository.deleteAll(roadmapRepository.findByStartupId(id));
        employeeRepository.deleteAll(employeeRepository.findByStartupId(id));
        departmentRepository.deleteAll(departmentRepository.findByStartupId(id));

        startupRepository.delete(startup);
    }
}
