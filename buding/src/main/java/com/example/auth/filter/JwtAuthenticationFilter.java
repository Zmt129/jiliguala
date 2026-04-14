package com.example.auth.filter;

import com.example.auth.utils.JwtUtil;
import com.example.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * JWT 认证过滤器
 * 拦截请求，解析 Token，设置用户上下文
 * 注意：不要加 @Component，由 SecurityConfig 通过 addFilterBefore 显式注册
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 复用 ObjectMapper 并注册 JavaTimeModule 以支持 LocalDateTime 序列化
    private static final ObjectMapper objectMapper =  new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(FORMATTER)))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 跳过登录、刷新接口（这些接口不需要认证）
        // 注意：logout 接口需要认证，已从放行列表中移除
        if (path.equals("/api/auth/login") || path.equals("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, "未提供认证Token");
            return;  // 不再继续执行
        }

        String token = header.substring(7);

        try {
            // 3. 校验 Token 类型：业务接口必须使用 Access Token
            if (!jwtUtil.isAccessToken(token)) {
                logger.warn("尝试使用非 Access Token 访问业务接口");
                writeUnauthorizedResponse(response, "请使用有效的 Access Token");
                return;
            }

            // 4. 解析 Token
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            if (userId == null) {
                writeUnauthorizedResponse(response, "Token 中缺少用户ID");
                return;
            }

            // 5. 构建认证对象
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userId,           
                    null,             
                    new ArrayList<>() 
                );
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 6. 设置到 Spring Security 上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("用户认证成功");

        } catch (Exception e) {
            logger.error("JWT Token 解析失败: " + e.getMessage());
            // Token 无效时，清除认证上下文，确保请求不会被错误认证
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, "Token 无效或已过期");
            return;  // 不再继续执行
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 返回 401 未授权响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Void> apiResponse = new ApiResponse<>(401, message, null);

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
