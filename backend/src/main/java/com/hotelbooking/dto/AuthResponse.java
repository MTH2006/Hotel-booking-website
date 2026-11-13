package com.hotelbooking.dto;

public class AuthResponse {
    private boolean success;
    private String token;
    private UserDto user;
    private String message;

    public AuthResponse() {}

    public AuthResponse(boolean success, String token, UserDto user) {
        this(success, token, user, null);
    }

    public AuthResponse(boolean success, String token, UserDto user, String message) {
        this.success = success;
        this.token = token;
        this.user = user;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String role;

        public UserDto(String id, String name, String email, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
    }
}
