package com.zerozoa.psik.global.security.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public static void addCookie(HttpServletResponse response, String name,
                                 String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(true);          // HTTPS only
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request,
                                    HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .forEach(c -> {
                    c.setValue("");
                    c.setPath("/");
                    c.setMaxAge(0);
                    response.addCookie(c);
                });
    }
}