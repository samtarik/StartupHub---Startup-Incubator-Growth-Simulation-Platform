package com.startuphub.controller;

import com.startuphub.dto.StartupDto;
import com.startuphub.dto.UserDto;
import com.startuphub.model.Investment;
import com.startuphub.model.Role;
import com.startuphub.model.Startup;
import com.startuphub.model.StartupStage;
import com.startuphub.model.User;
import com.startuphub.repository.InvestmentRepository;
import com.startuphub.service.StartupService;
import com.startuphub.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only. The whole /api/admin/** path is locked to ROLE_ADMIN in SecurityConfig,
 * so this controller does not repeat the check - the filter chain enforces it.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService userService;
    private final StartupService startupService;
    private final InvestmentRepository investmentRepository;

    public AdminController(UserService userService, StartupService startupService,
                           InvestmentRepository investmentRepository) {
        this.userService = userService;
        this.startupService = startupService;
        this.investmentRepository = investmentRepository;
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return userService.findAll().stream().map(UserDto::from).toList();
    }

    @GetMapping("/startups")
    public List<StartupDto> startups() {
        return startupService.findAll().stream().map(StartupDto::from).toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<User> users = userService.findAll();
        List<Startup> startups = startupService.findAll();
        List<Investment> investments = investmentRepository.findAll();

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            byRole.put(role.name(), users.stream().filter(u -> u.getRole() == role).count());
        }
        Map<String, Long> byStage = new LinkedHashMap<>();
        for (StartupStage stage : StartupStage.values()) {
            byStage.put(stage.name(), startups.stream().filter(s -> s.getStage() == stage).count());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("userCount", users.size());
        out.put("startupCount", startups.size());
        out.put("investmentCount", investments.size());
        out.put("totalRaised", investments.stream().mapToDouble(Investment::getAmount).sum());
        out.put("totalValuation", startups.stream().mapToDouble(Startup::getValuation).sum());
        out.put("byRole", byRole);
        out.put("byStage", byStage);
        return out;
    }
}
