# Windows 下 Node.js 版本管理完全指南

> 不同项目需要不同版本的 Node.js？别再手动卸载安装了！用版本管理工具一键切换。

---

## 📖 目录

1. [为什么需要版本管理？](#1-为什么需要版本管理)
2. [主流工具对比](#2-主流工具对比)
3. [方案一：nvm-windows（推荐）](#3-方案一nvm-windows推荐)
4. [方案二：fnm（快速替代）](#4-方案二fnm快速替代)
5. [方案三：Volta（现代化选择）](#5-方案三volta现代化选择)
6. [常见问题解答](#6-常见问题解答)
7. [最佳实践](#7-最佳实践)

---

## 1. 为什么需要版本管理？

### 🤔 实际场景

```
项目 A（老项目）：需要 Node.js 14.x
项目 B（新项目）：需要 Node.js 18.x LTS
项目 C（实验项目）：需要 Node.js 20.x 最新特性
```

**没有版本管理的痛苦：**
- ❌ 每次切换项目都要卸载重装
- ❌ npm 全局包需要重新安装
- ❌ 环境变量要手动修改
- ❌ 容易出错，浪费时间

**有了版本管理的好处：**
- ✅ 一行命令切换版本
- ✅ 多个版本共存
- ✅ 自动识别项目所需版本
- ✅ 节省磁盘空间（共享全局包）

---

## 2. 主流工具对比

| 特性 | nvm-windows | fnm | Volta |
|------|-------------|-----|-------|
| **速度** | 中等 | ⚡ 非常快 | 快 |
| **易用性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **跨平台** | ❌ 仅 Windows | ✅ 跨平台 | ✅ 跨平台 |
| **自动切换** | ❌ 需手动 | ✅ 支持 | ✅ 智能支持 |
| **社区活跃度** | 高 | 高 | 中 |
| **学习成本** | 低 | 低 | 低 |
| **推荐指数** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

**推荐选择：**
- 🥇 **新手/大多数用户**：nvm-windows（稳定、成熟、文档多）
- 🥈 **追求速度**：fnm（Rust 编写，极速）
- 🥉 **团队项目**：Volta（自动识别 package.json）

---

## 3. 方案一：nvm-windows（推荐）

### 📦 安装步骤

#### 步骤 1：卸载已有的 Node.js

```powershell
# 如果已经安装了 Node.js，先卸载
# 控制面板 → 程序和功能 → 找到 Node.js → 卸载

# 检查是否还有残留
where node
where npm

# 如果有输出，手动删除这些文件
```

**重要：** 必须卸载原有 Node.js，否则会有冲突！

#### 步骤 2：下载 nvm-windows

访问 GitHub  releases 页面：
```
https://github.com/coreybutler/nvm-windows/releases
```

下载最新版本：`nvm-setup.exe`

#### 步骤 3：安装

1. 双击运行 `nvm-setup.exe`
2. 选择安装路径（建议默认：`C:\Users\你的用户名\AppData\Roaming\nvm`）
3. 选择 Node.js  symlink 路径（建议默认：`C:\Program Files\nodejs`）
4. 点击 Install

#### 步骤 4：验证安装

```powershell
# 打开新的 PowerShell 窗口
nvm version

# 应该输出版本号，如：1.1.12
```

### 🎯 基本使用

#### 查看可用版本

```powershell
# 查看所有可安装的版本
nvm list available

# 输出示例：
#   CURRENT    LTS      OLD STABLE   OLD UNSTABLE
#   20.11.0    18.19.0  0.12.18      0.11.16
#   20.10.0    18.19.1  0.12.17      0.11.15
#   ...
```

#### 安装指定版本

```powershell
# 安装最新的 LTS 版本
nvm install lts

# 安装指定版本
nvm install 18.19.0

# 安装最新版本
nvm install latest

# 安装特定大版本（会自动安装该系列的最新版）
nvm install 18
```

#### 切换版本

```powershell
# 列出已安装的版本
nvm list

# 输出示例：
#     20.11.0
#   * 18.19.0 (Currently using 64-bit executable)
#     16.20.2

# 切换到指定版本
nvm use 18.19.0

# 切换到 LTS 版本
nvm use lts

# 切换到最新版本
nvm use latest
```

#### 卸载版本

```powershell
# 卸载指定版本
nvm uninstall 16.20.2
```

#### 设置默认版本

```powershell
# 设置默认使用的版本
nvm alias default 18.19.0
```

### 🔧 高级用法

#### 为不同项目设置不同版本

**方法 1：手动切换**

```powershell
# 进入项目 A 目录
cd D:\projects\project-a
nvm use 14.21.3

# 进入项目 B 目录
cd D:\projects\project-b
nvm use 18.19.0
```

**方法 2：使用 .nvmrc 文件（需要额外配置）**

在项目根目录创建 `.nvmrc` 文件：

```
# .nvmrc
18.19.0
```

然后使用这个脚本自动切换（保存为 `switch-node.ps1`）：

```powershell
# switch-node.ps1
if (Test-Path .nvmrc) {
    $version = Get-Content .nvmrc
    Write-Host "Switching to Node.js $version..."
    nvm use $version
} else {
    Write-Host "No .nvmrc file found"
}
```

每次进入项目目录后运行：
```powershell
.\switch-node.ps1
```

#### 配置镜像加速（国内必备）

编辑 nvm 安装目录下的 `settings.txt`：

```txt
root: C:\Users\你的用户名\AppData\Roaming\nvm
path: C:\Program Files\nodejs
arch: 64
proxy: none
node_mirror: https://npmmirror.com/mirrors/node/
npm_mirror: https://npmmirror.com/mirrors/npm/
```

**或者使用命令行：**

```powershell
nvm node_mirror https://npmmirror.com/mirrors/node/
nvm npm_mirror https://npmmirror.com/mirrors/npm/
```

### ⚠️ 常见问题

#### 问题 1：nvm use 后提示权限不足

```powershell
# 以管理员身份运行 PowerShell
# 右键 PowerShell → 以管理员身份运行

# 或者修改 nodejs 目录权限
icacls "C:\Program Files\nodejs" /grant Everyone:F
```

#### 问题 2：切换版本后 npm 不可用

```powershell
# 重新安装 npm
nvm use 18.19.0
npm install -g npm@latest
```

#### 问题 3：全局包丢失

```powershell
# nvm 切换版本后，全局包是独立的
# 需要在每个版本中重新安装常用的全局包

# 例如：
nvm use 18.19.0
npm install -g yarn pnpm nodemon

nvm use 20.11.0
npm install -g yarn pnpm nodemon
```

---

## 4. 方案二：fnm（快速替代）

### 🚀 特点

- ⚡ 基于 Rust，速度极快
- ✅ 跨平台支持
- 🔄 自动切换版本
- 📦 兼容 nvm 命令

### 📦 安装步骤

#### 方法 1：使用 Scoop（推荐）

```powershell
# 安装 Scoop（如果还没有）
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# 安装 fnm
scoop install fnm
```

#### 方法 2：使用 Chocolatey

```powershell
choco install fnm
```

#### 方法 3：手动安装

1. 从 GitHub 下载：https://github.com/Schniz/fnm/releases
2. 解压到任意目录
3. 添加到环境变量 PATH

### 🎯 基本使用

```powershell
# 查看版本
fnm --version

# 安装 Node.js
fnm install 18
fnm install 20

# 列出已安装版本
fnm ls

# 切换版本
fnm use 18

# 设置默认版本
fnm default 18

# 卸载版本
fnm uninstall 16

# 自动切换（根据 .node-version 或 .nvmrc 文件）
fnm use
```

### 🔧 配置自动切换

在 PowerShell 配置文件（`$PROFILE`）中添加：

```powershell
# 打开配置文件
notepad $PROFILE

# 添加以下内容
Invoke-Expression (&fnm env --use-on-cd | Out-String)
```

这样每次 `cd` 进入有 `.node-version` 文件的目录时，会自动切换 Node.js 版本。

---

## 5. 方案三：Volta（现代化选择）

### ✨ 特点

- 🎯 零配置，自动识别项目需求
- 🔒 确保团队使用相同版本
- ⚡ 快速且可靠
- 📦 同时管理 Node.js 和 npm/yarn/pnpm

### 📦 安装步骤

#### 方法 1：使用安装包

1. 访问：https://volta.sh/
2. 下载 Windows 安装程序
3. 运行安装

#### 方法 2：使用 PowerShell

```powershell
# 一行命令安装
iwr https://get.volta.sh/install.ps1 -useb | iex
```

### 🎯 基本使用

#### 安装 Node.js

```powershell
# 安装最新 LTS 版本
volta install node

# 安装指定版本
volta install node@18
volta install node@18.19.0

# 安装最新版本
volta install node@latest
```

#### 切换版本

```powershell
# 查看当前版本
node --version

# 为当前项目固定版本
volta pin node@18.19.0

# 这会修改 package.json，添加：
# {
#   "volta": {
#     "node": "18.19.0"
#   }
# }
```

#### 管理包管理器

```powershell
# 安装 Yarn
volta install yarn

# 安装 pnpm
volta install pnpm

# 指定版本
volta install yarn@1.22.19
volta install pnpm@8.15.0

# 切换包管理器
volta pin yarn
volta pin pnpm
```

### 🔧 团队协作

**优势：Volta 的配置会提交到 Git**

```json
// package.json
{
  "name": "my-project",
  "version": "1.0.0",
  "volta": {
    "node": "18.19.0",
    "yarn": "1.22.19"
  }
}
```

团队成员克隆项目后：
```powershell
git clone <repo>
cd my-project

# Volta 自动使用正确的版本
node --version  # 18.19.0
yarn --version  # 1.22.19
```

---

## 6. 常见问题解答

### Q1: 我应该选择哪个工具？

**答：**

```
如果你是：
- 新手 → 选 nvm-windows（文档多，社区活跃）
- 追求速度 → 选 fnm（Rust 编写，超快）
- 团队协作 → 选 Volta（自动同步版本）
- 已有 nvm 习惯 → 继续用 nvm-windows
```

### Q2: 多个工具可以共存吗？

**答：** ❌ **不建议！**

- 会导致环境变量混乱
- 可能产生冲突
- 选择一个就够了

**如果已经安装了多个：**
```powershell
# 卸载其他工具，只保留一个
# 清理环境变量中的相关路径
# 重启电脑
```

### Q3: 如何备份已安装的全局包？

**答：**

```powershell
# 导出全局包列表
npm list -g --depth=0 > global-packages.txt

# 查看内容
cat global-packages.txt

# 在新版本中恢复
npm install -g $(cat global-packages.txt | tail -n +2 | awk '{print $2}')
```

### Q4: 如何加快下载速度？

**答：** 配置国内镜像

**nvm-windows：**
```txt
# settings.txt
node_mirror: https://npmmirror.com/mirrors/node/
npm_mirror: https://npmmirror.com/mirrors/npm/
```

**fnm：**
```powershell
$env:FNM_NODE_DIST_MIRROR = "https://npmmirror.com/mirrors/node"
```

**Volta：**
```powershell
$env:VOLTA_INSTALL_ROOT = "https://npmmirror.com/mirrors/volta"
```

### Q5: 如何在 CI/CD 中使用？

**答：**

**GitHub Actions：**
```yaml
# .github/workflows/build.yml
name: Build
on: [push]

jobs:
  build:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      
      # 使用 nvm
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      # 或使用 Volta
      - name: Setup Volta
        uses: volta-cli/action@v4
      
      - run: npm ci
      - run: npm test
```

### Q6: 版本切换后环境变量没生效？

**答：**

```powershell
# 方法 1：重启终端
# 关闭所有 PowerShell/CMD 窗口，重新打开

# 方法 2：刷新环境变量
refreshenv  # 需要安装 chocolatey

# 方法 3：手动刷新
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

### Q7: 如何彻底卸载某个工具？

**答：**

**卸载 nvm-windows：**
```powershell
# 1. 运行卸载程序
# 控制面板 → 程序和功能 → nvm-windows → 卸载

# 2. 删除残留目录
Remove-Item -Recurse -Force "$env:APPDATA\nvm"
Remove-Item -Recurse -Force "C:\Program Files\nodejs"

# 3. 清理环境变量
# 系统属性 → 高级 → 环境变量
# 删除 NVM_HOME 和 NVM_SYMLINK
```

---

## 7. 最佳实践

### ✅ DO（应该做的）

#### 1. 使用 LTS 版本

```powershell
# 生产环境使用 LTS（长期支持版）
nvm install lts
nvm use lts

# 查看 LTS 版本
curl https://nodejs.org/dist/index.json | jq '.[] | select(.lts != false) | .version'
```

**LTS 版本生命周期：**
- Active LTS：积极维护，接收新功能
- Maintenance LTS：仅接收安全更新
- End of Life：停止支持

#### 2. 项目根目录添加版本文件

```
# .nvmrc（nvm-windows 和 fnm 支持）
18.19.0

# .node-version（fnm 和 Volta 支持）
18.19.0
```

#### 3. 定期更新 Node.js

```powershell
# 每季度检查一次新版本
nvm list available

# 测试新版本兼容性
nvm install 20
nvm use 20
npm test

# 确认无误后切换
nvm alias default 20
```

#### 4. 锁定项目版本

```json
// package.json
{
  "engines": {
    "node": ">=18.0.0 <19.0.0",
    "npm": ">=9.0.0"
  }
}
```

#### 5. 记录常用全局包

```powershell
# 创建安装脚本 install-global.ps1
$packages = @(
    "yarn",
    "pnpm",
    "nodemon",
    "typescript",
    "eslint",
    "prettier"
)

foreach ($pkg in $packages) {
    npm install -g $pkg
}
```

### ❌ DON'T（不应该做的）

#### 1. 不要在生产环境频繁切换版本

```
开发环境：可以随意切换测试
生产环境：固定一个稳定的 LTS 版本
```

#### 2. 不要混用多个版本管理工具

```
❌ 错误：同时安装 nvm、fnm、Volta
✅ 正确：只选择一个
```

#### 3. 不要忘记更新 npm

```powershell
# Node.js 自带的 npm 可能不是最新版
npm install -g npm@latest
```

#### 4. 不要忽略安全更新

```powershell
# 定期检查安全漏洞
npm audit

# 更新到有安全修复的版本
nvm install 18.19.1  # 修复了某些安全问题
```

### 📋 日常工作流

```powershell
# 早上开始工作
cd D:\projects\my-project

# 如果使用 Volta（自动切换）
node --version  # 自动使用项目指定的版本

# 如果使用 nvm/fnm
nvm use  # 或 fnm use

# 安装依赖
npm install

# 开发...

# 下班前
git add .
git commit -m "feat: xxx"
git push
```

### 🔐 安全建议

1. **定期更新 Node.js** - 修复安全漏洞
2. **使用官方源或可信镜像** - 避免恶意篡改
3. **检查 package.json 的 engines 字段** - 确保版本兼容
4. **生产环境锁定具体版本** - 不要用 `latest`

---

## 🎯 快速参考卡片

### nvm-windows 速查

```powershell
# 安装
nvm install 18.19.0

# 切换
nvm use 18.19.0

# 列表
nvm list

# 卸载
nvm uninstall 16.20.2

# 默认
nvm alias default 18.19.0
```

### fnm 速查

```powershell
# 安装
fnm install 18

# 切换
fnm use 18

# 列表
fnm ls

# 默认
fnm default 18
```

### Volta 速查

```powershell
# 安装
volta install node@18

# 固定项目版本
volta pin node@18.19.0

# 安装 yarn
volta install yarn

# 查看
volta list
```

---

## 📚 参考资料

- [nvm-windows GitHub](https://github.com/coreybutler/nvm-windows)
- [fnm GitHub](https://github.com/Schniz/fnm)
- [Volta 官网](https://volta.sh/)
- [Node.js 官网](https://nodejs.org/)
- [Node.js 发布计划](https://github.com/nodejs/release#release-schedule)

---

**记住：选择合适的工具，养成良好的版本管理习惯，能让你的开发效率提升一倍！** 🚀
