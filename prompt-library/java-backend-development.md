# Java 后端开发提示词

## 基础架构提示词

### Spring Boot 项目开发
```
你是一位资深 Java 后端开发工程师，精通 Spring Boot 框架。请根据以下要求完成任务：

【技术栈】
- Spring Boot 2.7+/3.x
- MyBatis/MyBatis-Plus
- MySQL 8.0+
- Redis
- 消息队列（RabbitMQ/Kafka/RocketMQ）
- Lombok
- Maven/Gradle

【开发规范】
1. 遵循 RESTful API 设计规范
2. 使用统一响应包装类 Result<T>
3. 全局异常处理 @RestControllerAdvice
4. 参数校验使用 Hibernate Validator
5. 日志使用 SLF4J + Logback
6. 数据库连接池使用 HikariCP

【代码风格】
- 类名：PascalCase
- 方法和变量：camelCase
- 常量：UPPER_SNAKE_CASE
- 包结构：com.company.project.module.controller/service/mapper/entity/config

请 [具体任务描述]
```

### Spring Cloud 微服务项目
```
你是一位微服务架构专家，精通 Spring Cloud 全家桶。请完成以下任务：

【技术栈】
- Spring Cloud Alibaba / Netflix
- Nacos（注册中心/配置中心）
- OpenFeign（服务调用）
- Sentinel（限流降级）
- Gateway（网关）
- Seata（分布式事务）
- Sleuth + Zipkin（链路追踪）

【架构原则】
1. 服务拆分遵循单一职责原则
2. 数据库按业务域拆分
3. 服务间通信优先使用 Feign
4. 实现完善的熔断降级机制
5. 分布式锁使用 Redisson
6. 接口幂等性设计

请 [具体任务描述]
```

## 分层开发提示词

### Controller 层开发
```
作为后端开发工程师，请创建 Controller 层的代码：

【要求】
1. 使用 @RestController 注解
2. 路径规划：/api/v1/{module}
3. HTTP 方法语义化：GET/POST/PUT/DELETE
4. 参数校验：@Validated + @RequestBody
5. 添加详细的 API 文档注释（Swagger/OpenAPI）
6. 记录请求日志
7. 统一返回 Result<T> 对象

【示例结构】
```java
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }
}
```

请创建：[具体功能描述]
```

### Service 层开发
```
请设计并实现 Service 层业务逻辑：

【要求】
1. 接口 + 实现分离（UserService + UserServiceImpl）
2. 使用 @Service 和 @Transactional 注解
3. 复杂业务需要事务控制
4. 业务异常使用自定义异常类
5. 避免在 Service 层出现 SQL 逻辑
6. 合理使用缓存（Redis）
7. 异步处理使用 @Async

【注意事项】
- 事务粒度控制
- 缓存穿透/击穿/雪崩解决方案
- 分布式锁场景识别
- 消息队列异步解耦

请实现：[具体业务逻辑]
```

### Mapper/DAO 层开发
```
请创建数据访问层代码：

【要求】
1. 使用 MyBatis/MyBatis-Plus
2. Mapper 接口继承 BaseMapper<T>
3. XML 方式编写复杂 SQL
4. 结果映射使用 ResultMap
5. 动态 SQL 使用 <if> <where> <foreach>
6. 分页使用 PageHelper 或 MyBatis-Plus PaginationInnerInterceptor
7. 防止 SQL 注入

【示例】
```xml
<mapper namespace="com.company.project.mapper.UserMapper">
    <resultMap id="UserResultMap" type="com.company.project.entity.User">
        <!-- 字段映射 -->
    </resultMap>
    
    <select id="selectByCondition" resultMap="UserResultMap">
        SELECT * FROM users
        <where>
            <if test="name != null">AND name = #{name}</if>
        </where>
    </select>
</mapper>
```

请创建：[具体 DAO 操作]
```

### Entity/DO 层设计
```
请设计数据库实体类：

【要求】
1. 使用 Lombok 简化代码（@Data, @Builder, etc.）
2. 添加序列化版本号
3. 字段添加详细注释
4. 使用 Jackson 注解处理日期格式化
5. 敏感字段添加脱敏注解
6. 逻辑删除使用 @TableLogic
7. 自动填充使用 @TableField(fill = FieldFill.INSERT)

【示例】
```java
@Data
@TableName("users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
```

请设计：[具体实体]
```

## 中间件使用提示词

### Redis 缓存设计
```
请设计 Redis 缓存方案：

【要求】
1. Key 命名规范：project:module:id (e.g., mall:user:1001)
2. 选择合适的数据结构（String/Hash/List/Set/ZSet）
3. 设置合理的过期时间
4. 缓存更新策略（Cache Aside/Read Through/Write Through）
5. 解决缓存穿透（布隆过滤器）
6. 解决缓存击穿（互斥锁）
7. 解决缓存雪崩（随机过期时间）

【使用场景】
- 热点数据缓存
- 分布式锁（Redisson）
- 计数器
- 排行榜（ZSet）
- 消息队列（List/Stream）

请实现：[具体缓存场景]
```

### 消息队列设计
```
请设计消息队列方案：

【要求】
1. 明确 MQ 类型（RabbitMQ/Kafka/RocketMQ）
2. 定义 Exchange/Topic 和 Queue
3. 消息序列化（JSON/Protobuf）
4. 消息确认机制（ACK）
5. 死信队列处理
6. 消息幂等性消费
7. 延迟队列场景

【配置示例】
```java
@Configuration
public class RabbitMQConfig {
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("order.exchange");
    }
    
    @Bean
    public Queue queue() {
        return QueueBuilder.durable("order.queue").build();
    }
}
```

请实现：[具体消息场景]
```

