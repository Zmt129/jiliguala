package com.example.auth.entity;

import lombok.Data;

/**
 * 用户实体类 (简化版，用于内存测试)
 */
@Data
public class SysUser {
    private Long id;
    private String username;
    private String password;
    private Integer status; // 1: 正常, 0: 禁用
}
