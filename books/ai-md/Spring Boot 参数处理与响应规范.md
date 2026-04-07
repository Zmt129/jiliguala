# Spring Boot 参数处理与响应规范完全指南

> 统一参数获取方式、响应格式和校验逻辑，让代码更规范、更安全、更易维护。

---

## 📖 目录

1. [前端参数获取方式](#1-前端参数获取方式)
2. [后端响应格式规范](#2-后端响应格式规范)
3. [参数校验实现位置](#3-参数校验实现位置)
4. [完整实战案例](#4-完整实战案例)
5. [常见错误与避坑](#5-常见错误与避坑)
6. [最佳实践总结](#6-最佳实践总结)

---

## 1. 前端参数获取方式

### 🎯 核心原则

**根据 HTTP 方法和参数类型选择合适的注解：**

| HTTP 方法 | 参数位置 | 推荐注解 | 示例 |
|-----------|---------|---------|------|
| GET | URL 路径 | `@PathVariable` | `/users/{id}` |
| GET | 查询字符串 | `@RequestParam` | `/users?page=1&size=10` |
| POST/PUT | 请求体（JSON） | `@RequestBody` | `{ "name": "张三" }` |
| POST | 表单数据 | `@ModelAttribute` | `name=张三&age=18` |
| 任意 | 请求头 | `@RequestHeader` | `Authorization: Bearer xxx` |
| 任意 | Cookie | `@CookieValue` | `JSESSIONID=xxx` |

---

### 📝 详细用法

#### 1.1 @PathVariable - 路径变量

**适用场景：** RESTful 风格的资源 ID

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // GET /api/users/123
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
    
    // GET /api/users/123/orders/456
    @GetMapping("/{userId}/orders/{orderId}")
    public Order getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        return orderService.findById(orderId);
    }
    
    // 可选的路径变量
    @GetMapping("/{id}/{name}")
    public User getUser(
            @PathVariable Long id,
            @PathVariable(required = false) String name) {
        // ...
    }
}
```

**特点：**
- ✅ 符合 RESTful 规范
- ✅ URL 语义清晰
- ❌ 不适合传递复杂参数

---

#### 1.2 @RequestParam - 查询参数

**适用场景：** 分页、筛选、排序等可选参数

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // GET /api/users?page=1&size=10&keyword=张三
    @GetMapping
    public Page<User> listUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "desc") String order) {
        
        return userService.listUsers(page, size, keyword, sortBy, order);
    }
    
    // GET /api/users?status=ACTIVE&type=VIP
    @GetMapping
    public List<User> filterUsers(
            @RequestParam UserStatus status,
            @RequestParam UserType type) {
        return userService.filter(status, type);
    }
}
```

**特点：**
- ✅ 适合可选参数
- ✅ 支持默认值
- ✅ 浏览器可直接访问
- ❌ 不适合传递复杂对象

**使用对象接收多个参数：**

```java
// 定义查询条件对象
@Data
public class UserQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
    private UserStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

// Controller
@GetMapping
public Page<User> listUsers(UserQuery query) {
    // Spring 会自动绑定查询参数到对象
    return userService.listUsers(query);
}
```

---

#### 1.3 @RequestBody - 请求体（最常用）

**适用场景：** POST/PUT 提交 JSON 数据

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // POST /api/users
    // Body: { "username": "zhangsan", "email": "zhang@example.com" }
    @PostMapping
    public User createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.create(request);
    }
    
    // PUT /api/users/123
    // Body: { "username": "lisi", "email": "li@example.com" }
    @PutMapping("/{id}")
    public User updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        return userService.update(id, request);
    }
    
    // 部分更新（PATCH）
    @PatchMapping("/{id}")
    public User patchUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        return userService.patch(id, updates);
    }
}
```

**请求对象示例：**

```java
@Data
public class CreateUserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄必须大于 0")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;
    
    private String phone;
    private String address;
}
```

**特点：**
- ✅ 适合复杂对象
- ✅ 支持嵌套结构
- ✅ 类型安全
- ❌ 只能用于 POST/PUT/PATCH

---

#### 1.4 @RequestHeader - 请求头

**适用场景：** Token 认证、自定义头部信息

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    // 获取 Token
    @GetMapping
    public List<Order> listOrders(
            @RequestHeader("Authorization") String authorization) {
        
        String token = authorization.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        
        return orderService.findByUserId(userId);
    }
    
    // 可选的请求头
    @GetMapping
    public List<Order> listOrders(
            @RequestHeader(value = "X-Request-ID", required = false) String requestId) {
        
        if (requestId != null) {
            MDC.put("requestId", requestId);
        }
        
        return orderService.findAll();
    }
}
```

