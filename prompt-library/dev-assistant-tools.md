# 开发辅助工具提示词

## AI 编程助手使用技巧

### 代码生成
```
请帮我生成以下功能的代码：

【上下文提供】
1. 项目背景和技术栈
2. 已有代码结构
3. 需要实现的功能
4. 特殊要求和约束

【生成要求】
1. 遵循项目现有代码风格
2. 包含必要的注释
3. 考虑异常处理
4. 提供单元测试示例
5. 标注潜在的性能优化点

【迭代优化】
- 如果生成的代码不满意，明确指出需要修改的地方
- 可以要求 AI 解释某段代码的逻辑
- 可以让 AI 提供多种实现方案进行对比

示例请求：
"请基于 Spring Boot 和 MyBatis-Plus，为用户管理模块生成完整的 CRUD 代码，
包括 Controller、Service、Mapper、Entity，要求：
1. 使用 JWT 进行认证
2. 密码使用 BCrypt 加密
3. 支持批量删除
4. 支持按条件分页查询
5. 包含参数校验"

请生成：[具体代码需求]
```

### 代码审查与优化
```
请帮我审查和优化这段代码：

【审查要点】
1. 代码规范
   - 命名是否规范
   - 格式是否统一
   - 注释是否充分

2. 潜在问题
   - 空指针风险
   - 资源泄漏
   - 并发问题
   - 安全漏洞

3. 性能优化
   - 时间复杂度
   - 空间复杂度
   - 数据库查询优化
   - 缓存使用

4. 可维护性
   - 代码复用
   - 模块化程度
   - 可扩展性

【提供信息】
1. 代码的业务逻辑说明
2. 预期的性能指标
3. 当前遇到的问题（如果有）

示例请求：
"请审查这段用户服务代码，重点关注：
1. 事务使用是否正确
2. 是否有 N+1 查询问题
3. 缓存使用是否合理
4. 异常处理是否完善"

请审查：[具体代码]
```

### Bug 排查
```
我遇到了一个 Bug，请协助排查：

【问题描述】
1. 错误现象（截图/日志）
2. 复现步骤
3. 预期行为 vs 实际行为
4. 发生频率

【环境信息】
1. 操作系统
2. JDK 版本
3. 框架版本
4. 数据库版本

【已尝试的解决方案】
列出已经尝试过的方法和结果

【相关代码】
提供相关的代码片段

示例请求：
"用户在调用支付接口时偶尔会收到 500 错误，日志显示：
'Cannot determine embedded database driver class for database type NONE'

环境：Spring Boot 2.7 + MySQL 8.0
发生频率：约 10% 的请求
已检查：数据库连接配置正确

请分析可能的原因和解决方案。"

请排查：[具体 Bug]
```

## 文档生成

### API 文档编写
```
请帮我编写 API 接口文档：

【文档结构】
1. 接口基本信息
   - 接口名称
   - 接口路径
   - HTTP 方法
   
2. 请求参数
   - Header
   - Path Variables
   - Query Parameters
   - Request Body（含示例）
   
3. 响应参数
   - 成功响应（含示例）
   - 失败响应（含示例）
   - 错误码说明
   
4. 业务说明
   - 功能描述
   - 使用场景
   - 注意事项
   
5. 权限说明
   - 是否需要登录
   - 需要的角色权限

【Swagger 注解示例】
```java
@ApiOperation("创建用户")
@PostMapping
public Result<UserVO> createUser(
    @ApiParam("用户信息") @Validated @RequestBody UserDTO userDTO) {
   return Result.success(userService.createUser(userDTO));
}
```

示例请求：
"请为以下接口编写详细的 API 文档：
POST /api/v1/orders
功能：创建订单
需要包含：请求参数说明、响应示例、错误码列表"

请编写：[具体接口文档]
```

