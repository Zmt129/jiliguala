# 测试相关提示词

## 单元测试提示词

### JUnit 单元测试
```
你是一位资深测试工程师，请为 Java 代码编写单元测试：

【技术栈】
- JUnit 5
- Mockito（模拟对象）
- AssertJ（流式断言）
- Spring Boot Test

【测试原则】
1. FIRST 原则
   - Fast（快速）
   - Independent（独立）
   - Repeatable（可重复）
   - Self-validating（自验证）
   - Timely（及时）

2. AAA 模式
   - Arrange（准备）
   - Act（执行）
   - Assert（断言）

3. 测试覆盖率要求
   - 行覆盖率 > 80%
   - 分支覆盖率 > 70%
   - 核心业务 100% 覆盖

【测试范围】
1. Service 层业务逻辑
2. Controller 层参数校验
3. 工具类方法
4. 异常处理
5. 边界条件

【示例】
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("应该成功创建用户")
    void shouldCreateUserSuccessfully() {
        // Arrange
        User user = new User();
        user.setUsername("test");
        when(userMapper.insert(any())).thenReturn(1);
        
        // Act
        boolean result = userService.createUser(user);
        
        // Assert
        assertThat(result).isTrue();
        verify(userMapper, times(1)).insert(any());
    }
    
    @Test
    @DisplayName("用户名已存在时应抛出异常")
    void shouldThrowExceptionWhenUsernameExists() {
        // Arrange
        when(userMapper.selectByUsername("test")).thenReturn(new User());
        
        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(new User()))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessage("用户名已存在");
    }
}
```

请为以下代码编写单元测试：[具体代码]
```

### Spring Boot 集成测试
```
请编写 Spring Boot 集成测试：

【注解使用】
1. @SpringBootTest（加载完整上下文）
2. @WebMvcTest（仅测试 Controller）
3. @DataJpaTest（仅测试 Repository）
4. @JsonTest（仅测试 JSON 序列化）
5. @AutoConfigureMockMvc（自动配置 MockMvc）
6. @Sql（执行 SQL 脚本）
7. @DirtiesContext（重置上下文）

【测试场景】
1. REST API 接口测试
2. 数据库操作测试
3. 缓存操作测试
4. 消息队列测试
5. 事务回滚测试

【示例】
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("应该返回用户详情")
    void shouldReturnUserDetail() throws Exception {
        // Given
        Long userId = 1L;
        
        // When
        ResultActions result = mockMvc.perform(
            get("/api/v1/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
        );
        
        // Then
       result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data.username").value("test"));
    }
    
    @Test
    @DisplayName("创建用户应该成功")
    void shouldCreateUserSuccessfully() throws Exception {
        // Given
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("newuser");
        userDTO.setEmail("test@example.com");
        
        // When & Then
       mockMvc.perform(
                post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(userDTO))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(201));
    }
}
```

请编写集成测试：[具体场景]
```

## 接口测试提示词

### Postman 接口测试
```
请设计 Postman 接口测试方案：

【集合组织】
1. 按模块划分 Collection
2. 使用 Folder 组织子功能
3. 环境变量管理（dev/test/prod）
4. 全局变量存储 Token

【测试流程】
1. 请求参数配置
2. Pre-request Script（前置处理）
3. Tests（断言验证）
4. 自动化测试集合运行
5. CI/CD 集成

【断言示例】
```javascript
// 状态码断言
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// JSON 响应验证
pm.test("Response has user data", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.have.property("username");
});

// 提取变量供后续使用
pm.test("Extract token", function () {
    var jsonData = pm.response.json();
    pm.environment.set("token", jsonData.data.token);
});

// 性能测试
pm.test("Response time is acceptable", function () {
    pm.expect(pm.response.responseTime).to.be.below(200);
});
```

【自动化测试】
1. 使用 Newman 命令行运行
2. 生成 HTML 报告
3. 集成 Jenkins/GitLab CI

请设计接口测试：[具体接口集合]
```

### API 自动化测试框架
```
请搭建 API 自动化测试框架：

【技术选型】
1. Rest Assured（Java）
2. Pytest + Requests（Python）
3. Supertest（Node.js）

【Rest Assured 示例】
```java
public class UserApiTest {
    
    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
        RestAssured.basePath = "/api/v1";
    }
    
    @Test
    @DisplayName("获取用户信息测试")
    public void testGetUser() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer token123")
        .when()
            .get("/users/{id}", 1L)
        .then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.username", equalTo("test"))
            .time(lessThan(200L));
    }
    
    @Test
    @DisplayName("创建用户测试")
    public void testCreateUser() {
        Map<String, Object> requestBody = new HashMap<>();
       requestBody.put("username", "newuser");
       requestBody.put("email", "test@example.com");
        
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .body("code", equalTo(201));
    }
}
```

【框架要求】
1. 测试数据与脚本分离
2. 支持多环境切换
3. 测试报告生成（Allure）
4. 失败重试机制
5. 并行执行

请搭建：[具体测试框架]
```

