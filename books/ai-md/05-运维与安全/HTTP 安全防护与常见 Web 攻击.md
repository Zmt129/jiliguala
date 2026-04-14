# HTTP 安全防护与常见 Web 攻击完全指南

> 保护你的 Spring Boot 应用免受 XSS、CSRF、SQL 注入等常见威胁。

---

## 📖 目录

1. [XSS (跨站脚本攻击)](#1-xss-跨站脚本攻击)
2. [CSRF (跨站请求伪造)](#2-csrf-跨站请求伪造)
3. [SQL 注入](#3-sql-注入)
4. [CORS (跨域资源共享)](#4-cors-跨域资源共享)
5. [HTTPS 与安全头配置](#5-https-与安全头配置)
6. [Spring Security 实战配置](#6-spring-security-实战配置)

---

## 1. XSS (跨站脚本攻击)

### 🔍 原理
攻击者在网页中注入恶意脚本（通常是 JavaScript），当其他用户浏览该页面时，脚本会在其浏览器中执行，从而窃取 Cookie、会话令牌或重定向到钓鱼网站。

### 🛡️ 防御方案
1. **输入过滤：** 对用户输入进行严格校验。
2. **输出转义：** 在将数据渲染到 HTML 之前，对特殊字符（如 `<`, `>`, `&`）进行转义。
3. **使用 HttpOnly Cookie：** 防止 JavaScript 访问敏感 Cookie。

### 💻 Spring Boot 实现
*   **Thymeleaf/JSP：** 默认会自动转义输出内容。
*   **手动转义：** 使用 `HtmlUtils.htmlEscape()`。
*   **Content-Security-Policy (CSP)：** 限制浏览器只加载受信任的资源。

---

## 2. CSRF (跨站请求伪造)

### 🔍 原理
攻击者诱导已登录的用户点击恶意链接或提交表单，利用用户的身份在不知情的情况下执行操作（如转账、修改密码）。

### 🛡️ 防御方案
1. **同步器令牌模式 (Synchronizer Token Pattern)：** 在每个表单中包含一个随机生成的 Token，服务端验证该 Token。
2. **SameSite Cookie 属性：** 设置为 `Strict` 或 `Lax`，禁止第三方网站携带 Cookie。
3. **验证 Referer/Origin 头。**

### 💻 Spring Security 配置
Spring Security 默认开启 CSRF 保护。如果是前后端分离项目（使用 JWT），通常建议**禁用 CSRF**，因为 JWT 存储在 LocalStorage 中，不依赖 Cookie 自动发送。

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable(); // 前后端分离通常禁用
    }
}
```

---

## 3. SQL 注入

### 🔍 原理
攻击者通过在输入字段中插入恶意的 SQL 代码，欺骗数据库执行非预期的命令（如删除表、泄露数据）。

### 🛡️ 防御方案
1. **预编译语句 (PreparedStatement)：** 永远不要拼接 SQL 字符串。
2. **ORM 框架：** 使用 MyBatis-Plus 或 JPA，它们默认使用参数化查询。
3. **最小权限原则：** 数据库账号只授予必要的读写权限。

### ❌ 错误示例 (MyBatis)
```xml
<select id="findUser" resultType="User">
    SELECT * FROM users WHERE username = '${username}' <!-- 危险！ -->
</select>
```

### ✅ 正确示例
```xml
<select id="findUser" resultType="User">
    SELECT * FROM users WHERE username = #{username} <!-- 安全！ -->
</select>
```

---

## 4. CORS (跨域资源共享)

### 🔍 原理
浏览器的同源策略限制了从一个源加载的文档或脚本如何与来自另一个源的资源进行交互。

### 🛡️ 配置建议
不要直接使用 `@CrossOrigin(origins = "*")`，应明确指定允许的域名。

### 💻 Spring Boot 全局配置
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://your-frontend.com") // 明确指定域名
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 5. HTTPS 与安全头配置

### 🔒 关键安全响应头
| 头部名称 | 作用 |
|----------|------|
| **Strict-Transport-Security (HSTS)** | 强制浏览器使用 HTTPS 连接 |
| **X-Content-Type-Options** | 禁止浏览器进行 MIME 类型嗅探 |
| **X-Frame-Options** | 防止点击劫持（Clickjacking） |
| **Content-Security-Policy (CSP)** | 限制资源加载来源，防范 XSS |

### 💻 Spring Security 自动配置
Spring Security 默认会添加大部分安全头：

```java
http.headers()
    .frameOptions().sameOrigin() // 允许同源 iframe
    .contentTypeOptions() // 开启 X-Content-Type-Options
    .xssProtection(); // 开启 XSS 防护
```

---

## 6. Spring Security 实战配置

### 🏗️ 综合安全配置类
```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 禁用 CSRF (针对 JWT 无状态认证)
            .csrf(csrf -> csrf.disable())
            
            // 2. 配置授权规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            
            // 3. 配置安全头
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            )
            
            // 4. 设置会话管理为无状态
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
```

---

## ⚠️ 避坑指南

1. **不要在日志中记录敏感信息：** 如密码、Token、身份证号。
2. **统一异常处理：** 避免将堆栈跟踪信息直接返回给前端，这会暴露系统架构细节。
3. **定期更新依赖：** 使用 `mvn dependency:analyze` 检查并修复已知漏洞的库。
4. **JWT 密钥保管：** 签名密钥必须存放在环境变量或密钥管理服务中，严禁硬编码在代码里。

---

**记住：安全是一个持续的过程，而不是一次性的任务。保持警惕，定期审计！** 🚀
