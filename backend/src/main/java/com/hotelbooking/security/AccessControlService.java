
package com.hotelbooking.security;

import com.hotelbooking.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccessControlService {

    private final AuthTokenService tokens;

    public AccessControlService(AuthTokenService tokens) {
        this.tokens = tokens;
    }

    // FR-84: Xac thuc nguoi dung tu JWT
    public Optional<User> authenticate(String authorization) {
        return tokens.authenticate(authorization);
    }

    // Kiem tra quyen ADMIN
    public boolean isAdmin(User user) {
        return user != null
                && user.isActive()
                && "ADMIN".equals(user.getRole());
    }

    // Kiem tra quyen MANAGER hoac ADMIN
    public boolean isManagerOrAdmin(User user) {
        return user != null
                && user.isActive()
                && ("MANAGER".equals(user.getRole())
                    || "ADMIN".equals(user.getRole()));
    }
}
