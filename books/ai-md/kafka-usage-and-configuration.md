## 背景 / 目标

你想学会 **Kafka（卡夫卡）怎么用、怎么配**，并且能在自己电脑上跑起来：能发消息（生产者）、能收消息（消费者），遇到常见问题也知道怎么排查。

## 适用范围

- 你是第一次用 Kafka
- 你需要在本地（Windows / macOS / Linux 都行）搭一个可用的 Kafka
- 你希望知道“配置项是什么意思、我应该改哪些”

> 说明：Kafka 从 3.x 开始通常用 **KRaft 模式**（不再依赖 ZooKeeper）。这篇文档默认按 **KRaft** 来讲；如果你遇到公司老项目还在用 ZooKeeper，我也在后面给了“旧模式”提示。

---

## 1. Kafka 是什么（用一句话理解）

Kafka 是一个“**消息中转站**”：  
你把消息交给 Kafka（生产者 Producer），Kafka 把消息按规则存起来；  
另一个程序从 Kafka 取走消息（消费者 Consumer）。

你可以把它想成：

- **主题（Topic）**：消息的“分类/频道”，比如 `order-created`
- **分区（Partition）**：同一个主题下面的“多条队列”，用来提升吞吐量
- **偏移量（Offset）**：消息在某个分区里的编号（从 0 往上）
- **消费者组（Consumer Group）**：一群消费者一起分工读同一个主题

---

## 2. 最常见的 3 个使用场景

- **解耦**：下单系统只负责把“下单成功”消息发出去，库存/物流/短信系统各自订阅处理
- **削峰填谷**：高峰期先把请求变成消息堆在 Kafka，后台慢慢消费
- **日志/埋点/数据管道**：把大量事件统一汇聚，再送往 Flink / Spark / ES / ClickHouse

---

## 3. 最小可跑方案（推荐）：用 Docker 一键启动（KRaft）

如果你电脑能用 Docker，这是最省事、最不容易出错的方式。

### 3.1 新建一个 `docker-compose.yml`

在任意空文件夹创建 `docker-compose.yml`，内容如下（单机单节点）：

```yaml
services:
  kafka:
    image: apache/kafka:3.8.0
    container_name: kafka
    ports:
      - "9092:9092"     # 给你电脑上的程序用
      - "9093:9093"     # 控制器端口（KRaft）
    environment:
      # 让 Kafka 同时当 broker + controller（单节点最简单）
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_LISTENERS: "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093"
      # 告诉客户端“怎么连我”：从你电脑连 -> localhost:9092
      KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://localhost:9092"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      # 单机开发用：把副本因子降到 1，否则会提示副本不足
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
```

### 3.2 启动

在该文件夹里运行：

```bash
docker compose up -d
```

### 3.3 验证是否启动成功

```bash
docker logs -f kafka
```

看到类似 “started (kafka.server.KafkaServer)” 就说明 OK。

---

## 4. 快速上手：创建 Topic、发消息、收消息

下面这些命令都在容器里执行（最简单，不用你装 Java）。

### 4.1 创建一个 Topic

```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --topic demo --partitions 1 --replication-factor 1
```

查看 Topic 列表：

```bash
docker exec -it kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 4.2 生产者：发几条消息

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic demo
```

然后你在终端里输入几行文字（每行一条消息），比如：

- hello
- kafka

按 `Ctrl+C` 退出。

### 4.3 消费者：把消息读出来

```bash
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic demo --from-beginning
```

你会看到刚才输入的 `hello`、`kafka`。

---

## 5. 你最需要懂的配置（会用就够了）

这里按“你实际会改到”的优先级排序。

### 5.1 `bootstrap.servers`（客户端连哪里）

你的程序（生产者/消费者）需要知道 Kafka 地址，例如：

- 本地：`localhost:9092`
- 公司环境：`kafka-1.internal:9092,kafka-2.internal:9092`