## 性能测试提示词

### JMeter 性能测试
```
请设计 JMeter 性能测试方案：

【测试类型】
1. 负载测试（Load Testing）
2. 压力测试（Stress Testing）
3. 并发测试（Concurrency Testing）
4. 耐久性测试（Soak Testing）
5. 峰值测试（Spike Testing）

【测试指标】
1. TPS/QPS（每秒事务/查询数）
2. 响应时间（RT）
   - 平均响应时间
   - 90/95/99 百分位
3. 错误率
4. 资源利用率（CPU/Memory/Disk/Network）

【线程组配置】
1. Number of Threads（虚拟用户数）
2. Ramp-Up Period（启动时间）
3. Loop Count（循环次数）
4. Scheduler（调度器）

【监听器】
1. View Results Tree（查看结果树）
2. Aggregate Report（聚合报告）
3. Response Time Graph（响应时间图）
4. Simple Data Writer（数据写入）

【测试计划结构】
1. 线程组
2. 配置元件（CSV 数据文件、HTTP 请求默认值）
3. 定时器（思考时间）
4. 断言（响应断言、JSON 断言）
5. 前置处理器
6. 后置处理器
7. 监听器

请设计性能测试：[具体场景]
```

### 性能测试报告
```
请生成性能测试报告：

【报告内容】
1. 测试概述
   - 测试目标
   - 测试范围
   - 测试环境
   
2. 测试场景
   - 并发用户数
   - 测试持续时间
   - 业务场景比例
   
3. 测试结果
   - TPS 汇总
   - 响应时间统计
   - 错误率分析
   - 资源监控
   
4. 性能分析
   - 瓶颈定位
   - 趋势分析
   - 对比分析
   
5. 优化建议
   - 代码优化
   - 数据库优化
   - 架构优化
   - 资源配置

【性能基线】
- 核心接口 RT < 200ms
- 普通接口 RT < 500ms
- TPS 达到预期目标
- 错误率 < 0.1%
- CPU 使用率 < 70%
- 内存无泄漏

请生成报告：[具体测试数据]
```

## 端到端测试提示词

### Selenium UI 自动化
```
请编写 Selenium UI 自动化测试：

【技术栈】
1. Selenium WebDriver
2. TestNG/JUnit
3. Page Object 模式
4. Maven/Gradle

【Page Object 设计】
```java
public class LoginPage {
    private WebDriver driver;
    
    @FindBy(id = "username")
    private WebElement usernameInput;
    
    @FindBy(id = "password")
    private WebElement passwordInput;
    
    @FindBy(id = "loginBtn")
    private WebElement loginButton;
    
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    
    public void enterUsername(String username) {
        usernameInput.sendKeys(username);
    }
    
    public void enterPassword(String password) {
       passwordInput.sendKeys(password);
    }
    
    public HomePage clickLogin() {
        loginButton.click();
       return new HomePage(driver);
    }
    
    public HomePage login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
       return clickLogin();
    }
}
```

【测试用例】
```java
public class LoginTest {
    private WebDriver driver;
    private LoginPage loginPage;
    
    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver();
        driver.manage().window().maximize();
        loginPage = new LoginPage(driver);
    }
    
    @Test
    public void testLoginSuccessfully() {
        HomePage homePage = loginPage.login("admin", "123456");
        Assert.assertTrue(homePage.isLoggedIn());
    }
    
    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
```

请编写 UI 测试：[具体页面]
```

