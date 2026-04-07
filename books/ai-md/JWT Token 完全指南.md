# JWT Token 完全指南：从原理到实战

> Token 是现代 Web 认证的核心，理解它的存储、使用、来源和生成，是后端开发的必备技能。

---

## 📖 目录

1. [Token 是什么？](#1-token-是什么)
2. [Token 的来源与生成](#2-token-的来源与生成)
3. [Token 的存储策略](#3-token-的存储策略)
4. [Token 的使用流程](#4-token-的使用流程)
5. [Token 刷新机制](#5-token-刷新机制)
6. [完整实战案例](#6-完整实战案例)
7. [安全最佳实践](#7-安全最佳实践)
8. [常见问题解答](#8-常见问题解答)

---

## 1. Token 是什么？

### 🤔 通俗解释

**Token 就像酒店的房卡：**

```
传统 Session（像前台登记）：
1. 你入住时，前台记录你的信息（Session 存服务器）
2. 给你一个房间号（Session ID 存 Cookie）
3. 每次出门回来，前台查记录确认你是谁
4. 问题：人多了，前台记不住（服务器压力大）

Token（像房卡）：
1. 你入住时，给你一张加密的房卡（Token）
2. 房卡里有你的信息（用户 ID、过期时间等）
3. 每次刷卡，门锁自己验证（服务端无状态）
4. 好处：前台不用记任何人（服务器无状态，易扩展）
```

### 🎯 Token 的类型

| 类型 | 说明 | 特点 |
|------|------|------|
| **JWT (JSON Web Token)** | 最常用的 Token 格式 | 自包含、可验证、无状态 |
| **Opaque Token** | 不透明 Token | 需要在服务端查询验证 |
| **Refresh Token** | 刷新 Token | 长期有效，用于获取新 Access Token |

**本文重点讲解 JWT Token。**

---

## 2. Token 的来源与生成

### 🔄 Token 的完整生命周期

```
1. 用户登录
   ↓
2. 服务端验证用户名密码
   ↓
3. 生成 Token（Access Token + Refresh Token）
   ↓
4. 返回给前端
   ↓
5. 前端存储 Token
   ↓
6. 后续请求携带 Token
   ↓
7. 服务端验证 Token
   ↓
8. Token 过期 → 使用 Refresh Token 刷新
   ↓
9. Refresh Token 过期 → 重新登录
```

---

### 🔐 Token 生成方法

#### 2.1 JWT 的结构

JWT 由三部分组成，用 `.` 分隔：

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

┌─────────────────────────────────┐ ┌──────────────────────────────────┐ ┌─────────────────────────────┐
│         Header (头部)            │ │         Payload (载荷)            │ │      Signature (签名)        │
│                                 │ │                                   │ │                              │
│ {                               │ │ {                                 │ │ HMACSHA256(                  │
│   "alg": "HS256",               │ │   "sub": "1234567890",            │ │   base64UrlEncode(header) +  │
│   "typ": "JWT"                  │ │   "name": "John Doe",             │ │   base64UrlEncode(payload),  │
│ }                               │ │   "iat": 1516239022,              │ │   secret                     │
│                                 │ │   "exp": 1516242622               │ │ )                            │
│ Base64Url 编码                  │ │ }                                 │ │                              │
│                                 │ │ Base64Url 编码                    │ │ 防止篡改                     │
└─────────────────────────────────┘ └──────────────────────────────────┘ └─────────────────────────────┘
```

**三部分说明：**

1. **Header（头部）**
   - 算法类型（alg）：HS256、RS256 等
   - Token 类型（typ）：JWT

2. **Payload（载荷）**
   - 标准字段：sub（主题）、iat（签发时间）、exp（过期时间）
   - 自定义字段：userId、username、roles 等
   - ⚠️ 注意：Base64 编码，不是加密，任何人都能解码看到内容

3. **Signature（签名）**
   - 使用密钥对 Header 和 Payload 进行签名
   - 防止 Token 被篡改
   - 服务端验证签名确保 Token 真实性

---

#### 2.2 Java 中生成 JWT Token

##### 依赖配置

```xml
<!-- JJWT (Java JWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
```

##### 工具类实现

```java
@Component
@Slf4j
public class JwtTokenUtil {
    
    // 密钥（生产环境应该从配置文件读取）
    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough}")
    private String secret;
    
    // Access Token 过期时间（默认 2 小时）
    @Value("${jwt.access-token-expiration:7200}")
    private long accessTokenExpiration;
    
    // Refresh Token 过期时间（默认 7 天）
    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;
    
    /**
     * 生成 Access Token
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        // 添加自定义 claims
        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            claims.put("userId", customUserDetails.getUserId());
            claims.put("username", customUserDetails.getUsername());
            claims.put("roles", customUserDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        }
        
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }
    
    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");  // 标记为刷新 Token
        
        if (userDetails instanceof CustomUserDetails) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
            claims.put("userId", customUserDetails.getUserId());
        }
        
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }
    
    /**
     * 创建 Token
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }
    
    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }
    
    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token 验证失败：{}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查 Token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * 解析 Token 获取 Claims
     */
    private Claims getClaimsFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

##### 配置文件

```yaml
# application.yml
jwt:
  secret: mySecretKeyForJWTTokenGenerationAndValidationMustBeLongEnough
  access-token-expiration: 7200  # 2 小时（秒）
  refresh-token-expiration: 604800  # 7 天（秒）
```

---

#### 2.3 登录接口生成 Token

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        
        // 1. 验证用户名密码
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );
        
        // 2. 获取用户信息
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        
        // 3. 生成 Token
        String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
        
        // 4. 保存 Refresh Token 到数据库或 Redis
        refreshTokenService.saveRefreshToken(
            userDetails.getUserId(),
            refreshToken,
            request.getDeviceInfo()
        );
        
        // 5. 构建响应
        LoginResponse response = LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenUtil.getAccessTokenExpiration())
            .userInfo(UserInfo.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()))
                .build())
            .build();
        
        return ApiResponse.success("登录成功", response);
    }
}
```

**登录响应示例：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 1,
      "username": "zhangsan",
      "email": "zhang@example.com",
      "roles": ["USER", "ADMIN"]
    }
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

---

## 3. Token 的存储策略

### 🎯 核心原则

**Token 存储需要考虑三个因素：**
1. **安全性** - 防止 XSS、CSRF 攻击
2. **便利性** - 自动携带、易于管理
3. **持久性** - 刷新页面后仍然有效

---

### 📦 前端存储方案对比

| 存储方式 | 安全性 | 便利性 | 持久性 | 推荐场景 |
|---------|-------|-------|-------|---------|
| **localStorage** | ⚠️ 中等（XSS 风险） | ✅ 高 | ✅ 持久 | 大多数场景 |
| **sessionStorage** | ⚠️ 中等（XSS 风险） | ✅ 高 | ❌ 会话级 | 临时会话 |
| **HttpOnly Cookie** | ✅ 高（防 XSS） | ⚠️ 中（需配置） | ✅ 可配置 | 高安全要求 |
| **Memory** | ✅ 高 | ❌ 低 | ❌ 刷新丢失 | 配合其他方案 |

---

### 💡 推荐方案：双 Token + 混合存储

#### 方案架构

```
Access Token（短期，2 小时）
  ↓
存储在 Memory（内存）中
  ↓
每次请求从内存读取，失效后用 Refresh Token 刷新

Refresh Token（长期，7 天）
  ↓
存储在 HttpOnly Cookie 中
  ↓
防止 XSS 攻击，自动携带
```

#### 前端实现

```javascript
// auth.js - 认证工具类

class AuthService {
    constructor() {
        this.accessToken = null;  // 内存存储 Access Token
        this.userInfo = null;
    }
    
    /**
     * 登录
     */
    async login(username, password) {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        if (!response.ok) {
            throw new Error('登录失败');
        }
        
        const data = await response.json();
        
        // 存储 Access Token 到内存
        this.accessToken = data.data.accessToken;
        this.userInfo = data.data.userInfo;
        
        // Refresh Token 已通过 HttpOnly Cookie 自动存储
        // 前端无法访问，但浏览器会自动携带
        
        return data.data;
    }
    
    /**
     * 获取 Access Token
     */
    getAccessToken() {
        return this.accessToken;
    }
    
    /**
     * 检查是否已登录
     */
    isAuthenticated() {
        return this.accessToken !== null;
    }
    
    /**
     * 刷新 Token
     */
    async refreshToken() {
        try {
            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                credentials: 'include'  // 自动携带 Cookie
            });
            
            if (!response.ok) {
                throw new Error('刷新失败');
            }
            
            const data = await response.json();
            
            // 更新内存中的 Access Token
            this.accessToken = data.data.accessToken;
            
            return this.accessToken;
        } catch (error) {
            // 刷新失败，清除登录状态
            this.logout();
            throw error;
        }
    }
    
    /**
     * 退出登录
     */
    async logout() {
        // 调用后端注销接口
        await fetch('/api/auth/logout', {
            method: 'POST',
            credentials: 'include'
        });
        
        // 清除本地状态
        this.accessToken = null;
        this.userInfo = null;
        
        // 跳转到登录页
        window.location.href = '/login';
    }
}

export default new AuthService();
```

#### Axios 拦截器自动携带 Token

```javascript
// request.js
import axios from 'axios';
import authService from './auth';

const instance = axios.create({
    baseURL: '/api',
    timeout: 10000
});

// 请求拦截器：自动添加 Token
instance.interceptors.request.use(
    config => {
        const token = authService.getAccessToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

// 响应拦截器：处理 Token 过期
instance.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;
        
        // 如果 401 且未重试过
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            
            try {
                // 尝试刷新 Token
                await authService.refreshToken();
                
                // 重试原请求
                const newToken = authService.getAccessToken();
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return instance(originalRequest);
            } catch (refreshError) {
                // 刷新失败，跳转登录
                authService.logout();
                return Promise.reject(refreshError);
            }
        }
        
        return Promise.reject(error);
    }
);

