
package com.hotelbooking.dto;

// FR-81: Du lieu yeu cau cap nhat thong tin nguoi dung
public class UpdateUserRequest {

    private String name;
    private String phone;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
