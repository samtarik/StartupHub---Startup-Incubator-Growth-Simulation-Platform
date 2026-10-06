package com.startuphub.dto;

import com.startuphub.model.MvpStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** status omitted on create means PLANNING. */
public record MvpRequest(@NotBlank(message = "MVP name is required") String name,
                         String description,
                         @NotBlank(message = "Version is required") String version,
                         MvpStatus status,
                         String targetUsers,
                         LocalDate launchDate) {}
