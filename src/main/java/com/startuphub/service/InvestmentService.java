package com.startuphub.service;

import java.time.LocalDate;
import com.startuphub.model.Investment;
import com.startuphub.model.InvestmentType;
import com.startuphub.model.Startup;
import com.startuphub.model.User;
import java.util.List;

public interface InvestmentService {
    Investment invest(Startup startup, User investor, InvestmentType type, double amount);
    List<Investment> findByStartup(Long startupId);
    List<Investment> findByInvestor(Long investorId);
    double totalRaised(Long startupId);
    Investment findById(Long investmentId);
    Investment update(Long investmentId, InvestmentType type, double amount, LocalDate investedOn);
    void delete(Long investmentId);
}
