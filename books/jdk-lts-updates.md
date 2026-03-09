# JDK 长期支持版本功能更新手册

## 目录

1. [JDK 版本概述](#jdk-版本概述)
2. [JDK 8 (LTS) - 2014 年 3 月](#jdk-8-lts---2014-年-3-月)
3. [JDK 11 (LTS) - 2018 年 9 月](#jdk-11-lts---2018-年-9-月)
4. [JDK 17 (LTS) - 2021 年 9 月](#jdk-17-lts---2021-年-9-月)
5. [JDK 21 (LTS) - 2023 年 9 月](#jdk-21-lts---2023-年-9-月)
6. [版本迁移指南](#版本迁移指南)
7. [性能对比](#性能对比)

---

## JDK 版本概述

### 什么是 LTS 版本？
LTS（Long-Term Support）长期支持版本，Oracle 提供数年甚至数十年的安全更新和技术支持。

### Java 发布周期
- **Java 7**: 2011 年 7 月
- **Java 8 (LTS)**: 2014 年 3 月 ⭐
- **Java 9**: 2017 年 9 月
- **Java 10**: 2018 年 3 月
- **Java 11 (LTS)**: 2018 年 9 月 ⭐
- **Java 12-16**: 每 6 个月发布一个版本
- **Java 17 (LTS)**: 2021 年 9 月 ⭐
- **Java 18-20**: 每 6 个月发布一个版本
- **Java 21 (LTS)**: 2023 年 9 月 ⭐

---

## JDK 8 (LTS) - 2014 年 3 月

**支持期限**: 至少到 2030 年 12 月（Oracle）

### 核心特性

#### 1. Lambda 表达式
```java
// 之前的写法
List<String> filtered = new ArrayList<>();
for (String s : list) {
    if (s.startsWith("A")) {
        filtered.add(s);
    }
}

// Lambda 表达式
List<String> filtered = list.stream()
    .filter(s -> s.startsWith("A"))
    .collect(Collectors.toList());
```

#### 2. Stream API
```java
// 函数式数据处理
List<Integer> squares = numbers.stream()
    .map(n -> n * n)
    .filter(n -> n > 10)
    .collect(Collectors.toList());

// 并行流
int sum = numbers.parallelStream()
    .mapToInt(Integer::intValue)
    .sum();
```

#### 3. 新的日期时间 API
```java
// 不可变的日期时间类
LocalDate date = LocalDate.now();
LocalTime time = LocalTime.now();
LocalDateTime dateTime = LocalDateTime.now();

// 日期计算
LocalDate tomorrow = date.plusDays(1);
LocalDate lastMonth = date.minusMonths(1);

// 格式化
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = dateTime.format(formatter);
```

#### 4. Optional 类
```java
// 避免空指针异常
Optional<String> optional = Optional.ofNullable(value);
String result = optional.orElse("default");

// 链式调用
String name = user.map(User::getAddress)
    .map(Address::getCity)
    .orElse("Unknown");
```

#### 5. 默认方法
```java
public interface Collection<T> {
    default void forEach(Consumer<? super T> action) {
        for (T element : this) {
            action.accept(element);
        }
    }
}
```

#### 6. Nashorn JavaScript 引擎
```java
ScriptEngine engine = new ScriptEngineManager()
    .getEngineByName("nashorn");
engine.eval("print('Hello, World!')");
```

#### 7. 新的并发工具
- `CompletableFuture` - 异步编程
- `StampedLock` - 优化的读写锁
- `LongAdder` - 高性能计数器

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .thenApply(data -> process(data))
    .thenAccept(result -> System.out.println(result));
```

#### 8. JVM 改进
- **G1 GC**: 新一代垃圾收集器
- **PermGen 移除**: 使用 Metaspace 替代
- **参数优化**: 更多的性能调优选项

### 性能提升
- 反射性能提升 35%
- 字符串操作性能优化
- 更好的类加载机制

---

## JDK 11 (LTS) - 2018 年 9 月

**支持期限**: 至少到 2026 年 9 月（Oracle）

### 从 JDK 9/10 继承的重要特性

#### 1. 模块系统（Project Jigsaw）
```java
// module-info.java
module com.example.mymodule {
    requires java.sql;
    exports com.example.api;
}
```

#### 2. JShell (REPL)
```bash
jshell> String greeting = "Hello"
greeting ==> "Hello"

jshell> greeting.length()
$2 ==> 5
```

#### 3. 接口私有方法
```java
public interface MyInterface {
    private void helper() {
        // 私有实现
    }
    
    default void publicMethod() {
        helper();
    }
}
```

### JDK 11 新增特性

#### 4. HTTP Client (标准化)
```java
HttpClient client = HttpClient.newHttpClient();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/data"))
    .GET()
    .build();

HttpResponse<String> response = client.send(
    request, 
    HttpResponse.BodyHandlers.ofString()
);

// 异步请求
client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
    .thenApply(HttpResponse::body)
    .thenAccept(System.out::println);
```

#### 5. String 新方法
```java
// isBlank() - 检查是否为空或只包含空白符
"   ".isBlank(); // true

// lines() - 按行分割
stream = text.lines();

// strip() - 去除空白（支持 Unicode）
"  hello  ".strip();

// repeat() - 重复字符串
"Ja".repeat(3); // "JaJaJa"
```

#### 6. Files 新方法
```java
// 读取字符串
String content = Files.readString(path);

// 写入字符串
Files.writeString(path, "content");
```

#### 7. Predicate 的 not()
```java
// 取反
list.stream().filter(Predicate.not(String::isEmpty));

// 等价于
list.stream().filter(s -> !s.isEmpty());
```

#### 8. 局部变量类型推断（增强）
```java
// JDK 10 引入，JDK 11 增强
var list = new ArrayList<String>();
var map = new HashMap<String, Integer>();

// Lambda 参数中使用
Function<String, Integer> func = (var str) -> str.length();
```

#### 9. Epsilon GC
```bash
# 适用于短期运行的程序
java -XX:+UseEpsilonGC -jar app.jar
```

#### 10. ZGC (实验性)
- 超低延迟的垃圾收集器
- 停顿时间 < 10ms
- 支持 TB 级堆内存

### 移除的特性
- JavaFX 模块
- CORBA 模块
- Java EE 模块

---

## JDK 17 (LTS) - 2021 年 9 月

**支持期限**: 至少到 2029 年 9 月（Oracle）

### 核心特性

#### 1. Sealed Classes（密封类）
```java
// 限制哪些类可以继承
public sealed class Shape permits Circle, Square, Triangle {
    // ...
}

public final class Circle extends Shape {
    // ...
}

public non-sealed class Square extends Shape {
    // 可以继续被继承
}
```

#### 2. Pattern Matching for instanceof（增强）
```java
// JDK 14-16 预览，JDK 17 正式
if (obj instanceof String s) {
    System.out.println(s.toLowerCase());
} else {
    System.out.println("Not a string");
}

// 可以在后续代码中使用变量 s
```

#### 3. Records（记录类）
```java
// JDK 14-16 预览，JDK 17 正式
public record Point(int x, int y) {
    // 自动生成 constructor, getters, equals, hashCode, toString
    
    // 紧凑构造函数
    public Point {
        if (x < 0) throw new IllegalArgumentException();
    }
}

// 使用
Point p = new Point(10, 20);
int x = p.x();
```

#### 4. Switch 表达式
```java
// JDK 12-14 预览，JDK 17 正式
String result = switch (day) {
    case MONDAY, FRIDAY -> "Work";
    case SATURDAY, SUNDAY -> "Weekend";
    default -> "Unknown";
};

// 使用 yield
int days = switch (month) {
    case FEBRUARY -> isLeap ? 29 : 28;
    case APRIL, JUNE, SEPTEMBER, NOVEMBER -> 30;
    default -> {
        System.out.println("Default case");
        yield 31;
    }
};
```

#### 5. Text Blocks（文本块）
```java
// JDK 13-15 预览，JDK 17 正式
String json = """
    {
        "name": "John",
        "age": 30,
        "city": "New York"
    }
    """;

String html = """
    <html>
        <body>
            <p>Hello, World</p>
        </body>
    </html>
    """;
```

#### 6. 新的 GC 特性
- **ZGC 生产就绪**: 可扩展的低延迟 GC
- **CMS GC 移除**: 已废弃多年
- **Parallel GC 优化**: 更好的吞吐量

#### 7. macOS/AArch64 支持
- 原生支持 Apple Silicon M1/M2 芯片
- 更好的 ARM 架构优化

#### 8. 强封装 JDK 内部 API
- 默认禁止访问内部 API
- 提高安全性和可维护性

### 性能提升
- G1 GC 性能提升 15-20%
- C2 编译器优化
- 更好的内联缓存

---

## JDK 21 (LTS) - 2023 年 9 月

**支持期限**: 至少到 2031 年 9 月（Oracle）

### 革命性特性

#### 1. Virtual Threads（虚拟线程）⭐
```java
// 轻量级线程，百万级并发
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        Thread.sleep(Duration.ofSeconds(1));
        return "Result";
    });
}

// 创建虚拟线程
Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
});

// 与传统线程对比
// 平台线程：1 万 + 消耗大量内存
// 虚拟线程：100 万 + 只需少量内存
```

#### 2. Structured Concurrency（结构化并发）
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<User> userFuture = scope.fork(this::findUser);
    Future<Order> orderFuture = scope.fork(this::findOrder);
    
    scope.join();
    scope.throwIfFailed();
    
    // 处理结果
    process(userFuture.get(), orderFuture.get());
}
```

#### 3. Pattern Matching for Switch（模式匹配）
```java
// JDK 17-20 预览，JDK 21 正式
static String format(Object obj) {
    return switch (obj) {
        case Integer i -> String.format("int %d", i);
        case Long l -> String.format("long %d", l);
        case Double d -> String.format("double %.2f", d);
        case String s -> String.format("String %s", s);
        default -> obj.toString();
    };
}

// 守卫条件
return switch (obj) {
    case Point(int x, int y) when x > 0 && y > 0 -> "第一象限";
    case Point(int x, int y) when x < 0 && y > 0 -> "第二象限";
    case Point(int x, int y) -> "其他象限";
    default -> "未知";
};
```

#### 4. Sequenced Collections（有序集合）
```java
// List, Set, Queue, Map 的新方法
List<Integer> list = new ArrayList<>(List.of(1, 2, 3));

// 获取首尾元素
Integer first = list.getFirst();
Integer last = list.getLast();

// 反向迭代
for (var it = list.reversed().iterator(); it.hasNext(); ) {
    System.out.println(it.next());
}

// 添加首尾元素
list.addFirst(0);
list.addLast(4);

// Map 也有类似方法
Map<String, Integer> map = new LinkedHashMap<>();
map.putFirst("a", 1);
map.putLast("z", 26);
```

#### 5. Record Patterns（记录模式）
```java
record Point(int x, int y) {}
record ColoredPoint(Point p, Color c) {}

// 嵌套模式匹配
void process(ColoredPoint cp) {
    if (cp instanceof ColoredPoint(Point(int x, int y), _)) {
        System.out.println("x=" + x + ", y=" + y);
    }
}

// switch 中解构
return switch (shape) {
    case Circle(double r) -> Math.PI * r * r;
    case Rectangle(double w, double h) -> w * h;
    default -> 0;
};
```

#### 6. Unnamed Classes（匿名类）⭐
```java
// 不需要 class 关键字
class Point {
    int x, y;
    
    // 隐式构造函数
    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    // 实例初始化
    {
        System.out.println("Creating point");
    }
}
```

#### 7. Unnamed Variables & Patterns（匿名变量）
```java
// 使用 _ 表示不需要的变量
List<String> list = Arrays.asList("a", "b", "c");
for (var _ : list) {
    System.out.println("Processing...");
}

// catch 块中
try {
    process();
} catch (Exception _) {
    System.out.println("Something failed");
}
```

#### 8. String Templates（字符串模板 - 预览）
```java
// 预览特性，需启用 --enable-preview
String name = "World";
String message = STR."Hello, \{name}!";

// 多行模板
String query = STR."""
    SELECT * FROM users
    WHERE id = \{userId}
    AND status = '\{status}'
    """;
```

#### 9. ZGC 改进
- **分代 ZGC**: 提升吞吐量
- **无分代 ZGC**: 更低延迟
- 支持更大的堆内存（TB 级别）

### 性能飞跃
- 虚拟线程：吞吐量提升 10-100 倍
- ZGC：延迟降低到亚毫秒级
- 启动时间优化 30%

---

## 版本迁移指南

### JDK 8 → JDK 11

#### 兼容性检查
```bash
# 使用 jdeps 分析依赖
jdeps --analyze-modules myapp.jar

# 检查使用的 API
jdeps -jdkinternals myapp.jar
```

#### 常见问题
1. **移除的模块**: JavaEE, CORBA, JavaFX
2. **内部 API 封装**: 使用 --add-opens 临时解决
3. **GC 变更**: CMS 移除，切换到 G1

#### 迁移步骤
```bash
# 1. 更新构建工具
# Maven
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>

# Gradle
sourceCompatibility = '11'
targetCompatibility = '11'

# 2. 替换废弃 API
Runtime.getRuntime().exec() → ProcessBuilder

# 3. 测试所有功能
mvn clean test
```

### JDK 11 → JDK 17

#### 新语法迁移
```java
// 1. 将数据类改为 Record
public class User {
    private final String name;
    private final int age;
    // getters, equals, hashCode...
}
// ↓
public record User(String name, int age) {}

// 2. 使用 switch 表达式
String type = switch (object) {
    case String s -> "string";
    case Integer i -> "integer";
    default -> "unknown";
};

// 3. 使用 sealed classes 限制继承
```

### JDK 17 → JDK 21

#### 虚拟线程迁移
```java
// 传统线程池
ExecutorService executor = Executors.newFixedThreadPool(200);

// 虚拟线程
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// 注意事项
// ✓ I/O 密集型任务效果最佳
// ✓ 避免长时间 CPU 计算
// ✓ 不要与线程池混用
```

### 构建工具配置

#### Maven
```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
</properties>

<dependencies>
    <!-- 预览特性需要 -->
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <configuration>
            <compilerArgs>
                <arg>--enable-preview</arg>
            </compilerArgs>
        </configuration>
    </plugin>
</dependencies>
```

#### Gradle
```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile) {
    options.compilerArgs += ['--enable-preview']
}
```

---

## 性能对比

### 启动时间对比（相对值）

| 版本 | 启动时间 | 内存占用 |
|------|---------|---------|
| JDK 8 | 100% | 100% |
| JDK 11 | 85% | 90% |
| JDK 17 | 75% | 85% |
| JDK 21 | 70% | 80% |

### 吞吐量对比

| 场景 | JDK 8 | JDK 11 | JDK 17 | JDK 21 |
|------|-------|--------|--------|--------|
| Web 应用 | 100% | 115% | 130% | 150%* |
| 批处理 | 100% | 110% | 125% | 140% |
| 高并发 I/O | 100% | 120% | 135% | 300%* |

*使用虚拟线程

### GC 性能对比

| GC 类型 | JDK 8 | JDK 11 | JDK 17 | JDK 21 |
|---------|-------|--------|--------|--------|
| G1 停顿时间 | 50ms | 40ms | 30ms | 25ms |
| ZGC 停顿时间 | N/A | <10ms | <5ms | <1ms |
| 最大堆支持 | 64GB | 4TB | 16TB | 16TB+ |

### 推荐升级路径

```
JDK 8 用户:
├─ 保守方案：继续 JDK 8（到 2030 年）
├─ 推荐方案：升级到 JDK 17（最成熟 LTS）
└─ 激进方案：直接 JDK 21（最新特性）

JDK 11 用户:
├─ 推荐方案：升级到 JDK 17
└─ 前瞻方案：升级到 JDK 21

JDK 17 用户:
└─ 推荐方案：升级到 JDK 21（虚拟线程）
```

---

## 附录：各版本速查表

### 特性时间线
```
JDK 8  (2014): Lambda, Stream, 新日期 API, Optional
JDK 9  (2017): 模块系统，JShell, 接口私有方法
JDK 10 (2018): var 类型推断
JDK 11 (2018): HTTP Client, String 新方法，Files 读写
JDK 12-16:     Switch 表达式，Records, instanceof 模式匹配
JDK 17 (2021): Sealed Classes, 模式匹配增强，文本块
JDK 18-20:     外部函数 API，向量 API，模式匹配增强
JDK 21 (2023): 虚拟线程，结构化并发，记录模式，有序集合
```

### 常用命令
```bash
# 查看 Java 版本
java -version
javac -version

# 编译时指定版本
javac --release 11 MyClass.java

# 运行预览特性
java --enable-preview -cp target/classes MyApp

# 查看支持的版本
java --list-modules

# 迁移分析工具
jdeprscan --for-removal myapp.jar
jdeps --jdk-internals myapp.jar
```

---

## 参考资源

- [OpenJDK 官网](https://openjdk.org/)
- [Oracle JDK 下载](https://www.oracle.com/java/technologies/downloads/)
- [JDK 发布计划](https://openjdk.org/projects/jdk/)
- [Java Release Notes](https://docs.oracle.com/en/java/javase/)
- [性能基准测试](https://mkittler.github.io/blog/posts/java-gc-performance/)