export default instance;
```

#### 后端配置 HttpOnly Cookie

```java
@Component
public class CookieUtil {
    
    /**
     * 设置 Refresh Token Cookie
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
            .httpOnly(true)          // 禁止 JavaScript 访问
            .secure(true)            // 仅 HTTPS 传输
            .path("/api/auth/refresh")  // 只在刷新接口携带
            .maxAge(7 * 24 * 60 * 60)   // 7 天
            .sameSite("Strict")      // 防止 CSRF
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    
    /**
     * 清除 Refresh Token Cookie
     */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(true)
            .path("/api/auth/refresh")
            .maxAge(0)  // 立即过期
            .sameSite("Strict")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

---

### 🔒 各存储方式详解

#### 3.1 localStorage（常用但不最安全）

```javascript
// 存储
localStorage.setItem('accessToken', token);
localStorage.setItem('userInfo', JSON.stringify(userInfo));

// 读取
const token = localStorage.getItem('accessToken');
const userInfo = JSON.parse(localStorage.getItem('userInfo'));

// 删除
localStorage.removeItem('accessToken');
localStorage.clear();
```

**优点：**
- ✅ 简单易用
- ✅ 持久化存储
- ✅ 容量大（5-10MB）

**缺点：**
- ❌ 容易受到 XSS 攻击
- ❌ 需要手动添加到请求头

**适用场景：** 对安全性要求不高的内部系统

---

#### 3.2 HttpOnly Cookie（最安全）

```java
// 后端设置 Cookie
ResponseCookie cookie = ResponseCookie.from("access_token", token)
    .httpOnly(true)    // JavaScript 无法访问
    .secure(true)      // 仅 HTTPS
    .path("/")
    .maxAge(7200)
    .sameSite("Lax")
    .build();

response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

**优点：**
- ✅ 防止 XSS 攻击（JavaScript 无法访问）
- ✅ 自动携带（无需手动添加）
- ✅ 可配置 SameSite 防止 CSRF

**缺点：**
- ❌ 需要配置 CORS
- ❌ 跨域复杂
- ❌ 无法在移动端 App 中使用

**适用场景：** 高安全性要求的 Web 应用

---

#### 3.3 Memory（最安全但不持久）

```javascript
// 存储在变量中
let accessToken = null;

// 登录后赋值
function login(token) {
    accessToken = token;
}

// 请求时使用
fetch('/api/data', {
    headers: {
        'Authorization': `Bearer ${accessToken}`
    }
});
```

**优点：**
- ✅ 最安全（页面关闭即消失）
- ✅ 不会被窃取

**缺点：**
- ❌ 刷新页面后丢失
- ❌ 需要配合 Refresh Token

**适用场景：** 配合 Refresh Token 使用

---

## 4. Token 的使用流程

### 🔄 完整的认证流程

#### 4.1 登录流程

```
前端                          后端
  |                             |
  |--- 1. 提交用户名密码 ------>|
  |                             |--- 验证用户名密码
  |                             |--- 生成 Access Token
  |                             |--- 生成 Refresh Token
  |                             |--- 保存 Refresh Token
  |                             |
  |<-- 2. 返回 Token ----------|
  |                             |
  |--- 3. 存储 Token ----------|
  |    Access Token → Memory   |
  |    Refresh Token → Cookie  |
  |                             |
```

#### 4.2 请求认证流程

```
前端                          后端
  |                             |
  |--- 1. 发起请求 ------------>|
  |    Header:                  |
  |    Authorization:           |
  |    Bearer <token>           |
  |                             |--- JWT 过滤器拦截
  |                             |--- 解析 Token
  |                             |--- 验证签名
  |                             |--- 检查是否过期
  |                             |--- 提取用户信息
  |                             |--- 放入 SecurityContext
  |                             |
  |<-- 2. 返回业务数据 ---------|
  |                             |
```

#### 4.3 Token 刷新流程

```
前端                          后端
  |                             |
  |--- 1. 请求接口 ------------>|
  |                             |--- Token 过期
  |                             |--- 返回 401
  |                             |
  |<-- 2. 401 Unauthorized ----|
  |                             |
  |--- 3. 刷新 Token ---------->|
  |    POST /api/auth/refresh   |--- 从 Cookie 读取 Refresh Token
  |    (自动携带 Cookie)        |--- 验证 Refresh Token
  |                             |--- 生成新的 Access Token
  |                             |--- 可选：轮换 Refresh Token
  |                             |
  |<-- 4. 新的 Access Token ---|
  |                             |
  |--- 5. 重试原请求 ---------->|
  |    使用新 Token             |--- 正常处理
  |                             |
  |<-- 6. 返回业务数据 ---------|
  |                             |
```

---

### 🛡️ 后端 Token 验证过滤器

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // 1. 从请求头获取 Token
            String token = extractToken(request);
            
            if (token != null && jwtTokenUtil.validateToken(token)) {
                
                // 2. 从 Token 中获取用户名
                String username = jwtTokenUtil.getUsernameFromToken(token);
                
                // 3. 加载用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 4. 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                
                // 5. 设置详细信息
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 6. 存入 SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
        } catch (Exception e) {
            logger.error("JWT 认证失败：", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 从请求头提取 Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}
```

**配置过滤器：**

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## 5. Token 刷新机制

### 🎯 为什么需要刷新机制？

**问题：**
- Access Token 设置太短 → 用户体验差（频繁登录）
- Access Token 设置太长 → 安全风险高（被盗后有效期长）

**解决方案：双 Token 机制**
- Access Token：短期（2 小时），用于日常请求
- Refresh Token：长期（7 天），用于刷新 Access Token

---

### 🔄 刷新 Token 实现

#### 5.1 Refresh Token 服务

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenUtil jwtTokenUtil;
    
