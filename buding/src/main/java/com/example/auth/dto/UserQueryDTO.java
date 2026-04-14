package com.example.auth.dto;

import lombok.Data;

/**
 * 用户查询 DTO
 */
@Data
public class UserQueryDTO {
    private Integer page = 1;
    private Integer size = 10;
    private String username;
    private Integer status;
}
