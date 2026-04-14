package com.example.auth.controller;

import com.example.auth.dto.*;
import com.example.auth.service.UserService;
import com.example.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取用户列表
     */
    @GetMapping
    public ApiResponse<List<UserVO>> list(UserQueryDTO query) {
        return ApiResponse.success(userService.listUsers(query));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ApiResponse<UserVO> create(@Validated @RequestBody UserCreateDTO dto) {
        return ApiResponse.success(userService.createUser(dto));
    }

    /**
     * 更新用户
     */
    @PutMapping
    public ApiResponse<UserVO> update(@Validated @RequestBody UserUpdateDTO dto) {
        return ApiResponse.success(userService.updateUser(dto));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }
}
