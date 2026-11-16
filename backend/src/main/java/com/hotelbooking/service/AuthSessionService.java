package com.hotelbooking.service;

import com.hotelbooking.model.User;
import com.hotelbooking.security.AuthTokenService;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthSessionService {
    private final AuthTokenService tokens;

    public AuthSessionService(AuthTokenService tokens) { this.tokens = tokens; }

    public boolean isAdmin(String authorization) {
        return currentUser(authorization).map(user -> "ADMIN".equalsIgnoreCase(user.getRole())).orElse(false);
    }

    public boolean isAuthenticated(String authorization) {
        return currentUser(authorization).isPresent();
    }

    public String userId(String authorization) {
        return currentUser(authorization).map(User::getUserUuid).orElse(null);
    }

    private Optional<User> currentUser(String authorization) {
        return tokens.authenticate(authorization);
    }
}