    /**
     * 保存 Refresh Token
     */
    public void saveRefreshToken(Long userId, String token, String deviceInfo) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        
        refreshTokenRepository.save(refreshToken);
    }
    
    /**
     * 验证并刷新 Token
     */
    @Transactional
    public String refreshToken(String refreshToken) {
        
        // 1. 查找 Refresh Token
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new BusinessException("无效的 Refresh Token"));
        
        // 2. 检查是否已撤销
        if (tokenEntity.isRevoked()) {
            revokeAllUserTokens(tokenEntity.getUserId());
            throw new BusinessException("Token 已撤销，请重新登录");
        }
        
        // 3. 检查是否过期
        if (tokenEntity.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(tokenEntity);
            throw new BusinessException("Refresh Token 已过期，请重新登录");
        }
        
        // 4. 验证 JWT Token
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new BusinessException("无效的 Token");
        }
        
        // 5. 获取用户信息
        Long userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        UserDetails userDetails = loadUserById(userId);
        
        // 6. 生成新的 Access Token
        String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails);
        
        // 7. （可选）轮换 Refresh Token
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
        tokenEntity.setToken(newRefreshToken);
        tokenEntity.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(tokenEntity);
        
        return newAccessToken;
    }
    
    /**
     * 撤销用户的所有 Token
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
    
    /**
     * 注销
     */
    @Transactional
    public void logout(String refreshToken) {
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
            .orElse(null);
        
        if (tokenEntity != null) {
            tokenEntity.setRevoked(true);
            refreshTokenRepository.save(tokenEntity);
        }
    }
}
```

#### 5.2 刷新接口

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;
    
    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        
        if (refreshToken == null) {
            return ApiResponse.error(401, "缺少 Refresh Token");
        }
        
        // 刷新 Token
        String newAccessToken = refreshTokenService.refreshToken(refreshToken);
        
        // 可选：返回新的 Refresh Token（如果实施了轮换）
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(...);
        cookieUtil.setRefreshTokenCookie(response, newRefreshToken);
        
        TokenResponse response = TokenResponse.builder()
            .accessToken(newAccessToken)
            .tokenType("Bearer")
            .expiresIn(7200)
            .build();
        
        return ApiResponse.success("刷新成功", response);
    }
    
    /**
     * 注销
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken) {
        
        if (refreshToken != null) {
            refreshTokenService.logout(refreshToken);
        }
        
        // 清除 Cookie
        cookieUtil.clearRefreshTokenCookie(response);
        
        return ApiResponse.success("注销成功", null);
    }
}
```

