package com.hotelbooking.controller;

import com.hotelbooking.dto.AuthRequest;
import com.hotelbooking.dto.AuthResponse;
import com.hotelbooking.model.User;
import com.hotelbooking.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // FR-3 & FR-4: Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = authService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        return ResponseEntity.ok(response);
    }

    // FR-1 & FR-2: Đăng ký tài khoản mới
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        AuthResponse response = authService.register(request);
        if (!response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // FR-5: Đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng xuất thành công."));
    }

    // FR-6: Xem thông tin tài khoản cá nhân
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> user = authService.getCurrentUser(authorization);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập."));
        }
        User u = user.get();
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "userUuid", u.getUserUuid(),
                "name", u.getName(),
                "email", u.getEmail(),
                "phone", u.getPhone() != null ? u.getPhone() : "",
                "role", u.getRole(),
                "isActive", u.isActive()
        ));
    }

    // FR-7: Cập nhật thông tin tài khoản cá nhân
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> userOpt = authService.getCurrentUser(authorization);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập."));
        }

        String name = body.get("name");
        String phone = body.get("phone");
        boolean ok = authService.updateProfile(userOpt.get(), name, phone);
        return ok ? ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật hồ sơ thành công!"))
                  : ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cập nhật thất bại."));
    }

    // FR-8: Đổi mật khẩu
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<User> userOpt = authService.getCurrentUser(authorization);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Chưa đăng nhập."));
        }

        String oldPass = body.get("oldPassword");
        String newPass = body.get("newPassword");
        boolean ok = authService.changePassword(userOpt.get(), oldPass, newPass);
        return ok ? ResponseEntity.ok(Map.of("success", true, "message", "Đổi mật khẩu thành công!"))
                  : ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mật khẩu cũ không chính xác hoặc mật khẩu mới chưa đủ độ dài (tối thiểu 6 ký tự)."));
    }

    // FR-9: Quên mật khẩu
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPass = body.get("newPassword");
        if (newPass == null || newPass.isBlank()) {
            newPass = "Password@" + (1000 + (int)(Math.random() * 9000));
        }
        boolean ok = authService.resetPassword(email, newPass);
        return ok ? ResponseEntity.ok(Map.of("success", true, "message", "Mật khẩu tạm thời đã được đặt lại thành công: " + newPass))
                  : ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email không tồn tại trong hệ thống."));
    }
}
