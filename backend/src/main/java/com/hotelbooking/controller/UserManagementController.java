
package com.hotelbooking.controller;

import com.hotelbooking.dto.UserSummaryResponse;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AuthTokenService;
import com.hotelbooking.service.UserManagementService;
import com.hotelbooking.dto.UpdateUserRequest;
import com.hotelbooking.dto.UpdateUserRoleRequest;
import com.hotelbooking.security.AccessControlService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = {
    "http://localhost:3000",
    "http://localhost:5173"
})
public class UserManagementController {

    private final UserManagementService userService;
    private final AccessControlService accessControl;

    public UserManagementController(
            UserManagementService userService,
            AccessControlService accessControl) {

        this.userService = userService;
        this.accessControl = accessControl;
    }

    // FR-79: ADMIN xem danh sach tai khoan
    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers(
            @RequestHeader(
                value = "Authorization",
                required = false
            ) String authorization) {

        // Kiem tra token dang nhap.
        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Chua dang nhap, token sai hoac het han.
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Chi ADMIN duoc xem danh sach nguoi dung.
        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        // Lay danh sach user, khong tra passwordHash.
        return ResponseEntity.ok(
            userService.getAllUsers()
        );
    }

    // FR-80: Tim kiem nguoi dung theo ten, email hoac so dien thoai
    @GetMapping("/search")
    public ResponseEntity<List<UserSummaryResponse>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // Kiem tra token dang nhap
        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Chua dang nhap hoac token khong hop le
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Chi ADMIN moi duoc tim kiem nguoi dung
        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        // Goi Service de tim kiem
        List<UserSummaryResponse> users =
                userService.searchUsers(keyword);

        return ResponseEntity.ok(users);
    }

    // FR-81: ADMIN cap nhat thong tin nguoi dung
    @PutMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> updateUser(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateUserRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // Kiem tra token dang nhap
        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Chua dang nhap hoac token khong hop le
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Chi ADMIN moi duoc cap nhat nguoi dung
        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Goi Service de cap nhat thong tin
            Optional<UserSummaryResponse> updatedUser =
                    userService.updateUser(id, request);

            // Khong tim thay nguoi dung
            if (updatedUser.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            // Cap nhat thanh cong
            return ResponseEntity.ok(updatedUser.get());

        } catch (IllegalArgumentException ex) {

            // Du lieu cap nhat khong hop le
            return ResponseEntity.badRequest().build();
        }
    }

    // FR-82: ADMIN khoa hoac mo khoa tai khoan
    @PutMapping("/{id}/status")
    public ResponseEntity<UserSummaryResponse> changeUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // Kiem tra token dang nhap
        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Chua dang nhap hoac token khong hop le
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Chi ADMIN moi co quyen khoa/mo khoa
        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Goi Service thay doi trang thai tai khoan
            Optional<UserSummaryResponse> result =
                    userService.changeUserStatus(
                            id,
                            active,
                            currentUser.get().getId()
                    );

            // Khong tim thay tai khoan
            if (result.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            // Cap nhat thanh cong
            return ResponseEntity.ok(result.get());

        } catch (IllegalArgumentException ex) {

            // Ngan ADMIN tu khoa tai khoan
            return ResponseEntity.badRequest().build();
        }
    }

    // FR-83: ADMIN phan quyen tai khoan nguoi dung
    @PutMapping("/{id}/role")
    public ResponseEntity<UserSummaryResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody(required = false) UpdateUserRoleRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization) {

        // Kiem tra token dang nhap
        Optional<User> currentUser =
                accessControl.authenticate(authorization);

        // Chua dang nhap hoac token khong hop le
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        // Chi ADMIN moi duoc phan quyen
        if (!accessControl.isAdmin(currentUser.get())) {
            return ResponseEntity.status(403).build();
        }

        try {
            // Goi Service de thay doi vai tro nguoi dung
            Optional<UserSummaryResponse> result =
                    userService.updateUserRole(
                            id,
                            request,
                            currentUser.get().getId()
                    );

            // Khong tim thay tai khoan
            if (result.isEmpty()) {
                return ResponseEntity.status(404).build();
            }

            // Phan quyen thanh cong
            return ResponseEntity.ok(result.get());

        } catch (IllegalArgumentException ex) {

            // Vai tro khong hop le hoac ADMIN tu doi quyen
            return ResponseEntity.badRequest().build();
        }
    }
}
