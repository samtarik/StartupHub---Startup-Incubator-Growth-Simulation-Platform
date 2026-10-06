package com.startuphub.service;

import com.startuphub.model.Role;
import com.startuphub.model.User;
import java.util.List;

public interface UserService {
    User register(String name, String email, String rawPassword, Role role);
    List<User> findAll();
    User findById(Long id);
    User updateProfile(Long userId, String name);
    void changePassword(Long userId, String currentPassword, String newPassword);
}
