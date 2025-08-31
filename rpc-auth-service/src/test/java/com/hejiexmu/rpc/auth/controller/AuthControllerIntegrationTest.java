package com.hejiexmu.rpc.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejiexmu.rpc.auth.config.TestConfig;
import com.hejiexmu.rpc.auth.dto.LoginRequest;
import com.hejiexmu.rpc.auth.dto.RegisterRequest;
import com.hejiexmu.rpc.auth.dto.AuthResponse;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * AuthController集成测试
 * 
 * @author hejiexmu
 */
@WebMvcTest(AuthController.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
public class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private AuthService authService;
    
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private AuthResponse authResponse;
    private User testUser;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRealName("Test User");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);
        
        // 创建注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setRealName("New User");
        registerRequest.setPhone("13800138000");
        
        // 创建认证响应
        authResponse = new AuthResponse();
        authResponse.setAccessToken("accessToken123");
        authResponse.setRefreshToken("refreshToken123");
        authResponse.setTokenType("Bearer");
        authResponse.setExpiresIn(3600L);
        authResponse.setUser(testUser);
    }
    
    @Test
    void testLoginSuccess() throws Exception {
        // 准备测试数据
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);
        
        // 执行测试
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("accessToken123")))
                .andExpect(jsonPath("$.data.refreshToken", is("refreshToken123")))
                .andExpect(jsonPath("$.data.user.username", is("testuser")))
                .andReturn();
        
        System.out.println("Login Response: " + result.getResponse().getContentAsString());
    }
    
    @Test
    void testLoginFailure_InvalidCredentials() throws Exception {
        // 准备测试数据
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid username or password"));
        
        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }
    
    @Test
    void testLoginValidation_EmptyUsername() throws Exception {
        // 准备测试数据
        loginRequest.setUsername("");
        
        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("validation")));
    }
    
    @Test
    void testLoginValidation_EmptyPassword() throws Exception {
        // 准备测试数据
        loginRequest.setPassword("");
        
        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpected(jsonPath("$.success", is(false)));
    }
    
    @Test
    void testRegisterSuccess() throws Exception {
        // 准备测试数据
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        
        // 执行测试
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.user.username", is("testuser")))
                .andReturn();
        
        System.out.println("Register Response: " + result.getResponse().getContentAsString());
    }
    
    @Test
    void testRegisterFailure_UsernameExists() throws Exception {
        // 准备测试数据
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));
        
        // 执行测试
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Username already exists")));
    }
    
    @Test
    void testRegisterValidation_InvalidEmail() throws Exception {
        // 准备测试数据
        registerRequest.setEmail("invalid-email");
        
        // 执行测试
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .header("X-Forwarded-For", "192.168.1.1")
                        .header("User-Agent", "Test Agent"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)));
    }
    
    @Test
    void testRefreshTokenSuccess() throws Exception {
        // 准备测试数据
        String refreshToken = "validRefreshToken";
        when(authService.refreshToken(anyString(), anyString(), anyString()))
                .thenReturn(authResponse);
        
        // 执行测试
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("accessToken123")));
    }
    
    @Test
    void testRefreshTokenFailure_InvalidToken() throws Exception {
        // 准备测试数据
        String refreshToken = "invalidRefreshToken";
        when(authService.refreshToken(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid refresh token"));
        
        // 执行测试
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid refresh token")));
    }
    
    @Test
    void testLogoutSuccess() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer accessToken123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refreshToken123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Logout successful")));
    }
    
    @Test
    void testHealthCheck() throws Exception {
        // 执行测试
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Auth service is healthy")));
    }
    
    @Test
    void testValidateTokenSuccess() throws Exception {
        // 准备测试数据
        when(authService.validateToken(anyString())).thenReturn(true);
        
        // 执行测试
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"validToken\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));
    }
    
    @Test
    void testValidateTokenFailure() throws Exception {
        // 准备测试数据
        when(authService.validateToken(anyString())).thenReturn(false);
        
        // 执行测试
        mockMvc.perform(post("/api/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalidToken\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(false)));
    }
}