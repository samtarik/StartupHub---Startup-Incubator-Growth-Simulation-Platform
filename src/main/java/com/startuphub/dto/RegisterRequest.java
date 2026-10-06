package com.startuphub.dto;

import com.startuphub.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank(message = "Name is required") String name,
                              @Email(message = "Enter a valid email address")
                              @NotBlank(message = "Email is required") String email,
                              @NotBlank(message = "Password is required")
                              @Size(min = 4, message = "Password must be at least 4 characters") String password,
                              @NotNull(message = "Please choose a role") Role role) {}
