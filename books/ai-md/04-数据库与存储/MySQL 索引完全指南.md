# MySQL 索引完全指南：从原理到实战

> 索引是数据库性能优化的核心，理解索引的工作原理和使用技巧，能让查询速度提升百倍。

---

## 📖 目录

1. [什么是索引？](#1-什么是索引)
2. [索引的数据结构](#2-索引的数据结构)
3. [索引的类型](#3-索引的类型)
4. [索引的使用](#4-索引的使用)
5. [联合索引与最左前缀](#5-联合索引与最左前缀)
6. [索引失效场景](#6-索引失效场景)
7. [索引优化实战](#7-索引优化实战)
8. [常见问题解答](#8-常见问题解答)

---

## 1. 什么是索引？

### 🤔 通俗解释

**索引就像书的目录：**

```
没有索引（全表扫描）：
要找"张三"的信息，需要从第一页翻到最后一页
100 万条数据 → 最多查找 100 万次

有索引（B+ 树查找）：
先查目录，直接定位到"张三"在哪一页
100 万条数据 → 只需查找 20 次左右（log₂(100万) ≈ 20）
```

**性能对比：**

| 数据量 | 全表扫描 | B+ 树索引 | 提升倍数 |
|--------|---------|----------|---------|
| 1,000 | 1,000 次 | 10 次 | 100 倍 |
| 10,000 | 10,000 次 | 14 次 | 714 倍 |
| 1,000,000 | 100 万次 | 20 次 | 50,000 倍 |
| 10,000,000 | 1000 万次 | 24 次 | 416,666 倍 |

### 🎯 索引的作用

✅ **优点：**
- 大幅提高查询速度
- 加速排序和分组
- 保证数据唯一性（唯一索引）

❌ **缺点：**
- 占用额外存储空间
- 降低写操作速度（INSERT/UPDATE/DELETE 需要维护索引）
- 过多的索引会影响性能

**原则：** 索引不是越多越好，要在查询性能和写入性能之间找到平衡。

---

## 2. 索引的数据结构

### 🌳 B+ 树（MySQL InnoDB 默认）

#### 为什么选择 B+ 树？

**对比其他数据结构：**

| 数据结构 | 查询效率 | 范围查询 | 磁盘 IO | 适用场景 |
|---------|---------|---------|--------|---------|
| **哈希表** | O(1) | ❌ 不支持 | - | 等值查询 |
| **二叉搜索树** | O(log n) | ✅ | 高（树太高） | 内存中 |
| **平衡二叉树** | O(log n) | ✅ | 高（树太高） | 内存中 |
| **B 树** | O(log n) | ✅ | 中 | 文件系统 |
| **B+ 树** | O(log n) | ✅ | **低**（树矮胖） | **数据库** |

#### B+ 树的特点

```
        [10, 20, 30]         ← 非叶子节点（只存索引）
       /    |    |    \
   [1,5]  [11,15] [21,25] [31,35]  ← 叶子节点（存数据）
     |      |      |      |
   数据   数据   数据   数据
```

**特点：**
1. **所有数据都在叶子节点** - 非叶子节点只存索引，不存数据
2. **叶子节点形成链表** - 方便范围查询
3. **树的高度低** - 通常 2-3 层，减少磁盘 IO
4. **每个节点存多个键** - 减少树的高度

**为什么适合数据库？**
- 磁盘 IO 是瓶颈，B+ 树高度低，IO 次数少
- 叶子节点链表，范围查询高效
- 一个节点大小 = 一页大小（16KB），充分利用预读

---

## 3. 索引的类型

### 📊 MySQL 索引分类

#### 3.1 按数据结构分类

| 类型 | 说明 | 适用场景 |
|------|------|---------|
| **B+ 树索引** | 默认索引类型 | 大部分场景 |
| **哈希索引** | Memory 引擎支持 | 等值查询 |
| **全文索引** | FULLTEXT | 全文搜索 |
| **R-Tree** | 空间索引 | 地理信息 |

#### 3.2 按物理存储分类

| 类型 | 说明 | 特点 |
|------|------|------|
| **聚簇索引** | 数据和索引在一起 | InnoDB 主键索引 |
| **非聚簇索引** | 索引和数据分离 | 二级索引 |

**聚簇索引 vs 非聚簇索引：**

```
聚簇索引（主键索引）：
┌─────────────────────────────┐
│  B+ 树叶子节点               │
│  ┌──────┬──────────┐        │
│  │ id=1 │ 完整行数据 │        │
│  ├──────┼──────────┤        │
│  │ id=2 │ 完整行数据 │        │
│  └──────┴──────────┘        │
└─────────────────────────────┘
特点：数据就在索引中，查询快


非聚簇索引（二级索引）：
┌─────────────────────────────┐
│  B+ 树叶子节点               │
│  ┌────────┬──────┐          │
│  │ name=张三│ id=1 │          │
│  ├────────┼──────┤          │
│  │ name=李四│ id=2 │          │
│  └────────┴──────┘          │
└─────────────────────────────┘
         ↓ 回表查询
┌─────────────────────────────┐
│  聚簇索引中获取完整数据       │
│  WHERE id=1                 │
└─────────────────────────────┘
特点：需要回表，多一次查询
```

#### 3.3 按逻辑分类

| 类型 | 说明 | 示例 |
|------|------|------|
| **主键索引** | PRIMARY KEY，唯一且非空 | `PRIMARY KEY (id)` |
| **唯一索引** | UNIQUE，值唯一 | `UNIQUE KEY uk_email (email)` |
| **普通索引** | INDEX，允许重复 | `KEY idx_name (name)` |
| **联合索引** | 多列组合 | `KEY idx_name_age (name, age)` |
| **前缀索引** | 字符串前缀 | `KEY idx_name (name(10))` |

---

## 4. 索引的使用

### 🔨 创建索引

#### 4.1 建表时创建

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    age INT COMMENT '年龄',
    status TINYINT DEFAULT 1 COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- 唯一索引
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    
    -- 普通索引
    KEY idx_age (age),
    KEY idx_status (status),
    
    -- 联合索引
    KEY idx_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

#### 4.2 单独创建索引

```sql
-- 创建普通索引
CREATE INDEX idx_username ON users(username);

-- 创建唯一索引
CREATE UNIQUE INDEX uk_email ON users(email);

-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 创建前缀索引
CREATE INDEX idx_name_prefix ON users(name(10));
```

#### 4.3 删除索引

```sql
-- 删除索引
DROP INDEX idx_username ON users;

-- 或者
ALTER TABLE users DROP INDEX idx_username;
```

---

### 🔍 查看索引

```sql
-- 查看表的所有索引
SHOW INDEX FROM users;

-- 或者
SHOW KEYS FROM users;

-- 查看索引使用情况
EXPLAIN SELECT * FROM users WHERE username = 'zhangsan';
```

**输出示例：**

```
+-------+------------+--------------+------+---------------+--------------+---------+-------+------+-------+
| table | type       | key          | rows | Extra         | ...          |         |       |      |       |
+-------+------------+--------------+------+---------------+--------------+---------+-------+------+-------+
| users | ref        | uk_username  |    1 | Using where   | ...          |         |       |      |       |
+-------+------------+--------------+------+---------------+--------------+---------+-------+------+-------+
```

**关键字段说明：**
- `type`: 访问类型（const > eq_ref > ref > range > index > ALL）
- `key`: 实际使用的索引
- `rows`: 估计扫描的行数
- `Extra`: 额外信息（Using index = 覆盖索引）

---

### 📈 索引使用原则

#### 应该创建索引的场景

✅ **高频查询字段**
```sql
-- 经常作为 WHERE 条件
SELECT * FROM users WHERE username = 'zhangsan';
CREATE INDEX idx_username ON users(username);

-- 经常作为 JOIN 条件
SELECT * FROM orders o
JOIN users u ON o.user_id = u.id;
CREATE INDEX idx_user_id ON orders(user_id);

-- 经常作为 ORDER BY 字段
SELECT * FROM users ORDER BY created_at DESC;
CREATE INDEX idx_created_at ON users(created_at);

-- 经常作为 GROUP BY 字段
SELECT status, COUNT(*) FROM users GROUP BY status;
CREATE INDEX idx_status ON users(status);
```

✅ **区分度高的字段**
```sql
-- 区分度 = 不同值的数量 / 总记录数
-- 区分度越高，索引效果越好

-- 身份证号：区分度高，适合建索引
CREATE INDEX idx_id_card ON users(id_card);

-- 性别：区分度低（只有男女），不适合建索引
-- CREATE INDEX idx_gender ON users(gender);  -- ❌ 不推荐
```

✅ **联合查询的字段**
```sql
-- 经常一起查询的字段，建立联合索引
SELECT * FROM users WHERE status = 1 AND age > 18;
CREATE INDEX idx_status_age ON users(status, age);
```

#### 不应该创建索引的场景

❌ **区分度低的字段**
```sql
-- 性别只有 2 个值，索引效果差
gender ENUM('male', 'female')

-- 状态只有几个值
status TINYINT COMMENT '0-禁用 1-启用 2-冻结'
```

❌ **频繁更新的字段**
```sql
-- 每次更新都要维护索引，影响性能
UPDATE users SET login_count = login_count + 1 WHERE id = 1;
-- login_count 不适合建索引
```

❌ **小表（< 1000 条）**
```sql
-- 数据量少，全表扫描更快
-- 不需要建索引
```

❌ **TEXT/BLOB 大字段**
```sql
-- 如果必须建索引，使用前缀索引
CREATE INDEX idx_content ON articles(content(100));
```

---

## 5. 联合索引与最左前缀

### 🔗 什么是联合索引？

**联合索引（复合索引）：** 在多个列上建立的索引

```sql
-- 单列索引
CREATE INDEX idx_name ON users(name);
CREATE INDEX idx_age ON users(age);

-- 联合索引（推荐）
CREATE INDEX idx_name_age ON users(name, age);
```

**优势：**
- ✅ 一个索引当多个用（最左前缀）
- ✅ 减少索引数量，节省空间
- ✅ 提高查询性能

---

### 📐 最左前缀原则（重点！）

#### 核心规则

**联合索引 `(a, b, c)` 相当于创建了三个索引：**
1. `(a)`
2. `(a, b)`
3. `(a, b, c)`

**查询时必须从最左边开始匹配，不能跳过。**

#### 实战示例

```sql
-- 创建联合索引
CREATE INDEX idx_name_age_status ON users(name, age, status);
```

**✅ 能使用索引的查询：**

```sql
-- 1. 精确匹配最左列
SELECT * FROM users WHERE name = '张三';
-- 使用索引：idx_name_age_status (name)

-- 2. 精确匹配前两列
SELECT * FROM users WHERE name = '张三' AND age = 25;
-- 使用索引：idx_name_age_status (name, age)

-- 3. 精确匹配所有列
SELECT * FROM users 
WHERE name = '张三' AND age = 25 AND status = 1;
-- 使用索引：idx_name_age_status (name, age, status)

-- 4. 最左列范围查询
SELECT * FROM users WHERE name LIKE '张%';
-- 使用索引：idx_name_age_status (name)

-- 5. 最左列精确，第二列范围
SELECT * FROM users WHERE name = '张三' AND age > 20;
-- 使用索引：idx_name_age_status (name, age)
-- 注意：status 列无法使用索引
```

**❌ 不能使用索引的查询：**

```sql
-- 1. 跳过最左列
SELECT * FROM users WHERE age = 25;
-- ❌ 不使用索引，全表扫描
-- 原因：没有从最左边的 name 开始

-- 2. 跳过中间列
SELECT * FROM users WHERE name = '张三' AND status = 1;
-- ⚠️ 只使用部分索引：idx_name_age_status (name)
-- 原因：跳过了 age，status 无法使用索引

-- 3. 最左列范围查询后，后面的列无法使用
SELECT * FROM users WHERE name > '张三' AND age = 25;
-- ⚠️ 只使用部分索引：idx_name_age_status (name)
-- 原因：name 是范围查询，age 无法使用索引
```

---

### 🎯 最左前缀图解

```
联合索引：(name, age, status)

B+ 树结构：
                    (name)
                   /      \
           (name='李四')  (name='张三')
              /    \           |
      (age=20) (age=30)   (age=25)
         |        |           |
    (status=1) (status=1) (status=1)


查询路径：

✅ WHERE name = '张三'
   路径：根 → '张三' → 返回结果
   使用了索引的：name

✅ WHERE name = '张三' AND age = 25
   路径：根 → '张三' → age=25 → 返回结果
   使用了索引的：name, age

✅ WHERE name = '张三' AND age = 25 AND status = 1
   路径：根 → '张三' → age=25 → status=1 → 返回结果
   使用了索引的：name, age, status

❌ WHERE age = 25
   路径：无法定位，因为没有 name
   结果：全表扫描

⚠️ WHERE name = '张三' AND status = 1
   路径：根 → '张三' → 遍历所有 age → 过滤 status=1
   使用了索引的：name
   未使用索引的：status（因为跳过了 age）
```

---

### 💡 联合索引设计技巧

#### 技巧 1：区分度高的列放前面

```sql
-- ❌ 错误：区分度低的放前面
CREATE INDEX idx_status_name ON users(status, name);
-- status 只有 3 个值，区分度低

-- ✅ 正确：区分度高的放前面
CREATE INDEX idx_name_status ON users(name, status);
-- name 区分度高，能快速定位
```

#### 技巧 2：等值查询的列放前面，范围查询的列放后面

```sql
-- 查询场景
SELECT * FROM users WHERE name = '张三' AND age > 20 AND status = 1;

-- ❌ 错误顺序
CREATE INDEX idx_age_status_name ON users(age, status, name);
-- age 是范围查询，放在前面会导致后面的列无法使用索引

-- ✅ 正确顺序
CREATE INDEX idx_name_status_age ON users(name, status, age);
-- name 和 status 是等值查询，放前面
-- age 是范围查询，放最后
```

**原则：** 等值查询的列优先，范围查询的列靠后

#### 技巧 3：考虑 ORDER BY

```sql
-- 查询场景
SELECT * FROM users 
WHERE name = '张三' 
ORDER BY age DESC;

-- ✅ 索引可以消除排序
CREATE INDEX idx_name_age ON users(name, age);
-- name 用于 WHERE，age 用于 ORDER BY
```

#### 技巧 4：覆盖索引优化

```sql
-- 查询场景
SELECT name, age FROM users WHERE name = '张三';

-- ✅ 覆盖索引（不需要回表）
CREATE INDEX idx_name_age ON users(name, age);
-- 索引中已经包含了查询所需的所有字段
```

---

## 6. 索引失效场景

### ⚠️ 常见索引失效情况

#### 6.1 函数操作导致失效

```sql
-- 创建索引
CREATE INDEX idx_username ON users(username);

-- ❌ 索引失效：对索引列使用函数
SELECT * FROM users WHERE LOWER(username) = 'zhangsan';
SELECT * FROM users WHERE SUBSTRING(username, 1, 3) = 'zha';
SELECT * FROM users WHERE YEAR(created_at) = 2024;

-- ✅ 正确写法
SELECT * FROM users WHERE username = 'zhangsan';
SELECT * FROM users WHERE username LIKE 'zha%';
SELECT * FROM users WHERE created_at >= '2024-01-01' 
                      AND created_at < '2025-01-01';
```

#### 6.2 类型转换导致失效

```sql
-- phone 是 VARCHAR 类型
CREATE INDEX idx_phone ON users(phone);

-- ❌ 索引失效：隐式类型转换
SELECT * FROM users WHERE phone = 13800138000;
-- 数字会自动转换为字符串，导致索引失效

-- ✅ 正确写法
SELECT * FROM users WHERE phone = '13800138000';
```

#### 6.3 模糊查询左模糊导致失效

```sql
CREATE INDEX idx_username ON users(username);

-- ❌ 索引失效：左模糊
SELECT * FROM users WHERE username LIKE '%zhang';
SELECT * FROM users WHERE username LIKE '%zhang%';

-- ✅ 可以使用索引：右模糊
SELECT * FROM users WHERE username LIKE 'zhang%';

-- ✅ 可以使用索引：精确匹配
SELECT * FROM users WHERE username = 'zhangsan';
```

**原因：** B+ 树是按顺序排列的，`'zhang%'` 可以从某个节点开始遍历，但 `'%zhang'` 无法确定起始位置。

#### 6.4 OR 条件导致失效

```sql
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);

-- ❌ 索引可能失效
SELECT * FROM users WHERE username = 'zhangsan' OR email = 'zhang@example.com';

-- ✅ 改写为 UNION
SELECT * FROM users WHERE username = 'zhangsan'
UNION
SELECT * FROM users WHERE email = 'zhang@example.com';
```

**例外：** 如果 OR 两边的列都有索引，MySQL 可能会使用 `index_merge` 优化。

#### 6.5 NOT、!=、<> 导致失效

```sql
CREATE INDEX idx_status ON users(status);

-- ❌ 索引失效
SELECT * FROM users WHERE status != 1;
SELECT * FROM users WHERE status <> 1;
SELECT * FROM users WHERE NOT status = 1;

-- ✅ 改写为 IN
SELECT * FROM users WHERE status IN (0, 2, 3);
```

**原因：** 不等于操作需要扫描大部分数据，优化器可能认为全表扫描更快。

#### 6.6 IS NULL / IS NOT NULL

```sql
-- ⚠️ 视情况而定
SELECT * FROM users WHERE name IS NULL;      -- 可能使用索引
SELECT * FROM users WHERE name IS NOT NULL;  -- 通常不使用索引
```

**建议：** 尽量避免字段为 NULL，设置默认值。

#### 6.7 联合索引违反最左前缀

```sql
CREATE INDEX idx_name_age_status ON users(name, age, status);

-- ❌ 索引失效或部分失效
SELECT * FROM users WHERE age = 25;              -- 完全失效
SELECT * FROM users WHERE age = 25 AND status = 1;  -- 完全失效
SELECT * FROM users WHERE name = '张三' AND status = 1;  -- 部分失效（只用 name）
```

#### 6.8 数据量太少或太多

```sql
-- 数据量太少（< 1000 条）
-- 优化器可能选择全表扫描，因为索引开销更大

-- 数据量太多（> 20% 的记录）
SELECT * FROM users WHERE status = 1;
-- 如果 status=1 的记录超过 20%，优化器可能选择全表扫描
```

---

### 🔍 如何判断索引是否生效？

```sql
-- 使用 EXPLAIN 分析
EXPLAIN SELECT * FROM users WHERE username = 'zhangsan';

-- 关注以下字段：
-- type: const > eq_ref > ref > range > index > ALL
-- key: 实际使用的索引名称
-- rows: 扫描的行数（越少越好）
-- Extra: Using index（覆盖索引）, Using where（需要回表）
```

**type 类型说明（从好到差）：**

| type | 说明 | 示例 |
|------|------|------|
| **const** | 常量查询，最多一行 | `WHERE id = 1` |
| **eq_ref** | 唯一索引扫描 | `JOIN ... ON a.id = b.id` |
| **ref** | 非唯一索引扫描 | `WHERE name = '张三'` |
| **range** | 范围扫描 | `WHERE age > 20` |
| **index** | 全索引扫描 | `SELECT name FROM users` |
| **ALL** | 全表扫描 | `WHERE gender = 'male'` |

---

## 7. 索引优化实战

### 📦 案例 1：慢查询优化

#### 问题场景

```sql
-- 原始查询（慢）
SELECT * FROM orders 
WHERE user_id = 123 
  AND status = 1 
  AND created_at > '2024-01-01'
ORDER BY created_at DESC
LIMIT 10;

-- 执行时间：2.5 秒
-- 扫描行数：50 万行
```

#### 优化步骤

**步骤 1：分析查询**

```sql
EXPLAIN SELECT * FROM orders 
WHERE user_id = 123 
  AND status = 1 
  AND created_at > '2024-01-01'
ORDER BY created_at DESC
LIMIT 10;

-- 结果：
-- type: ALL（全表扫描）
-- key: NULL（没有使用索引）
-- rows: 500000
-- Extra: Using where; Using filesort
```

**步骤 2：创建联合索引**

```sql
-- 分析查询条件：
-- WHERE: user_id（等值）, status（等值）, created_at（范围）
-- ORDER BY: created_at

-- 创建联合索引
CREATE INDEX idx_user_status_created ON orders(user_id, status, created_at);
```

**步骤 3：验证优化效果**

```sql
EXPLAIN SELECT * FROM orders 
WHERE user_id = 123 
  AND status = 1 
  AND created_at > '2024-01-01'
ORDER BY created_at DESC
LIMIT 10;

-- 结果：
-- type: range（范围扫描）
-- key: idx_user_status_created（使用了索引）
-- rows: 100（扫描行数大幅减少）
-- Extra: Using where; Using index（覆盖索引）

-- 执行时间：0.01 秒（提升 250 倍！）
```

---

### 📦 案例 2：分页优化

#### 问题场景

```sql
-- 深分页（慢）
SELECT * FROM articles 
ORDER BY id DESC 
LIMIT 100000, 10;

-- 执行时间：5 秒
-- 原因：需要扫描前 100010 条记录，然后丢弃前 100000 条
```

#### 优化方案

**方案 1：子查询优化**

```sql
-- 先查出 ID，再关联查询
SELECT * FROM articles 
WHERE id <= (
    SELECT id FROM articles 
    ORDER BY id DESC 
    LIMIT 100000, 1
)
ORDER BY id DESC 
LIMIT 10;

-- 执行时间：0.1 秒
```

**方案 2：延迟关联**

```sql
-- 先查 ID，再 JOIN
SELECT a.* 
FROM articles a
INNER JOIN (
    SELECT id FROM articles 
    ORDER BY id DESC 
    LIMIT 100000, 10
) b ON a.id = b.id;

-- 执行时间：0.1 秒
```

**方案 3：游标分页（推荐）**

```sql
-- 记住上一页最后一条记录的 ID
-- 第一页
SELECT * FROM articles ORDER BY id DESC LIMIT 10;
-- 最后一条 ID = 999990

-- 第二页
SELECT * FROM articles 
WHERE id < 999990 
ORDER BY id DESC 
LIMIT 10;

-- 执行时间：0.01 秒
-- 优点：无论多少页，速度都一样
```

---

### 📦 案例 3：COUNT 优化

#### 问题场景

```sql
-- 统计总数（慢）
SELECT COUNT(*) FROM orders WHERE status = 1 AND created_at > '2024-01-01';

-- 执行时间：3 秒
```

#### 优化方案

**方案 1：使用覆盖索引**

```sql
-- 创建联合索引
CREATE INDEX idx_status_created ON orders(status, created_at);

-- COUNT 会使用覆盖索引，不需要回表
SELECT COUNT(*) FROM orders 
WHERE status = 1 AND created_at > '2024-01-01';

-- 执行时间：0.1 秒
```

**方案 2：近似计数**

```sql
-- 如果不需要精确值，可以使用
SELECT TABLE_ROWS 
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_NAME = 'orders';

-- 或者
SHOW TABLE STATUS LIKE 'orders';
```

---

### 📦 案例 4：GROUP BY 优化

#### 问题场景

```sql
-- 分组统计（慢）
SELECT status, COUNT(*), AVG(amount) 
FROM orders 
GROUP BY status;

-- 执行时间：2 秒
-- Extra: Using temporary; Using filesort
```

#### 优化方案

```sql
-- 创建联合索引
CREATE INDEX idx_status_amount ON orders(status, amount);

-- 使用覆盖索引
SELECT status, COUNT(*), AVG(amount) 
FROM orders 
GROUP BY status;

-- 执行时间：0.2 秒
-- Extra: Using index（不需要临时表和文件排序）
```

---

## 8. 常见问题解答

### Q1: 为什么有了索引还是很慢？

**可能原因：**

1. **索引失效** - 检查是否违反了最左前缀、使用了函数等
2. **索引选择不当** - 优化器选择了错误的索引
3. **数据分布不均** - 某些值的数据量太大
4. **索引碎片化** - 需要重建索引
5. **服务器资源不足** - CPU、内存、磁盘 IO 瓶颈

**排查方法：**
```sql
-- 1. 使用 EXPLAIN 分析
EXPLAIN SELECT ...;

-- 2. 检查索引使用情况
SHOW STATUS LIKE 'Handler_read%';

-- 3. 重建索引
ALTER TABLE users ENGINE=InnoDB;
```

---

### Q2: 索引越多越好吗？

**答：** ❌ **不是！**

**索引的代价：**
- 占用存储空间
- 降低写操作性能（INSERT/UPDATE/DELETE 需要维护索引）
- 增加优化器的选择复杂度

**建议：**
- 单表索引数量控制在 5-10 个以内
- 优先使用联合索引，减少索引数量
- 定期审查 unused indexes（未使用的索引）

**查找未使用的索引：**
```sql
-- MySQL 5.7+
SELECT * 
FROM sys.schema_unused_indexes 
WHERE object_schema = 'your_database';
```

---

### Q3: 主键用什么类型好？

**对比：**

| 类型 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **自增 INT/BIGINT** | 有序、插入快、占用小 | 可预测、分布式困难 | 单机应用 |
| **UUID** | 全局唯一、分布式友好 | 无序、占用大、插入慢 | 分布式系统 |
| **雪花算法 ID** | 有序、全局唯一、分布式友好 | 需要实现算法 | **推荐** |

**推荐：雪花算法生成的 BIGINT**

```java
// 示例：雪花算法 ID
// 优点：
// 1. 趋势递增，插入性能好
// 2. 全局唯一，支持分布式
// 3. BIGINT 类型，占用 8 字节
// 4. 可读性好（纯数字）
```

---

### Q4: 什么时候需要重建索引？

**场景：**
1. 大量删除数据后
2. 频繁更新索引列
3. 索引碎片率高

**检查索引碎片：**
```sql
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    ROUND(STAT_VALUE * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE database_name = 'your_database'
  AND stat_name = 'size';
```

**重建索引：**
```sql
-- 方法 1：OPTIMIZE TABLE
OPTIMIZE TABLE users;

-- 方法 2：ALTER TABLE
ALTER TABLE users ENGINE=InnoDB;

-- 方法 3：删除重建
DROP INDEX idx_name ON users;
CREATE INDEX idx_name ON users(name);
```

---

### Q5: 覆盖索引是什么？

**定义：** 查询的列都在索引中，不需要回表查询数据。

```sql
-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- ✅ 覆盖索引（不需要回表）
SELECT name, age FROM users WHERE name = '张三';
-- Extra: Using index

-- ❌ 非覆盖索引（需要回表）
SELECT name, age, email FROM users WHERE name = '张三';
-- Extra: Using where
-- 需要回到聚簇索引中查询 email
```

**优势：** 减少 IO 次数，大幅提升性能。

---

### Q6: 如何选择合适的索引列顺序？

**原则：**

1. **区分度高的列放前面**
   ```sql
   -- name 区分度 > status 区分度
   CREATE INDEX idx_name_status ON users(name, status);
   ```

2. **等值查询的列放前面，范围查询的列放后面**
   ```sql
   -- name（等值）, age（等值）, created_at（范围）
   CREATE INDEX idx_name_age_created ON users(name, age, created_at);
   ```

3. **考虑 ORDER BY 和 GROUP BY**
   ```sql
   -- WHERE name = ? ORDER BY age
   CREATE INDEX idx_name_age ON users(name, age);
   ```

4. **覆盖索引优先**
   ```sql
   -- SELECT name, age WHERE name = ?
   CREATE INDEX idx_name_age ON users(name, age);
   ```

---

## 🎯 核心要点速记

```
索引原理：
- B+ 树结构，树矮胖，减少磁盘 IO
- 叶子节点存数据，形成链表，支持范围查询

索引类型：
- 聚簇索引：主键索引，数据和索引在一起
- 非聚簇索引：二级索引，需要回表
- 联合索引：多列组合，遵循最左前缀

最左前缀原则：
- 联合索引 (a, b, c) 相当于 (a), (a,b), (a,b,c)
- 查询必须从最左边开始，不能跳过
- 等值查询优先，范围查询靠后

索引失效：
- 函数操作、类型转换、左模糊
- OR 条件、NOT/!=、违反最左前缀

优化技巧：
- 使用 EXPLAIN 分析
- 区分度高的列优先
- 覆盖索引避免回表
- 深分页用游标方式

最佳实践：
- 索引不是越多越好（5-10 个）
- 定期清理未使用的索引
- 主键推荐雪花算法 ID
- 监控慢查询日志
```

---

## 📋 索引设计检查清单

开发时问自己这些问题：

- [ ] 这个字段是否高频查询？
- [ ] 字段的区分度是否足够高？
- [ ] 是否可以利用联合索引？
- [ ] 是否符合最左前缀原则？
- [ ] 是否存在索引失效的风险？
- [ ] 是否可以使用覆盖索引？
- [ ] 索引数量是否合理？
- [ ] 是否影响了写操作性能？

---

**记住：好的索引设计 = 理解原理 + 合理使用 + 持续优化！** 🚀
