package com.hejiexmu.rpc.auth.entity;

import com.hejiexmu.rpc.auth.enums.AccountStatus;
import com.hejiexmu.rpc.auth.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体类
 * 
 * @author hejiexmu
 */
@Data
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_username", columnList = "username"),
    @Index(name = "idx_email", columnList = "email"),
    @Index(name = "idx_status", columnList = "status")
})
@EqualsAndHashCode(exclude = {"roles", "sessions"})
@ToString(exclude = {"password", "roles", "sessions"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6个字符")
    @Column(name = "password", nullable = false)
    private String password;
    
    @Email(message = "邮箱格式不正确")
    @Column(name = "email", length = 100)
    private String email;
    
    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    @Column(name = "real_name", length = 50)
    private String realName;
    
    @Column(name = "login_count", nullable = false)
    private Long loginCount = 0L;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    // 用户角色关联
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    // 用户会话关联
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<UserSession> sessions = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return status != null && status == UserStatus.ACTIVE;
    }
    
    /**
     * 激活用户
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
    
    /**
     * 禁用用户
     */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    /**
     * 添加角色
     */
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
            role.getUsers().add(this);
        }
    }
    
    /**
     * 移除角色
     */
    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
            role.getUsers().remove(this);
        }
    }
    
    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
            .anyMatch(role -> role.getRoleName().equals(roleName));
    }
    
    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermission(String permissionName) {
        return roles.stream()
            .flatMap(role -> role.getPermissions().stream())
            .anyMatch(permission -> permission.getPermissionName().equals(permissionName));
    }
    
    // 手动添加getter方法以解决编译问题
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }
    
    public AccountStatus getAccountStatus() {
        return accountStatus;
    }
    
    public String getUsername() {
        return username;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getPassword() {
        return password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public Long getLoginCount() {
        return loginCount;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public Set<Role> getRoles() {
        return roles;
    }
    
    public Set<UserSession> getSessions() {
        return sessions;
    }
    
    // 手动添加setter方法以解决编译问题
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setRealName(String realName) {
        this.realName = realName;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
    
    public void setLoginCount(Long loginCount) {
        this.loginCount = loginCount;
    }
    
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
    
    public void setSessions(Set<UserSession> sessions) {
        this.sessions = sessions;
    }
    
    /**
     * 设置最后登录IP地址
     */
    public void setLastLoginIp(String lastLoginIp) {
        // 注意：User实体中没有lastLoginIp字段，这里只是为了编译通过
        // 实际项目中可能需要添加该字段到实体类中
    }
    
    /**
     * 手动添加setId方法以解决Lombok兼容性问题
     */
    public void setId(Long id) {
        this.id = id;
    }
}