### 技术方案文档
```
请帮我编写技术方案文档：

【文档结构】
1. 背景和目标
   - 业务背景
   - 技术目标
   - 非技术目标
   
2. 需求分析
   - 功能需求
   - 非功能需求
   - 约束条件
   
3. 方案设计
   - 架构设计（架构图）
   - 模块划分
   - 接口设计
   - 数据设计（ER 图）
   
4. 技术选型
   - 技术栈选择
   - 选型理由
   - 替代方案对比
   
5. 详细设计
   - 核心流程（流程图/时序图）
   - 关键算法
   - 数据结构
   
6. 实施计划
   - 里程碑
   - 人员分工
   - 时间安排
   
7. 风险评估
   - 技术风险
   - 业务风险
   - 应对措施
   
8. 测试策略
   - 测试范围
   - 测试类型
   - 验收标准

示例请求：
"请为一个在线支付系统编写技术方案文档，
要求支持支付宝、微信支付，
日订单量预计 100 万，
需要保证高可用和数据一致性"

请编写：[具体技术方案]
```

## 学习辅导

### 新技术学习
```
我想学习一项新技术，请提供学习路径：

【技术名称】
[e.g., Spring Cloud Alibaba, Kubernetes, React]

【当前基础】
[e.g., 熟悉 Spring Boot，了解 Docker 基础]

【学习目标】
[e.g., 
- 能够搭建微服务项目
- 理解核心组件原理
- 能够解决常见问题
- 通过相关认证考试]

【期望的学习方式】
- [ ] 理论学习（文档/书籍）
- [ ] 实践操作（Demo 项目）
- [ ] 视频教程
- [ ] 源码阅读

【可用时间】
[e.g., 每天 2 小时，持续 1 个月]

示例请求：
"我想在 1 个月内掌握 Spring Cloud Alibaba，
目前熟悉 Spring Boot 和 Spring Cloud Netflix，
希望重点学习 Nacos、Sentinel、Seata、RocketMQ，
请提供一个详细的学习计划和资源推荐"

请规划：[具体技术学习]
```

### 概念解释
```
请解释这个技术概念：

【概念名称】
[e.g., 分布式事务、CAP 定理、响应式编程]

【期望的解释深度】
- [ ] 入门级（是什么，为什么用）
- [ ] 进阶级（怎么用，注意事项）
- [ ] 专家级（底层原理，源码分析）

【相关知识背景】
[e.g., 了解数据库事务，不了解分布式系统]

【希望包含的内容】
- [ ] 定义和概念
- [ ] 应用场景
- [ ] 代码示例
- [ ] 优缺点分析
- [ ] 最佳实践
- [ ] 相关工具/框架

示例请求：
"请用通俗易懂的方式解释分布式事务的 CAP 理论和 BASE 理论，
包括：
1. 基本概念
2. 三者之间的关系
3. 实际应用中的权衡
4. 常见的分布式事务解决方案对比"

请解释：[具体概念]
```

## 面试准备

### 技术面试题库
```
请为我生成 Java 后端开发的面试题库：

【面试级别】
- [ ] 初级（1-3 年）
- [ ] 中级（3-5 年）
- [ ] 高级（5-8 年）
- [ ] 专家级（8 年以上）

【考察范围】
1. Java 基础
   - 集合框架
   - 并发编程
   - JVM 原理
   - IO/NIO
   
2. 框架技术
   - Spring/Spring Boot
   - Spring MVC
   - MyBatis/JPA
   
3. 数据库
   - MySQL
   - Redis
   - MongoDB
   
4. 分布式技术
   - 微服务
   - 消息队列
   - 分布式缓存
   - 分布式锁
   
5. 系统设计
   - 高并发
   - 高可用
   - 可扩展性

【题型要求】
- [ ] 概念题（简答题）
- [ ] 代码题（写代码/读代码）
- [ ] 场景题（设计方案）
- [ ] 故障排查题

示例请求：
"请生成一份中级 Java 开发工程师的面试题，
包含 20 道题，覆盖：
- Java 基础（5 题）
- Spring 框架（5 题）
- MySQL 和 Redis（5 题）
- 分布式系统（5 题）
每道题都要有参考答案和评分标准"

请生成：[具体面试题库]
```

