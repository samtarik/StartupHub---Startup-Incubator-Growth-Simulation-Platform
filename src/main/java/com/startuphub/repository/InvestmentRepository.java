package com.startuphub.repository;

import com.startuphub.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    List<Investment> findByStartupId(Long startupId);
    List<Investment> findByInvestorId(Long investorId);
}