---

#### 1.5 @ModelAttribute - 表单数据

**适用场景：** 传统表单提交（application/x-www-form-urlencoded）

```java
@RestController
@RequestMapping("/api/login")
public class LoginController {
    
    // POST /api/login
    // Content-Type: application/x-www-form-urlencoded
    // Body: username=zhangsan&password=123456
    @PostMapping
    public LoginResponse login(@ModelAttribute LoginRequest request) {
        return authService.login(request);
    }
}

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

**注意：** 现代前后端分离项目很少用这个，优先使用 `@RequestBody`。

---

#### 1.6 组合使用

**实际开发中经常组合使用：**

```java
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    
    // POST /api/articles?categoryId=1
    // Header: Authorization: Bearer xxx
    // Body: { "title": "标题", "content": "内容" }
    @PostMapping
    public Article createArticle(
            @RequestParam Long categoryId,
            @RequestHeader("Authorization") String token,
            @RequestBody @Valid CreateArticleRequest request) {
        
        Long userId = jwtUtil.getUserId(token);
        return articleService.create(categoryId, userId, request);
    }
    
    // GET /api/articles/123?fields=title,content
    @GetMapping("/{id}")
    public Article getArticle(
            @PathVariable Long id,
            @RequestParam(required = false) String fields) {
        
        return articleService.findById(id, fields);
    }
}
```

---

## 2. 后端响应格式规范

### 🎯 统一响应结构

**所有接口返回统一的格式：**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan"
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

### 📦 实现方案

#### 2.1 定义统一响应类

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    // ========== 成功响应 ==========
    
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, LocalDateTime.now());
    }
    
    // ========== 失败响应 ==========
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(BusinessException ex) {
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null, LocalDateTime.now());
    }
}
```

#### 2.2 定义业务异常

```java
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
```

#### 2.3 定义状态码枚举

```java
@Getter
@AllArgsConstructor
public enum ResultCode {
    
    // 成功
    SUCCESS(200, "操作成功"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    
    // 服务端错误 5xx
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // 业务错误 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    ORDER_NOT_FOUND(2001, "订单不存在"),
    INSUFFICIENT_STOCK(2002, "库存不足");
    
    private final Integer code;
    private final String message;
}
```

