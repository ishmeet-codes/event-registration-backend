package com.registration.management.auth.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private  String userId;
    private String userEmail;
    private String userPassword;
    private Role role;

    public UserResponse(String userId, String userEmail, Role role) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
