package com.example.auth.service;

import com.example.auth.dto.*;

import java.util.List;

public interface UserService {
    List<UserVO> listUsers(UserQueryDTO query);
    UserVO createUser(UserCreateDTO dto);
    UserVO updateUser(UserUpdateDTO dto);
    void deleteUser(Long id);
}
