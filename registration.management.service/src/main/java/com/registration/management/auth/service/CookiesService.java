package com.registration.management.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CookiesService {

    private final String refreshTokenCookieName;
    private final boolean cookieHttpOnly;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String cookieSameSite;
    private final long cookieExpires;

    public CookiesService(
            @Value("${app.cookie.refresh-token-name:refreshToken}") String refreshTokenCookieName,
            @Value("${app.cookie.http-only:true}") boolean cookieHttpOnly,
            @Value("${app.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.cookie.domain:}") String cookieDomain,
            @Value("${app.cookie.same-site:Strict}") String cookieSameSite,
            @Value("${app.cookie.expires:604800}") long cookieExpires) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = cookieDomain;
        this.cookieSameSite = cookieSameSite;
        this.cookieExpires = cookieExpires;
    }

    public ResponseCookie generateRefreshTokenCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, refreshToken)
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(cookieExpires))
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }

    public ResponseCookie getCleanRefreshTokenCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(cookieHttpOnly)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite(cookieSameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }
}
