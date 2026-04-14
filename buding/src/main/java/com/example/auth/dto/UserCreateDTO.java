package com.example.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户新增 DTO
 */
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String nickname;
    private String email;
}
