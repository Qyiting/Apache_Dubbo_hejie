# Simple RPC Framework API 文档

## 概述

本文档描述了 Simple RPC Framework 认证服务的 REST API 接口。所有 API 都遵循 RESTful 设计原则，使用 JSON 格式进行数据交换。

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **字符编码**: `UTF-8`
- **API 版本**: `v1`

## 认证机制

### JWT Token 认证

大部分 API 需要在请求头中包含有效的 JWT Token：

```http
Authorization: Bearer <your-jwt-token>
```

### 会话认证

部分 API 支持会话认证，需要在请求头中包含会话 ID：

```http
X-Session-Id: <your-session-id>
```

## 响应格式

### 成功响应

```json
{
  "success": true,
  "data": {
    // 响应数据
  },
  "message": "操作成功",
  "timestamp": "2024-01-20T10:30:00Z"
}
```

### 错误响应

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": "详细错误信息"
  },
  "timestamp": "2024-01-20T10:30:00Z"
}
```

## 错误码说明

| 错误码 | HTTP状态码 | 描述 |
|--------|------------|------|
| `AUTH_001` | 401 | 未授权访问 |
| `AUTH_002` | 401 | Token 已过期 |
| `AUTH_003` | 401 | Token 无效 |
| `AUTH_004` | 403 | 权限不足 |
| `AUTH_005` | 400 | 用户名或密码错误 |
| `AUTH_006` | 400 | 用户已存在 |
| `AUTH_007` | 404 | 用户不存在 |
| `AUTH_008` | 400 | 密码强度不足 |
| `AUTH_009` | 429 | 请求过于频繁 |
| `SYS_001` | 500 | 系统内部错误 |
| `SYS_002` | 503 | 服务暂时不可用 |

---

## 认证相关 API

### 1. 用户注册

**接口描述**: 注册新用户账户

**请求信息**:
- **URL**: `POST /auth/register`
- **认证**: 无需认证

**请求参数**:

```json
{
  "username": "string",     // 用户名，3-20字符，字母数字下划线
  "password": "string",     // 密码，8-50字符，需包含大小写字母、数字、特殊字符
  "email": "string",        // 邮箱地址
  "phone": "string",        // 手机号码（可选）
  "nickname": "string"      // 昵称（可选）
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "testuser",
    "email": "test@example.com",
    "status": "ACTIVE",
    "createdAt": "2024-01-20T10:30:00Z"
  },
  "message": "用户注册成功"
}
```

**错误响应**:

```json
{
  "success": false,
  "error": {
    "code": "AUTH_006",
    "message": "用户名已存在",
    "details": "用户名 'testuser' 已被注册"
  }
}
```

### 2. 用户登录

**接口描述**: 用户登录获取访问令牌

**请求信息**:
- **URL**: `POST /auth/login`
- **认证**: 无需认证

**请求参数**:

```json
{
  "username": "string",     // 用户名或邮箱
  "password": "string"      // 密码
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "sessionId": "sess_1234567890",
    "user": {
      "userId": 12345,
      "username": "testuser",
      "email": "test@example.com",
      "roles": ["USER"],
      "permissions": ["READ_PROFILE", "UPDATE_PROFILE"]
    }
  },
  "message": "登录成功"
}
```

### 3. 刷新令牌

**接口描述**: 使用刷新令牌获取新的访问令牌

**请求信息**:
- **URL**: `POST /auth/refresh`
- **认证**: 需要有效的刷新令牌

**请求参数**:

```json
{
  "refreshToken": "string"   // 刷新令牌
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "message": "令牌刷新成功"
}
```

### 4. 用户登出

**接口描述**: 用户登出，使令牌失效

**请求信息**:
- **URL**: `POST /auth/logout`
- **认证**: 需要有效的访问令牌或会话ID

**请求参数**:

```json
{
  "sessionId": "string"      // 会话ID（可选，如果提供则登出指定会话）
}
```

**响应示例**:

```json
{
  "success": true,
  "data": null,
  "message": "登出成功"
}
```

### 5. 验证令牌

**接口描述**: 验证访问令牌的有效性

**请求信息**:
- **URL**: `POST /auth/validate`
- **认证**: 需要有效的访问令牌

**请求参数**:

```json
{
  "token": "string"          // 要验证的令牌
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "valid": true,
    "userId": 12345,
    "username": "testuser",
    "roles": ["USER"],
    "permissions": ["READ_PROFILE", "UPDATE_PROFILE"],
    "expiresAt": "2024-01-21T10:30:00Z"
  },
  "message": "令牌验证成功"
}
```

---

## 用户管理 API

### 1. 获取用户信息

**接口描述**: 获取当前用户的详细信息

**请求信息**:
- **URL**: `GET /users/profile`
- **认证**: 需要有效的访问令牌

**响应示例**:

```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "testuser",
    "email": "test@example.com",
    "phone": "+86-13800138000",
    "nickname": "测试用户",
    "avatar": "https://example.com/avatar.jpg",
    "status": "ACTIVE",
    "roles": ["USER"],
    "permissions": ["READ_PROFILE", "UPDATE_PROFILE"],
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-20T10:30:00Z",
    "lastLoginAt": "2024-01-20T09:00:00Z"
  },
  "message": "获取用户信息成功"
}
```

### 2. 更新用户信息

**接口描述**: 更新当前用户的信息

**请求信息**:
- **URL**: `PUT /users/profile`
- **认证**: 需要有效的访问令牌

**请求参数**:

```json
{
  "email": "string",         // 邮箱地址（可选）
  "phone": "string",         // 手机号码（可选）
  "nickname": "string",      // 昵称（可选）
  "avatar": "string"         // 头像URL（可选）
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "username": "testuser",
    "email": "newemail@example.com",
    "phone": "+86-13900139000",
    "nickname": "新昵称",
    "updatedAt": "2024-01-20T10:35:00Z"
  },
  "message": "用户信息更新成功"
}
```

### 3. 修改密码

**接口描述**: 修改当前用户的密码

**请求信息**:
- **URL**: `PUT /users/password`
- **认证**: 需要有效的访问令牌

**请求参数**:

```json
{
  "currentPassword": "string",  // 当前密码
  "newPassword": "string"       // 新密码
}
```

**响应示例**:

```json
{
  "success": true,
  "data": null,
  "message": "密码修改成功"
}
```

---

## 权限管理 API

### 1. 检查权限

**接口描述**: 检查用户是否具有指定权限

**请求信息**:
- **URL**: `POST /permissions/check`
- **认证**: 需要有效的访问令牌

**请求参数**:

```json
{
  "permission": "string",    // 权限名称
  "resource": "string"       // 资源标识（可选）
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "hasPermission": true,
    "permission": "READ_PROFILE",
    "resource": "user:12345",
    "userId": 12345,
    "checkedAt": "2024-01-20T10:30:00Z"
  },
  "message": "权限检查完成"
}
```

### 2. 获取用户权限列表

**接口描述**: 获取当前用户的所有权限

**请求信息**:
- **URL**: `GET /permissions/list`
- **认证**: 需要有效的访问令牌

**响应示例**:

```json
{
  "success": true,
  "data": {
    "userId": 12345,
    "roles": [
      {
        "roleId": 1,
        "roleName": "USER",
        "description": "普通用户"
      }
    ],
    "permissions": [
      {
        "permissionId": 1,
        "permissionName": "READ_PROFILE",
        "description": "读取用户资料",
        "resource": "user"
      },
      {
        "permissionId": 2,
        "permissionName": "UPDATE_PROFILE",
        "description": "更新用户资料",
        "resource": "user"
      }
    ]
  },
  "message": "获取权限列表成功"
}
```

---

## 会话管理 API

### 1. 获取活跃会话列表

**接口描述**: 获取当前用户的所有活跃会话

**请求信息**:
- **URL**: `GET /sessions/active`
- **认证**: 需要有效的访问令牌

**响应示例**:

```json
{
  "success": true,
  "data": {
    "sessions": [
      {
        "sessionId": "sess_1234567890",
        "userId": 12345,
        "deviceInfo": "Chrome 120.0 on Windows 10",
        "ipAddress": "192.168.1.100",
        "location": "北京市",
        "createdAt": "2024-01-20T09:00:00Z",
        "lastAccessAt": "2024-01-20T10:30:00Z",
        "expiresAt": "2024-01-20T11:00:00Z",
        "current": true
      },
      {
        "sessionId": "sess_0987654321",
        "userId": 12345,
        "deviceInfo": "Safari 17.0 on iPhone",
        "ipAddress": "192.168.1.101",
        "location": "上海市",
        "createdAt": "2024-01-19T15:00:00Z",
        "lastAccessAt": "2024-01-19T18:30:00Z",
        "expiresAt": "2024-01-19T19:00:00Z",
        "current": false
      }
    ],
    "total": 2
  },
  "message": "获取会话列表成功"
}
```

### 2. 终止指定会话

**接口描述**: 终止指定的用户会话

**请求信息**:
- **URL**: `DELETE /sessions/{sessionId}`
- **认证**: 需要有效的访问令牌

**路径参数**:
- `sessionId`: 要终止的会话ID

**响应示例**:

```json
{
  "success": true,
  "data": {
    "sessionId": "sess_0987654321",
    "terminatedAt": "2024-01-20T10:35:00Z"
  },
  "message": "会话终止成功"
}
```

### 3. 终止所有其他会话

**接口描述**: 终止当前用户的所有其他会话（保留当前会话）

**请求信息**:
- **URL**: `DELETE /sessions/others`
- **认证**: 需要有效的访问令牌

**响应示例**:

```json
{
  "success": true,
  "data": {
    "terminatedCount": 3,
    "currentSessionId": "sess_1234567890",
    "terminatedAt": "2024-01-20T10:35:00Z"
  },
  "message": "其他会话终止成功"
}
```

---

## 系统管理 API

### 1. 健康检查

**接口描述**: 检查系统健康状态

**请求信息**:
- **URL**: `GET /system/health`
- **认证**: 无需认证

**响应示例**:

```json
{
  "success": true,
  "data": {
    "status": "UP",
    "components": {
      "database": {
        "status": "UP",
        "details": {
          "connectionPool": "healthy",
          "responseTime": "5ms"
        }
      },
      "redis": {
        "status": "UP",
        "details": {
          "cluster": "healthy",
          "responseTime": "2ms"
        }
      },
      "diskSpace": {
        "status": "UP",
        "details": {
          "free": "15GB",
          "threshold": "10GB"
        }
      }
    },
    "timestamp": "2024-01-20T10:30:00Z"
  },
  "message": "系统健康"
}
```

### 2. 系统信息

**接口描述**: 获取系统基本信息

**请求信息**:
- **URL**: `GET /system/info`
- **认证**: 需要管理员权限

**响应示例**:

```json
{
  "success": true,
  "data": {
    "application": {
      "name": "Simple RPC Framework Auth Service",
      "version": "1.0.0",
      "buildTime": "2024-01-15T08:00:00Z",
      "profile": "production"
    },
    "system": {
      "javaVersion": "17.0.2",
      "osName": "Linux",
      "osVersion": "5.4.0",
      "cpuCores": 4,
      "totalMemory": "8GB",
      "freeMemory": "2GB"
    },
    "database": {
      "type": "MySQL",
      "version": "8.0.33",
      "connectionPool": {
        "active": 5,
        "idle": 10,
        "max": 20
      }
    },
    "redis": {
      "version": "6.2.7",
      "mode": "cluster",
      "nodes": 6
    }
  },
  "message": "获取系统信息成功"
}
```

---

## 请求示例

### cURL 示例

#### 用户注册

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!",
    "email": "test@example.com",
    "nickname": "测试用户"
  }'
```

