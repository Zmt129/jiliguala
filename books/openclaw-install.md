# OpenClaw 安装指南

**OpenClaw** - 你的 AI 编程助手，让大模型帮你写代码、调试程序、管理项目。

---

## 目录

- [系统要求](#系统要求)
- [Windows 安装](#windows-安装)
  - [方式一：一键自动安装（推荐）](#方式一一键自动安装推荐)
  - [方式二：手动安装](#方式二手动安装)
  - [方式三：WSL 2 安装（官方推荐）](#方式三wsl-2-安装官方推荐)
- [Linux 安装](#linux-安装)
  - [Ubuntu/Debian](#ubuntudebian)
  - [CentOS/RHEL](#centosrhel)
- [macOS 安装](#macos-安装)
- [配置向导](#配置向导)
- [验证安装](#验证安装)
- [常见问题](#常见问题)

---

## 系统要求

### 硬件要求
- **内存**: ≥ 8GB（推荐 16GB+）
- **磁盘空间**: ≥ 2GB 可用空间
- **网络**: 稳定的互联网连接

### 软件要求
- **Windows**: Windows 10 64位 / Windows 11
- **Linux**: Ubuntu 20.04+ / Debian 10+ / CentOS 7+
- **macOS**: macOS 12.0+
- **Node.js**: 版本 ≥ 22（如未安装，安装脚本会自动处理）
- **Git**: 建议安装（用于版本控制功能）

---

## Windows 安装

### 方式一：一键自动安装（推荐）⭐

适合大多数用户，全自动完成所有依赖安装和配置。

#### 步骤 1：以管理员身份打开 PowerShell

1. 按 `Win + S` 搜索 "PowerShell"
2. 右键点击 "Windows PowerShell"
3. 选择 **"以管理员身份运行"**
4. 弹出提示时点击 **"是"**

#### 步骤 2：执行安装命令

```powershell
iwr-useb https://openclaw.ai/install.ps1 |iex
```

**说明**：
- 该命令会下载并执行官方安装脚本
- 自动检测并安装Node.js（如果未安装）
- 自动安装 OpenClaw CLI 和所有依赖
- 配置国内镜像源（针对中国大陆用户优化）

#### 步骤 3：等待安装完成

安装过程约需 **2-5 分钟**，期间会显示进度信息。

看到以下提示表示安装成功：
```
✅ OpenClaw installation completed successfully!
```

#### 步骤 4：进入配置向导

安装完成后自动启动配置向导，或手动运行：

```powershell
openclaw onboard --install-daemon
```

---

### 方式二：手动安装

如果方式一因网络或其他问题失败，可使用此方法。

#### 步骤 1：安装Node.js

1. 访问 [Node.js 官网](https://nodejs.org/zh-cn/download)
2. 下载 **Windows Installer (.msi)** 版本（确保版本 ≥ 22）
3. 双击安装包，一路点击 **Next** 完成安装
4. 验证安装：
   ```powershell
   node -v
   ```
   应显示版本号（如 v22.x.x）

#### 步骤 2：安装 Git（可选但推荐）

1. 访问 [Git 官网](https://git-scm.com/download/win)
2. 下载 **64-bit Git for Windows Setup**
3. 双击安装，建议勾选 **"Add a Git Bash Profile to Windows Terminal"**
4. 验证安装：
   ```powershell
   git --version
   ```

#### 步骤 3：配置 npm 镜像（中国大陆用户）

```powershell
npm config set registry https://registry.npmmirror.com
```

#### 步骤 4：安装 OpenClaw

```powershell
npm install-g openclaw@latest
```

#### 步骤 5：启动配置向导

```powershell
openclaw onboard --install-daemon
```

---

### 方式三：WSL 2 安装（官方推荐）

适合需要更纯净 Linux 环境的开发者。

#### 步骤 1：安装 WSL 2

以管理员身份打开 PowerShell：

```powershell
wsl --install-d Ubuntu-22.04
wsl --set-default-version 2
```

按提示重启电脑。

#### 步骤 2：进入 WSL 环境

重启后，在 PowerShell 或 CMD 中输入：

```powershell
wsl
```

即可进入 Ubuntu 命令行界面。

#### 步骤 3：在 WSL 中安装 OpenClaw

```bash
curl -fsSL https://openclaw.ai/install.sh |bash
```

#### 步骤4：访问控制台

配置完成后，在 **Windows 的浏览器** 中访问：

```
http://127.0.0.1:18789
```

---

## Linux 安装

### Ubuntu/Debian

#### 步骤 1：更新系统包

```bash
sudo apt update
sudo apt upgrade -y
```

#### 步骤 2：安装Node.js

**方式 A：使用 NodeSource（推荐）**

```bash
# 安装Node.js 22.x
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

**方式 B：使用 apt（版本可能较旧）**

```bash
sudo apt install-y nodejs npm
```

#### 步骤 3：验证 Node.js 版本

```bash
node -v
```

确保版本 ≥ 22。

#### 步骤 4：安装 Git（如未安装）

```bash
sudo apt install -y git
```

#### 步骤 5：安装 OpenClaw

```bash
npm install -g openclaw@latest
```

#### 步骤 6：启动配置向导

```bash
openclaw onboard --install-daemon
```

---

### CentOS/RHEL

#### 步骤 1：安装 EPEL 仓库

```bash
sudo yum install -y epel-release
```

#### 步骤 2：安装Node.js

```bash
# 安装Node.js 22.x
curl -fsSL https://rpm.nodesource.com/setup_22.x | sudo bash -
sudo yum install -y nodejs
```

#### 步骤 3：安装 Git

```bash
sudo yum install -y git
```

#### 步骤 4：安装 OpenClaw

```bash
npm install -g openclaw@latest
```

#### 步骤 5：启动配置向导

```bash
openclaw onboard --install-daemon
```

---

## macOS 安装

#### 步骤 1：检查 Homebrew

```bash
brew --version
```

如果报错，安装 Homebrew：

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

#### 步骤 2：安装Node.js

```bash
brew install node
```

验证版本：

```bash
node -v
```

确保版本 ≥ 22。

#### 步骤 3：安装 OpenClaw

```bash
npm install -g openclaw@latest
```

#### 步骤4：启动配置向导

```bash
openclaw onboard --install-daemon
```

---

## 配置向导

无论使用哪种安装方式，最后都需要完成配置向导。

### 交互式配置流程

运行配置向导后，会依次提示：

#### 1. 确认免责声明

```
Do you agree to the terms of service? (yes/no): yes
```

#### 2. 选择安装模式

```
Select installation mode:
1. Quickstart(快速开始 - 推荐新手)
2. Custom (自定义配置)
Choose [1]: 1
```

#### 3. 选择模型提供商

```
Select AI provider:
1. WellAPI (推荐，支持 500+ 模型)
2. OpenAI
3. Anthropic
4. Skip (稍后配置)
Choose [4]: 1
```

#### 4. 输入API Key

前往 [WellAPI](https://wellapi.ai) 注册账号获取 API Key，然后输入：

```
Enter your API key: sk-xxxxxxxxxxxxx
```

#### 5. 选择启用的模型

```
Enable all models? (yes/no): yes
```

或手动选择需要的模型。

#### 6. 设置默认模型

```
Set default model [claude-sonnet-4-20250514]: 
```

直接回车使用推荐模型，或输入其他模型名称。

### 配置文件位置

配置完成后，会在用户目录下创建配置文件：

- **Windows**: `C:\Users\{用户名}\.openclaw\`
- **Linux/macOS**: `~/.openclaw/`

主要配置文件：
- `.clawdbot/clawdbot.json` - 主配置文件
- `config.yaml` - 高级配置选项

---

## 验证安装

### 检查版本

```bash
openclaw --version
```

或：

```bash
python3 -m openclaw --version
```

### 启动OpenClaw

```bash
openclaw chat
```

或在浏览器访问 Web 控制台：

```
http://127.0.0.1:18789
```

### 测试对话

在聊天界面输入：

```
你好，请做个自我介绍
```

如果能正常回复，说明安装成功！🎉

---

## 常见问题

### ❌ 安装过程中遇到权限错误

**Windows**:
```powershell
# 以管理员身份运行 PowerShell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

**Linux/macOS**:
```bash
# 添加 sudo 权限
sudo npm install -g openclaw@latest
```

---

### ❌ 网络超时或下载失败

**解决方案 1**: 使用国内镜像

```bash
# 配置 npm 镜像
npm config set registry https://registry.npmmirror.com

# 重新安装
npm install-g openclaw@latest
```

**解决方案 2**: 使用 WSL 2（Windows 用户）

参考 [方式三：WSL 2 安装](#方式三 wsl-2-安装官方推荐)

---

### ❌ Node.js 版本过低

**检查版本**:
```bash
node -v
```

**升级 Node.js**:

Windows: 卸载旧版本，从官网下载最新版重新安装

Linux/macOS:
```bash
# 使用 nvm 管理 Node 版本
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh |bash
nvm install 22
nvm use 22
```

---

### ❌ 找不到 openclaw 命令

**解决方案**:

1. 检查 npm 全局路径是否在 PATH 中

```bash
# Linux/macOS
echo $PATH

# Windows
echo %PATH%
```

2. 手动添加路径或使用完整路径启动

```bash
# 找到安装位置
npm root -g

# 使用完整路径
/path/to/openclaw chat
```

---

### ❌ 配置向导无法启动

**手动创建配置文件**:

找到配置目录（通常在 `~/.openclaw/` 或 `C:\Users\{用户名}\.openclaw\`），编辑 `clawdbot.json`：

```json
{
  "api_key": "sk-your-api-key-here",
  "provider": "wellapi",
  "default_model": "claude-sonnet-4-20250514",
  "enabled_models": ["all"]
}
```

然后重新启动：

```bash
openclaw chat
```

---

### ❌ API Key 无效或额度不足

1. 检查 API Key 是否正确复制（包含完整前缀）
2. 登录 WellAPI 后台查看余额和用量
3. 确认选择的模型在 API Key 的可用范围内

---

### ❌ 防火墙阻止连接

**Windows**:
- 允许 Node.js 通过防火墙
- 关闭第三方杀毒软件测试

**Linux**:
```bash
# 开放端口 18789
sudo ufw allow 18789/tcp
```

---

## 卸载指南

### Windows

```powershell
# 使用 npm 卸载
npm uninstall -g openclaw

# 删除配置目录
Remove-Item -Recurse -Force C:\Users\{用户名}\.openclaw
```

### Linux/macOS

```bash
# 使用 npm 卸载
npm uninstall -g openclaw

# 删除配置目录
rm -rf ~/.openclaw
```

---

## 获取帮助

- **官方网站**: https://openclaw.ai
- **文档中心**: https://docs.openclaw.ai
- **GitHub Issues**: https://github.com/openclaw/openclaw/issues
- **社区论坛**: https://community.openclaw.ai

---

## 更新日志

### v1.0.0 (2026-03)
- ✅ 支持 Windows 原生安装
- ✅ 优化中国大陆地区网络访问
- ✅ 新增 WellAPI 集成
- ✅ 改进配置向导用户体验
- ✅ 支持 500+ AI 大模型

---

**最后更新**: 2026 年 3 月 10 日  
**适用版本**: OpenClaw v1.0.0+
