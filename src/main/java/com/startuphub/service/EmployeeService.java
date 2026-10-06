package com.startuphub.service;

import com.startuphub.model.Department;
import com.startuphub.model.Employee;
import com.startuphub.model.Startup;
import java.util.List;

public interface EmployeeService {
    Employee hire(String name, String jobTitle, double salary, Startup startup, Department department);
    List<Employee> findByStartup(Long startupId);
    Employee findById(Long employeeId);
    void promote(Long employeeId, int scoreIncrease);
    void fire(Long employeeId);
    Employee update(Long employeeId, String name, String jobTitle, double salary, Integer performanceScore);
}
