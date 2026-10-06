package com.startuphub.dto;

import java.time.LocalDateTime;

/**
 * One row of the Recent Activity feed. Built in DashboardServiceImpl from real
 * rows (investments, finance entries, notifications) - never invented.
 */
public record ActivityDto(String type, String message, LocalDateTime at,
                          Long startupId, String startupName) {}
