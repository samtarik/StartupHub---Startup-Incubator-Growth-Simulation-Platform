package com.startuphub.controller;

import com.startuphub.dto.ChangePasswordRequest;
import com.startuphub.dto.RegisterRequest;
import com.startuphub.dto.UpdateProfileRequest;
import com.startuphub.dto.UserDto;
import com.startuphub.security.CustomUserDetails;
import com.startuphub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) { this.userService = userService; }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req) {
        var user = userService.register(req.name(), req.email(), req.password(), req.role());
        return ResponseEntity.ok(UserDto.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(UserDto.from(principal.getUser()));
    }

    /** Profile page: rename yourself. Role and email are intentionally not editable. */
    @PutMapping("/me")
    public UserDto updateMe(@Valid @RequestBody UpdateProfileRequest req,
                            @AuthenticationPrincipal CustomUserDetails principal) {
        return UserDto.from(userService.updateProfile(principal.getUser().getId(), req.name()));
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        userService.changePassword(principal.getUser().getId(), req.currentPassword(), req.newPassword());
        return Map.of("message", "Password updated");
    }
    // /api/auth/login and /api/auth/logout are handled by Spring Security (see SecurityConfig).
}