#### 用户登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "TestPass123!"
  }'
```

#### 获取用户信息

```bash
curl -X GET http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

#### 检查权限

```bash
curl -X POST http://localhost:8080/api/permissions/check \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "permission": "READ_PROFILE",
    "resource": "user:12345"
  }'
```

### JavaScript 示例

```javascript
// 用户登录
const login = async (username, password) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  
  const result = await response.json();
  if (result.success) {
    localStorage.setItem('accessToken', result.data.accessToken);
    localStorage.setItem('refreshToken', result.data.refreshToken);
    return result.data;
  } else {
    throw new Error(result.error.message);
  }
};

// 获取用户信息
const getUserProfile = async () => {
  const token = localStorage.getItem('accessToken');
  const response = await fetch('http://localhost:8080/api/users/profile', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  const result = await response.json();
  if (result.success) {
    return result.data;
  } else {
    throw new Error(result.error.message);
  }
};

// 刷新令牌
const refreshToken = async () => {
  const refreshToken = localStorage.getItem('refreshToken');
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  
  const result = await response.json();
  if (result.success) {
    localStorage.setItem('accessToken', result.data.accessToken);
    localStorage.setItem('refreshToken', result.data.refreshToken);
    return result.data;
  } else {
    throw new Error(result.error.message);
  }
};
```

