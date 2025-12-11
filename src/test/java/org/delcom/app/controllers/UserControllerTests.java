package org.delcom.app.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserControllerTests {

    private UserService userService;
    private AuthTokenService authTokenService;
    private UserController userController;
    private AuthContext authContext;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        authTokenService = mock(AuthTokenService.class);
        authContext = new AuthContext();

        userController = new UserController(userService, authTokenService);
        userController.authContext = authContext;
    }

    // ==========================================
    // 1. TEST REGISTER
    // ==========================================
    @Test
    @DisplayName("Test Register")
    void testRegister() {
        // --- Skenario 1: Invalid Data ---
        User invalidUser = new User("", "", "");
        var res = userController.registerUser(invalidUser);
        assert (res.getStatusCode().is4xxClientError());
        
        res = userController.registerUser(new User("Budi", "", "123"));
        assert (res.getStatusCode().is4xxClientError());
        
        res = userController.registerUser(new User("Budi", "b@b.com", ""));
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 2: Email Sudah Ada ---
        when(userService.getUserByEmail("exist@mail.com")).thenReturn(new User());
        User existingUser = new User("Budi", "exist@mail.com", "123");
        
        res = userController.registerUser(existingUser);
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 3: Sukses ---
        when(userService.getUserByEmail("new@mail.com")).thenReturn(null);
        User createdUser = new User(); 
        createdUser.setId(UUID.randomUUID());
        when(userService.createUser(anyString(), anyString(), anyString())).thenReturn(createdUser);

        User newUser = new User("Budi", "new@mail.com", "123");
        res = userController.registerUser(newUser);
        
        assert (res.getStatusCode().is2xxSuccessful());
        assert (res.getBody().getStatus().equals("success"));
    }

    // ==========================================
    // 2. TEST LOGIN (DIPERLENGKAP)
    // ==========================================
    @Test
    @DisplayName("Test Login")
    void testLogin() {
        String rawPass = "123";
        String encodedPass = new BCryptPasswordEncoder().encode(rawPass);
        
        User validUser = new User("Budi", "valid@mail.com", encodedPass);
        validUser.setId(UUID.randomUUID());

        // --- Skenario 1: Invalid Input ---
        assert (userController.loginUser(new User("", "123")).getStatusCode().is4xxClientError());
        assert (userController.loginUser(new User("a@b.com", "")).getStatusCode().is4xxClientError());

        // --- Skenario 2: User Tidak Ditemukan ---
        when(userService.getUserByEmail("unknown@mail.com")).thenReturn(null);
        var res = userController.loginUser(new User("unknown@mail.com", "123"));
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 3: Password Salah ---
        when(userService.getUserByEmail("valid@mail.com")).thenReturn(validUser);
        res = userController.loginUser(new User("valid@mail.com", "wrongpass"));
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 4: Gagal Buat Token ---
        when(userService.getUserByEmail("valid@mail.com")).thenReturn(validUser);
        when(authTokenService.createAuthToken(any())).thenReturn(null); 

        res = userController.loginUser(new User("valid@mail.com", rawPass));
        assert (res.getStatusCode().is5xxServerError());

        // --- Skenario 5: Login Sukses & Hapus Token Lama (IMPORTANT FOR COVERAGE) ---
        // Setup ulang mock sukses
        when(authTokenService.createAuthToken(any())).thenReturn(new AuthToken());
        // Simulasi ADA token lama (Return Object, bukan null)
        when(authTokenService.findUserToken(any(), anyString())).thenReturn(new AuthToken());

        res = userController.loginUser(new User("valid@mail.com", rawPass));
        
        assert (res.getStatusCode().is2xxSuccessful());
        // Verifikasi bahwa deleteAuthToken benar-benar dipanggil
        verify(authTokenService, times(1)).deleteAuthToken(any());
        
        // --- Skenario 6: Login Sukses & Tidak Ada Token Lama ---
        when(authTokenService.findUserToken(any(), anyString())).thenReturn(null);
        res = userController.loginUser(new User("valid@mail.com", rawPass));
        assert (res.getStatusCode().is2xxSuccessful());
    }

    // ==========================================
    // 3. TEST GET USER INFO
    // ==========================================
    @Test
    void testGetUserInfo() {
        // --- Skenario 1: Unauthorized ---
        authContext.setAuthUser(null);
        var res = userController.getUserInfo();
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 2: Sukses ---
        User loginUser = new User("Budi", "budi@mail.com", "pass");
        authContext.setAuthUser(loginUser);
        
        res = userController.getUserInfo();
        assert (res.getStatusCode().is2xxSuccessful());
    }

    // ==========================================
    // 4. TEST UPDATE USER
    // ==========================================
    @Test
    void testUpdateUser() {
        User loginUser = new User("Budi", "budi@mail.com", "pass");
        loginUser.setId(UUID.randomUUID());
        
        // --- Skenario 1: Unauthorized ---
        authContext.setAuthUser(null);
        var res = userController.updateUser(loginUser);
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 2: Invalid Data ---
        authContext.setAuthUser(loginUser);
        assert (userController.updateUser(new User("", "b@b.com")).getStatusCode().is4xxClientError());
        assert (userController.updateUser(new User("Budi", "")).getStatusCode().is4xxClientError());

        // --- Skenario 3: User Tidak Ditemukan ---
        when(userService.updateUser(any(), anyString(), anyString())).thenReturn(null);
        res = userController.updateUser(new User("Baru", "baru@mail.com", ""));
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 4: Sukses ---
        when(userService.updateUser(any(), anyString(), anyString())).thenReturn(loginUser);
        res = userController.updateUser(new User("Baru", "baru@mail.com", ""));
        assert (res.getStatusCode().is2xxSuccessful());
    }

    // ==========================================
    // 5. TEST UPDATE PASSWORD
    // ==========================================
    @Test
    void testUpdatePassword() {
        String oldPass = "old123";
        String encodedOld = new BCryptPasswordEncoder().encode(oldPass);
        
        User loginUser = new User("Budi", "budi@mail.com", encodedOld);
        loginUser.setId(UUID.randomUUID());

        Map<String, String> payload = Map.of("password", oldPass, "newPassword", "new123");

        // --- Skenario 1: Unauthorized ---
        authContext.setAuthUser(null);
        var res = userController.updateUserPassword(payload);
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 2: Invalid Data ---
        authContext.setAuthUser(loginUser);
        assert (userController.updateUserPassword(Map.of("password", "", "newPassword", "1")).getStatusCode().is4xxClientError());

        // --- Skenario 3: Password Lama Salah ---
        var wrongPayload = Map.of("password", "wrong", "newPassword", "new123");
        res = userController.updateUserPassword(wrongPayload);
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 4: User Tidak Ditemukan ---
        when(userService.updatePassword(any(), anyString())).thenReturn(null);
        res = userController.updateUserPassword(payload);
        assert (res.getStatusCode().is4xxClientError());

        // --- Skenario 5: Sukses ---
        when(userService.updatePassword(any(), anyString())).thenReturn(loginUser);
        res = userController.updateUserPassword(payload);
        assert (res.getStatusCode().is2xxSuccessful());
    }
}