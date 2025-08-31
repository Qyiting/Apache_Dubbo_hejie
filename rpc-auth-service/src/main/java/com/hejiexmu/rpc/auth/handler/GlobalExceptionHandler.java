package com.hejiexmu.rpc.auth.handler;

import com.hejiexmu.rpc.auth.dto.ApiResponse;
import com.hejiexmu.rpc.auth.exception.BusinessException;
import com.hejiexmu.rpc.auth.exception.ValidationException;
import com.hejiexmu.rpc.auth.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * @author hejiexmu
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        
        logger.warn("认证异常: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.authError(e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理Spring Security认证异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException e, HttpServletRequest request) {
        
        logger.warn("用户名或密码错误: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.authError("用户名或密码错误");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理账户被禁用异常
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledException(
            DisabledException e, HttpServletRequest request) {
        
        logger.warn("账户被禁用: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.authError("账户已被禁用");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理账户被锁定异常
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedException(
            LockedException e, HttpServletRequest request) {
        
        logger.warn("账户被锁定: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.authError("账户已被锁定");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        logger.warn("权限不足: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.permissionError("权限不足");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        
        logger.warn("业务异常: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.businessError(e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException e, HttpServletRequest request) {
        
        logger.warn("验证异常: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.validationError(e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException e, HttpServletRequest request) {
        
        logger.warn("资源未找到: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.notFoundError(e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * 处理请求参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        logger.warn("请求参数验证失败: uri={}, errors={}", request.getRequestURI(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.validationError("参数验证失败: " + errorMessage);
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(
            BindException e, HttpServletRequest request) {
        
        String errorMessage = e.getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        logger.warn("数据绑定失败: uri={}, errors={}", request.getRequestURI(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.validationError("数据绑定失败: " + errorMessage);
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        String errorMessage = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));
        
        logger.warn("约束违反: uri={}, errors={}", request.getRequestURI(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.validationError("约束违反: " + errorMessage);
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理方法参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        String errorMessage = String.format("参数 '%s' 的值 '%s' 类型不正确，期望类型: %s", 
            e.getName(), e.getValue(), e.getRequiredType().getSimpleName());
        
        logger.warn("参数类型不匹配: uri={}, error={}", request.getRequestURI(), errorMessage);
        
        ApiResponse<Void> response = ApiResponse.validationError(errorMessage);
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        
        logger.warn("非法参数: uri={}, message={}", request.getRequestURI(), e.getMessage());
        
        ApiResponse<Void> response = ApiResponse.validationError("参数错误: " + e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(
            NullPointerException e, HttpServletRequest request) {
        
        logger.error("空指针异常: uri={}, message={}", request.getRequestURI(), e.getMessage(), e);
        
        ApiResponse<Void> response = ApiResponse.internalError("系统内部错误");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {
        
        logger.error("运行时异常: uri={}, message={}", request.getRequestURI(), e.getMessage(), e);
        
        ApiResponse<Void> response = ApiResponse.internalError("系统运行时错误: " + e.getMessage());
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception e, HttpServletRequest request) {
        
        logger.error("未知异常: uri={}, message={}", request.getRequestURI(), e.getMessage(), e);
        
        ApiResponse<Void> response = ApiResponse.internalError("系统内部错误，请联系管理员");
        response.setPath(request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}