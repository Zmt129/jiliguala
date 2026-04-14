package com.example.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 用户更新 DTO
 */
@Data
public class UserUpdateDTO {
    @NotNull(message = "用户 ID 不能为空")
    private Long id;
    private String nickname;
    private String email;
    private Integer status;
}
