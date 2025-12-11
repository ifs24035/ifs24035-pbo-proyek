package org.delcom.app.interceptors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.delcom.app.utils.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthInterceptorTests {

    @Test
    @DisplayName("Pengujian AuthInterceptor dengan berbagai skenario")
    public void testVariousAuthInterceptor() throws Exception {

        UUID userId = UUID.randomUUID();
        String bearerToken = JwtUtil.generateToken(userId);
        AuthToken authToken = new AuthToken(userId, bearerToken);

        User user = new User("testuser", "testuser@example.com");
        user.setId(userId);

        // Mock AuthTokenService
        AuthTokenService authTokenService = Mockito.mock(AuthTokenService.class);

        // Mock UserService
        UserService userService = Mockito.mock(UserService.class);

        // Mock HttpServletRequest
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // Mock HttpServletResponse
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Instance AuthInterceptor dengan service palsu
        AuthInterceptor authInterceptor = new AuthInterceptor();
        authInterceptor.authTokenService = authTokenService;
        authInterceptor.userService = userService;
        authInterceptor.authContext = new AuthContext();

        // Menguji method preHandle yang berhasil
        {
            // Mocking behavior dari authTokenService
            when(authTokenService.findUserToken(Mockito.any(UUID.class), Mockito.anyString()))
                    .thenReturn(authToken);

            // Mocking behavior dari userService
            when(userService.getUserById(userId)).thenReturn(user);

            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/users/me");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + bearerToken);

            boolean isPublic = authInterceptor.preHandle(request, response, null);
            assertTrue(isPublic);
        }

        // Menguji method preHandle yang berhasil dengan path public
        {
            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/auth");
            boolean isPublic = authInterceptor.preHandle(request, response, null);
            assertTrue(isPublic);

            when(request.getRequestURI()).thenReturn("/error");
            isPublic = authInterceptor.preHandle(request, response, null);
            assertTrue(isPublic);
        }

        // Menguji method preHandle yang tidak valid dengan token null
        {
            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/users/me");

            // Header Authorization null
            when(request.getHeader("Authorization")).thenReturn(null);
            boolean isAuth = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isAuth);

            // Header Authorization kosong
            when(request.getHeader("Authorization")).thenReturn("");
            isAuth = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isAuth);

            // Header Authorization empty
            when(request.getHeader("Authorization")).thenReturn("Bearer ");
            isAuth = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isAuth);

            // Header Authorization tidak valid
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid_token");
            isAuth = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isAuth);
        }

        // Menguji method preHandle dengan extract user id gagal
        {
            String invalidToken = Jwts.builder()
                    .subject(userId.toString() + "invalid")
                    .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 3)) // 3 jam yang lalu
                    .expiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 1)) // expired 1 jam yang lalu
                    .signWith(JwtUtil.getKey()) // Perlu menambahkan method getKey() di JwtUtil
                    .compact();

            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/users/me");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + invalidToken);

            boolean isAuth = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isAuth);
        }

        // Menguji method preHandle yang tidak valid dengan token tidak ditemukan
        {
            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/users/me");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + bearerToken);

            // Token tidak ditemukan di database
            when(authTokenService.findUserToken(Mockito.any(UUID.class), Mockito.anyString()))
                    .thenReturn(null);

            boolean isPublic = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isPublic);
        }

        // Menguji method preHandle yang tidak valid dengan user tidak ditemukan
        {
            // Mocking behavior dari request
            when(request.getRequestURI()).thenReturn("/api/users/me");
            when(request.getHeader("Authorization")).thenReturn("Bearer " + bearerToken);

            // Mocking behavior dari authTokenService
            when(authTokenService.findUserToken(Mockito.any(UUID.class), Mockito.anyString()))
                    .thenReturn(authToken);

            // User tidak ditemukan
            when(userService.getUserById(userId)).thenReturn(null);

            boolean isPublic = authInterceptor.preHandle(request, response, null);
            assertEquals(false, isPublic);
        }
    }
}