---

## 最佳实践

### 1. 安全建议

- **HTTPS**: 生产环境必须使用 HTTPS
- **Token 存储**: 客户端应安全存储访问令牌，避免 XSS 攻击
- **刷新令牌**: 定期刷新访问令牌，避免长期有效的令牌
- **权限检查**: 每次 API 调用都应进行权限验证
- **输入验证**: 客户端和服务端都应进行输入验证

### 2. 性能优化

- **缓存**: 合理使用缓存减少数据库查询
- **分页**: 大量数据查询使用分页
- **压缩**: 启用 HTTP 压缩减少传输大小
- **连接池**: 合理配置数据库连接池

### 3. 错误处理

- **统一格式**: 使用统一的错误响应格式
- **错误码**: 使用明确的错误码便于客户端处理
- **日志记录**: 记录详细的错误日志便于排查
- **用户友好**: 向用户显示友好的错误信息

### 4. 监控和日志

- **API 监控**: 监控 API 响应时间和错误率
- **访问日志**: 记录所有 API 访问日志
- **安全日志**: 记录登录、权限检查等安全相关操作
- **性能指标**: 监控系统性能指标

---

## 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.0.0 | 2024-01-20 | 初始版本，包含基础认证和用户管理功能 |

---

## 联系支持

如果在使用 API 过程中遇到问题，请：

1. 查看错误响应中的错误码和描述
2. 检查请求格式和参数
3. 查看系统日志
4. 联系开发团队获取支持

**注意**: 本 API 文档适用于 Simple RPC Framework v1.0.0，不同版本可能存在差异，请参考对应版本的文档。