### Cypress 前端测试
```
请编写 Cypress 端到端测试：

【特点】
1. 自动等待
2. 时间旅行调试
3. 实时重载
4. 截图/录屏
5. 网络控制

【测试示例】
```javascript
// tests/e2e/login.spec.js
describe('登录测试', () => {
  beforeEach(() => {
    cy.visit('/login')
  })
  
  it('应该成功登录', () => {
    cy.get('#username').type('admin')
    cy.get('#password').type('123456')
    cy.get('#loginBtn').click()
    
    cy.url().should('include', '/home')
    cy.get('.welcome-message')
      .should('be.visible')
      .and('contain', '欢迎回来')
  })
  
  it('密码错误时显示提示', () => {
    cy.get('#username').type('admin')
    cy.get('#password').type('wrong')
    cy.get('#loginBtn').click()
    
    cy.get('.error-message')
      .should('be.visible')
      .and('contain', '密码错误')
  })
  
  it('表单验证', () => {
    cy.get('#loginBtn').click()
    
    cy.get('#username').should('have.class', 'is-invalid')
    cy.get('.invalid-feedback')
      .should('contain', '请输入用户名')
  })
})
```

【自定义命令】
```javascript
// support/commands.js
Cypress.Commands.add('login', (username, password) => {
  cy.session([username, password], () => {
    cy.visit('/login')
    cy.get('#username').type(username)
    cy.get('#password').type(password)
    cy.get('#loginBtn').click()
    cy.url().should('include', '/home')
  })
})

// 使用
beforeEach(() => {
  cy.login('admin', '123456')
})
```

请编写测试：[具体功能]
```

## 测试策略提示词

### 测试金字塔
```
请制定测试策略：

【测试金字塔模型】
1. 单元测试（70%）
   - 快速执行
   - 隔离测试
   - 高覆盖率
   
2. 集成测试（20%）
   - 模块间交互
   - 接口测试
   - 数据库集成
   
3. E2E 测试（10%）
   - 完整流程
   - 用户视角
   - 关键路径

【测试优先级】
P0：核心业务流程（必须自动化）
P1：重要功能（优先自动化）
P2：次要功能（逐步自动化）
P3：边缘场景（手动测试）

【CI/CD 集成】
1. 提交触发单元测试
2. 合并触发集成测试
3. 部署前 E2E 测试
4. 生产环境冒烟测试

请制定策略：[具体项目]
```

### 测试数据管理
```
请设计测试数据管理方案：

【数据来源】
1. 测试数据工厂（推荐）
2. CSV/Excel 文件
3. 数据库备份
4. API 动态生成
5. 第三方数据服务

【数据管理策略】
1. 数据隔离（每个测试独立数据）
2. 数据清理（测试后自动清理）
3. 数据版本控制
4. 敏感数据脱敏

【数据工厂示例】
```java
public class UserFactory {
    public static User createValidUser() {
        User user = new User();
        user.setUsername("test" + System.currentTimeMillis());
        user.setPassword("123456");
        user.setEmail("test@example.com");
        user.setPhone("13800138000");
       return user;
    }
    
    public static User createAdminUser() {
        User user = createValidUser();
        user.setRole(Role.ADMIN);
       return user;
    }
    
    public static User createDisabledUser() {
        User user = createValidUser();
        user.setStatus(Status.DISABLED);
       return user;
    }
}
```

请设计：[具体数据方案]
```

## 质量保障提示词

### Code Review 检查清单
```
请进行代码审查：

【功能正确性】
- [ ] 需求是否完全实现
- [ ] 边界条件是否处理
- [ ] 异常场景是否覆盖
- [ ] 业务逻辑是否正确

【代码质量】
- [ ] 命名是否规范
- [ ] 方法是否过长（< 50 行）
- [ ] 圈复杂度是否过高（< 10）
- [ ] 是否有重复代码
- [ ] 是否有死代码

【可读性】
- [ ] 注释是否充分
- [ ] 代码格式是否统一
- [ ] 变量命名是否有意义
- [ ] 是否有魔法值

【性能】
- [ ] 是否有 N+1 查询
- [ ] 是否有内存泄漏风险
- [ ] 是否有不必要的循环
- [ ] 是否合理使用缓存

【安全】
- [ ] 是否有 SQL 注入风险
- [ ] 是否有 XSS 漏洞
- [ ] 是否有 CSRF 防护
- [ ] 敏感数据是否加密

【测试】
- [ ] 单元测试是否充分
- [ ] 测试覆盖率是否达标
- [ ] 是否有集成测试
- [ ] 是否有性能测试

请审查：[具体代码]
```

### Bug 分析与预防
```
请分析 Bug 并提供预防方案：

【Bug 分类】
1. 功能缺陷
2. 性能问题
3. 安全漏洞
4. 兼容性问题
5. 用户体验问题

【根因分析】
1. 5 Why 分析法
2. 鱼骨图
3. 故障树分析

【预防措施】
1. 完善需求评审
2. 加强设计评审
3. 严格执行 Code Review
4. 提高测试覆盖率
5. 自动化回归测试
6. 建立 CheckList
7. 经验教训总结

请分析：[具体 Bug]
```
