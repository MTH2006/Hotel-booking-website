package com.hotelbooking.service;

import com.hotelbooking.dto.AuthRequest;
import com.hotelbooking.dto.AuthResponse;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.security.AuthTokenService;
import com.hotelbooking.security.PasswordService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenService tokens;
    private final PasswordService passwords;

    public AuthService(
            UserRepository userRepository,
            AuthTokenService tokens,
            PasswordService passwords) {

        this.userRepository = userRepository;
        this.tokens = tokens;
        this.passwords = passwords;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getPassword() == null
                || request.getPassword().isBlank()) {
            return denied();
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<User> found =
                userRepository.findByEmail(email);

        if (found.isEmpty()) {
            return denied();
        }

        User user = found.get();

        // FR-82: Chan tai khoan da bi khoa dang nhap
        if (!user.isActive()) {
            return denied();
        }

        if (!passwords.matches(
                request.getPassword(),
                user.getPasswordHash())) {
            return denied();
        }

        if (passwords.needsUpgrade(user.getPasswordHash())) {
            user.setPasswordHash(
                passwords.hash(request.getPassword())
            );
            userRepository.save(user);
        }

        return granted(user);
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {

        if (request == null
                || request.getEmail() == null
                || request.getPassword() == null
                || request.getPassword().length() < 6) {
            return denied();
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
                || userRepository.existsByEmail(email)) {
            return denied();
        }

        String name = request.getName();

        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }

        String uuid = "usr_"
                + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16);

        User user = new User(
            uuid,
            name.trim(),
            email,
            passwords.hash(request.getPassword()),
            null,
            "USER"
        );

        userRepository.save(user);

        return granted(user);
    }

    // FR-6: Lấy thông tin cá nhân hiện tại
    public Optional<User> getCurrentUser(String authorization) {
        return tokens.authenticate(authorization);
    }

    // FR-7: Cập nhật thông tin tài khoản cá nhân
    @Transactional
    public boolean updateProfile(User currentUser, String newName, String newPhone) {
        if (newName != null && !newName.isBlank()) {
            currentUser.setName(newName.trim());
        }
        if (newPhone != null && !newPhone.isBlank()) {
            currentUser.setPhone(newPhone.trim());
        }
        userRepository.save(currentUser);
        return true;
    }

    // FR-8: Đổi mật khẩu
    @Transactional
    public boolean changePassword(User currentUser, String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return false;
        }
        if (!passwords.matches(oldPassword, currentUser.getPasswordHash())) {
            return false;
        }
        currentUser.setPasswordHash(passwords.hash(newPassword));
        userRepository.save(currentUser);
        return true;
    }

    // FR-9: Khôi phục mật khẩu (reset password)
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        if (email == null || newPassword == null || newPassword.length() < 6) return false;
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        user.setPasswordHash(passwords.hash(newPassword));
        userRepository.save(user);
        return true;
    }

    private AuthResponse granted(User user) {

        AuthResponse.UserDto dto =
            new AuthResponse.UserDto(
                user.getUserUuid(),
                user.getName(),
                user.getEmail(),
                user.getRole()
            );

        return new AuthResponse(
            true,
            tokens.issue(user),
            dto
        );
    }

    private AuthResponse denied() {
        return new AuthResponse(false, null, null);
    }
}
