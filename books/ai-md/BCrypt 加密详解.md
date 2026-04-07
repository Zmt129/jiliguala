# BCrypt 加密详解：从原理到实战

> 为什么你的密码不应该用 MD5？BCrypt 到底是什么？看完这篇你就懂了！

---

## 📖 目录

1. [什么是 BCrypt？](#1-什么是-bcrypt)
2. [为什么要用 BCrypt 而不是 MD5？](#2-为什么要用 bcrypt-而不是-md5)
3. [BCrypt 工作原理（通俗版）](#3-bcrypt-工作原理通俗版)
4. [BCrypt vs MD5 vs SHA](#4-bcrypt-vs-md5-vs-sha)
5. [Java 中使用 BCrypt](#5-java-中使用-bcrypt)
6. [最佳实践](#6-最佳实践)
7. [常见问题解答](#7-常见问题解答)

---

## 1. 什么是 BCrypt？

### 🤔 一句话解释

**BCrypt 是一种专门用来加密密码的算法，它的设计目标就是：慢！**

没错，你没看错。其他加密算法都在追求"更快"，只有 BCrypt 在追求"更慢"。

### 🎯 为什么需要"慢"？

想象一个场景：

**场景：** 黑客偷走了数据库，里面存储了用户的密码。

```
数据库泄露：
用户 A: 123456 → 加密后：$2a$10$N9qo8uLOickgx2ZMRZoMye...
用户 B: password → 加密后：$2a$10$8QdRXVPVhTb6O.FwMvHxO...
用户 C: admin888 → 加密后：$2a$10$xIqwXGFrCnFmFhZqKPxJO...
```

**如果加密很快（比如 MD5）：**

```java
// 黑客可以每秒尝试 10 亿次
for (String guess : allPossiblePasswords) {
    String hashed = md5(guess);  // 耗时：0.0000001 秒
    if (hashed.equals(stolenHash)) {
        System.out.println("破解成功！密码是：" + guess);
    }
}
```

**结果：** 简单密码几秒钟就被破解了！

**如果加密很慢（BCrypt）：**

```java
// 黑客每秒只能尝试 10 次
for (String guess : allPossiblePasswords) {
    String hashed = bcrypt(guess);  // 耗时：0.1 秒
    if (hashed.equals(stolenHash)) {
        System.out.println("破解成功！密码是：" + guess);
    }
}
```

**结果：** 破解一个 8 位密码可能需要几百年！

### 💡 BCrypt 的特点

- ✅ **加盐（Salt）**：每个密码都有独特的"调料"
- ✅ **可调节成本（Cost）**：可以控制加密的"难度"
- ✅ **自适应**：随着硬件变强，可以增加成本保持安全性
- ✅ **不可逆**：无法从加密结果还原原始密码

---

## 2. 为什么要用 BCrypt 而不是 MD5？

### ⚠️ MD5 的问题

#### 问题 1：太快了！

MD5 设计于 1991 年，那个年代电脑很慢，所以追求速度。但现在：

```
普通电脑：每秒可计算 10 亿次 MD5
高端显卡：每秒可计算 1000 亿次 MD5
```

这意味着什么？

**实验对比：**

| 密码长度 | MD5 破解时间 | BCrypt 破解时间 |
|---------|------------|---------------|
| 6 位纯数字 | 0.001 秒 | 3 小时 |
| 8 位字母+数字 | 2 分钟 | 300 年 |
| 10 位复杂密码 | 3 小时 | 3 万年 |

#### 问题 2：彩虹表攻击

**什么是彩虹表？**

想象一个"密码字典"：

```
明文       →      MD5
123456     →      e10adc3949ba59abbe56e057f20f883e
password   →      5f4dcc3b5aa765d61d8327deb882cf99
admin888   →      c93ccd78b2076528346216b3b6f2390f
...
```

黑客预先计算好几亿个常用密码的 MD5 值，做成一个大表（彩虹表）。

**攻击过程：**

```
1. 偷到你的密码 MD5: e10adc3949ba59abbe56e057f20f883e
2. 查彩虹表：e10adc3949ba59abbe56e057f20f883e → 123456
3. 破解成功！耗时：0.0001 秒
```

#### 问题 3：碰撞攻击

MD5 存在严重的碰撞问题，即不同的输入可能产生相同的输出：

```java
// 理论上可以找到两个不同的字符串，它们的 MD5 相同
md5("字符串 A") == md5("字符串 B")  // 虽然 A ≠ B
```

虽然这对密码破解影响不大，但说明 MD5 已经不安全了。

### ✅ BCrypt 的优势

#### 优势 1：内置盐值（Salt）

**什么是盐？**

想象你在做菜：

```
原始密码：123456
不加盐：MD5(123456) = e10adc3949ba59abbe56e057f20f883e
加盐：MD5(123456 + "随机字符串") = 完全不同的结果
```

**BCrypt 的盐值：**

```java
// 即使密码相同，每次加密结果都不同
bcrypt("123456") → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
bcrypt("123456") → $2a$10$k87L.ZQvvDkGGN7EKxvmR.xDyHgKzJJKvxMaYiSBEAcCdwxkHN1fG
bcrypt("123456") → $2a$10$vU8ZwmGjkRSnTiBpoNmRO.pDLBMzUKCPqYNPJEfVLh1WZExsmN5/u
```

**为什么这样安全？**

```
用户 A 和 B 都用密码：123456

传统 MD5:
A: e10adc3949ba59abbe56e057f20f883e
B: e10adc3949ba59abbe56e057f20f883e
// 黑客一看就知道他们密码相同！

BCrypt:
A: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
B: $2a$10$k87L.ZQvvDkGGN7EKxvmR.xDyHgKzJJKvxMaYiSBEAcCdwxkHN1fG
// 黑客完全看不出任何关联！
```

#### 优势 2：成本因子（Cost Factor）

BCrypt 有一个成本参数（通常是 2 的 N 次方），控制迭代次数：

```java
// cost = 10，迭代 2^10 = 1024 轮
bcrypt("password", 10) → 耗时约 0.1 秒

// cost = 12，迭代 2^12 = 4096 轮
bcrypt("password", 12) → 耗时约 0.4 秒

// cost = 15，迭代 2^15 = 32768 轮
bcrypt("password", 15) → 耗时约 3 秒
```

**成本每增加 1，计算时间翻倍！**

这意味着：
- 现在用 cost=10，加密一次 0.1 秒
- 5 年后电脑快了一倍，我们改成 cost=11，还是 0.1 秒
- 永远让黑客很难受！

#### 优势 3：抗 GPU 攻击

GPU 擅长并行计算，但 BCrypt 的设计使其难以被 GPU 加速：

```
MD5 on GPU: 1000 亿次/秒
BCrypt on GPU: 仅比 CPU 快 2-3 倍
```

因为 BCrypt 需要大量内存访问，这不是 GPU 的强项。

---

## 3. BCrypt 工作原理（通俗版）

### 🎓 核心概念

#### 第一步：生成盐值

```java
// 生成一个随机的 16 字节盐值
byte[] salt = generateSalt();
// 例如：[0x4a, 0x7b, 0x2c, 0x9d, ...]
```

这个盐值就像做菜时的"独家秘方"，每次都不同。

#### 第二步：合并密码和盐

```
密码：123456
盐值：[0x4a, 0x7b, 0x2c, 0x9d, ...]
合并：123456 + [盐值的 Base64 编码]
```

#### 第三步：反复哈希（关键步骤）

```java
String input = 密码 + 盐值;
String result = input;

// 重复 2^cost 次
for (int i = 0; i < Math.pow(2, cost); i++) {
    result = hash(result);  // 哈希函数
}

return result;
```

**这个过程有多慢？**

```
cost = 10: 循环 1024 次
cost = 12: 循环 4096 次
cost = 15: 循环 32768 次
```

#### 第四步：组合最终结果

BCrypt 的结果包含所有信息：

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 │  │  └────────────┬─────────────┘ └──────────┬──────────┘
 │  │               │                          │
 │  │           盐值 (22 字符)              哈希值 (31 字符)
 │  │
 │  成本因子 (10 = 2^10 次迭代)
 │
算法版本 (2a)
```

**验证时：**

```java
// 1. 从结果中提取盐值和成本因子
String salt = extractSalt("$2a$10$N9qo8uLOickgx2ZMRZoMye...");
int cost = extractCost("$2a$10$...");

// 2. 用同样的盐值和成本加密输入的密码
String newHash = bcrypt(inputPassword, salt, cost);

// 3. 比较哈希值
if (newHash.equals(storedHash)) {
    // 密码正确
}
```

### 🔍 完整流程图解

```
用户注册：
┌─────────────┐
│ 密码：123456 │
└──────┬──────┘
       ↓
┌─────────────────┐
│ 1. 生成随机盐值   │ → [0x4a, 0x7b, 0x2c, ...]
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 2. 密码 + 盐值    │ → "123456 + 盐值"
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 3. 多次哈希      │ → 循环 1024 次 (cost=10)
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 4. 组合结果      │ → $2a$10$盐值 + 哈希值
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 存入数据库      │ → $2a$10$N9qo8uLOickgx2ZMRZoMye...
└─────────────────┘


用户登录：
┌─────────────┐
│ 输入密码     │
└──────┬──────┘
       ↓
┌─────────────────┐
│ 从数据库取盐值   │ → $2a$10$N9qo8uLOickgx2ZMRZoMye...
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 用同样盐值加密   │ → bcrypt(输入密码，盐值)
└──────┬──────────┘
       ↓
┌─────────────────┐
│ 比较哈希值      │ → 相同则密码正确
└─────────────────┘
```

---

## 4. BCrypt vs MD5 vs SHA

### 📊 对比表格

| 特性 | MD5 | SHA-256 | BCrypt |
|------|-----|---------|--------|
| **设计目的** | 快速哈希 | 快速哈希 | **慢速密码哈希** |
| **速度** | 极快（纳秒级） | 快（纳秒级） | **慢（毫秒级）** |
| **盐值** | 需手动添加 | 需手动添加 | **内置盐值** |
| **成本因子** | ❌ 无 | ❌ 无 | ✅ **可调节** |
| **抗 GPU 攻击** | ❌ 差 | ❌ 差 | ✅ **好** |
| **抗彩虹表** | ❌ 差（除非加盐） | ❌ 差（除非加盐） | ✅ **优秀** |
| **输出长度** | 128 位 | 256 位 | 184 位 + 盐值 |
| **是否可逆** | ✅ 不可逆 | ✅ 不可逆 | ✅ 不可逆 |
| **适合密码** | ❌ 不适合 | ❌ 不适合 | ✅ **专门设计** |
| **安全性** | ❌ 已破解 | ⚠️ 一般 | ✅ **高** |

### 🔬 实际测试

```java
// 测试环境：Intel i7, Java 11

public class SpeedTest {
    public static void main(String[] args) {
        String password = "MySecurePassword123!";
        
        // MD5
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            DigestUtils.md5Hex(password);
        }
        long md5Time = (System.nanoTime() - start) / 10000;
        
        // SHA-256
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            DigestUtils.sha256Hex(password);
        }
        long sha256Time = (System.nanoTime() - start) / 10000;
        
        // BCrypt (cost=10)
        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            BCrypt.hashpw(password, BCrypt.gensalt(10));
        }
        long bcryptTime = (System.nanoTime() - start) / 100;
        
        System.out.println("MD5 平均耗时：" + md5Time + " 纳秒");
        System.out.println("SHA-256 平均耗时：" + sha256Time + " 纳秒");
        System.out.println("BCrypt 平均耗时：" + bcryptTime + " 纳秒");
    }
}

// 输出：
// MD5 平均耗时：150 纳秒
// SHA-256 平均耗时：200 纳秒
// BCrypt 平均耗时：80000000 纳秒 (80 毫秒)
```

**结论：BCrypt 比 MD5 慢了约 50 万倍！**

### 🎯 使用场景

#### 什么时候用 MD5？

- ✅ 文件校验（检查文件完整性）
- ✅ 快速查找（哈希表）
- ✅ 非敏感数据
- ❌ **绝对不要用于密码！**

#### 什么时候用 SHA-256？

- ✅ 数字签名
- ✅ 证书验证
- ✅ 区块链
- ❌ **不推荐用于密码**（除非配合 PBKDF2 等算法）

#### 什么时候用 BCrypt？

- ✅ **密码存储（首选）**
- ✅ 任何需要慢速哈希的场景
- ✅ 长期存储的敏感数据
- ❌ 文件校验（太慢了）
- ❌ 需要快速查找的场景

---

## 5. Java 中使用 BCrypt

### 🛠️ 依赖配置

#### Maven

```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

或者使用 Spring Security：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
    <version>6.2.0</version>
</dependency>
```

### 📝 基本用法

#### 方式 1：使用 jbcrypt 库

```java
import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    
    /**
     * 加密密码
     */
    public static String hashPassword(String plainPassword) {
        // gensalt(10) 表示成本因子为 10
        // 会自动生成随机盐值
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }
    
    /**
     * 验证密码
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // 防止传入无效的哈希值
            return false;
        }
    }
    
    public static void main(String[] args) {
        // 示例
        String password = "MySecret123!";
        
        // 加密
        String hashed = hashPassword(password);
        System.out.println("加密后：" + hashed);
        // 输出：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
        
        // 验证
        boolean correct = checkPassword(password, hashed);
        System.out.println("密码正确：" + correct);  // true
        
        boolean wrong = checkPassword("WrongPassword", hashed);
        System.out.println("密码错误：" + wrong);  // false
    }
}
```

#### 方式 2：使用 Spring Security

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SpringSecurityExample {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    public static String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }
    
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return encoder.matches(plainPassword, hashedPassword);
    }
}
```

### 🎯 实战应用

#### 在 Spring Boot 中的完整示例

**1. 实体类**

```java
@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String passwordHash;  // 存储加密后的密码
    
    private String email;
    
    private LocalDateTime createdAt;
}
```

**2. Service 层**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * 用户注册
     */
    @Transactional
    public User register(String username, String password, String email) {
        // 检查用户名是否存在
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        
        // 加密密码
        String hashedPassword = passwordEncoder.encode(password);
        
        // 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    /**
     * 用户登录
     */
    public User login(String username, String password) {
        // 查找用户
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("密码错误");
        }
        
        return user;
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        
        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
```

**3. 配置类**

```java
@Configuration
public class PasswordConfig {
    
    /**
     * BCrypt 编码器 Bean
     * cost factor = 10
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

**4. Controller 层**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
            );
            return ResponseEntity.ok("注册成功");
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(
                request.getUsername(),
                request.getPassword()
            );
            // 生成 token...
            return ResponseEntity.ok(user);
        } catch (BusinessException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
```

### 🔧 高级用法

#### 自定义盐值（不推荐，除非有特殊需求）

```java
// 通常不需要这样做，BCrypt 会自动生成随机盐值
// 但如果你需要确定性测试：

String fixedSalt = BCrypt.gensalt(10, new SecureRandom("fixed_seed".getBytes()));
String hashed = BCrypt.hashpw(password, fixedSalt);
```

#### 升级已有系统的成本因子

```java
@Service
public class PasswordUpgradeService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 检查是否需要重新加密（成本因子升级）
     */
    public boolean needsRehash(String hashedPassword) {
        // BCrypt 哈希值的前 7 个字符包含版本和成本信息
        // $2a$10$... 
        String currentVersion = hashedPassword.substring(0, 7);
        return !"$2a$10$".equals(currentVersion);
    }
    
    /**
     * 登录时自动升级加密
     */
    public User loginAndUpgrade(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 验证密码
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            // 如果需要升级，重新加密
            if (needsRehash(user.getPasswordHash())) {
                String newHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                user.setPasswordHash(newHash);
                userRepository.save(user);
            }
            return user;
        }
        
        throw new BusinessException("密码错误");
    }
}
```

---

## 6. 最佳实践

### ✅ DO（应该做的）

#### 1. 选择合适的成本因子

```
2024 年推荐：
- 普通应用：cost = 10（约 0.1 秒）
- 高安全应用：cost = 12（约 0.4 秒）
- 极高安全：cost = 14（约 1.6 秒）

原则：
- 用户体验：登录延迟不超过 1 秒
- 安全性：让黑客难以暴力破解
```

#### 2. 密码策略配合

```java
// BCrypt 不是万能的，还需要强密码策略
public void validatePassword(String password) {
    // 最小长度
    if (password.length() < 8) {
        throw new BusinessException("密码至少 8 位");
    }
    
    // 包含大小写字母、数字、特殊字符
    if (!password.matches(".*[A-Z].*")) {
        throw new BusinessException("必须包含大写字母");
    }
    
    if (!password.matches(".*[a-z].*")) {
        throw new BusinessException("必须包含小写字母");
    }
    
    if (!password.matches(".*\\d.*")) {
        throw new BusinessException("必须包含数字");
    }
    
    if (!password.matches(".*[!@#$%^&*].*")) {
        throw new BusinessException("必须包含特殊字符");
    }
    
    // 检查常见密码
    if (isCommonPassword(password)) {
        throw new BusinessException("不要使用常见密码");
    }
}
```

#### 3. 限制登录尝试

```java
@Service
public class LoginAttemptService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;
    
    /**
     * 记录登录失败
     */
    public void recordFailedAttempt(String username) {
        String key = "login:attempts:" + username;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
    }
    
    /**
     * 检查账户是否被锁定
     */
    public boolean isLocked(String username) {
        String key = "login:attempts:" + username;
        String attempts = redisTemplate.opsForValue().get(key);
        
        if (attempts == null) {
            return false;
        }
        
        return Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }
    
    /**
     * 重置失败计数
     */
    public void resetAttempts(String username) {
        String key = "login:attempts:" + username;
        redisTemplate.delete(key);
    }
}
```

#### 4. 定期安全检查

```java
@Component
public class SecurityAudit {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 检查弱密码（可选，有争议）
     */
    @Scheduled(cron = "0 0 2 * * 0")  // 每周日凌晨 2 点
    public void auditWeakPasswords() {
        List<User> users = userRepository.findAll();
        
        for (User user : users) {
            // 检查是否是常见密码（需要维护一个常见密码库）
            if (isWeakPassword(user.getPasswordHash())) {
                // 通知用户修改密码
                notifyUserToChangePassword(user);
            }
        }
    }
}
```

### ❌ DON'T（不应该做的）

#### 1. 不要双重加密

```java
// ❌ 错误做法
String hashed = md5(password);
String doubleHashed = bcrypt(hashed);

// ✅ 正确做法
String hashed = bcrypt(password);
```

#### 2. 不要自己实现加密算法

```java
// ❌ 千万不要这样做
public String myOwnEncryption(String password) {
    // 一些奇怪的操作...
    return weirdResult;
}

// ✅ 使用成熟的库
String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
```

#### 3. 不要存储明文密码

```java
// ❌ 错误
@Entity
public class User {
    private String password;  // 明文存储！
}

// ✅ 正确
@Entity
public class User {
    private String passwordHash;  // 存储哈希值
}
```

#### 4. 不要在日志中打印密码

```java
// ❌ 错误
log.info("用户登录，密码：" + password);

// ✅ 正确
log.info("用户登录，用户名：" + username);
```

---

## 7. 常见问题解答

### Q1: BCrypt 可以被破解吗？

**答：** 理论上不可破解，因为它是单向哈希。

但实际上，如果：
- 密码太简单（如 123456）
- 成本因子太低（如 cost=5）
- 硬件非常强大

黑客仍然可以通过暴力破解尝试所有可能的密码组合。

**解决方案：**
- 使用强密码（12 位以上，包含大小写、数字、特殊字符）
- 使用合适的成本因子（至少 cost=10）
- 配合登录限制（失败 5 次锁定账户）

### Q2: BCrypt 加密后的密码能解密吗？

**答：** **不能！** BCrypt 是单向哈希算法。

```
密码 → BCrypt → 哈希值
哈希值 ↛ 密码 （无法反向推导）
```

**忘记密码怎么办？**
- 提供"找回密码"功能（通过邮箱或手机验证）
- 不要试图"解密"密码

### Q3: 为什么每次加密结果都不一样？

**答：** 因为每次都会生成随机的盐值。

```java
BCrypt.hashpw("123456", BCrypt.gensalt()) 
→ $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

BCrypt.hashpw("123456", BCrypt.gensalt())
→ $2a$10$k87L.ZQvvDkGGN7EKxvmR.xDyHgKzJJKvxMaYiSBEAcCdwxkHN1fG
```

**这是正常的，也是安全的表现！**

验证时使用 `checkpw` 方法，它会自动处理盐值：

```java
BCrypt.checkpw("123456", "$2a$10$N9qo8uLOickgx2ZMRZoMye...") → true
BCrypt.checkpw("123456", "$2a$10$k87L.ZQvvDkGGN7EKxvmR...") → true
```

### Q4: 成本因子设置多少合适？

**答：** 取决于你的应用场景和硬件性能。

**推荐值（2024 年）：**

| 场景 | Cost | 耗时 | 安全性 |
|------|------|------|--------|
| 移动端 App | 8-10 | 0.05-0.1s | 中等 |
| Web 应用 | 10-12 | 0.1-0.4s | 高 |
| 金融系统 | 12-14 | 0.4-1.6s | 很高 |
| 军事/政府 | 14-16 | 1.6-6.4s | 极高 |

**如何测试？**

```java
long start = System.currentTimeMillis();
BCrypt.hashpw("test_password", BCrypt.gensalt(cost));
long elapsed = System.currentTimeMillis() - start;
System.out.println("Cost " + cost + " took " + elapsed + "ms");
```

在你的服务器上测试，选择耗时时在 0.1-0.5 秒之间的成本因子。

### Q5: 已有的 MD5 密码如何迁移到 BCrypt？

**答：** 渐进式迁移方案：

```java
@Service
public class MigrationService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder encoder;
    
    /**
     * 登录时自动迁移
     */
    public User loginAndMigrate(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("用户不存在"));
        
        // 先尝试用 BCrypt 验证
        if (encoder.matches(password, user.getPasswordHash())) {
            return user;
        }
        
        // 如果是 MD5 格式，验证并升级
        if (isMd5Format(user.getPasswordHash())) {
            String md5Hash = user.getPasswordHash();
            String expectedMd5 = DigestUtils.md5Hex(password);
            
            if (md5Hash.equals(expectedMd5)) {
                // 升级为 BCrypt
                String newHash = encoder.encode(password);
                user.setPasswordHash(newHash);
                userRepository.save(user);
                return user;
            }
        }
        
        throw new BusinessException("密码错误");
    }
    
    private boolean isMd5Format(String hash) {
        return hash.length() == 32 && hash.matches("[a-f0-9]+");
    }
}
```

**迁移流程：**

```
1. 用户用旧密码登录
2. 验证 MD5 密码
3. 如果正确，用 BCrypt 重新加密
4. 更新数据库
5. 下次登录时已经是 BCrypt 了
```

### Q6: BCrypt 有什么缺点吗？

**答：** 有以下几个缺点：

1. **速度慢**（这也是优点）
   - 不适合需要快速哈希的场景
   
2. **内存占用较大**
   - 比 MD5、SHA 占用更多内存
   
3. **输出较长**
   - 60 个字符，比 MD5（32 字符）长
   
4. **不支持并行优化**
   - 难以利用多核 CPU 加速

但这些缺点对于密码存储来说都不是问题。

### Q7: 除了 BCrypt，还有哪些密码哈希算法？

**答：** 常见的有：

1. **Argon2**（最新推荐）
   - 2015 年密码哈希大赛冠军
   - 抗 GPU、抗 ASIC 攻击
   - Java 支持：`argon2-jvm`

2. **scrypt**
   - 内存密集型算法
   - 比 BCrypt 更抗 GPU 攻击
   - Java 支持：`SCrypt`

3. **PBKDF2**
   - NIST 推荐
   - 广泛支持
   - 但抗 GPU 能力不如 Argon2

**推荐优先级：**
```
Argon2 > scrypt > BCrypt > PBKDF2 >> SHA-256 >>> MD5
```

**现状：**
- BCrypt 最成熟，应用最广泛
- Argon2 是未来趋势，但生态还在完善中

---

## 🎯 总结

### 核心要点

1. **永远不要用 MD5 存储密码！**
2. **BCrypt 专为密码设计，内置盐值和成本因子**
3. **BCrypt 故意很慢，让黑客难以暴力破解**
4. **成本因子建议 10-12，根据服务器性能调整**
5. **配合强密码策略和登录限制**

### 最佳实践清单

- ✅ 使用 BCrypt 或 Argon2
- ✅ 成本因子 ≥ 10
- ✅ 密码最小长度 8 位
- ✅ 限制登录失败次数
- ✅ 使用 HTTPS 传输
- ✅ 定期安全审计
- ❌ 不要存储明文密码
- ❌ 不要在日志中打印密码
- ❌ 不要自己发明加密算法

### 代码模板

```java
// 直接复制使用
@Service
public class PasswordService {
    
    @Autowired
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
    
    public String hash(String password) {
        return encoder.encode(password);
    }
    
    public boolean check(String password, String hash) {
        return encoder.matches(password, hash);
    }
}
```

---

## 📚 参考资料

- [BCrypt 论文](https://www.usenix.org/legacy/events/usenix99/provos/provos.pdf)
- [Spring Security 文档](https://docs.spring.io/spring-security/reference/)
- [OWASP 密码存储指南](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [jbcrypt GitHub](https://github.com/jeremyh/jBCrypt)

---

**记住：安全无小事，从正确使用 BCrypt 开始！** 🔒
