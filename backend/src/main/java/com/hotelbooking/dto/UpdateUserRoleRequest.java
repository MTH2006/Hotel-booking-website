
package com.hotelbooking.dto;

// FR-83: Yeu cau thay doi vai tro nguoi dung
public class UpdateUserRoleRequest {

    private String role;

    public UpdateUserRoleRequest() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
