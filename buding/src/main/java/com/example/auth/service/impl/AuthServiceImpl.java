package com.example.auth.service.impl;

import com.example.auth.dto.LoginResponse;
import com.example.auth.dto.UserInfoDTO;
import com.example.auth.entity.SysUser;
import com.example.auth.service.AuthService;
import com.example.auth.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

@Service
public class AuthServiceImpl implements AuthService {
    
    @Autowired
    private JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 模拟数据库：预置 admin 账号 (密码: 123456)
    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD_HASH = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi";

    @Override
    public LoginResponse login(String username, String password) {
        if (!TEST_USERNAME.equals(username) || !passwordEncoder.matches(password, TEST_PASSWORD_HASH)) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        Long userId = 1L;
        String accessToken = jwtUtil.generateAccessToken(userId, username);
        String refreshToken = jwtUtil.generateRefreshToken(userId, username);
        
        return new LoginResponse(accessToken, refreshToken, "Bearer", 900L);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // 1. 验证 Refresh Token 是否有效
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new RuntimeException("无效的 Refresh Token");
        }
        
        // 2. 解析 Refresh Token 获取用户信息
        try {
            Claims claims = jwtUtil.parseToken(refreshToken);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            
            // 3. 生成新的 Access Token 和 Refresh Token
            String newAccessToken = jwtUtil.generateAccessToken(userId, username);
            String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);
            
            return new LoginResponse(newAccessToken, newRefreshToken, "Bearer", 900L);
        } catch (Exception e) {
            throw new RuntimeException("Refresh Token 已过期或无效");
        }
    }

    @Override
    public void logout(String token) {
        // 1. 清除 Spring Security 上下文
        SecurityContextHolder.clearContext();
        
        // 2. 在实际项目中，这里应该将 Token 加入 Redis 黑名单
        // 示例：redisTemplate.opsForValue().set("blacklist:" + getJtiFromToken(token), "1", getRemainingTime(token));
        
        // 3. 当前无状态实现：仅清除上下文，客户端需自行删除本地 Token
        System.out.println("用户已退出登录，Token: " + (token != null ? token.substring(0, 20) + "..." : "null"));
    }

    @Override
    public UserInfoDTO getCurrentUserInfo() {
        UserInfoDTO dto = new UserInfoDTO();
        
        // 从 Spring Security 上下文中获取当前用户 ID
        Long userId = getCurrentUserId();
        
        // 1. 组装用户信息
        SysUser user = new SysUser();
        user.setId(userId);
        user.setUsername(TEST_USERNAME);
        user.setStatus(1);
        dto.setUser(user);
        
        // 2. 组装权限列表（实际项目中应从数据库或 Redis 查询）
        dto.setPerms(Arrays.asList("system:user:list", "system:user:add", "system:user:delete"));
        
        // 3. 组装菜单树
        List<Map<String, Object>> menus = new ArrayList<>();
        Map<String, Object> menu = new HashMap<>();
        menu.put("id", 1);
        menu.put("name", "系统管理");
        menu.put("path", "/system");
        menu.put("component", "Layout");
        menus.add(menu);
        dto.setMenus(menus);
        
        return dto;
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        // 如果未登录或 Token 无效，返回 null
        return null;
    }
}
