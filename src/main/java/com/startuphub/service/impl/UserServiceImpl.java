package com.startuphub.service.impl;

import com.startuphub.exception.ResourceNotFoundException;
import com.startuphub.model.Role;
import com.startuphub.model.User;
import com.startuphub.repository.UserRepository;
import com.startuphub.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String name, String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("That email is already registered.");
        }
        User user = new User(name, email, passwordEncoder.encode(rawPassword), role == null ? Role.FOUNDER : role);
        return userRepository.save(user);
    }

    @Override
    public List<User> findAll() { return userRepository.findAll(); }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public User updateProfile(Long userId, String name) {
        User user = findById(userId);
        user.setName(name);
        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Your current password is not correct.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
