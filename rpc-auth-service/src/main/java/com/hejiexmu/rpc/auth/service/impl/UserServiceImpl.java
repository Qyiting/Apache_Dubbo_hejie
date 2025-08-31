package com.hejiexmu.rpc.auth.service.impl;

import com.hejiexmu.rpc.auth.dto.AuthResponse;
import com.hejiexmu.rpc.auth.dto.LoginRequest;
import com.hejiexmu.rpc.auth.dto.PageResponse;
import com.hejiexmu.rpc.auth.dto.RegisterRequest;
import com.hejiexmu.rpc.auth.dto.UserDTO;
import com.hejiexmu.rpc.auth.entity.Permission;
import com.hejiexmu.rpc.auth.entity.Role;
import com.hejiexmu.rpc.auth.entity.User;
import com.hejiexmu.rpc.auth.entity.UserSession;
import com.hejiexmu.rpc.auth.enums.UserStatus;
import com.hejiexmu.rpc.auth.repository.PermissionRepository;
import com.hejiexmu.rpc.auth.repository.RoleRepository;
import com.hejiexmu.rpc.auth.repository.UserRepository;
import com.hejiexmu.rpc.auth.repository.UserSessionRepository;
import com.hejiexmu.rpc.auth.service.UserService;
import com.hejiexmu.rpc.auth.util.JwtUtil;
import com.hejiexmu.rpc.auth.util.PasswordUtil;
import com.hejiexmu.rpc.auth.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 
 * @author hejiexmu
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordUtil passwordUtil;
    private final SessionUtil sessionUtil;
    
    public UserServiceImpl(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PermissionRepository permissionRepository,
                          UserSessionRepository userSessionRepository,
                          JwtUtil jwtUtil,
                          PasswordUtil passwordUtil,
                          SessionUtil sessionUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtUtil = jwtUtil;
        this.passwordUtil = passwordUtil;
        this.sessionUtil = sessionUtil;
    }
    
    @Override
    public AuthResponse register(RegisterRequest request) {
        try {
            // 验证用户名是否已存在
            if (userRepository.existsByUsername(request.getUsername())) {
                return AuthResponse.failure("用户名已存在");
            }
            
            // 验证邮箱是否已存在
            if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                return AuthResponse.failure("邮箱已存在");
            }
            
            // 验证手机号是否已存在
            if (StringUtils.hasText(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
                return AuthResponse.failure("手机号已存在");
            }
            
            // 验证密码强度
            PasswordUtil.PasswordValidationResult passwordValidation = passwordUtil.validatePassword(request.getPassword());
            if (!passwordValidation.isValid()) {
                return AuthResponse.failure("密码强度不足: " + String.join(", ", passwordValidation.getErrors()));
            }
            
            // 创建用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordUtil.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setRealName(request.getRealName());
            user.setStatus(UserStatus.ACTIVE);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            user.setLoginCount(0L);
            
            user = userRepository.save(user);
            
            // 创建会话
            UserSession session = createUserSession(user, request.getIpAddress(), request.getUserAgent(), false);
            
            // 生成JWT令牌
            Set<String> roles = getUserRoles(user.getId());
            Set<String> permissions = getUserPermissions(user.getId());
            
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), session.getSessionId(), roles, permissions);
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), session.getSessionId());
            
            logger.info("用户注册成功: username={}, userId={}", user.getUsername(), user.getId());
            
            return AuthResponse.success(accessToken, refreshToken, session.getSessionId(), convertToUserInfo(user), new ArrayList<>(permissions), new ArrayList<>(roles));
            
        } catch (Exception e) {
            logger.error("用户注册失败: username={}", request.getUsername(), e);
            return AuthResponse.failure("注册失败");
        }
    }
    
    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            // 查找用户
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            if (!userOpt.isPresent()) {
                logger.warn("登录失败，用户不存在: username={}", request.getUsername());
                return AuthResponse.failure("用户名或密码错误");
            }
            
            User user = userOpt.get();
            
            // 检查用户状态
            if (user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("登录失败，用户状态异常: username={}, status={}", request.getUsername(), user.getStatus());
                return AuthResponse.failure("账户已被禁用或锁定");
            }
            
            // 验证密码
            if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
                logger.warn("登录失败，密码错误: username={}", request.getUsername());
                return AuthResponse.failure("用户名或密码错误");
            }
            
            // 更新用户登录信息
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(request.getIpAddress());
            user.setLoginCount(user.getLoginCount() + 1);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // 创建会话
            UserSession session = createUserSession(user, request.getIpAddress(), request.getUserAgent(), request.isRememberMe());
            
            // 生成JWT令牌
            Set<String> roles = getUserRoles(user.getId());
            Set<String> permissions = getUserPermissions(user.getId());
            
            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), session.getSessionId(), roles, permissions);
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), session.getSessionId());
            
            logger.info("用户登录成功: username={}, userId={}, sessionId={}", user.getUsername(), user.getId(), session.getSessionId());
            
            return AuthResponse.success(accessToken, refreshToken, session.getSessionId(), convertToUserInfo(user), new ArrayList<>(roles), new ArrayList<>(permissions));
            
        } catch (Exception e) {
            logger.error("用户登录失败: username={}", request.getUsername(), e);
            return AuthResponse.failure("登录失败");
        }
    }
    
    @Override
    public boolean logout(String sessionId) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionId(sessionId);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.setStatus(0); // 设置为非活跃状态
                session.setUpdatedAt(LocalDateTime.now());
                userSessionRepository.save(session);
                
                logger.info("用户登出成功: sessionId={}, userId={}", sessionId, session.getUser().getId());
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("用户登出失败: sessionId={}", sessionId, e);
            return false;
        }
    }
    
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // 验证刷新令牌
            if (!jwtUtil.validateToken(refreshToken)) {
                return AuthResponse.failure("刷新令牌无效");
            }
            
            // 从令牌中获取信息
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);
            String username = jwtUtil.getUsernameFromToken(refreshToken);
            String sessionId = jwtUtil.getSessionIdFromToken(refreshToken);
            
            // 验证会话
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionIdAndValid(sessionId, true);
            if (!sessionOpt.isPresent()) {
                return AuthResponse.failure("会话已过期");
            }
            
            // 验证用户
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent() || userOpt.get().getStatus() != UserStatus.ACTIVE) {
                return AuthResponse.failure("用户状态异常");
            }
            
            User user = userOpt.get();
            UserSession session = sessionOpt.get();
            
            // 更新会话最后访问时间
            session.setLastAccessedAt(LocalDateTime.now());
            userSessionRepository.save(session);
            
            // 生成新的访问令牌
            Set<String> roles = getUserRoles(user.getId());
            Set<String> permissions = getUserPermissions(user.getId());
            
            String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), session.getSessionId(), roles, permissions);
            
            logger.info("令牌刷新成功: userId={}, sessionId={}", userId, sessionId);
            
            return AuthResponse.success(newAccessToken, refreshToken, session.getSessionId(), convertToUserInfo(user), new ArrayList<>(permissions), new ArrayList<>(roles));
            
        } catch (Exception e) {
            logger.error("令牌刷新失败", e);
            return AuthResponse.failure("令牌刷新失败");
        }
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    @Override
    public UserDTO getUserDetails(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(this::convertToUserDTO).orElse(null);
    }
    
    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Override
    public User createUser(User user) {
        user.setPassword(passwordUtil.encode(user.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLoginCount(0L);
        
        return userRepository.save(user);
    }
    
    @Override
    public UserDTO createUser(RegisterRequest request) {
        // 验证用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证邮箱是否已存在
        if (StringUtils.hasText(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRealName(request.getRealName());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        
        User savedUser = userRepository.save(user);
        return convertToUserDTO(savedUser);
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    @Override
    public UserDTO updateUser(UserDTO userDTO) {
        // 查找现有用户
        Optional<User> userOpt = userRepository.findById(userDTO.getId());
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在: " + userDTO.getId());
        }
        
        User user = userOpt.get();
        
        // 更新用户信息
        if (userDTO.getUsername() != null) {
            user.setUsername(userDTO.getUsername());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getRealName() != null) {
            user.setRealName(userDTO.getRealName());
        }
        if (userDTO.getStatus() != null) {
            user.setStatus(UserStatus.values()[userDTO.getStatus()]);
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        
        return convertToUserDTO(updatedUser);
    }
    
    @Override
    public boolean deleteUser(Long userId) {
        try {
            if (userRepository.existsById(userId)) {
                userRepository.deleteById(userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("删除用户失败: userId={}", userId, e);
            return false;
        }
    }
    
    @Override
    public boolean enableUser(Long userId) {
        return updateUserStatus(userId, UserStatus.ACTIVE);
    }
    
    @Override
    public boolean disableUser(Long userId) {
        return updateUserStatus(userId, UserStatus.INACTIVE);
    }
    
    @Override
    public boolean lockUser(Long userId) {
        return updateUserStatus(userId, UserStatus.LOCKED);
    }
    
    @Override
    public boolean unlockUser(Long userId) {
        return updateUserStatus(userId, UserStatus.ACTIVE);
    }
    
    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // 验证旧密码
                if (!passwordUtil.matches(oldPassword, user.getPassword())) {
                    return false;
                }
                
                // 验证新密码强度
                PasswordUtil.PasswordValidationResult validation = passwordUtil.validatePassword(newPassword);
                if (!validation.isValid()) {
                    return false;
                }
                
                // 更新密码
                user.setPassword(passwordUtil.encode(newPassword));
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                
                logger.info("用户密码修改成功: userId={}", userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("修改密码失败: userId={}", userId, e);
            return false;
        }
    }
    
    // 删除重复的resetPassword方法，保留返回String的版本
    
    @Override
    public Set<String> getUserRoles(Long userId) {
        return roleRepository.findRoleNamesByUserId(userId);
    }

    @Override
    public Set<String> getUserPermissions(Long userId) {
        return permissionRepository.findPermissionNamesByUserId(userId);
    }
    
    @Override
    public boolean assignRole(Long userId, String roleName) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
            
            if (userOpt.isPresent() && roleOpt.isPresent()) {
                User user = userOpt.get();
                Role role = roleOpt.get();
                
                user.getRoles().add(role);
                userRepository.save(user);
                
                logger.info("用户角色分配成功: userId={}, roleName={}", userId, roleName);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("分配角色失败: userId={}, roleName={}", userId, roleName, e);
            return false;
        }
    }
    
    @Override
    public boolean removeRole(Long userId, String roleName) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
            
            if (userOpt.isPresent() && roleOpt.isPresent()) {
                User user = userOpt.get();
                Role role = roleOpt.get();
                
                user.getRoles().remove(role);
                userRepository.save(user);
                
                logger.info("用户角色移除成功: userId={}, roleName={}", userId, roleName);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("移除角色失败: userId={}, roleName={}", userId, roleName, e);
            return false;
        }
    }
    
    @Override
    public boolean hasPermission(Long userId, String permission) {
        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.contains(permission);
    }
    
    @Override
    public boolean hasRole(Long userId, String roleName) {
        Set<String> userRoles = getUserRoles(userId);
        return userRoles.contains(roleName);
    }
    
    @Override
    public boolean assignRoles(Long userId, Set<String> roleNames) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                for (String roleName : roleNames) {
                    Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
                    if (roleOpt.isPresent()) {
                        user.getRoles().add(roleOpt.get());
                    }
                }
                userRepository.save(user);
                logger.info("用户批量角色分配成功: userId={}, roleNames={}", userId, roleNames);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("批量分配角色失败: userId={}, roleNames={}", userId, roleNames, e);
            return false;
        }
    }
    
    @Override
    public boolean removeRoles(Long userId, Set<String> roleNames) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                for (String roleName : roleNames) {
                    Optional<Role> roleOpt = roleRepository.findByRoleName(roleName);
                    if (roleOpt.isPresent()) {
                        user.getRoles().remove(roleOpt.get());
                    }
                }
                userRepository.save(user);
                logger.info("用户批量角色移除成功: userId={}, roleNames={}", userId, roleNames);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("批量移除角色失败: userId={}, roleNames={}", userId, roleNames, e);
            return false;
        }
    }
    
    @Override
    public boolean hasAnyPermission(Long userId, Set<String> permissionNames) {
        Set<String> userPermissions = getUserPermissions(userId);
        return permissionNames.stream().anyMatch(userPermissions::contains);
    }
    
    @Override
    public boolean hasAllPermissions(Long userId, Set<String> permissionNames) {
        Set<String> userPermissions = getUserPermissions(userId);
        return userPermissions.containsAll(permissionNames);
    }
    
    @Override
    public Page<UserDTO> findUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserDTO);
    }
    
    @Override
    public PageResponse<UserDTO> findUsers(int page, int size, String username, String email, Boolean enabled) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;
        
        if (StringUtils.hasText(username) || StringUtils.hasText(email)) {
            // 如果有搜索条件，使用搜索方法
            String keyword = StringUtils.hasText(username) ? username : email;
            userPage = userRepository.findByUsernameContainingOrEmailContainingOrRealNameContaining(
                keyword, keyword, keyword, pageable);
        } else if (enabled != null) {
            // 根据启用状态查询
            int status = enabled ? 1 : 0;
            List<User> users = userRepository.findByStatus(status);
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), users.size());
            List<User> pageContent = users.subList(start, end);
            userPage = new PageImpl<>(pageContent, pageable, users.size());
        } else {
            // 查询所有用户
            userPage = userRepository.findAll(pageable);
        }
        
        List<UserDTO> userDTOs = userPage.getContent().stream()
            .map(this::convertToUserDTO)
            .collect(Collectors.toList());
            
        PageResponse<UserDTO> response = new PageResponse<>();
        response.setContent(userDTOs);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(userPage.getTotalElements());
        response.setTotalPages(userPage.getTotalPages());
        response.setFirst(userPage.isFirst());
        response.setLast(userPage.isLast());
        response.setHasNext(userPage.hasNext());
        response.setHasPrevious(userPage.hasPrevious());
        
        return response;
    }
    
    @Override
    public Page<UserDTO> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToUserDTO);
    }
    
    @Override
    public Page<UserDTO> findUsersByStatus(Integer status, Pageable pageable) {
        List<User> users = userRepository.findByStatus(status);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());
        List<User> pageContent = users.subList(start, end);
        return new PageImpl<>(pageContent, pageable, users.size()).map(this::convertToUserDTO);
    }
    
    @Override
    public Page<UserDTO> findUsersByRole(String roleName, Pageable pageable) {
        List<User> users = userRepository.findByRoleName(roleName);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());
        List<User> pageContent = users.subList(start, end);
        return new PageImpl<>(pageContent, pageable, users.size()).map(this::convertToUserDTO);
    }
    
    @Override
    public Page<UserDTO> searchUsers(String keyword, Pageable pageable) {
        return userRepository.findByUsernameContainingOrEmailContainingOrRealNameContaining(
                keyword, keyword, keyword, pageable).map(this::convertToUserDTO);
    }
    
    @Override
    public List<UserDTO> getRecentlyLoggedInUsers(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7); // 获取最近7天登录的用户
        List<User> users = userRepository.findRecentlyLoggedInUsers(since);
        return users.stream().limit(limit).map(this::convertToUserDTO).collect(Collectors.toList());
    }
    
    @Override
    public List<UserDTO> getOnlineUsers() {
        // 查找状态为活跃(1)的用户作为在线用户
        return userRepository.findByStatus(1).stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateLastLoginInfo(Long userId, String ipAddress, String userAgent) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.updateLastLoginTime(); // 使用User实体的方法
                // 注意：User实体中没有lastLoginIp字段，这里只更新登录时间
                userRepository.save(user);
                logger.info("更新用户最后登录信息成功: userId={}, ip={}", userId, ipAddress);
            }
        } catch (Exception e) {
            logger.error("更新用户最后登录信息失败: userId={}", userId, e);
        }
    }
    
    @Override
    public boolean validateCredentials(String usernameOrEmail, String password) {
        try {
            Optional<User> userOpt = findByUsernameOrEmail(usernameOrEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return passwordUtil.matches(password, user.getPassword());
            }
            return false;
        } catch (Exception e) {
            logger.error("验证用户凭据失败: usernameOrEmail={}", usernameOrEmail, e);
            return false;
        }
    }
    
    @Override
    public UserService.AccountStatus checkAccountStatus(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                UserStatus status = user.getStatus();
                if (status == UserStatus.INACTIVE) {
                    return UserService.AccountStatus.DISABLED;
                } else if (status == UserStatus.LOCKED) {
                    return UserService.AccountStatus.LOCKED;
                } else if (status == UserStatus.ACTIVE) {
                    return UserService.AccountStatus.ACTIVE;
                } else {
                    return UserService.AccountStatus.PENDING;
                }
            }
            return UserService.AccountStatus.PENDING;
        } catch (Exception e) {
            logger.error("检查用户账户状态失败: userId={}", userId, e);
            return UserService.AccountStatus.PENDING;
        }
    }
    
    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            return findByEmail(usernameOrEmail);
        } else {
            return findByUsername(usernameOrEmail);
        }
    }
    

    
    @Override
    public String resetPassword(Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String newPassword = passwordUtil.generateRandomPassword(12);
                user.setPassword(passwordUtil.encode(newPassword));
                userRepository.save(user);
                logger.info("重置用户密码成功: userId={}", userId);
                return newPassword;
            }
            return null;
        } catch (Exception e) {
            logger.error("重置用户密码失败: userId={}", userId, e);
            return null;
        }
    }
    
    @Override
    public boolean resetPasswordByEmail(String email) {
        try {
            Optional<User> userOpt = findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String newPassword = passwordUtil.generateRandomPassword(12);
                user.setPassword(passwordUtil.encode(newPassword));
                userRepository.save(user);
                // TODO: 发送邮件通知新密码
                logger.info("通过邮箱重置用户密码成功: email={}", email);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("通过邮箱重置用户密码失败: email={}", email, e);
            return false;
        }
    }
    
    @Override
    public UserStatistics getUserStatistics() {
        UserStatistics stats = new UserStatistics();
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(userRepository.countByStatus(UserStatus.ACTIVE));
        stats.setInactiveUsers(userRepository.countByStatus(UserStatus.INACTIVE));
        stats.setLockedUsers(userRepository.countByStatus(UserStatus.LOCKED));
        stats.setTodayRegistrations(userRepository.countByCreatedAtAfter(LocalDateTime.now().toLocalDate().atStartOfDay()));
        stats.setOnlineUsers(userSessionRepository.countByValidAndExpiresAtAfter(true, LocalDateTime.now()));
        return stats;
    }
    
    /**
     * 创建用户会话
     */
    private UserSession createUserSession(User user, String ipAddress, String userAgent, boolean rememberMe) {
        UserSession session = new UserSession();
        session.setSessionId(sessionUtil.generateSessionId());
        session.setUser(user);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setStatus(1); // 设置为活跃状态
        session.setCreatedAt(LocalDateTime.now());
        session.setLastAccessedAt(LocalDateTime.now());
        
        // 设置过期时间
        int expirationMinutes = rememberMe ? 30 * 24 * 60 : 24 * 60; // 记住我30天，否则1天
        session.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        
        return userSessionRepository.save(session);
    }
    
    /**
     * 更新用户状态
     */
    private boolean updateUserStatus(Long userId, UserStatus status) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setStatus(status);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                
                logger.info("用户状态更新成功: userId={}, status={}", userId, status);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("更新用户状态失败: userId={}, status={}", userId, status, e);
            return false;
        }
    }
    
    /**
     * 转换User实体为UserDTO
     */
    private UserDTO convertToUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRealName(user.getRealName());
        dto.setStatus(user.getStatus().ordinal()); // 转换为Integer
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setLastLoginIp(null); // User实体中没有此字段，设置为null
        dto.setLoginCount(user.getLoginCount().intValue()); // Long转Integer
        
        // 设置角色和权限
        dto.setRoles(new ArrayList<>(getUserRoles(user.getId())));
        dto.setPermissions(new ArrayList<>(getUserPermissions(user.getId())));
        
        // 设置在线状态和会话数量
        long validSessionCount = userSessionRepository.countByUserIdAndValidAndExpiresAtAfter(
                user.getId(), true, LocalDateTime.now());
        dto.setOnline(validSessionCount > 0);
        dto.setSessionCount((int) validSessionCount);
        
        return dto;
    }
    
    /**
     * 转换User实体为AuthResponse.UserInfo
     */
    private AuthResponse.UserInfo convertToUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .status(user.getStatus().ordinal())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}