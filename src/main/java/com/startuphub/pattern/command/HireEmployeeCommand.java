package com.startuphub.pattern.command;

import com.startuphub.model.Department;
import com.startuphub.model.Employee;
import com.startuphub.model.Startup;
import com.startuphub.repository.EmployeeRepository;

public class HireEmployeeCommand implements Command {
    private final EmployeeRepository employeeRepository;
    private final String name;
    private final String jobTitle;
    private final double salary;
    private final Startup startup;
    private final Department department;
    private Employee result;

    public HireEmployeeCommand(EmployeeRepository employeeRepository, String name, String jobTitle,
                                double salary, Startup startup, Department department) {
        this.employeeRepository = employeeRepository;
        this.name = name;
        this.jobTitle = jobTitle;
        this.salary = salary;
        this.startup = startup;
        this.department = department;
    }

    @Override
    public void execute() {
        result = employeeRepository.save(new Employee(name, jobTitle, salary, startup, department));
    }

    public Employee getResult() { return result; }

    @Override
    public String describe() {
        return "Hire " + name + " as " + jobTitle + " at " + startup.getName();
    }
}
