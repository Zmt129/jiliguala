package com.example.auth.config;

import com.example.auth.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 注册 JWT 过滤器为 Spring Bean，以便注入 JwtUtil 等依赖
     * 注意：不要加 @Component，避免被注册为全局 Servlet Filter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * 禁止 JWT 过滤器被自动注册为全局 Servlet Filter
     * 确保它只在 Spring Security 过滤链中执行
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);  // 禁止自动注册为全局过滤器
        return registration;
    }

    /**
     * 完全忽略静态资源（不进入 Spring Security 过滤链）
     * 性能最佳，推荐方式
     * 注意：此处的路径会被 WebSecurity 直接忽略，不经过任何 Spring Security Filter
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
            .antMatchers("/**/*.html", "/**/*.css", "/**/*.js",
                        "/**/*.ico", "/**/*.png", "/**/*.jpg", "/**/*.gif", "/**/*.svg",
                        "/css/**", "/js/**", "/images/**", "/fonts/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // 放行登录和刷新接口（不需要认证）
            .antMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
            // 其他所有请求（包括 /api/auth/logout）都需要认证
            .anyRequest().authenticated()
            .and()
            // 在 UsernamePasswordAuthenticationFilter 之前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
                
        return http.build();
    }
}
