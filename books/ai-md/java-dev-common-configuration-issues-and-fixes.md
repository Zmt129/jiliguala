# Java 开发中常见的软件/环境配置问题与解决办法（实战手册）

## 背景 / 目标

你在做 Java 开发时，经常会遇到“明明照着教程做了，但就是跑不起来”的情况。问题通常不在代码本身，而在 **软件安装、环境变量、构建工具、网络代理、证书、IDE 设置** 等配置上。

这篇文档的目标是：给你一份 **遇到问题就能照着排查** 的清单，按“现象 → 常见原因 → 排查 → 解决办法”写清楚。

## 适用范围

- 你用的是 **Windows（优先）/ macOS / Linux**
- 你写的是 **Java（JDK 8/11/17/21 都可能）**
- 常见构建工具：**Maven / Gradle**
- 常见框架：**Spring Boot**
- 常见中间件：**MySQL / Redis / Kafka**（以连接与配置问题为主）

---

## 0. 先记住：排查优先级（90% 的问题都在这几项）

你遇到“跑不起来/连不上/编译失败”时，先按这个顺序检查：

1. **JDK 版本是否正确**（项目需要 8，你装了 21；或者 IDE 用了另一个 JDK）
2. **环境变量与 PATH 是否指向同一个 JDK**
3. **Maven/Gradle 是否能下载依赖**（网络、代理、镜像、证书）
4. **IDE 是否开启注解处理（Lombok/MapStruct）**
5. **端口是否冲突 / 配置文件是否生效（profile）**

---

## 1. JDK/Java 环境问题

### 1.1 现象：`java` / `javac` 命令找不到

**常见报错**
- `'java' 不是内部或外部命令，也不是可运行的程序...`（Windows）
- `command not found: java`（macOS/Linux）

**常见原因**
- 没装 JDK（只装了 JRE 或什么都没装）
- PATH 没配
- 配了多个 JDK，PATH 指向了错误的那个

**排查（Windows PowerShell）**

```bash
where java
where javac
java -version
javac -version
echo $env:JAVA_HOME
```

**解决办法**
- 安装 JDK（建议选 LTS：11/17/21）
- 设置 `JAVA_HOME` 指向 JDK 根目录（例如 `C:\Program Files\Java\jdk-17`）
- 把 `%JAVA_HOME%\bin` 加到 PATH（Windows 系统环境变量）
- 重新打开终端（环境变量变更后需要新开窗口）

---

### 1.2 现象：项目要求 Java 8，但你用 Java 17/21 编译报错

**常见报错**
- `Unsupported class file major version XX`
- `release version 8 not supported`
- `Source option 8 is no longer supported. Use 11 or later.`

**常见原因**
- **运行时**或**编译时**的 JDK 版本与项目要求不一致
- 依赖/插件与新 JDK 不兼容

**排查**
- 先确认命令行 JDK：

```bash
java -version
javac -version
```

- 再确认 IDE 使用的 JDK（IDEA：Project SDK / Maven/Gradle JVM）

**解决办法（常用几种）**
- **方案 A（推荐）**：用项目要求的 JDK 版本开发（例如项目写明 Java 8）
- **方案 B**：保持高版本 JDK，但编译目标降级（需要构建工具配置）
  - Maven 用 `maven.compiler.release` 或 `source/target`
  - Gradle 用 `toolchain` + `options.release`

> 重要：**“运行用的 JDK”** 和 **“编译用的 JDK”** 可以不一样，但新手更建议先统一，减少坑。

---

### 1.3 现象：IDEA 里能跑，命令行跑不了（或反过来）

**常见原因**
- IDEA 配置了某个 JDK，但命令行用的是另一个
- IDEA 的运行参数（VM options / 环境变量）与命令行不一致

**解决办法**
- 在 IDEA 运行配置里把 JDK 明确指定
- 在命令行里也明确使用同一个 JDK（或修正 `JAVA_HOME`/PATH）
- 把关键配置写进项目的构建文件，而不是只写在 IDE 里

---

## 2. Maven 常见配置问题

### 2.1 现象：`mvn` 命令找不到 / Maven 版本不对

**排查**

```bash
mvn -v
where mvn
```

**解决办法**
- 安装 Maven，并把 `bin` 加到 PATH
- 或者使用项目自带的 Maven Wrapper（如果有 `mvnw`）

---

### 2.2 现象：依赖下载失败（超时、连接被拒绝、407 代理）

**常见报错**
- `Could not transfer artifact ...`
- `Connection timed out`
- `407 Proxy Authentication Required`
- `PKIX path building failed`（证书问题，见 2.3）

**常见原因**
- 网络不通/被墙/公司代理
- Maven 没配镜像或代理
- 私服需要账号密码

**排查**
- 看 Maven 本地仓库有没有在疯狂重试（默认在 `~/.m2/repository`）
- 检查是否有 `settings.xml`（通常在 `~/.m2/settings.xml`）

