package com.registration.management.auth.entities;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserRequest {
    private String fullName;
    private String email;
    private String password;
    private Role role;

}
