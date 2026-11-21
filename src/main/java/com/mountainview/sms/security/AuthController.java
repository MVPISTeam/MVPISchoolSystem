package com.mountainview.sms.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mountainview.sms.common.NotFoundException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record CurrentUser(String username, String fullName, Role role) {
    }

    private final AppUserRepository users;

    public AuthController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        AppUser user = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new CurrentUser(user.getUsername(), user.getFullName(), user.getRole());
    }
}