**解决办法（按场景）**
- **国内网络慢**：配置镜像（例如阿里云 Maven 镜像）到 `settings.xml`
- **公司代理**：在 `settings.xml` 配 `proxies`（带用户名密码）
- **私服（Nexus/Artifactory）**：在 `settings.xml` 配 `servers`（账号）+ `mirrors`（地址）

> 小建议：如果你不知道 `settings.xml` 在哪，就直接搜索你的用户目录下是否有 `.m2/settings.xml`。

---

### 2.3 现象：Maven 报 `PKIX path building failed`（HTTPS 证书错误）

**常见原因**
- 公司网络做了 HTTPS 代理/证书替换（中间人证书）
- 你访问的仓库用自签名证书
- 你 JDK 的证书库（cacerts）缺少对应 CA

**解决办法（推荐顺序）**
- **方案 A（推荐）**：让网络/运维给你公司的根证书（CA），导入到 JDK 的 `cacerts`
- **方案 B**：改用公司内网提供的可信仓库地址（Nexus/Artifactory）

> 不建议：为了省事直接“跳过证书校验”。这会带来安全风险，也常常在别的机器上又复现问题。

---

### 2.4 现象：依赖冲突/版本混乱，运行时报 `NoSuchMethodError` / `ClassNotFoundException`

**常见原因**
- 依赖树里同一个库出现了多个版本
- 运行时加载到的是旧版本

**排查（Maven）**

```bash
mvn -q -DskipTests dependency:tree
```

**解决办法**
- 用 `dependencyManagement` 统一版本
- 对冲突依赖做 `exclusions`
- 尽量使用框架提供的 BOM（比如 Spring Boot 的依赖管理）

---

## 3. Gradle 常见配置问题

### 3.1 现象：Gradle 下载慢/失败（Wrapper 卡住）

**常见原因**
- 网络访问 `services.gradle.org` 不稳定
- 代理/证书问题

**解决办法**
- 优先使用 Gradle Wrapper（项目里的 `gradlew`），版本更一致
- 在 `gradle.properties` 配置代理（如果你在公司网络）
- 配置镜像（部分环境会提供 gradle 分发包镜像）

---

### 3.2 现象：编译用的 JDK 不对（Gradle 用了别的 JVM）

**常见原因**
- Gradle Daemon 使用了系统默认 JVM
- IDEA 的 Gradle JVM 设置不一致

**解决办法**
- 用 Gradle Toolchain 明确指定目标 JDK（推荐）
- 或在 IDEA 里指定 Gradle JVM

---

## 4. IDE（IntelliJ IDEA）常见配置坑

### 4.1 现象：Lombok/MapStruct 相关代码全红，编译报“找不到符号”

**常见原因**
- IDEA 没开启 **Annotation Processing（注解处理）**
- 没装 Lombok 插件（或版本不兼容）
- 编译器不是 javac（某些设置会影响）

**解决办法（IDEA 方向）**
- 开启：`Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors` → 勾选 `Enable annotation processing`
- Lombok：安装插件 + 在依赖里加 Lombok（并设置为 `provided/compileOnly`）

---

### 4.2 现象：明明改了 `application.yml`，但运行时不生效

**常见原因**
- Spring Boot 使用了其他 profile（比如 `dev`/`prod`）
- 运行配置里指定了 `--spring.profiles.active=...`
- 配置被环境变量覆盖了

**排查**
- 看启动日志里是否有 `The following 1 profile is active: ...`
- 搜索启动参数里有没有 `spring.profiles.active`

**解决办法**
- 明确设置一个 profile，并把不同环境的配置分文件：`application-dev.yml`、`application-prod.yml`
- 尽量不要同时在多处设置同一个配置（会很乱）

---

## 5. Spring Boot 运行时常见配置问题

### 5.1 现象：端口被占用 `Port 8080 was already in use`

**常见原因**
- 另一个服务占用了 8080
- 同一个服务启动了两次

**排查（Windows）**

```bash
netstat -ano | findstr :8080
```

**解决办法**
- 结束占用端口的进程（用 PID）
- 或改端口：`server.port=8081`

---

### 5.2 现象：启动时报“连不上数据库/Redis/Kafka”

这类问题最关键的是两句话：

- **“你连的是谁？”**（host/port/用户名/密码/库名/协议）
- **“你从哪里连？”**（本机、Docker 容器、公司内网、远程服务器）

下面按中间件拆开讲。

#### 5.2.1 MySQL 连不上

**常见报错**
- `Communications link failure`
- `Access denied for user ...`
- `Unknown database ...`
- `Public Key Retrieval is not allowed`

**常见原因**
- MySQL 没启动 / 端口不对（默认 3306）
- 用户名密码错 / 没权限
- 数据库名写错
- MySQL 8 的认证/加密参数导致连接失败
- 你在 Docker 里跑应用，但连接写了 `localhost`（这会连到容器自己，不是宿主机）

