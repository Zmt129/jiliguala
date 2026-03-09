# Git 操作手册

## 目录

1. [Git 基础概念](#git-基础概念)
2. [安装与配置](#安装与配置)
3. [常用命令](#常用命令)
4. [分支管理](#分支管理)
5. [远程仓库操作](#远程仓库操作)
6. [版本回退与恢复](#版本回退与恢复)
7. [最佳实践](#最佳实践)
8. [常见问题解决](#常见问题解决)

---

## Git 基础概念

### 什么是 Git？
Git 是一个分布式版本控制系统，用于跟踪计算机文件的变更，并协调多人之间的工作。

### 核心概念
- **工作区 (Working Directory)**：你正在编辑的文件
- **暂存区 (Staging Area)**：准备提交的文件
- **仓库 (Repository)**：存储项目历史和元数据
- **提交 (Commit)**：保存的快照
- **分支 (Branch)**：独立的开发线

---

## 安装与配置

### 安装 Git
```bash
# Windows
# 下载安装包：https://git-scm.com/download/win

# macOS
brew install git

# Linux
sudo apt-get install git
```

### 初始配置
```bash
# 设置用户信息（全局）
git config --global user.name "你的名字"
git config --global user.email "你的邮箱@example.com"

# 查看配置
git config --list

# 设置默认分支名称
git config --global init.defaultBranch main

# 设置自动换行符
git config --global core.autocrlf true  # Windows
git config --global core.autocrlf input  # macOS/Linux
```

---

## 常用命令

### 初始化仓库
```bash
# 在当前目录初始化新仓库
git init

# 克隆现有仓库
git clone <repository-url>
git clone <repository-url> <directory-name>
```

### 查看状态
```bash
# 查看文件状态
git status

# 查看详细变更
git diff
git diff --staged  # 查看已暂存的变更
```

### 添加文件
```bash
# 添加单个文件
git add filename.txt

# 添加所有文件
git add .
git add -A

# 交互式添加
git add -p
```

### 提交变更
```bash
# 提交暂存区的文件
git commit -m "提交说明"

# 跳过暂存区直接提交
git commit -a -m "提交说明"

# 修改上一次提交
git commit --amend -m "新的提交说明"
```

### 查看历史
```bash
# 查看提交历史
git log
git log --oneline  # 简洁模式
git log --graph  # 图形化显示
git log --author="名字"  # 按作者筛选
git log --since="2 weeks ago"  # 按时间筛选
```

### 撤销操作
```bash
# 取消暂存
git reset HEAD <file>

# 丢弃工作区修改
git checkout -- <file>

# 恢复删除的文件
git checkout <commit-hash> <file>
```

---

## 分支管理

### 创建和切换分支
```bash
# 创建新分支
git branch <branch-name>

# 切换分支
git checkout <branch-name>

# 创建并切换到新分支
git checkout -b <branch-name>

# 查看当前分支
git branch
git branch -v  # 详细信息
```

### 合并分支
```bash
# 切换到目标分支
git checkout main

# 合并特性分支
git merge <feature-branch>

# 使用变基合并
git rebase <branch-name>
```

### 删除分支
```bash
# 删除本地分支
git branch -d <branch-name>
git branch -D <branch-name>  # 强制删除

# 删除远程分支
git push origin --delete <branch-name>
```

### 常用分支策略
```bash
# 功能分支开发流程
git checkout -b feature/new-feature
git add .
git commit -m "feat: 添加新功能"
git checkout main
git merge feature/new-feature
git branch -d feature/new-feature
```

---

## 远程仓库操作

### 远程仓库管理
```bash
# 查看远程仓库
git remote -v

# 添加远程仓库
git remote add origin <repository-url>

# 重命名远程仓库
git remote rename old-name new-name

# 删除远程仓库
git remote remove origin
```

### 推送和拉取
```bash
# 推送到远程仓库
git push origin main
git push -u origin main  # 设置上游分支

# 强制推送（谨慎使用）
git push -f origin main

# 拉取远程变更
git pull origin main
git fetch origin  # 只获取不合并

# 拉取特定分支
git pull origin <branch-name>
```

### 同步远程变更
```bash
# 获取所有远程分支更新
git fetch --all

# 清理不再存在的远程分支
git remote prune origin

# 查看远程分支信息
git branch -r
```

---

## 版本回退与恢复

### 回退提交
```bash
# 软回退（保留更改在暂存区）
git reset --soft HEAD~1

# 混合回退（保留更改在工作区）
git reset HEAD~1

# 硬回退（完全丢弃更改）
git reset --hard HEAD~1
git reset --hard <commit-hash>
```

### 恢复提交
```bash
# 查看 reflog
git reflog

# 恢复到之前的状态
git reset --hard HEAD@{n}
git checkout <commit-hash>
```

### 撤销合并
```bash
# 撤销未推送的合并
git reset --hard HEAD

# 撤销已推送的合并
git revert -m 1 <merge-commit-hash>
```

### Cherry-pick
```bash
# 应用特定提交到当前分支
git cherry-pick <commit-hash>
git cherry-pick <commit-hash-1> <commit-hash-2>
```

---

## 最佳实践

### 提交规范
```bash
# Conventional Commits 格式
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建工具或依赖更新

# 示例
git commit -m "feat: 添加用户登录功能"
git commit -m "fix: 修复支付页面加载错误"
```

### 工作流程
```bash
# 标准开发流程
# 1. 从主分支创建功能分支
git checkout main
git pull origin main
git checkout -b feature/xxx

# 2. 开发和提交
git add .
git commit -m "feat: 实现 xxx 功能"

# 3. 同步主分支变更
git checkout main
git pull origin main
git checkout feature/xxx
git rebase main

# 4. 推送并创建 Pull Request
git push -u origin feature/xxx
```

### .gitignore 配置
```bash
# 常见忽略规则
node_modules/
*.log
.DS_Store
dist/
build/
.env
*.swp
coverage/
```

---

## 常见问题解决

### 冲突解决
```bash
# 1. 发生冲突时，查看冲突文件
git status

# 2. 编辑冲突文件，解决冲突标记
<<<<<<< HEAD
你的代码
其他人的代码
>>>>>>>

# 3. 标记为解决
git add <resolved-file>

# 4. 完成合并
git commit
```

### 忘记添加到 .gitignore
```bash
# 从 Git 追踪中移除已追踪的文件
git rm -r --cached .
git add .
git commit -m "chore: 清理已忽略的文件"
```

### 大文件处理
```bash
# 查看大文件
git rev-list --objects --all | grep -E '^[0-9a-f]{40} \S+$'

# 使用 Git LFS
git lfs install
git lfs track "*.psd"
git lfs track "*.zip"
```

### SSH 密钥配置
```bash
# 生成 SSH 密钥
ssh-keygen -t ed25519 -C "your_email@example.com"

# 查看公钥
cat ~/.ssh/id_ed25519.pub

# 测试连接
ssh -T git@github.com
```

### 加速国内访问
```bash
# 使用镜像加速
git clone https://ghproxy.com/https://github.com/user/repo.git

# 配置代理
git config --global http.proxy http://proxyserver.com:port
git config --global https.proxy https://proxyserver.com:port

# 取消代理
git config --global --unset http.proxy
git config --global --unset https.proxy
```

---

## 附录：快捷别名

```bash
# 配置常用别名
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.last "log -1 HEAD"
git config --global alias.lg "log --oneline --graph --decorate"
git config --global alias.unstage "reset HEAD --"

# 使用示例
git st      # 代替 git status
git co -b   # 代替 git checkout -b
git lg      # 代替 git log --oneline --graph --decorate
```

---

## 参考资源

- [Git 官方文档](https://git-scm.com/doc)
- [Pro Git 书籍](https://git-scm.com/book/zh/v2)
- [GitHub 学习实验室](https://lab.github.com/)
- [Learn Git Branching](https://learngitbranching.js.org/)