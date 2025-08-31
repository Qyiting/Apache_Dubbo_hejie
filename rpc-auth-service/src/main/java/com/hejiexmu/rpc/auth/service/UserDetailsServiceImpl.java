package com.hejiexmu.rpc.auth.service;

import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.entity.Role;
import com.hejiexmu.rpc.auth.entity.Permission;
import com.hejiexmu.rpc.auth.enums.AccountStatus;
import com.hejiexmu.rpc.auth.repository.impl.UserRepositoryImpl;
import com.hejiexmu.rpc.auth.repository.impl.RoleRepositoryImpl;
import com.hejiexmu.rpc.auth.repository.impl.PermissionRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * UserDetailsService实现类
 * 用于加载用户详情和权限信息
 * 
 * @author hejiexmu
 */
@Service
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    private final UserRepositoryImpl userRepository;
    private final RoleRepositoryImpl roleRepository;
    private final PermissionRepositoryImpl permissionRepository;
    
    public UserDetailsServiceImpl(UserRepositoryImpl userRepository,
                                RoleRepositoryImpl roleRepository,
                                PermissionRepositoryImpl permissionRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        try {
            // 查找用户
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                logger.warn("User not found: {}", username);
                throw new UsernameNotFoundException("User not found: " + username);
            }
            
            User user = userOpt.get();
            
            // 检查用户状态
            if (!isUserEnabled(user)) {
                logger.warn("User account is disabled: {}", username);
                throw new UsernameNotFoundException("User account is disabled: " + username);
            }
            
            // 加载用户权限
            Collection<? extends GrantedAuthority> authorities = loadUserAuthorities(user.getId());
            
            // 创建UserDetails对象
            UserDetailsImpl userDetails = new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getRealName(),
                user.getAccountStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                authorities
            );
            
            logger.debug("Successfully loaded user: {} with {} authorities", username, authorities.size());
            return userDetails;
            
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error loading user by username: {}", username, e);
            throw new UsernameNotFoundException("Error loading user: " + username, e);
        }
    }
    
    /**
     * 检查用户是否启用
     */
    private boolean isUserEnabled(User user) {
        return user.getAccountStatus() == AccountStatus.ACTIVE;
    }
    
    /**
     * 加载用户权限
     */
    private Collection<? extends GrantedAuthority> loadUserAuthorities(Long userId) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        try {
            // 加载用户角色
            List<Role> roles = roleRepository.findByUserId(userId);
            
            for (Role role : roles) {
                // 添加角色权限（以ROLE_前缀）
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
                
                // 加载角色的权限
                List<Permission> permissions = permissionRepository.findByRoleId(role.getId());
                
                for (Permission permission : permissions) {
                    // 添加权限（不带前缀）
                    authorities.add(new SimpleGrantedAuthority(permission.getPermissionName()));
                }
            }
            
            // 如果用户没有任何角色，给予默认用户角色
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            logger.debug("Loaded {} authorities for user ID: {}", authorities.size(), userId);
            
        } catch (Exception e) {
            logger.error("Error loading authorities for user ID: {}", userId, e);
            // 返回默认权限
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return authorities;
    }
    
    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        logger.debug("Loading user by ID: {}", userId);
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                logger.warn("User not found with ID: {}", userId);
                throw new UsernameNotFoundException("User not found with ID: " + userId);
            }
            
            User user = userOpt.get();
            return loadUserByUsername(user.getUsername());
            
        } catch (Exception e) {
            logger.error("Error loading user by ID: {}", userId, e);
            throw new UsernameNotFoundException("Error loading user with ID: " + userId, e);
        }
    }
    
    /**
     * UserDetails实现类
     */
    public static class UserDetailsImpl implements UserDetails {
        
        private final Long id;
        private final String username;
        private final String password;
        private final String email;
        private final String fullName;
        private final AccountStatus accountStatus;
        private final LocalDateTime lastLoginAt;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
        private final Collection<? extends GrantedAuthority> authorities;
        
        public UserDetailsImpl(Long id,
                             String username,
                             String password,
                             String email,
                             String fullName,
                             AccountStatus accountStatus,
                             LocalDateTime lastLoginAt,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt,
                             Collection<? extends GrantedAuthority> authorities) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.email = email;
            this.fullName = fullName;
            this.accountStatus = accountStatus;
            this.lastLoginAt = lastLoginAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.authorities = authorities;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return password;
        }
        
        @Override
        public String getUsername() {
            return username;
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return accountStatus != AccountStatus.EXPIRED;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return accountStatus != AccountStatus.LOCKED;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            // 可以根据业务需求实现密码过期逻辑
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return accountStatus == AccountStatus.ACTIVE;
        }
        
        // 额外的getter方法
        public Long getId() {
            return id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public AccountStatus getAccountStatus() {
            return accountStatus;
        }
        
        public LocalDateTime getLastLoginAt() {
            return lastLoginAt;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }
        
        @Override
        public String toString() {
            return "UserDetailsImpl{" +
                   "id=" + id +
                   ", username='" + username + '\'' +
                   ", email='" + email + '\'' +
                   ", fullName='" + fullName + '\'' +
                   ", accountStatus=" + accountStatus +
                   ", authorities=" + authorities.size() +
                   '}';
        }
    }
}