**排查（先确认“目标 MySQL 能不能连”）**
- 用命令行或客户端工具连一下（例如 MySQL Workbench、DBeaver）
- 看 Spring Boot 的 `spring.datasource.url`、`username`、`password`

**解决办法（高频）**
- **Docker 场景**：
  - 应用在宿主机跑，MySQL 在 Docker：通常用 `localhost:3306`
  - 应用也在 Docker：不要写 `localhost`，改用 Docker 网络里的服务名（如 `mysql:3306`）
- **MySQL 8 连接参数**（遇到 `Public Key Retrieval...` 时常用）：
  - 在 JDBC URL 里加：`allowPublicKeyRetrieval=true&useSSL=false`
- **时区问题**（报时区相关错误时）：
  - JDBC URL 加：`serverTimezone=UTC` 或 `serverTimezone=Asia/Shanghai`

---

#### 5.2.2 Redis 连不上

**常见报错**
- `Unable to connect to Redis`
- `NOAUTH Authentication required`

**常见原因**
- Redis 没启动 / 端口不对（默认 6379）
- Redis 设置了密码，你没配
- 你连到了错误的环境（测试/生产）

**排查**
- 先用 `redis-cli` 试连（或用可视化客户端）
- 看 Spring 配置 `spring.data.redis.host/port/password`

**解决办法**
- 如果 Redis 有密码，务必在配置里写对 `password`
- Docker 场景同 MySQL：容器里不要用 `localhost` 连宿主机服务

---

#### 5.2.3 Kafka 连不上/生产消费失败（配置类）

**常见报错**
- `Connection to node ... could not be established`
- `Bootstrap broker ... disconnected`
- `Not authorized to access topics`

**常见原因**
- `bootstrap.servers` 写错（host/port）
- Kafka 的 `advertised.listeners` 配错（导致客户端拿到一个“连不上”的地址）
- 公司环境有 SASL/ACL，你没配认证

**排查**
- 先确认你在哪运行客户端（宿主机还是容器）
- 检查 Kafka 服务端对外暴露的地址是否能从你当前机器访问

**解决办法（本地最常见）**
- 本地单机：通常 `bootstrap.servers=localhost:9092`
- 如果 Kafka 在 Docker：确认端口映射以及 `advertised.listeners` 指向宿主机可达地址

> Kafka 细节很多，如果你是“本地从 0 跑通”，建议直接看同目录那篇：`kafka-usage-and-configuration.md`。

---

## 6. 编码/换行/中文乱码（非常常见）

### 6.1 现象：控制台输出中文是乱码 / 文件读出来乱码

**常见原因**
- 项目文件编码不是 UTF-8
- Windows 终端编码与 JVM 默认编码不一致
- 依赖/日志输出用了系统默认编码

**排查**
- 看 IDE 里文件编码（UTF-8 还是 GBK）
- 看 JVM 默认编码（可在启动参数里打印，或在日志中输出 `file.encoding`）

**解决办法（推荐）**
- **统一使用 UTF-8**：
  - IDEA：项目编码设置为 UTF-8
  - Maven：设置 `project.build.sourceEncoding=UTF-8`
  - 运行时：必要时加 JVM 参数 `-Dfile.encoding=UTF-8`

---

## 7. 时间/时区问题（接口时间差 8 小时）

### 7.1 现象：数据库里时间和接口返回差 8 小时（或少/多几个小时）

**常见原因**
- JVM 时区、数据库时区、JSON 序列化时区不一致

**排查**
- 看数据库时区（MySQL 的 `time_zone`）
- 看应用运行环境时区（服务器/容器）
- 看序列化配置（Jackson 时区）

**解决办法（建议选一种统一策略）**
- **统一用 UTC 存储 + 展示时转本地时区**（更规范，但需要你理解）
- 或者（新手更常用）：统一配置为 `Asia/Shanghai`（确保 DB、JVM、Jackson 都一致）

---

## 8. Windows 特有的坑（路径/权限/端口）

### 8.1 现象：路径里有空格导致工具失败（例如 `Program Files`）

**解决办法**
- 尽量把开发工具装在无空格路径（如 `C:\dev\jdk-17`、`D:\tools\maven`）
- 在命令行里引用路径时用引号包起来

### 8.2 现象：权限不足写不了文件/打不开端口

**解决办法**
- 尝试以管理员权限运行（只用于确认是否权限问题）
- 把项目放到自己有权限的盘符/目录（如 `D:\git\...`）

---

## 9. 最小化排错模板（你可以直接复制来问我）

当你卡住时，按下面格式把信息补齐，我能最快定位：

- **你在做什么**：运行/打包/测试/连接数据库/连 Kafka
- **你用的系统**：Windows/macOS/Linux
- **JDK 版本**：`java -version` 的输出
- **构建工具**：Maven/Gradle + 版本（`mvn -v` 或 `gradle -v`）
- **完整报错**：从第一行到最后一行（不要只截一行）
- **关键配置**：比如 `spring.datasource.url`（密码可以打码）