#### 2.4 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常：{}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex));
    }
    
    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationException(
            MethodArgumentNotValidException ex) {
        
        // 获取第一个校验错误
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("参数校验失败");
        
        log.warn("参数校验失败：{}", message);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, message));
    }
    
    /**
     * 参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<?>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("参数绑定失败");
        
        log.warn("参数绑定失败：{}", message);
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(400, message));
    }
    
    /**
     * 404 异常
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFoundException(NoSuchElementException ex) {
        log.warn("资源不存在：{}", ex.getMessage());
        return ResponseEntity.status(404)
            .body(ApiResponse.error(404, "资源不存在"));
    }
    
    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(500)
            .body(ApiResponse.error("系统繁忙，请稍后再试"));
    }
}
```

---

### 📊 不同场景的响应示例

#### 场景 1：返回单个对象

```java
@GetMapping("/{id}")
public ApiResponse<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ApiResponse.success(user);
}

// 响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "email": "zhang@example.com"
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 场景 2：返回列表

```java
@GetMapping
public ApiResponse<List<User>> listUsers() {
    List<User> users = userService.findAll();
    return ApiResponse.success(users);
}

// 响应：
{
  "code": 200,
  "message": "success",
  "data": [
    { "id": 1, "username": "zhangsan" },
    { "id": 2, "username": "lisi" }
  ],
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 场景 3：返回分页数据

```java
// 定义分页响应
@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> records;
    private Long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;
}

@GetMapping
public ApiResponse<PageResponse<User>> listUsers(
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "10") Integer size) {
    
    Page<User> userPage = userService.page(page, size);
    
    PageResponse<User> response = new PageResponse<>(
        userPage.getContent(),
        userPage.getTotalElements(),
        page,
        size,
        userPage.getTotalPages()
    );
    
    return ApiResponse.success(response);
}

// 响应：
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 10,
    "totalPages": 10
  },
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 场景 4：无返回数据

```java
@DeleteMapping("/{id}")
public ApiResponse<Void> deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
    return ApiResponse.success("删除成功", null);
}

// 响应：
{
  "code": 200,
  "message": "删除成功",
  "data": null,
  "timestamp": "2024-01-01T12:00:00"
}
```

#### 场景 5：创建资源（返回 201）

```java
@PostMapping
public ResponseEntity<ApiResponse<User>> createUser(
        @RequestBody @Valid CreateUserRequest request) {
    
    User user = userService.create(request);
    
    ApiResponse<User> response = ApiResponse.success("创建成功", user);
    
    return ResponseEntity.status(201)
        .header("Location", "/api/users/" + user.getId())
        .body(response);
}

// 响应状态码：201 Created
// Header: Location: /api/users/123
```

#### 场景 6：文件下载

```java
@GetMapping("/{id}/download")
public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
    File file = fileService.getFile(id);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + file.getName() + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(new FileSystemResource(file));
}
```

---

## 3. 参数校验实现位置

### 🎯 核心原则

**三层校验策略：**

```
1️⃣ 前端校验（用户体验）
   ↓
2️⃣ Controller 层校验（快速失败）✅ 重点
   ↓
3️⃣ Service 层校验（业务规则）✅ 重点
   ↓
4️⃣ 数据库约束（最后防线）
```

---

### 📝 各层校验详解

#### 3.1 Controller 层校验（格式校验）

**职责：** 校验参数的格式、类型、必填项

**实现方式：** JSR-303/JSR-380 注解校验

```java
@RestController
@RequestMapping("/api/users")
@Validated  // 开启校验
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 创建用户
     * 在 Controller 层校验参数格式
     */
    @PostMapping
    public ApiResponse<User> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        
        // 校验通过后，调用 Service
        User user = userService.create(request);
        return ApiResponse.success("创建成功", user);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        
        User user = userService.update(id, request);
        return ApiResponse.success("更新成功", user);
    }
}
```

**请求对象中的校验注解：**

```java
@Data
public class CreateUserRequest {
    
    // 字符串校验
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 之间")
    private String password;
    
    // 邮箱校验
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    // 数值校验
    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄必须大于等于 0")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;
    
    // 手机号校验
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    // 日期校验
    @NotNull(message = "生日不能为空")
    @Past(message = "生日必须是过去的日期")
    private LocalDate birthday;
    
