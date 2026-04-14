package com.example.auth.service;

import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.UserInfoDTO;

public interface AuthService {
    
    /**
     * 用户登录，返回双 Token
     */
    LoginResponse login(String username, String password);

    /**
     * 刷新 Access Token
     */
    LoginResponse refreshToken(String refreshToken);

    /**
     * 用户退出登录
     */
    void logout(String token);

    /**
     * 获取当前用户完整信息（含权限和菜单）
     */
    UserInfoDTO getCurrentUserInfo();
}
