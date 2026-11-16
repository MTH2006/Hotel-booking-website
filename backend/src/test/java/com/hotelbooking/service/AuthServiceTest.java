package com.hotelbooking.service;

import com.hotelbooking.dto.AuthRequest;
import com.hotelbooking.dto.AuthResponse;
import com.hotelbooking.model.User;
import com.hotelbooking.repository.UserRepository;
import com.hotelbooking.security.AuthTokenService;
import com.hotelbooking.security.PasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private UserRepository userRepository;
    private AuthTokenService tokenService;
    private PasswordService passwordService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenService = mock(AuthTokenService.class);
        passwordService = mock(PasswordService.class);
        authService = new AuthService(userRepository, tokenService, passwordService);
    }

    @Test
    void testRegisterSuccess() {
        AuthRequest req = new AuthRequest();
        req.setName("Test User");
        req.setEmail("test@example.com");
        req.setPassword("Password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordService.hash("Password123")).thenReturn("hashedPassword");
        when(tokenService.issue(any(User.class))).thenReturn("mock_token");

        AuthResponse resp = authService.register(req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getToken());
        assertEquals("test@example.com", resp.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmail() {
        AuthRequest req = new AuthRequest();
        req.setEmail("duplicate@example.com");
        req.setPassword("Password123");

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        AuthResponse resp = authService.register(req);
        assertFalse(resp.isSuccess());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        AuthRequest req = new AuthRequest();
        req.setEmail("login@example.com");
        req.setPassword("Password123");

        User user = new User("uuid_1", "Login User", "login@example.com", "hashedPassword", "0901234567", "USER");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordService.matches("Password123", "hashedPassword")).thenReturn(true);
        when(tokenService.issue(user)).thenReturn("mock_token");

        AuthResponse resp = authService.login(req);
        assertTrue(resp.isSuccess());
        assertNotNull(resp.getToken());
    }

    @Test
    void testLoginLockedUser() {
        AuthRequest req = new AuthRequest();
        req.setEmail("locked@example.com");
        req.setPassword("Password123");

        User user = new User("uuid_2", "Locked User", "locked@example.com", "hashedPassword", "0901234567", "USER");
        user.setActive(false);
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(user));

        AuthResponse resp = authService.login(req);
        assertFalse(resp.isSuccess());
    }
}