    // 自定义校验
    @ValidPassword  // 自定义注解
    private String password;
}
```

**常用校验注解：**

| 注解 | 适用类型 | 说明 |
|------|---------|------|
| `@NotNull` | 任意 | 不能为 null |
| `@NotBlank` | String | 不能为 null 且去除空格后长度 > 0 |
| `@NotEmpty` | Collection/String | 不能为 null 且不为空 |
| `@Size(min, max)` | String/Collection | 长度/大小范围 |
| `@Min(value)` | Number | 最小值 |
| `@Max(value)` | Number | 最大值 |
| `@DecimalMin` | BigDecimal | 最小值（支持小数） |
| `@DecimalMax` | BigDecimal | 最大值（支持小数） |
| `@Email` | String | 邮箱格式 |
| `@Pattern` | String | 正则表达式 |
| `@Past` | Date/LocalDate | 必须是过去的日期 |
| `@Future` | Date/LocalDate | 必须是未来的日期 |
| `@Positive` | Number | 正数 |
| `@Negative` | Number | 负数 |

---

#### 3.2 Service 层校验（业务校验）

**职责：** 校验业务规则、数据一致性、权限控制

```java
@Service
@Transactional
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 创建用户
     * 在 Service 层校验业务规则
     */
    @Override
    public User create(CreateUserRequest request) {
        
        // 1. 校验用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        
        // 2. 校验邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册");
        }
        
        // 3. 校验手机号是否已存在
        if (StringUtils.hasText(request.getPhone()) 
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("手机号已被注册");
        }
        
        // 4. 密码加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        // 5. 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPasswordHash(encodedPassword);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * 更新用户
     */
    @Override
    public User update(Long id, UpdateUserRequest request) {
        
        // 1. 校验用户是否存在
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        
        // 2. 校验邮箱唯一性（排除自己）
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("邮箱已被其他用户使用");
        }
        
        // 3. 校验账号状态
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("账号已被禁用，无法更新");
        }
        
        // 4. 更新字段
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * 删除用户
     */
    @Override
    public void delete(Long id) {
        
        // 1. 校验用户是否存在
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        
        // 2. 校验是否有未完成订单
        long orderCount = orderRepository.countByUserIdAndStatusNot(
            id, OrderStatus.COMPLETED
        );
        if (orderCount > 0) {
            throw new BusinessException("用户有未完成的订单，无法删除");
        }
        
        // 3. 逻辑删除
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
```

---

#### 3.3 自定义校验注解

**场景：** 内置注解无法满足需求时

```java
// 1. 定义注解
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {
    
    String message() default "密码必须包含大小写字母、数字和特殊字符";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}

// 2. 实现校验器
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$");
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true; // 由 @NotBlank 处理
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}

// 3. 使用
@Data
public class CreateUserRequest {
    
    @ValidPassword
    private String password;
}
```

---

#### 3.4 分组校验

**场景：** 创建和更新时需要校验不同的字段

```java
// 1. 定义分组接口
public interface CreateGroup {}
public interface UpdateGroup {}

// 2. 在请求对象中标注分组
@Data
public class UserRequest {
    
    @NotNull(message = "ID 不能为空", groups = UpdateGroup.class)
    private Long id;
    
    @NotBlank(message = "用户名不能为空", groups = {CreateGroup.class, UpdateGroup.class})
    private String username;
    
    @NotBlank(message = "密码不能为空", groups = CreateGroup.class)
    private String password;
    
    @Email(message = "邮箱格式不正确", groups = {CreateGroup.class, UpdateGroup.class})
    private String email;
}

// 3. Controller 中使用
@PostMapping
public ApiResponse<User> createUser(
        @RequestBody @Validated(CreateGroup.class) UserRequest request) {
    // 只校验 CreateGroup 分组的字段
}

@PutMapping("/{id}")
public ApiResponse<User> updateUser(
        @RequestBody @Validated(UpdateGroup.class) UserRequest request) {
    // 只校验 UpdateGroup 分组的字段
}
```

---

#### 3.5 手动校验

**场景：** 复杂校验逻辑

```java
@Service
public class OrderService {
    
    @Autowired
    private Validator validator;
    
    public void createOrder(CreateOrderRequest request) {
        
        // 手动校验
        Set<ConstraintViolation<CreateOrderRequest>> violations = 
            validator.validate(request);
        
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
            throw new BusinessException(message);
        }
        
        // 业务逻辑...
    }
}
```

---

## 4. 完整实战案例

### 📦 用户管理模块

#### 4.1 请求对象

```java
// 创建用户请求
@Data
public class CreateUserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3-50 之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在 6-100 之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Min(value = 0, message = "年龄必须大于等于 0")
    @Max(value = 150, message = "年龄不能超过 150")
    private Integer age;
}

// 更新用户请求
@Data
public class UpdateUserRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}

// 查询用户请求
@Data
public class UserQuery {
    
    @Min(value = 1, message = "页码必须大于 0")
    private Integer page = 1;
    
    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页数量不能超过 100")
    private Integer size = 10;
    
    private String keyword;
    
