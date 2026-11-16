
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
                && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    // Kiem tra quyen STAFF
    public boolean isStaff(User user) {
        return user != null
                && user.isActive()
                && "STAFF".equalsIgnoreCase(user.getRole());
    }

    // Kiem tra quyen CUSTOMER (hoac USER tuong thich cu)
    public boolean isCustomer(User user) {
        return user != null
                && user.isActive()
                && ("CUSTOMER".equalsIgnoreCase(user.getRole()) || "USER".equalsIgnoreCase(user.getRole()));
    }

    // Kiem tra quyen STAFF hoac ADMIN
    public boolean isStaffOrAdmin(User user) {
        return user != null
                && user.isActive()
                && ("ADMIN".equalsIgnoreCase(user.getRole())
                    || "STAFF".equalsIgnoreCase(user.getRole())
                    || "MANAGER".equalsIgnoreCase(user.getRole()));
    }

    // Kiem tra quyen MANAGER hoac ADMIN
    public boolean isManagerOrAdmin(User user) {
        return user != null
                && user.isActive()
                && ("MANAGER".equalsIgnoreCase(user.getRole())
                    || "ADMIN".equalsIgnoreCase(user.getRole()));
    }
}
