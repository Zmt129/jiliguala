(function() {
    'use strict';

    // 获取 Token 工具函数
    function getToken() {
        return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
    }

    // 检查登录状态
    function checkAuth() {
        if (!getToken()) {
            window.location.href = '/login.html';
            return false;
        }
        return true;
    }

    // 页面加载完成后执行
    document.addEventListener('DOMContentLoaded', function() {
        if (!checkAuth()) return;

        // 绑定事件
        document.getElementById('userForm').addEventListener('submit', handleSubmit);
        document.getElementById('searchUsername').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') fetchUsers();
        });

        // 获取当前登录用户信息并显示用户名
        loadCurrentUser();
        // 初始加载列表
        fetchUsers();
    });

    // 获取当前登录用户信息
    async function loadCurrentUser() {
        try {
            const response = await fetch('/api/auth/userinfo', {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${getToken()}` }
            });

            const result = await response.json();
            if (result.code === 200 && result.data && result.data.user) {
                document.getElementById('username').textContent = result.data.user.username;
            } else {
                document.getElementById('username').textContent = '未知用户';
            }
        } catch (error) {
            console.error('获取用户信息失败:', error);
            document.getElementById('username').textContent = '获取失败';
        }
    }

    // 缓存用户数据
    let usersData = [];

    // 获取用户列表
    window.fetchUsers = async function() {
        if (!checkAuth()) return;
        
        const keyword = document.getElementById('searchUsername').value.trim();
        let url = '/api/users';
        if (keyword) url += `?username=${encodeURIComponent(keyword)}`;

        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: { 'Authorization': `Bearer ${getToken()}` }
            });

            const result = await response.json();
            if (result.code === 200) {
                usersData = result.data;
                renderTable(usersData);
            } else {
                handleTokenError();
            }
        } catch (error) {
            console.error('获取用户列表失败:', error);
            alert('网络请求失败，请检查后端服务是否启动');
        }
    };

    // 渲染表格
    function renderTable(users) {
        const tbody = document.querySelector('#userTable tbody');
        if (!users || users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:#999; padding:30px;">暂无数据</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(u => `
            <tr>
                <td>${u.id}</td>
                <td>${u.username}</td>
                <td>${u.nickname || '-'}</td>
                <td><span class="status-badge ${u.status === 1 ? 'status-1' : 'status-0'}">${u.status === 1 ? '正常' : '禁用'}</span></td>
                <td>${u.createTime ? new Date(u.createTime).toLocaleString() : '-'}</td>
                <td>
                    ${u.id !== 1 ? `<button class="btn-sm btn-edit" onclick="editUser(${u.id})">编辑</button>` : '<span style="color:#999; font-size:12px;">管理员</span>'}
                    ${u.id !== 1 ? `<button class="btn-sm btn-delete" onclick="deleteUser(${u.id})">删除</button>` : ''}                
                </td>
            </tr>
        `).join('');
    }

    // 打开模态框
    window.openModal = function(isEdit = false) {
        document.getElementById('userModal').style.display = 'block';
        document.getElementById('modalTitle').innerText = isEdit ? '编辑用户' : '新增用户';
        document.getElementById('pwdGroup').style.display = isEdit ? 'none' : 'block';
        
        const usernameInput = document.getElementById('f_username');
        usernameInput.readOnly = isEdit; // 编辑时禁止修改用户名
        usernameInput.style.backgroundColor = isEdit ? '#f8f9fa' : 'white';

        if (!isEdit) {
            document.getElementById('userForm').reset();
            document.getElementById('userId').value = '';
        }
    };

    // 关闭模态框
    window.closeModal = function() {
        document.getElementById('userModal').style.display = 'none';
    };

    // 编辑用户
    window.editUser = function(id) {
        if (id === 1) {
            alert('系统限制：不允许编辑超级管理员账户');
            return;
        }

        const user = usersData.find(u => u.id === id);
        if (!user) return;

        document.getElementById('userId').value = user.id;
        document.getElementById('f_username').value = user.username;
        document.getElementById('f_nickname').value = user.nickname || '';
        document.getElementById('f_status').value = user.status;
        
        openModal(true);
    };

    // 提交表单
    async function handleSubmit(e) {
        e.preventDefault();
        if (!checkAuth()) return;

        const id = document.getElementById('userId').value;

        // 防止通过修改表单隐藏域 ID 绕过前端限制
        if (id && parseInt(id) === 1) {
            alert('系统限制：不允许修改超级管理员信息');
            return;
        }

        const payload = {
            username: document.getElementById('f_username').value.trim(),
            nickname: document.getElementById('f_nickname').value.trim(),
            status: parseInt(document.getElementById('f_status').value)
        };

        if (!id) {
            payload.password = document.getElementById('f_password').value;
        } else {
            payload.id = parseInt(id);
        }

        try {
            const response = await fetch('/api/users', {
                method: id ? 'PUT' : 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getToken()}` 
                },
                body: JSON.stringify(payload)
            });

            const result = await response.json();
            if (result.code === 200) {
                closeModal();
                fetchUsers();
                alert(id ? '更新成功' : '创建成功');
            } else {
                alert(result.message || '操作失败');
            }
        } catch (error) {
            alert('提交失败，请检查网络');
        }
    }

    // 删除用户
    window.deleteUser = async function(id) {
        if (id === 1) {
            alert('系统限制：不允许删除超级管理员账户');
            return;
        }
        if (!confirm('确定要删除该用户吗？此操作不可恢复。')) return;
        if (!checkAuth()) return;

        try {
            const response = await fetch(`/api/users/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${getToken()}` }
            });

            const result = await response.json();
            if (result.code === 200) {
                fetchUsers();
                alert('删除成功');
            } else {
                alert(result.message || '删除失败');
            }
        } catch (error) {
            alert('删除失败，请检查网络');
        }
    };

    // Token 失效处理
    async function handleTokenError() {
        const refreshToken = localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
        if (!refreshToken) return redirectToLogin();

        try {
            const res = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ refreshToken })
            });
            const json = await res.json();
            
            if (json.code === 200) {
                const storage = localStorage.getItem('accessToken') ? localStorage : sessionStorage;
                storage.setItem('accessToken', json.data.accessToken);
                storage.setItem('refreshToken', json.data.refreshToken);
                fetchUsers(); // 刷新后重试
            } else {
                redirectToLogin();
            }
        } catch (e) {
            redirectToLogin();
        }
    }

    function redirectToLogin() {
        localStorage.clear();
        sessionStorage.clear();
        alert('登录已过期，请重新登录');
        window.location.href = '/login.html';
    }

})();
