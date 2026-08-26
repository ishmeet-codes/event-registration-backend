package com.registration.management.auth.controller;

import com.registration.management.auth.entities.AuthRequest;
import com.registration.management.auth.entities.Role;
import com.registration.management.auth.util.JwtUtil;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtUtil jwtUtil;
    @PostMapping("/authenticate")
    public String generateToken(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
                );
            return jwtUtil.generateToken(authRequest.getEmail(), authRequest.getPassword());

            } catch (Exception e) {
            throw new IllegalStateException("Invalid JWT Token");
                }
    }
}