### 5.2 `listeners` vs `advertised.listeners`（最容易踩坑）

- **listeners**：Kafka “自己在哪些网卡/端口上监听”
- **advertised.listeners**：Kafka “告诉客户端你应该连哪个地址”

常见坑：

- 你在 Docker 里启动，但 `advertised.listeners` 写了容器名 `kafka:9092`  
  结果你电脑上的程序根本找不到 `kafka` 这个域名，连接失败。

本地开发推荐：

- `advertised.listeners = localhost:9092`

### 5.3 Topic 相关：分区数、副本因子

- **分区数 partitions**：吞吐量更高，但也更复杂；新手从 1 开始
- **副本因子 replication.factor**：生产环境通常 ≥ 3；本地开发用 1

### 5.4 消费者组 `group.id`（消费者怎么“分工”）

同一组（group）内的多个消费者会自动“分摊”分区：

- 1 个分区 + 2 个消费者（同组）=> 只有 1 个能读到，另一个闲着
- 2 个分区 + 2 个消费者（同组）=> 各读一个分区

### 5.5 `auto.offset.reset`（第一次消费从哪里开始）

当一个消费者组第一次来读、没有历史 offset 时：

- `earliest`：从最早的消息开始读（新手更直观）
- `latest`：从最新的消息开始读（更常用于线上）

### 5.6 `enable.auto.commit`（要不要自动提交 offset）

简单理解：

- 提交 offset = “我读到哪了”的进度条

新手建议：

- 先用自动提交（更省事）
- 真做业务（要保证不丢不重）再改手动提交

---

## 6. 可靠性你要懂的 3 件事（避免丢消息/重复）

### 6.1 至少一次（At-least-once）

最常见：**可能重复，但尽量不丢**。  
实现方式通常是：

- 生产者开启可靠投递（acks 等）
- 消费者处理成功后再提交 offset

### 6.2 至多一次（At-most-once）

**可能丢，但不重复**。  
比如先提交 offset 再处理消息，如果处理失败就丢了。

### 6.3 正好一次（Exactly-once）

更复杂，通常需要：

- 幂等生产者（idempotence）
- 事务（transaction）
- 下游也要配合（比如数据库写入要能去重/幂等）

> 你现在先把 Kafka 跑通就够了。等你说“我需要不丢消息”，我再按你的业务流程帮你选方案。

---

## 7. 常见问题排查（99% 用得上）

### 7.1 连接不上 / 超时

检查顺序：

- 你程序里的 `bootstrap.servers` 是不是 `localhost:9092`
- Kafka 容器端口有没有映射：`9092:9092`
- `advertised.listeners` 有没有写错（最常见）

### 7.2 创建 Topic 报 “replication factor larger than available brokers”

你只有 1 个 broker，却设置了副本因子 3。  
本地开发把副本因子设为 1。

### 7.3 消费者读不到消息

先用控制台消费者验证：

- `--from-beginning` 能不能读到
- 你是不是用了同一个 `group.id`，之前 offset 已经读完了  
  解决：换一个新的 `group.id`，或者把 offset 重置（进阶）

---

## 8. （可选）不用 Docker：本地安装（需要 Java）

如果你不想用 Docker：

- 安装 Java（建议 17+）
- 下载 Kafka 官方二进制包（3.x）
- 选择 KRaft 模式初始化存储并启动

这条路在 Windows 上更容易因为环境变量/路径/权限出问题。你告诉我你是否必须“不用 Docker”，我再按你的电脑环境写一份**完全可复制粘贴**的安装步骤。

---

## 9. （旧项目提示）ZooKeeper 模式你可能会遇到

老项目里常见说法：

- “先起 ZooKeeper，再起 Kafka”
- 连接串里会出现 `zookeeper.connect`

如果你给我看一眼你项目里的配置文件（把敏感地址打码就行），我可以帮你判断它到底是 **KRaft 还是 ZooKeeper**，并给你对应的配置修改方案。