---

## 6. 完整实战案例

### 📦 项目结构

```
com.example.auth
├── config
│   ├── JwtProperties.java        # JWT 配置属性
│   ├── SecurityConfig.java       # 安全配置
│   └── CookieUtil.java           # Cookie 工具类
├── controller
│   └── AuthController.java       # 认证控制器
├── service
│   ├── AuthService.java          # 认证服务接口
│   ├── AuthServiceImpl.java      # 认证服务实现
│   └── RefreshTokenService.java  # Refresh Token 服务
├── filter
│   └── JwtAuthenticationFilter.java  # JWT 过滤器
├── util
│   └── JwtTokenUtil.java         # JWT 工具类
├── entity
│   ├── User.java                 # 用户实体
│   └── RefreshToken.java         # Refresh Token 实体
├── repository
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── dto
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── TokenResponse.java
└── exception
    └── BusinessException.java
```

### 📝 关键代码

#### 实体类

```java
@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    private String deviceInfo;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private Boolean revoked = false;
    
    private LocalDateTime createdAt;
}
```

#### DTO

```java
@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String deviceInfo;
}

@Data
@Builder
public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo userInfo;
}

@Data
@Builder
public class UserInfo {
    
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
}
```

---

## 7. 安全最佳实践

### 🔐 Token 安全要点

