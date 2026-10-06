package com.startuphub.dto;

import com.startuphub.model.Employee;

public record EmployeeDto(Long id, String name, String jobTitle, double salary, int performanceScore) {
    public static EmployeeDto from(Employee e) {
        return new EmployeeDto(e.getId(), e.getName(), e.getJobTitle(), e.getSalary(), e.getPerformanceScore());
    }
}