### 简历优化
```
请帮我优化这份技术简历：

【目标岗位】
[e.g., 高级 Java 开发工程师]

【目标行业】
[e.g., 电商、金融、云计算]

【当前简历问题】
[e.g., 
- 项目经验描述不够突出
- 技术栈罗列不清晰
- 缺少量化成果]

【优化方向】
1. 专业技能描述
   - 技术栈分类整理
   - 熟练程度分级
   - 关键词优化
   
2. 项目经验优化
   - STAR 法则（情境、任务、行动、结果）
   - 突出技术难点
   - 量化成果
   
3. 自我评价
   - 突出优势
   - 匹配岗位需求
   - 展现职业规划

示例请求：
"请优化我的项目经历描述，使用 STAR 法则：
原描述：'负责开发了公司的电商平台，使用了 Spring Boot 和 Vue'
要求：
1. 突出技术挑战和解决方案
2. 量化业务成果（如 QPS、转化率提升等）
3. 体现个人贡献和技术成长"

请优化：[具体简历内容]
```

## 效率工具

### Git 工作流
```
请指导 Git 版本控制的最佳实践：

【Git 工作流选择】
1. Git Flow（适合传统发布流程）
2. GitHub Flow（适合持续部署）
3. GitLab Flow（结合两者优点）

【分支管理规范】
- main/master：生产环境分支
- develop：开发主分支
- feature/*: 功能分支
- release/*: 发布分支
- hotfix/*: 热修复分支

【Commit 规范】
格式：<type>(<scope>): <subject>

Type 类型：
- feat: 新功能
- fix: Bug 修复
- docs: 文档更新
- style: 代码格式调整
- refactor: 重构
- test: 测试相关
- chore: 构建/工具变动

示例：
feat(user): 添加用户注册功能
fix(order): 修复订单金额计算错误
docs(readme): 更新安装说明

【常用命令】
```bash
# 查看状态
git status
git log --oneline --graph --all

# 分支操作
git checkout -b feature/new-feature
git merge --no-ff develop
git rebase -i HEAD~3

# 暂存修改
git stash
git stash list
git stash pop

# 撤销操作
git reset --soft HEAD~1
git reset --mixed HEAD~1
git reset --hard HEAD~1
git revert <commit>
```

请指导：[具体 Git 使用场景]
```

### 命令行技巧
```
请提供一些常用的命令行技巧：

【Linux/Mac 常用命令】
1. 文件操作
   - find/grep/awk/sed
   - tree/ls/du/df
   
2. 系统监控
   - top/htop/vmstat/iostat
   - netstat/ss/lsof
   - ps/pstree
   
3. 文本处理
   - cat/tail/head
   - sort/uniq/wc
   - cut/diff/patch
   
4. 网络工具
   - curl/wget
   - ping/traceroute
   - telnet/nc
   - tcpdump

【PowerShell 常用命令】
1. 文件操作
   - Get-ChildItem
   - Copy-Item/Move-Item
   - Remove-Item
   
2. 内容处理
   - Get-Content
   - Select-String
   - Measure-Object
   
3. 进程管理
   - Get-Process
   - Stop-Process
   - Start-Process

【实用脚本示例】
```bash
# 查找占用磁盘空间最大的前 10 个文件
du -ah | sort -rh | head -n 10

# 统计日志中 ERROR 出现的次数
grep -c"ERROR" application.log

# 监控接口调用频率
watch -n 1 'curl -s http://localhost:8080/actuator/metrics/http.server.requests'

# 批量替换文件名中的字符
rename 's/old/new/g' *.txt
```

请提供：[具体命令需求]
```