## 特定场景开发提示词

### 用户认证与授权
```
请实现用户认证授权功能：

【技术要求】
1. Spring Security + JWT
2. 密码加密（BCrypt）
3. Token 刷新机制
4. 基于 RBAC 的权限控制
5. 接口级别权限注解 @PreAuthorize
6. 单点登录（SSO）考虑
7. 防止重放攻击

【功能清单】
- 用户登录/登出
- JWT 生成与验证
- 权限动态加载
- 菜单权限控制
- 数据权限控制

请实现：[具体认证功能]
```

### 文件上传下载
```
请实现文件存储功能：

【要求】
1. 本地存储或 OSS（阿里云/七牛云/MinIO）
2. 文件类型校验（MIME Type + 魔数）
3. 文件大小限制
4. 文件名处理（UUID 避免冲突）
5. 分片上传（大文件）
6. 断点续传
7. 下载限速

【安全考虑】
- 防止 XSS（图片验证码）
- 防止上传可执行文件
- CDN 加速配置

请实现：[具体文件功能]
```

### 定时任务
```
请设计定时任务方案：

【可选方案】
1. Spring @Scheduled（简单任务）
2. Quartz（复杂调度）
3. XXL-Job（分布式任务调度）
4. Elastic-Job（弹性伸缩）

【要求】
1. 任务幂等性
2. 失败重试机制
3. 任务监控告警
4. 分布式环境下的任务分片
5. 避免任务堆积

请实现：[具体定时任务]
```

### 搜索引擎集成
```
请集成 Elasticsearch 搜索引擎：

【要求】
1. 使用 Spring Data Elasticsearch
2. 索引映射设计
3. 分词器选择（IK Analyzer）
4. 复杂查询 DSL
5. 高亮显示
6. 聚合分析
7. 数据同步（ canal + MQ / 定时任务）

【使用场景】
- 商品搜索
- 日志检索
- 全文检索

请实现：[具体搜索功能]
```

## 性能优化提示词

### 数据库优化
```
请优化数据库性能：

【优化方向】
1. 慢查询分析与索引优化
2. 执行计划 explain 分析
3. 表结构优化（范式与反范式权衡）
4. 分库分表策略（ShardingSphere）
5. 读写分离
6. SQL 语句优化（避免 N+1 查询）
7. 连接池参数调优

【监控指标】
- QPS/TPS
- 慢查询日志
- 锁等待时间
- 缓冲池命中率

请优化：[具体问题]
```

### 接口性能优化
```
请优化接口性能：

【优化手段】
1. 多级缓存（浏览器→CDN→Nginx→Redis→本地）
2. 异步处理（线程池/MQ）
3. 批量处理代替循环调用
4. 数据库连接优化
5. 减少锁竞争
6. 对象池化（Commons Pool）
7. 响应数据压缩（GZIP）

【监控工具】
- Arthas 诊断
- APM（Skywalking/Pinpoint）
- JProfiler

请优化：[具体接口]
```

## 安全防护提示词

### Web 安全
```
请加强 Web 安全防护：

【防护要点】
1. SQL 注入（预编译）
2. XSS 攻击（过滤 + 转义）
3. CSRF 攻击（Token 验证）
4. SSRF 攻击（URL 白名单）
5. 文件包含漏洞
6. 命令注入
7. 敏感信息加密传输（HTTPS）

【安全措施】
- 输入校验
- 输出编码
- CORS 配置
- 请求频率限制
- SQL 审计日志

请加固：[具体模块]
```

### 数据安全
```
请设计数据安全方案：

【要求】
1. 敏感数据加密存储（AES/RSA）
2. 数据传输加密（TLS/SSL）
3. 数据脱敏展示
4. 数据备份策略
5. 数据恢复预案
6. 访问审计日志
7. GDPR/隐私合规

【加密场景】
- 用户密码（BCrypt）
- 手机号/身份证（AES）
- 银行卡号（RSA + AES）

请实现：[具体数据安全需求]
```

## 代码质量提示词

### Code Review
```
请进行代码审查：

【审查要点】
1. 代码规范（阿里巴巴 Java 开发手册）
2. 潜在 Bug（空指针、资源未关闭）
3. 性能问题（循环查库、N+1）
4. 安全问题（SQL 注入、XSS）
5. 可读性（命名、注释、复杂度）
6. 可维护性（耦合度、扩展性）
7. 单元测试覆盖率

【工具辅助】
- SonarQube
- CheckStyle
- PMD
- FindBugs

请审查：[具体代码]
```

### 重构建议
```
请提供代码重构建议：

【重构方向】
1. 提取公共方法
2. 消除魔法值（枚举/常量）
3. 简化条件判断（策略模式/责任链）
4. 合并重复逻辑
5. 拆分大方法/大类
6. 优化异常处理
7. 改进命名

【设计模式应用】
- 工厂模式（对象创建）
- 单例模式（资源共享）
- 观察者模式（事件驱动）
- 模板方法模式（流程标准化）

请重构：[具体代码]
```
