package com.startuphub.controller;

import com.startuphub.dto.InvestmentUpdateRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.dto.InvestmentDto;
import com.startuphub.dto.InvestmentRequest;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.InvestmentService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class InvestmentController {
    private final InvestmentService investmentService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public InvestmentController(InvestmentService investmentService, StartupService startupService,
                                AccessGuard accessGuard) {
        this.investmentService = investmentService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/startups/{startupId}/investments")
    public Map<String, Object> list(@PathVariable Long startupId) {
        List<InvestmentDto> investments = investmentService.findByStartup(startupId).stream()
                .map(InvestmentDto::from).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("investments", investments);
        out.put("totalRaised", investmentService.totalRaised(startupId));
        out.put("investorCount", investments.stream().map(InvestmentDto::investorName).distinct().count());
        return out;
    }

    /**
     * Any signed-in user may back a startup - that is the point of the platform,
     * so there is deliberately no ownership guard here.
     */
    @PostMapping("/api/startups/{startupId}/investments")
    public InvestmentDto invest(@PathVariable Long startupId, @Valid @RequestBody InvestmentRequest req,
                                 @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        return InvestmentDto.from(investmentService.invest(startup, principal.getUser(), req.type(), req.amount()));
    }

    /** Investor portfolio view: everything this user has personally put money into. */
    @GetMapping("/api/investments/mine")
    public Map<String, Object> mine(@AuthenticationPrincipal CustomUserDetails principal) {
        List<InvestmentDto> mine = investmentService.findByInvestor(principal.getUser().getId()).stream()
                .map(InvestmentDto::from).toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("investments", mine);
        out.put("totalInvested", mine.stream().mapToDouble(InvestmentDto::amount).sum());
        out.put("startupCount", mine.stream().map(InvestmentDto::startupId).distinct().count());
        return out;
    }

    /** Founder, admin, or the investor who made the investment may edit it. */
    @PutMapping("/api/investments/{investmentId}")
    public InvestmentDto update(@PathVariable Long investmentId, @Valid @RequestBody InvestmentUpdateRequest req,
                                @AuthenticationPrincipal CustomUserDetails principal) {
        var investment = investmentService.findById(investmentId);
        accessGuard.requireOwnerOrInvestor(principal.getUser(), investment.getStartup(), investment.getInvestor());
        return InvestmentDto.from(investmentService.update(investmentId, req.type(), req.amount(), req.investedOn()));
    }

    @DeleteMapping("/api/investments/{investmentId}")
    public void delete(@PathVariable Long investmentId, @AuthenticationPrincipal CustomUserDetails principal) {
        var investment = investmentService.findById(investmentId);
        accessGuard.requireOwnerOrInvestor(principal.getUser(), investment.getStartup(), investment.getInvestor());
        investmentService.delete(investmentId);
    }
}