    private UserStatus status;
}
```

#### 4.2 Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    
    private final UserService userService;
    
    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<UserVO> createUser(
            @RequestBody @Valid CreateUserRequest request) {
        
        UserVO user = userService.create(request);
        return ApiResponse.success("创建成功", user);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable @Min(1) Long id) {
        UserVO user = userService.findById(id);
        return ApiResponse.success(user);
    }
    
    /**
     * 分页查询用户
     */
    @GetMapping
    public ApiResponse<PageResponse<UserVO>> listUsers(@Valid UserQuery query) {
        PageResponse<UserVO> page = userService.listUsers(query);
        return ApiResponse.success(page);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ApiResponse<UserVO> updateUser(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid UpdateUserRequest request) {
        
        UserVO user = userService.update(id, request);
        return ApiResponse.success("更新成功", user);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable @Min(1) Long id) {
        userService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
```

#### 4.3 Service

```java
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public UserVO create(CreateUserRequest request) {
        
        // 业务校验
        checkUsernameUnique(request.getUsername());
        checkEmailUnique(request.getEmail());
        
        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        
        User saved = userRepository.save(user);
        
        return convertToVO(saved);
    }
    
    @Override
    public UserVO findById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        
        return convertToVO(user);
    }
    
    @Override
    public PageResponse<UserVO> listUsers(UserQuery query) {
        Pageable pageable = PageRequest.of(
            query.getPage() - 1, 
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Specification<User> spec = buildSpecification(query);
        Page<User> page = userRepository.findAll(spec, pageable);
        
        List<UserVO> vos = page.getContent().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        
        return new PageResponse<>(
            vos,
            page.getTotalElements(),
            query.getPage(),
            query.getSize(),
            page.getTotalPages()
        );
    }
    
    @Override
    public UserVO update(Long id, UpdateUserRequest request) {
        
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        
        // 业务校验
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("邮箱已被其他用户使用");
        }
        
        // 更新
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        
        User updated = userRepository.save(user);
        
        return convertToVO(updated);
    }
    
    @Override
    public void delete(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        
        // 逻辑删除
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    // ========== 私有方法 ==========
    
    private void checkUsernameUnique(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
    }
    
    private void checkEmailUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("邮箱已被注册");
        }
    }
    
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 脱敏处理
        vo.setPasswordHash(null);
        return vo;
    }
    
    private Specification<User> buildSpecification(UserQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (StringUtils.hasText(query.getKeyword())) {
                Predicate p1 = cb.like(root.get("username"), "%" + query.getKeyword() + "%");
                Predicate p2 = cb.like(root.get("email"), "%" + query.getKeyword() + "%");
                predicates.add(cb.or(p1, p2));
            }
            
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            
            predicates.add(cb.isNull(root.get("deletedAt")));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

---

## 5. 常见错误与避坑

### ❌ 错误 1：在 Controller 中写业务逻辑

```java
// ❌ 错误
@PostMapping
public User createUser(@RequestBody CreateUserRequest request) {
    // 不应该在 Controller 中校验业务规则
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new RuntimeException("用户名已存在");
    }
    
    User user = new User();
    BeanUtils.copyProperties(request, user);
    user.setPassword(encode(request.getPassword()));
    return userRepository.save(user);
}

// ✅ 正确
@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody @Valid CreateUserRequest request) {
    UserVO user = userService.create(request);
    return ApiResponse.success(user);
}
```

---

### ❌ 错误 2：直接返回实体类

```java
// ❌ 错误：暴露了密码等敏感信息
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id).get();
}

// ✅ 正确：返回 VO（View Object）
@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) {
    UserVO user = userService.findById(id);
    return ApiResponse.success(user);
}
```

---

### ❌ 错误 3：忽略校验结果

```java
// ❌ 错误：忘记加 @Valid
@PostMapping
public ApiResponse<User> createUser(@RequestBody CreateUserRequest request) {
    // 校验不会生效！
}

// ✅ 正确
@PostMapping
public ApiResponse<User> createUser(@RequestBody @Valid CreateUserRequest request) {
    // 校验生效
}
```

---

### ❌ 错误 4：不统一的响应格式

```java
// ❌ 错误：有的返回对象，有的返回字符串，有的返回 Map
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) { ... }

