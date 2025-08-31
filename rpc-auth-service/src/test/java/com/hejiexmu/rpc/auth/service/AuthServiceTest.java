package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.config.TestConfig;
import com.hejiexmu.rpc.auth.dto.LoginRequest;
import com.hejiexmu.rpc.auth.dto.RegisterRequest;
import com.hejiexmu.rpc.auth.dto.AuthResponse;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.repository.impl.UserRepositoryImpl;
import com.hejiexmu.rpc.auth.repository.impl.SessionRepositoryImpl;
import com.hejiexmu.rpc.auth.config.PasswordConfig;
import com.hejiexmu.rpc.auth.cache.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthService单元测试
 * 
 * @author hejiexmu
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@Import(TestConfig.class)
@ActiveProfiles("test")
public class AuthServiceTest {
    
    @Mock
    private UserRepositoryImpl userRepository;
    
    @Mock
    private SessionRepositoryImpl sessionRepository;
    
    @Mock
    private JwtTokenService jwtTokenService;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private PasswordConfig.PasswordValidator passwordValidator;
    
    @Mock
    private RedisCacheService redisCacheService;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    
    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        // 创建登录请求
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);
        loginRequest.setIpAddress("192.168.1.1");
        loginRequest.setUserAgent("Test Agent");
        
        // 创建注册请求
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setRealName("New User");
        registerRequest.setPhone("13800138000");
        registerRequest.setIpAddress("192.168.1.1");
        registerRequest.setUserAgent("Test Agent");
    }
    
    @Test
    void testLoginSuccess() {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtTokenService.createTokenPair(any(User.class), anyString(), anyString()))
                .thenReturn(new JwtTokenService.TokenPair("accessToken", "refreshToken"));
        
        // 执行测试
        AuthResponse response = authService.login(loginRequest);
        
        // 验证结果
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtTokenService).createTokenPair(any(User.class), anyString(), anyString());
        verify(redisCacheService).delete("login_failure:testuser");
    }
    
    @Test
    void testLoginFailure_UserNotFound() {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    @Test
    void testLoginFailure_WrongPassword() {
        // 准备测试数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);
        when(redisCacheService.get("login_failure:testuser", Integer.class)).thenReturn(0);
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(redisCacheService).set(eq("login_failure:testuser"), eq(1), eq(300L));
    }
    
    @Test
    void testLoginFailure_AccountLocked() {
        // 准备测试数据
        testUser.setAccountNonLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    @Test
    void testLoginFailure_AccountDisabled() {
        // 准备测试数据
        testUser.setEnabled(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
    
    @Test
    void testRegisterSuccess() {
        // 准备测试数据
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordValidator.validatePassword("password123")).thenReturn(new PasswordConfig.PasswordValidationResult(true, "密码有效"));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // 执行测试
        AuthResponse response = authService.register(registerRequest);
        
        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getUser());
        
        // 验证方法调用
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("newuser@example.com");
        verify(passwordValidator).validate("password123");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testRegisterFailure_UsernameExists() {
        // 准备测试数据
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(testUser));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterFailure_EmailExists() {
        // 准备测试数据
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.of(testUser));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("newuser@example.com");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterFailure_PasswordMismatch() {
        // 准备测试数据
        registerRequest.setConfirmPassword("differentPassword");
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        
        // 验证方法调用
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterFailure_WeakPassword() {
        // 准备测试数据
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordValidator.validatePassword("password123")).thenReturn(new PasswordConfig.PasswordValidationResult(false, "密码无效"));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> authService.register(registerRequest));
        
        // 验证方法调用
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).findByEmail("newuser@example.com");
        verify(passwordValidator).validate("password123");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRefreshToken() {
        // 准备测试数据
        String refreshToken = "validRefreshToken";
        when(jwtTokenService.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenService.getUserIdFromToken(refreshToken)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenService.createTokenPair(any(User.class), anyString(), anyString()))
                .thenReturn(new JwtTokenService.TokenPair("newAccessToken", "newRefreshToken"));
        
        // 执行测试
        AuthResponse response = authService.refreshToken(refreshToken);
        
        // 验证结果
        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        
        // 验证方法调用
        verify(jwtTokenService).validateRefreshToken(refreshToken);
        verify(jwtTokenService).getUserIdFromToken(refreshToken);
        verify(userRepository).findById(1L);
        verify(jwtTokenService).blacklistToken(refreshToken);
    }
    
    @Test
    void testLogout() {
        // 准备测试数据
        String accessToken = "validAccessToken";
        String refreshToken = "validRefreshToken";
        
        // 执行测试
        authService.logout("sessionId123");
        
        // 验证方法调用
        verify(jwtTokenService).blacklistToken(accessToken);
        verify(jwtTokenService).blacklistToken(refreshToken);
    }
    
    @Test
    void testChangePassword() {
        // 准备测试数据
        String oldPassword = "oldPassword";
        String newPassword = "newPassword123";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldPassword, "encodedPassword")).thenReturn(true);
        when(passwordValidator.validatePassword(newPassword)).thenReturn(new PasswordConfig.PasswordValidationResult(true, "密码有效"));
        when(passwordEncoder.encode(newPassword)).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // 执行测试
        assertDoesNotThrow(() -> authService.changePassword(1L, oldPassword, newPassword));
        
        // 验证方法调用
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches(oldPassword, "encodedPassword");
        verify(passwordValidator).validatePassword(newPassword);
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(any(User.class));
    }
}