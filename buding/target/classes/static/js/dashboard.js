document.addEventListener('DOMContentLoaded', function() {
    const logoutBtn = document.getElementById('logoutBtn');
    const usernameEl = document.getElementById('username');
    const userInfoEl = document.getElementById('userInfo');

    // 获取 Token（优先从 localStorage，其次 sessionStorage）
    const token = localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');

    // 未登录则跳转到登录页
    if (!token) {
        window.location.href = '/login.html';
        return;
    }

    // 加载用户信息
    loadUserInfo(token);

    // 退出登录
    logoutBtn.addEventListener('click', async function() {
        if (confirm('确定要退出登录吗？')) {
            try {
                const refreshToken = localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
                
                await fetch('/api/auth/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({
                        refreshToken: refreshToken
                    })
                });
            } catch (error) {
                console.error('退出登录错误:', error);
            } finally {
                // 清除本地存储
                localStorage.removeItem('accessToken');
                localStorage.removeItem('refreshToken');
                sessionStorage.removeItem('accessToken');
                sessionStorage.removeItem('refreshToken');
                
                // 跳转到登录页
                window.location.href = '/login.html';
            }
        }
    });

    async function loadUserInfo(token) {
        try {
            const response = await fetch('/api/auth/userinfo', {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (response.ok) {
                const result = await response.json();
                if (result.code === 200 && result.data) {
                    displayUserInfo(result.data);
                } else {
                    // Token 可能已过期，尝试刷新
                    await refreshAccessToken();
                }
            } else {
                // 可能是 401，尝试刷新 Token
                await refreshAccessToken();
            }
        } catch (error) {
            console.error('获取用户信息错误:', error);
            showError('获取用户信息失败');
        }
    }

    function displayUserInfo(user) {
        // 显示用户名
        usernameEl.textContent = user.username || user.nickname || '用户';

        // 显示详细信息
        userInfoEl.innerHTML = `
            <div class="user-info-item">
                <div class="user-info-label">用户名</div>
                <div class="user-info-value">${user.username || '-'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">昵称</div>
                <div class="user-info-value">${user.nickname || '-'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">邮箱</div>
                <div class="user-info-value">${user.email || '-'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">手机号</div>
                <div class="user-info-value">${user.phone || '-'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">角色</div>
                <div class="user-info-value">${user.roles ? user.roles.join(', ') : '-'}</div>
            </div>
            <div class="user-info-item">
                <div class="user-info-label">状态</div>
                <div class="user-info-value">${user.status === 1 ? '✅ 正常' : '❌ 禁用'}</div>
            </div>
        `;
    }

    async function refreshAccessToken() {
        try {
            const refreshToken = localStorage.getItem('refreshToken') || sessionStorage.getItem('refreshToken');
            
            if (!refreshToken) {
                throw new Error('没有 Refresh Token');
            }

            const response = await fetch('/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    refreshToken: refreshToken
                })
            });

            const result = await response.json();

            if (response.ok && result.code === 200) {
                const { accessToken, refreshToken: newRefreshToken } = result.data;
                
                // 更新 Token
                if (localStorage.getItem('accessToken')) {
                    localStorage.setItem('accessToken', accessToken);
                    localStorage.setItem('refreshToken', newRefreshToken);
                } else {
                    sessionStorage.setItem('accessToken', accessToken);
                    sessionStorage.setItem('refreshToken', newRefreshToken);
                }

                // 重新加载用户信息
                loadUserInfo(accessToken);
            } else {
                // 刷新失败，跳转到登录页
                redirectToLogin();
            }
        } catch (error) {
            console.error('刷新 Token 错误:', error);
            redirectToLogin();
        }
    }

    function redirectToLogin() {
        // 清除所有 Token
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
        
        alert('登录已过期，请重新登录');
        window.location.href = '/login.html';
    }

    function showError(message) {
        userInfoEl.innerHTML = `<div class="error-message" style="color: #c33; text-align: center;">${message}</div>`;
    }
});
