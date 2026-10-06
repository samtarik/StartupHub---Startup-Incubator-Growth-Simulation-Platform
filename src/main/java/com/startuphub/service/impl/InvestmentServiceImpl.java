package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Investment;
import com.startuphub.model.InvestmentType;
import com.startuphub.model.Startup;
import com.startuphub.model.User;
import com.startuphub.pattern.observer.InvestmentReceivedEvent;
import com.startuphub.repository.InvestmentRepository;
import com.startuphub.repository.StartupRepository;
import com.startuphub.service.InvestmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvestmentServiceImpl implements InvestmentService {
    private final InvestmentRepository investmentRepository;
    private final StartupRepository startupRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InvestmentServiceImpl(InvestmentRepository investmentRepository, StartupRepository startupRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.investmentRepository = investmentRepository;
        this.startupRepository = startupRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Investment invest(Startup startup, User investor, InvestmentType type, double amount) {
        Investment investment = new Investment(startup, investor, type, amount);
        investment = investmentRepository.save(investment);

        startup.setValuation(startup.getValuation() + amount);
        startupRepository.save(startup);

        // Observer pattern: publish and forget. StartupEventListener reacts by creating
        // the notification and pushing it over WebSocket; this service knows about neither.
        eventPublisher.publishEvent(new InvestmentReceivedEvent(this, investment));

        return investment;
    }

    @Override
    public List<Investment> findByStartup(Long startupId) { return investmentRepository.findByStartupId(startupId); }

    @Override
    public List<Investment> findByInvestor(Long investorId) { return investmentRepository.findByInvestorId(investorId); }

    @Override
    public double totalRaised(Long startupId) {
        return investmentRepository.findByStartupId(startupId).stream().mapToDouble(Investment::getAmount).sum();
    }

    @Override
    public Investment findById(Long investmentId) {
        return investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found: " + investmentId));
    }

    /**
     * Valuation grows by the amount of every investment (see invest), so changing the amount
     * moves the valuation by the same difference and the two never drift apart.
     */
    @Override
    @Transactional
    public Investment update(Long investmentId, InvestmentType type, double amount, LocalDate investedOn) {
        Investment inv = findById(investmentId);
        double delta = amount - inv.getAmount();
        inv.setType(type);
        inv.setAmount(amount);
        if (investedOn != null) inv.setInvestedOn(investedOn);
        inv = investmentRepository.save(inv);
        adjustValuation(inv.getStartup(), delta);
        return inv;
    }

    @Override
    @Transactional
    public void delete(Long investmentId) {
        Investment inv = findById(investmentId);
        Startup startup = inv.getStartup();
        double amount = inv.getAmount();
        investmentRepository.delete(inv);
        adjustValuation(startup, -amount);
    }

    private void adjustValuation(Startup startup, double delta) {
        if (delta == 0) return;
        startup.setValuation(Math.max(0, startup.getValuation() + delta));
        startupRepository.save(startup);
    }
}