#### 1. 密钥管理

```java
// ❌ 错误：硬编码密钥
private String secret = "mySecret";

// ✅ 正确：从环境变量或配置中心读取
@Value("${jwt.secret}")
private String secret;

// 生产环境建议使用 RSA 非对称加密
@Bean
public KeyPair keyPair() {
    return KeyPairGenerator.getInstance("RSA")
        .generateKeyPair();
}
```

#### 2. Token 过期时间

```yaml
# 推荐配置
jwt:
  access-token-expiration: 7200      # 2 小时
  refresh-token-expiration: 604800   # 7 天
```

**原则：**
- Access Token：越短越好（15 分钟 - 2 小时）
- Refresh Token：根据业务需求（7 天 - 30 天）

#### 3. Token 黑名单（可选）

**场景：** 用户注销后，Access Token 应立即失效

```java
@Service
public class TokenBlacklistService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 将 Token 加入黑名单
     */
    public void blacklist(String token, long expiration) {
        redisTemplate.opsForValue().set(
            "blacklist:" + token,
            "1",
            expiration,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("blacklist:" + token)
        );
    }
}
```

**注意：** 使用黑名单会破坏 JWT 的无状态特性，增加复杂度，慎用！

#### 4. HTTPS 传输

```yaml
# application.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
```

**所有 Token 传输必须使用 HTTPS！**

#### 5. 防止重放攻击