@PostMapping
public String createUser(@RequestBody User user) { ... }

@DeleteMapping("/{id}")
public Map<String, Object> deleteUser(@PathVariable Long id) { ... }

// ✅ 正确：统一返回 ApiResponse
@GetMapping("/{id}")
public ApiResponse<UserVO> getUser(@PathVariable Long id) { ... }

@PostMapping
public ApiResponse<UserVO> createUser(@RequestBody @Valid CreateUserRequest request) { ... }

@DeleteMapping("/{id}")
public ApiResponse<Void> deleteUser(@PathVariable Long id) { ... }
```

---

### ❌ 错误 5：在 Service 中不做校验

```java
// ❌ 错误：假设 Controller 已经校验过了
public User create(CreateUserRequest request) {
    // 直接使用，不做任何校验
    return userRepository.save(convert(request));
}

// ✅ 正确：Service 层也要做业务校验
public User create(CreateUserRequest request) {
    // 校验业务规则
    checkUsernameUnique(request.getUsername());
    checkEmailUnique(request.getEmail());
    
    return userRepository.save(convert(request));
}
```

---

## 6. 最佳实践总结

### ✅ DO（应该做的）

1. **参数获取**
   - ✅ RESTful 风格用 `@PathVariable`
   - ✅ 查询参数用 `@RequestParam` 或对象接收
   - ✅ JSON 数据用 `@RequestBody`
   - ✅ 始终添加 `@Valid` 或 `@Validated`

2. **响应格式**
   - ✅ 统一使用 `ApiResponse<T>` 包装
   - ✅ 定义清晰的状态码枚举
   - ✅ 使用 VO 而不是 Entity
   - ✅ 敏感信息脱敏

3. **参数校验**
   - ✅ Controller 层：格式校验（JSR-303）
   - ✅ Service 层：业务校验（唯一性、权限等）
   - ✅ 数据库：最后防线（唯一索引、非空约束）
   - ✅ 自定义校验注解处理复杂逻辑

4. **异常处理**
   - ✅ 使用全局异常处理器
   - ✅ 区分业务异常和系统异常
   - ✅ 记录详细的日志
   - ✅ 返回友好的错误提示

### ❌ DON'T（不应该做的）

1. ❌ 不要在 Controller 中写业务逻辑
2. ❌ 不要直接返回 Entity
3. ❌ 不要忘记加 `@Valid`
4. ❌ 不要在不用的响应格式
5. ❌ 不要在 Service 中跳过校验
6. ❌ 不要捕获异常后吞掉（empty catch）
7. ❌ 不要在日志中打印敏感信息（密码、身份证）

---

### 📋 检查清单

开发接口时，问自己这些问题：

- [ ] 参数获取方式是否正确？
- [ ] 是否添加了 `@Valid` 注解？
- [ ] 请求对象中是否有校验注解？
- [ ] Service 层是否有业务校验？
- [ ] 返回值是否用 `ApiResponse` 包装？
- [ ] 是否返回 VO 而不是 Entity？
- [ ] 敏感信息是否脱敏？
- [ ] 是否有全局异常处理？
- [ ] 是否记录了关键日志？
- [ ] 状态码是否符合 HTTP 规范？

---

## 🎯 核心要点速记

```
参数获取：
- 路径变量 → @PathVariable
- 查询参数 → @RequestParam
- JSON 数据 → @RequestBody
- 请求头 → @RequestHeader

响应格式：
- 统一 ApiResponse<T>
- 明确的状态码
- VO 代替 Entity
- 敏感信息脱敏

参数校验：
- Controller：格式校验（@Valid）
- Service：业务校验（唯一性、权限）
- Database：最后防线（约束）

异常处理：
- 全局异常处理器
- 区分业务/系统异常
- 友好错误提示
- 详细日志记录
```

---

**记住：好的接口设计 = 清晰的参数获取 + 统一的响应格式 + 完善的校验机制！** 🚀
