package com.rpc.example.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class User implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 用户ID */
    private Long id;
    
    /** 用户名 */
    private String username;
    
    /** 密码 */
    private String password;
    
    /** 邮箱 */
    private String email;
    
    /** 手机号 */
    private String phone;
    
    /** 年龄 */
    private Integer age;
    
    /** 性别 */
    private String gender;
    
    /** 地址 */
    private String address;
    
    /** 状态 */
    private String status;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /**
     * 默认构造函数
     */
    public User() {
    }
    
    /**
     * 构造函数
     * 
     * @param username 用户名
     * @param email 邮箱
     * @param age 年龄
     */
    public User(String username, String email, Integer age) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.status = "ACTIVE";
    }
    
    /**
     * 完整构造函数
     * 
     * @param id 用户ID
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param phone 手机号
     * @param age 年龄
     * @param gender 性别
     * @param address 地址
     * @param status 状态
     */
    public User(Long id, String username, String password, String email, 
                String phone, Integer age, String gender, String address, String status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.age = age;
        this.gender = gender;
        this.address = address;
        this.status = status;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 检查用户是否有效
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               age != null && age > 0 && age < 150;
    }
    
    /**
     * 检查用户是否激活
     * 
     * @return 是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
    
    /**
     * 激活用户
     */
    public void activate() {
        this.status = "ACTIVE";
        updateTimestamp();
    }
    
    /**
     * 停用用户
     */
    public void deactivate() {
        this.status = "INACTIVE";
        updateTimestamp();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(username, user.username) &&
               Objects.equals(email, user.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                ", address='" + address + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
    
    /**
     * 创建用户构建器
     * 
     * @return 用户构建器
     */
    public static UserBuilder builder() {
        return new UserBuilder();
    }
    
    /**
     * 用户构建器
     */
    public static class UserBuilder {
        private Long id;
        private String username;
        private String password;
        private String email;
        private String phone;
        private Integer age;
        private String gender;
        private String address;
        private String status = "ACTIVE";
        
        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }
        
        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public UserBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public UserBuilder age(Integer age) {
            this.age = age;
            return this;
        }
        
        public UserBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }
        
        public UserBuilder address(String address) {
            this.address = address;
            return this;
        }
        
        public UserBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public User build() {
            User user = new User();
            user.setId(this.id);
            user.setUsername(this.username);
            user.setPassword(this.password);
            user.setEmail(this.email);
            user.setPhone(this.phone);
            user.setAge(this.age);
            user.setGender(this.gender);
            user.setAddress(this.address);
            user.setStatus(this.status);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            return user;
        }
    }
}