```java
// 在 Token 中加入 jti（唯一 ID）
String jti = UUID.randomUUID().toString();

Jwts.builder()
    .id(jti)  // 唯一标识
    // ...
    .compact();

// 服务端记录已使用的 jti，防止重复使用
```

#### 6. 最小权限原则

```java
// Token 中只包含必要信息
Map<String, Object> claims = new HashMap<>();
claims.put("userId", user.getId());
claims.put("roles", user.getRoles());  // 只包含角色，不包含敏感信息

// 不要包含：
// - 密码
// - 身份证号
// - 手机号
// - 其他敏感信息
```

---

## 8. 常见问题解答

### Q1: JWT Token 可以撤销吗？

**答：** JWT 本身不可撤销（无状态），但可以通过以下方式实现：

1. **短有效期** - Access Token 设置较短时间
2. **Refresh Token 撤销** - 撤销 Refresh Token，阻止获取新 Access Token
3. **Token 黑名单** - 将 Token 加入 Redis 黑名单（牺牲无状态性）
4. **版本号机制** - 用户表中增加 tokenVersion，Token 中包含版本号

**推荐方案：** 短有效期 + Refresh Token 撤销

---

### Q2: Access Token 和 Refresh Token 有什么区别？

| 特性 | Access Token | Refresh Token |
|------|-------------|---------------|
| **用途** | 访问受保护资源 | 获取新的 Access Token |
| **有效期** | 短（15 分钟 - 2 小时） | 长（7 天 - 30 天） |
| **存储** | Memory | HttpOnly Cookie |
| **携带方式** | Authorization Header | Cookie |
| **敏感性** | 中等 | 高 |

---

### Q3: Token 被盗了怎么办？

**应对措施：**

1. **立即撤销 Refresh Token**
   ```java
   refreshTokenService.revokeAllUserTokens(userId);
   ```

2. **强制用户重新登录**

3. **记录异常登录**
   - IP 地址
   - 设备信息
   - 地理位置

4. **通知用户**
   - 邮件通知
   - 短信提醒

5. **加强安全措施**
   - 启用双因素认证（2FA）
   - 限制登录地点
   - 设备绑定

---

### Q4: 如何实现单点登录（SSO）？

**方案：**

1. **共享 JWT 密钥** - 多个服务使用相同的密钥验证 Token
2. **认证中心** - 统一的认证服务颁发 Token
3. **OAuth2/OIDC** - 使用标准协议

**架构：**
```
用户 → 认证中心（颁发 Token）
  ↓
服务 A（验证 Token）
服务 B（验证 Token）
服务 C（验证 Token）
```

---

### Q5: Token 太大怎么办？

**优化方案：**

1. **减少 Payload 内容**
   ```java
   // ❌ 不要放太多信息
   claims.put("user", entireUserObject);
   
   // ✅ 只放必要信息
   claims.put("userId", user.getId());
   claims.put("roles", user.getRoles());
   ```

2. **使用压缩**
   ```java
   // GZIP 压缩后再 Base64 编码
   ```

3. **服务端缓存用户信息**
   ```java
   // Token 中只存 userId
   // 从 Redis 缓存中获取完整用户信息
   ```

---

### Q6: 移动端如何存储 Token？

**推荐方案：**

- **iOS**: Keychain
- **Android**: EncryptedSharedPreferences
- **React Native**: react-native-keychain
- **Flutter**: flutter_secure_storage

**不要使用：**
- ❌ localStorage（Webview）
- ❌ 明文存储

---

## 🎯 核心要点速记

```
Token 生成：
- 使用 JWT 格式
- HS256 或 RS256 签名
- 包含必要信息（userId、roles）
- 设置合理的过期时间

Token 存储：
- Access Token → Memory（内存）
- Refresh Token → HttpOnly Cookie
- 或者全部 localStorage（简单场景）

Token 使用：
- 请求头：Authorization: Bearer <token>
- 后端过滤器验证
- 提取用户信息放入 SecurityContext

Token 刷新：
- Access Token 过期 → 401
- 使用 Refresh Token 刷新
- 获取新的 Access Token
- 重试原请求

安全要点：
- HTTPS 传输
- 短有效期
- 密钥保密
- 防止 XSS/CSRF
- 最小权限原则
```

---

**记住：Token 安全 = 正确的生成 + 安全的存储 + 严格的使用 + 及时的刷新！** 🔒
