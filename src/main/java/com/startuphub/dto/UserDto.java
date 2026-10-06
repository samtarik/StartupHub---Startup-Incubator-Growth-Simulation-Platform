package com.startuphub.dto;

import com.startuphub.model.Role;
import com.startuphub.model.User;

public record UserDto(Long id, String name, String email, Role role) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
