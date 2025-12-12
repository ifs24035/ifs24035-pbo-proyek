package org.delcom.app.controllers;

import org.delcom.app.configs.ApiResponse;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.entities.AuthToken;
import org.delcom.app.entities.User;
import org.delcom.app.services.AuthTokenService;
import org.delcom.app.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTests {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthContext authContext;

    private User mockUser;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
        mockUser.setPassword(encoder.encode(rawPassword));

        ReflectionTestUtils.setField(userController, "authContext", authContext);
    }

    // --- 1. Register Validation Tests (Coverage for Name, Email, Password null/empty) ---
    
    @Test
    void testRegisterUser_Validation_Name() {
        // Case 1: Name Null
        User reqNull = new User(null, "a@b.c", "pass");
        ResponseEntity<ApiResponse<Map<String, UUID>>> resNull = userController.registerUser(reqNull);
        assertEquals(HttpStatus.BAD_REQUEST, resNull.getStatusCode());
        assertTrue(resNull.getBody().getMessage().contains("Data nama tidak valid"));

        // Case 2: Name Empty ""
        User reqEmpty = new User("", "a@b.c", "pass");
        ResponseEntity<ApiResponse<Map<String, UUID>>> resEmpty = userController.registerUser(reqEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, resEmpty.getStatusCode());
        assertTrue(resEmpty.getBody().getMessage().contains("Data nama tidak valid"));
    }

    @Test
    void testRegisterUser_Validation_Email() {
        // Case 1: Email Null
        User reqNull = new User("Name", null, "pass");
        ResponseEntity<ApiResponse<Map<String, UUID>>> resNull = userController.registerUser(reqNull);
        assertEquals(HttpStatus.BAD_REQUEST, resNull.getStatusCode());
        assertTrue(resNull.getBody().getMessage().contains("Data email tidak valid"));

        // Case 2: Email Empty ""
        User reqEmpty = new User("Name", "", "pass");
        ResponseEntity<ApiResponse<Map<String, UUID>>> resEmpty = userController.registerUser(reqEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, resEmpty.getStatusCode());
        assertTrue(resEmpty.getBody().getMessage().contains("Data email tidak valid"));
    }

    @Test
    void testRegisterUser_Validation_Password() {
        // Case 1: Password Null
        User reqNull = new User("Name", "a@b.c", null);
        ResponseEntity<ApiResponse<Map<String, UUID>>> resNull = userController.registerUser(reqNull);
        assertEquals(HttpStatus.BAD_REQUEST, resNull.getStatusCode());
        assertTrue(resNull.getBody().getMessage().contains("Data password tidak valid"));

        // Case 2: Password Empty ""
        User reqEmpty = new User("Name", "a@b.c", "");
        ResponseEntity<ApiResponse<Map<String, UUID>>> resEmpty = userController.registerUser(reqEmpty);
        assertEquals(HttpStatus.BAD_REQUEST, resEmpty.getStatusCode());
        assertTrue(resEmpty.getBody().getMessage().contains("Data password tidak valid"));
    }

    @Test
    void testRegisterUser_ExistingEmail() {
        User req = new User("Name", "exist@example.com", "pass");
        when(userService.getUserByEmail(req.getEmail())).thenReturn(new User());

        ResponseEntity<ApiResponse<Map<String, UUID>>> res = userController.registerUser(req);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().getMessage().contains("sudah terdaftar"));
    }

    @Test
    void testRegisterUser_Success() {
        User req = new User("Name", "new@example.com", "pass");
        when(userService.getUserByEmail(req.getEmail())).thenReturn(null);
        when(userService.createUser(eq("Name"), eq("new@example.com"), anyString())).thenReturn(mockUser);

        ResponseEntity<ApiResponse<Map<String, UUID>>> res = userController.registerUser(req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(mockUser.getId(), res.getBody().getData().get("id"));
    }

    // --- 2. Login Validation Tests (Coverage for Email, Password null/empty) ---

    @Test
    void testLoginUser_Validation_Email() {
        // Null
        User reqNull = new User(); // email null
        reqNull.setPassword("pass");
        assertEquals(HttpStatus.BAD_REQUEST, userController.loginUser(reqNull).getStatusCode());

        // Empty
        User reqEmpty = new User();
        reqEmpty.setEmail("");
        reqEmpty.setPassword("pass");
        assertEquals(HttpStatus.BAD_REQUEST, userController.loginUser(reqEmpty).getStatusCode());
    }

    @Test
    void testLoginUser_Validation_Password() {
        // Null
        User reqNull = new User();
        reqNull.setEmail("a@b.c");
        reqNull.setPassword(null);
        assertEquals(HttpStatus.BAD_REQUEST, userController.loginUser(reqNull).getStatusCode());

        // Empty
        User reqEmpty = new User();
        reqEmpty.setEmail("a@b.c");
        reqEmpty.setPassword("");
        assertEquals(HttpStatus.BAD_REQUEST, userController.loginUser(reqEmpty).getStatusCode());
    }

    @Test
    void testLoginUser_UserNotFound() {
        User req = new User("a", "unknown@example.com", "pass");
        when(userService.getUserByEmail(req.getEmail())).thenReturn(null);

        ResponseEntity<ApiResponse<Map<String, String>>> res = userController.loginUser(req);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void testLoginUser_WrongPassword() {
        User req = new User("a", "test@example.com", "WRONG_PASS");
        when(userService.getUserByEmail(req.getEmail())).thenReturn(mockUser);

        ResponseEntity<ApiResponse<Map<String, String>>> res = userController.loginUser(req);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().getMessage().contains("salah"));
    }

    @Test
    void testLoginUser_Success() {
        User req = new User("a", "test@example.com", rawPassword);
        when(userService.getUserByEmail(req.getEmail())).thenReturn(mockUser);
        
        when(authTokenService.findUserToken(any(), anyString())).thenReturn(null);
        when(authTokenService.createAuthToken(any(AuthToken.class))).thenReturn(new AuthToken());

        ResponseEntity<ApiResponse<Map<String, String>>> res = userController.loginUser(req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody().getData().get("authToken"));
    }

    @Test
    void testLoginUser_Success_WithOldTokenDeletion() {
        User req = new User("a", "test@example.com", rawPassword);
        when(userService.getUserByEmail(req.getEmail())).thenReturn(mockUser);

        when(authTokenService.findUserToken(any(), anyString())).thenReturn(new AuthToken());
        when(authTokenService.createAuthToken(any(AuthToken.class))).thenReturn(new AuthToken());

        userController.loginUser(req);
        
        verify(authTokenService).deleteAuthToken(mockUser.getId());
    }

    @Test
    void testLoginUser_TokenCreationFail() {
        User req = new User("a", "test@example.com", rawPassword);
        when(userService.getUserByEmail(req.getEmail())).thenReturn(mockUser);
        when(authTokenService.createAuthToken(any(AuthToken.class))).thenReturn(null);

        ResponseEntity<ApiResponse<Map<String, String>>> res = userController.loginUser(req);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
    }

    // --- 3. Get User Info ---
    @Test
    void testGetUserInfo_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, userController.getUserInfo().getStatusCode());
    }

    @Test
    void testGetUserInfo_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        ResponseEntity<ApiResponse<Map<String, User>>> res = userController.getUserInfo();
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Test User", res.getBody().getData().get("user").getName());
        assertNull(res.getBody().getData().get("user").getPassword());
    }

    // --- 4. Update User Validation Tests (Coverage for Name/Email null/empty) ---
    @Test
    void testUpdateUser_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, userController.updateUser(new User()).getStatusCode());
    }

    @Test
    void testUpdateUser_Validation_Name() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        // Name Null
        User reqNull = new User(null, "a@b.c", null);
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUser(reqNull).getStatusCode());

        // Name Empty
        User reqEmpty = new User("", "a@b.c", null);
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUser(reqEmpty).getStatusCode());
    }

    @Test
    void testUpdateUser_Validation_Email() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        // Email Null
        User reqNull = new User("Name", null, null);
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUser(reqNull).getStatusCode());

        // Email Empty
        User reqEmpty = new User("Name", "", null);
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUser(reqEmpty).getStatusCode());
    }

    @Test
    void testUpdateUser_NotFound() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        User req = new User("New Name", "new@email.com", null);
        when(userService.updateUser(mockUser.getId(), req.getName(), req.getEmail())).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, userController.updateUser(req).getStatusCode());
    }

    @Test
    void testUpdateUser_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        User req = new User("New Name", "new@email.com", null);
        when(userService.updateUser(mockUser.getId(), req.getName(), req.getEmail())).thenReturn(mockUser);

        assertEquals(HttpStatus.OK, userController.updateUser(req).getStatusCode());
    }

    // --- 5. Update Password Validation Tests ---
    @Test
    void testUpdateUserPassword_Unauthorized() {
        when(authContext.isAuthenticated()).thenReturn(false);
        assertEquals(HttpStatus.UNAUTHORIZED, userController.updateUserPassword(Map.of()).getStatusCode());
    }

    @Test
    void testUpdateUserPassword_Validation() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);

        // 1. OldPassword Null (Implicit by map.get missing key)
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUserPassword(Map.of("newPassword", "new")).getStatusCode());

        // 2. OldPassword Empty
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUserPassword(Map.of("password", "", "newPassword", "new")).getStatusCode());

        // 3. NewPassword Null (Implicit)
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUserPassword(Map.of("password", "old")).getStatusCode());

        // 4. NewPassword Empty (Ini yang sering kelewatan, makanya kuning)
        assertEquals(HttpStatus.BAD_REQUEST, userController.updateUserPassword(Map.of("password", "old", "newPassword", "")).getStatusCode());
    }

    @Test
    void testUpdateUserPassword_WrongOldPassword() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        Map<String, String> payload = Map.of("password", "WRONG", "newPassword", "new");
        
        ResponseEntity<ApiResponse<Void>> res = userController.updateUserPassword(payload);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().getMessage().contains("tidak cocok"));
    }

    @Test
    void testUpdateUserPassword_UserNotFound() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        Map<String, String> payload = Map.of("password", rawPassword, "newPassword", "new");
        when(userService.updatePassword(eq(mockUser.getId()), anyString())).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, userController.updateUserPassword(payload).getStatusCode());
    }

    @Test
    void testUpdateUserPassword_Success() {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(mockUser);
        
        Map<String, String> payload = Map.of("password", rawPassword, "newPassword", "new");
        when(userService.updatePassword(eq(mockUser.getId()), anyString())).thenReturn(mockUser);

        ResponseEntity<ApiResponse<Void>> res = userController.updateUserPassword(payload);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        
        verify(authTokenService).deleteAuthToken(mockUser.getId());
    }
}