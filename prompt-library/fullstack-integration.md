# 全栈开发综合提示词

## 项目初始化

### Spring Boot + Vue3 前后端分离项目
```
请帮我搭建一个完整的前后端分离项目：

【后端技术栈】
- Spring Boot 3.x
- MyBatis-Plus
- MySQL 8.0+
- Redis
- RabbitMQ/Kafka
- Spring Security + JWT
- Lombok
- Maven
- Swagger/OpenAPI

【前端技术栈】
- Vue 3.x + TypeScript
- Vite
- Pinia
- Vue Router 4
- Axios
- Element Plus
- Sass
- ESLint + Prettier

【项目要求】
1. 后端提供完整的 RESTful API
2. 前端实现响应式布局
3. 统一的错误处理机制
4. 完善的日志记录
5. 接口文档自动生成
6. 支持跨域配置
7. 数据库迁移脚本（Flyway/Liquibase）
8. Docker 部署配置

【目录结构】
```
project/
├── backend/                 # 后端项目
│   ├── src/main/java/
│   │   └── com/company/project/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── mapper/
│   │       ├── entity/
│   │       ├── dto/
│   │       ├── vo/
│   │       ├── config/
│   │       └── common/
│   ├── src/main/resources/
│   │   ├── mapper/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   └── pom.xml
│
├── frontend/                # 前端项目
│   ├── src/
│   │   ├── api/
│   │   ├── assets/
│   │   ├── components/
│   │   ├── composables/
│   │   ├── layouts/
│   │   ├── router/
│   │   ├── stores/
│   │   ├── styles/
│   │   ├── types/
│   │   ├── utils/
│   │   ├── views/
│   │   └── App.vue
│   ├── public/
│   ├── package.json
│   └── vite.config.ts
│
└── docker/
    ├── docker-compose.yml
    └── Dockerfile
```

请创建：[具体模块]
```

## DevOps 与部署

### CI/CD 流水线配置
```
请设计 CI/CD 流水线：

【GitLab CI 示例】
```yaml
stages:
  - build
  - test
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2"

cache:
  paths:
    - .m2/repository/

