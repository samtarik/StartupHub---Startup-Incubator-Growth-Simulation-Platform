package com.startuphub.pattern.command;

import com.startuphub.model.Employee;
import com.startuphub.repository.EmployeeRepository;

/** Command pattern: editing an employee is an action, so it runs through CommandInvoker like hiring does. */
public class UpdateEmployeeCommand implements Command {
    private final EmployeeRepository employeeRepository;
    private final Employee employee;
    private final String name;
    private final String jobTitle;
    private final double salary;
    private final Integer performanceScore;
    private Employee result;

    public UpdateEmployeeCommand(EmployeeRepository employeeRepository, Employee employee, String name,
                                 String jobTitle, double salary, Integer performanceScore) {
        this.employeeRepository = employeeRepository;
        this.employee = employee;
        this.name = name;
        this.jobTitle = jobTitle;
        this.salary = salary;
        this.performanceScore = performanceScore;
    }

    @Override
    public void execute() {
        employee.setName(name);
        employee.setJobTitle(jobTitle);
        employee.setSalary(salary);
        if (performanceScore != null) employee.setPerformanceScore(performanceScore);
        result = employeeRepository.save(employee);
    }

    public Employee getResult() { return result; }

    @Override
    public String describe() {
        return "Update employee #" + employee.getId() + " (" + name + ", " + jobTitle + ")";
    }
}
