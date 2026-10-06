package com.startuphub.dto;

import com.startuphub.model.Investment;
import com.startuphub.model.InvestmentType;
import java.time.LocalDate;

public record InvestmentDto(Long id, String investorName, InvestmentType type, double amount,
                            LocalDate investedOn, Long startupId, String startupName, Long investorId) {
    public static InvestmentDto from(Investment i) {
        return new InvestmentDto(i.getId(), i.getInvestor().getName(), i.getType(), i.getAmount(),
                i.getInvestedOn(), i.getStartup().getId(), i.getStartup().getName(), i.getInvestor().getId());
    }
}
