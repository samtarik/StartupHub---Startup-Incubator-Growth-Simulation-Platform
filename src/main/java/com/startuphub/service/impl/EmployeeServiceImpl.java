package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Department;
import com.startuphub.model.Employee;
import com.startuphub.model.Startup;
import com.startuphub.pattern.command.CommandInvoker;
import com.startuphub.pattern.command.HireEmployeeCommand;
import com.startuphub.pattern.command.UpdateEmployeeCommand;
import com.startuphub.repository.EmployeeRepository;
import com.startuphub.service.EmployeeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final CommandInvoker commandInvoker;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, CommandInvoker commandInvoker) {
        this.employeeRepository = employeeRepository;
        this.commandInvoker = commandInvoker;
    }

    @Override
    public Employee hire(String name, String jobTitle, double salary, Startup startup, Department department) {
        HireEmployeeCommand cmd = new HireEmployeeCommand(employeeRepository, name, jobTitle, salary, startup, department);
        commandInvoker.run(cmd);
        return cmd.getResult();
    }

    // Command pattern: same route as hiring, so edits appear in the command audit history too.
    @Override
    public Employee update(Long employeeId, String name, String jobTitle, double salary, Integer performanceScore) {
        Employee employee = findById(employeeId);
        UpdateEmployeeCommand cmd = new UpdateEmployeeCommand(employeeRepository, employee,
                name.trim(), jobTitle.trim(), salary, performanceScore);
        commandInvoker.run(cmd);
        return cmd.getResult();
    }

    @Override
    public List<Employee> findByStartup(Long startupId) { return employeeRepository.findByStartupId(startupId); }

    @Override
    public Employee findById(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    @Override
    public void promote(Long employeeId, int scoreIncrease) {
        Employee e = findById(employeeId);
        e.setPerformanceScore(Math.min(100, e.getPerformanceScore() + scoreIncrease));
        employeeRepository.save(e);
    }

    @Override
    public void fire(Long employeeId) { employeeRepository.deleteById(employeeId); }
}
