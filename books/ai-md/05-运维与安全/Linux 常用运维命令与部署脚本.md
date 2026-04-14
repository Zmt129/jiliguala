# Linux 常用运维命令与部署脚本完全指南

> 后端开发者的 Linux 生存手册：从日志排查到自动化部署。

---

## 📖 目录

1. [文件与目录操作](#1-文件与目录操作)
2. [系统监控与性能分析](#2-系统监控与性能分析)
3. [网络排查与连接测试](#3-网络排查与连接测试)
4. [进程管理与服务控制](#4-进程管理与服务控制)
5. [日志查看与文本处理](#5-日志查看与文本处理)
6. [Spring Boot 自动化部署脚本](#6-spring-boot-自动化部署脚本)

---

## 1. 文件与目录操作

### 📂 基础命令
| 命令 | 作用 | 示例 |
|------|------|------|
| `ls -l` | 列出详细信息 | `ls -lh` (以人类可读格式显示大小) |
| `cd` | 切换目录 | `cd /var/log` |
| `pwd` | 显示当前路径 | - |
| `mkdir -p` | 递归创建目录 | `mkdir -p /opt/app/logs` |
| `rm -rf` | **强制删除** (慎用！) | `rm -rf temp_dir` |

### 🔍 查找与压缩
*   **查找文件：** `find / -name "application.yml"`
*   **实时查看文件大小：** `du -sh *`
*   **压缩/解压：**
    *   `tar -czvf app.tar.gz folder/` (压缩)
    *   `tar -xzvf app.tar.gz` (解压)

---

## 2. 系统监控与性能分析

### 🚀 核心指标查看
1.  **CPU 与内存概况：** `top` 或 `htop`
    *   按 `P` 键按 CPU 排序，按 `M` 键按内存排序。
2.  **磁盘空间：** `df -h`
    *   如果根分区 `/` 使用率超过 90%，需立即清理日志。
3.  **内存详情：** `free -h`
    *   关注 `available` 列，而不是 `free`。

### 🐢 负载分析
*   **查看系统负载：** `uptime`
    *   `load average: 0.5, 1.2, 0.8` (分别代表 1、5、15 分钟的平均负载)。
*   **查看 IO 等待：** `iostat -x 1`
    *   如果 `%iowait` 很高，说明瓶颈在磁盘读写。

---

## 3. 网络排查与连接测试

### 🌐 端口与连接
1.  **查看端口占用：** `netstat -tunlp | grep 8080`
    *   或者使用更现代的 `ss -tunlp | grep 8080`。
2.  **测试连通性：** `ping baidu.com`
3.  **测试端口通断：** `telnet 192.168.1.100 3306`
    *   如果连不上，检查防火墙或安全组。

### 🔥 防火墙操作 (CentOS 7+)
*   开启端口：`firewall-cmd --zone=public --add-port=8080/tcp --permanent`
*   重载配置：`firewall-cmd --reload`

---

## 4. 进程管理与服务控制

### ⚙️ 进程操作
1.  **查找进程：** `ps -ef | grep java`
2.  **杀死进程：** `kill -9 <PID>`
    *   **建议先尝试：** `kill -15 <PID>` (优雅退出)。

### 🔄 Systemd 服务管理
现代 Linux 发行版推荐使用 `systemctl`：
*   启动：`systemctl start myapp`
*   停止：`systemctl stop myapp`
*   开机自启：`systemctl enable myapp`
*   查看状态：`systemctl status myapp`

---

## 5. 日志查看与文本处理

这是后端开发者**最高频**使用的技能组合。

### 📝 日志查看三剑客
1.  **tail (看最新日志)：**
    *   `tail -f application.log` (实时监控)
    *   `tail -n 100 application.log` (看最后 100 行)
2.  **grep (搜索关键字)：**
    *   `grep "ERROR" application.log`
    *   `grep -C 5 "Exception" application.log` (显示匹配行前后 5 行)
3.  **less (分页查看大文件)：**
    *   `less application.log`
    *   进入后按 `/` 输入关键字搜索，按 `G` 跳到底部。

### 🛠️ 高级组合拳
*   **统计错误出现次数：** `grep "ERROR" app.log | wc -l`
*   **提取特定 IP 的访问记录：** `grep "192.168.1.5" access.log | awk '{print $1}'`

---

## 6. Spring Boot 自动化部署脚本

这是一个标准的 `deploy.sh` 脚本模板，支持备份、重启和日志检查。

```bash
#!/bin/bash

# 配置变量
APP_NAME="buding.jar"
APP_PORT=8080
LOG_FILE="logs/application.log"
BACKUP_DIR="backup"

echo "🚀 开始部署应用: $APP_NAME ..."

# 1. 备份旧版本
if [ -f "$APP_NAME" ]; then
    mkdir -p $BACKUP_DIR
    cp $APP_NAME $BACKUP_DIR/$APP_NAME-$(date +%Y%m%d%H%M%S)
    echo "✅ 旧版本已备份"
fi

# 2. 停止旧进程
PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
if [ -n "$PID" ]; then
    echo "⏹️ 正在停止进程: $PID"
    kill -15 $PID
    # 等待 10 秒，如果还没停则强杀
    sleep 10
    if ps -p $PID > /dev/null; then
        kill -9 $PID
    fi
else
    echo "ℹ️ 未发现运行中的进程"
fi

# 3. 启动新进程
echo "▶️ 正在启动应用..."
nohup java -jar -Xms512m -Xmx512m $APP_NAME > /dev/null 2>&1 &

# 4. 检查启动状态
sleep 5
NEW_PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
if [ -n "$NEW_PID" ]; then
    echo "🎉 应用启动成功! PID: $NEW_PID"
    echo "📄 实时日志预览:"
    tail -n 20 $LOG_FILE
else
    echo "❌ 应用启动失败，请检查日志: $LOG_FILE"
fi
```

### 💡 使用方法
1.  赋予执行权限：`chmod +x deploy.sh`
2.  执行部署：`./deploy.sh`

---

## ⚠️ 避坑指南

1.  **权限问题：** 遇到 `Permission denied` 时，不要盲目加 `sudo`，先检查文件所有者 (`chown`) 和权限 (`chmod`)。
2.  **换行符陷阱：** 在 Windows 下编写的脚本放到 Linux 运行会报错，使用 `dos2unix script.sh` 转换格式。
3.  **环境变量：** 确保 Java 环境变量 (`JAVA_HOME`) 已在 `/etc/profile` 或 `~/.bashrc` 中配置并生效 (`source /etc/profile`)。
4.  **日志轮转：** 生产环境务必配置 `logback-spring.xml` 进行日志切割，否则单个日志文件过大会撑爆磁盘。

---

**记住：熟练掌握 Linux 命令，能让你在排查线上问题时比别人快 10 倍！** 🚀
