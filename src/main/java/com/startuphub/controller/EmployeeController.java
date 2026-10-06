package com.startuphub.controller;

import com.startuphub.dto.EmployeeUpdateRequest;
import com.startuphub.dto.EmployeeDto;
import com.startuphub.dto.EmployeeRequest;
import com.startuphub.security.AccessGuard;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.EmployeeService;
import com.startuphub.service.StartupService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class EmployeeController {
    private final EmployeeService employeeService;
    private final StartupService startupService;
    private final AccessGuard accessGuard;

    public EmployeeController(EmployeeService employeeService, StartupService startupService, AccessGuard accessGuard) {
        this.employeeService = employeeService;
        this.startupService = startupService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/startups/{startupId}/employees")
    public List<EmployeeDto> list(@PathVariable Long startupId) {
        return employeeService.findByStartup(startupId).stream().map(EmployeeDto::from).toList();
    }

    @PostMapping("/api/startups/{startupId}/employees")
    public EmployeeDto hire(@PathVariable Long startupId, @Valid @RequestBody EmployeeRequest req,
                            @AuthenticationPrincipal CustomUserDetails principal) {
        var startup = startupService.findById(startupId);
        accessGuard.requireOwner(principal.getUser(), startup);
        return EmployeeDto.from(employeeService.hire(req.name(), req.jobTitle(), req.salary(), startup, null));
    }

    @PostMapping("/api/employees/{employeeId}/promote")
    public EmployeeDto promote(@PathVariable Long employeeId, @AuthenticationPrincipal CustomUserDetails principal) {
        var employee = employeeService.findById(employeeId);
        accessGuard.requireOwner(principal.getUser(), employee.getStartup());
        employeeService.promote(employeeId, 10);
        return EmployeeDto.from(employeeService.findById(employeeId));
    }

    @DeleteMapping("/api/employees/{employeeId}")
    public void fire(@PathVariable Long employeeId, @AuthenticationPrincipal CustomUserDetails principal) {
        var employee = employeeService.findById(employeeId);
        accessGuard.requireOwner(principal.getUser(), employee.getStartup());
        employeeService.fire(employeeId);
    }

    @PutMapping("/api/employees/{employeeId}")
    public EmployeeDto update(@PathVariable Long employeeId, @Valid @RequestBody EmployeeUpdateRequest req,
                              @AuthenticationPrincipal CustomUserDetails principal) {
        var employee = employeeService.findById(employeeId);
        accessGuard.requireOwner(principal.getUser(), employee.getStartup());
        return EmployeeDto.from(employeeService.update(employeeId, req.name(), req.jobTitle(),
                req.salary(), req.performanceScore()));
    }
}