build_backend:
  stage: build
  image: maven:3.8-openjdk-17
  script:
    - cd backend
    - mvn clean package -DskipTests
  artifacts:
   paths:
      - backend/target/*.jar

test_backend:
  stage: test
  image: maven:3.8-openjdk-17
  script:
    - cd backend
    - mvn test
  coverage: '/Total.*?([0-9]{1,3})%/'
  artifacts:
   reports:
      junit: backend/target/surefire-reports/TEST-*.xml

build_frontend:
  stage: build
  image: node:18
  script:
    - cd frontend
    - npm install
    - npm run build
  artifacts:
   paths:
      - frontend/dist/

deploy_dev:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl apply -f k8s/dev/
  environment:
   name: development
  only:
    - develop

deploy_prod:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl apply -f k8s/prod/
  environment:
   name: production
  only:
    - main
  when: manual
```

【Jenkins Pipeline 示例】
```groovy
pipeline {
    agent any
    
   parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: '分支名')
       choice(name: 'ENV', choices: ['dev', 'prod'], description: '部署环境')
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: "${params.BRANCH}", 
                    url: 'git@gitlab.com:company/project.git'
            }
        }
        
        stage('Build Backend') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Test Backend') {
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
            }
            post {
                always {
                    junit 'backend/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    if (params.ENV == 'dev') {
                        sh 'kubectl apply -f k8s/dev/'
                    } else if (params.ENV == 'prod') {
                        sh 'kubectl apply -f k8s/prod/'
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '部署成功！'
        }
        failure {
            echo '部署失败！'
        }
    }
}
```

请配置：[具体 CI/CD 需求]
```

### Docker 容器化部署
```
请编写 Docker 配置文件：

【Dockerfile - 后端】
```dockerfile
FROM openjdk:17-slim

LABEL maintainer="your@email.com"

WORKDIR /app

COPY target/*.jar app.jar

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1g -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

【Dockerfile - 前端】
```dockerfile
FROM nginx:alpine

COPY dist/ /usr/share/nginx/html/

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

【Nginx 配置】
```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
       root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

【Docker Compose】
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
   container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: project_db
      MYSQL_USER: app_user
      MYSQL_PASSWORD: app_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - app_network

  redis:
    image: redis:alpine
   container_name: redis
    ports:
      - "6379:6379"
    networks:
      - app_network

  rabbitmq:
    image: rabbitmq:management
   container_name: rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # Management UI
    networks:
      - app_network

  backend:
    build:
     context: ./backend
      dockerfile: Dockerfile
   container_name: backend
    depends_on:
      - mysql
      - redis
      - rabbitmq
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_HOST: mysql
      REDIS_HOST: redis
      RABBITMQ_HOST: rabbitmq
    networks:
      - app_network

  frontend:
    build:
     context: ./frontend
      dockerfile: Dockerfile
   container_name: frontend
    depends_on:
      - backend
    ports:
      - "80:80"
    networks:
      - app_network

volumes:
  mysql_data:

networks:
  app_network:
    driver: bridge
```

请配置：[具体部署需求]
```

## 常见问题解决

### 性能问题排查
```
系统出现性能问题，请协助排查：

【排查步骤】
1. 应用层检查
   - JVM 内存使用率（jstat/jmap）
   - GC 频率和时长
   - 线程状态（jstack）
   - CPU 使用率（top/htop）

2. 数据库检查
   - 慢查询日志
   - 执行计划分析
   - 锁等待情况
   - 连接池状态

3. 缓存检查
   - Redis 内存使用
   - 缓存命中率
   - 大 Key 分析
   - 热 Key 分析

4. 消息队列检查
   - 队列堆积情况
   - 消费速率
   - 死信队列

5. 网络检查
   - 带宽使用率
   - 网络连接数
   - DNS 解析时间

【诊断命令】
```bash
# JVM 诊断
jstat -gcutil <pid> 1000
jmap -heap <pid>
jstack <pid> > thread_dump.txt

# 系统监控
top -H -p <pid>
vmstat 1 10
iostat -x 1 10

# 网络监控
netstat -nat | grep ESTABLISHED | wc -l
ss -s

# MySQL 诊断
SHOW PROCESSLIST;
SHOW ENGINE INNODB STATUS;
EXPLAIN SELECT ...
```

【优化方向】
1. 代码层面
   - 减少循环查库
   - 优化算法复杂度
   - 合理使用缓存
   
2. 数据库层面
   - 索引优化
   - SQL 优化
   - 表结构优化
   
3. 架构层面
   - 读写分离
   - 分库分表
   - CDN 加速

请排查：[具体性能问题现象]
```

### 线上故障处理
```
线上出现故障，请协助处理：

【故障分级】
P0：核心功能不可用（立即响应）
P1：重要功能受损（15 分钟内响应）
P2：次要功能问题（1 小时内响应）
P3：轻微问题（当天处理）

【处理流程】
1. 故障发现
   - 监控告警
   - 用户反馈
   - 巡检发现

2. 快速止损
   - 回滚版本
   - 降级服务
   - 切换流量
   - 重启实例

3. 问题定位
   - 查看日志
   - 分析监控
   - 复现问题
   - 根因分析

4. 问题解决
   - 修复 Bug
   - 数据修复
   - 配置调整

5. 复盘总结
   - 故障报告
   - 改进措施
   - 预案完善

【常用命令】
```bash
# 日志查看
tail -f application.log
grep "ERROR" application.log | tail -100
awk '/2024-01-01/,/2024-01-02/' application.log

# 服务管理
systemctl status service
journalctl -u service -f

# 数据库操作
mysqldump-u user -p database table > backup.sql
mysql -u user -p database < fix.sql

# 缓存清理
redis-cli FLUSHDB
redis-cli KEYS "pattern*" | xargs redis-cli DEL
```

请处理：[具体故障现象]
```

## 技术选型建议

### 技术对比分析
```
请对比以下技术方案并给出建议：

【对比维度】
1. 功能特性
2. 性能表现
3. 学习成本
4. 社区生态
5. 文档质量
6. 生产案例
7. 维护活跃度
8. 商业支持

【常见技术对比】
1. ORM 框架
   - MyBatis vs JPA/Hibernate
   
2. 缓存方案
   - Redis vs Memcached vs Caffeine
   
3. 消息队列
   - RabbitMQ vs Kafka vs RocketMQ
   
4. 微服务框架
   - Spring Cloud Netflix vs Spring Cloud Alibaba
   
5. 前端框架
   - React vs Vue vs Angular
   
6. 数据库
   - MySQL vs PostgreSQL
   
7. 搜索引擎
   - Elasticsearch vs Solr

请对比：[具体技术选型]
```
