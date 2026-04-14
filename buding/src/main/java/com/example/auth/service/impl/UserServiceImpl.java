package com.example.auth.service.impl;

import com.example.auth.dto.*;
import com.example.auth.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    // 模拟数据库存储
    private final Map<Long, UserVO> userDb = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);
    private final PasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @PostConstruct
    public void init() {
        UserVO admin = new UserVO();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNickname("超级管理员");
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        userDb.put(1L, admin);

        UserVO test = new UserVO();
        test.setId(2L);
        test.setUsername("test");
        test.setNickname("测试用户");
        test.setStatus(1);
        test.setCreateTime(LocalDateTime.now());
        userDb.put(2L, test);
    }

    @Override
    public List<UserVO> listUsers(UserQueryDTO query) {
        List<UserVO> list = new ArrayList<>(userDb.values());
        
        if (query.getUsername() != null && !query.getUsername().isEmpty()) {
            list = list.stream()
                .filter(u -> u.getUsername().contains(query.getUsername()))
                .collect(Collectors.toList());
        }
        
        if (query.getStatus() != null) {
            list = list.stream()
                .filter(u -> u.getStatus().equals(query.getStatus()))
                .collect(Collectors.toList());
        }
        
        return list;
    }

    @Override
    public UserVO createUser(UserCreateDTO dto) {
        if (userDb.values().stream().anyMatch(u -> u.getUsername().equals(dto.getUsername()))) {
            throw new RuntimeException("用户名已存在");
        }
        
        UserVO user = new UserVO();
        user.setId(idGenerator.incrementAndGet());
        BeanUtils.copyProperties(dto, user);
        //这里并没用使用传入的创建用户状态
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        userDb.put(user.getId(), user);
        
        log.info("创建用户成功: {}", user.getUsername());
        return user;
    }

    @Override
    public UserVO updateUser(UserUpdateDTO dto) {
        if (dto.getId() == 1L) {
            throw new RuntimeException("超级管理员不可修改");
        }
        UserVO user = userDb.get(dto.getId());
        if (user == null) throw new RuntimeException("用户不存在");
        
        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getStatus() != null) user.setStatus(dto.getStatus());
        
        log.info("更新用户成功: {}", user.getUsername());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (id == 1L) throw new RuntimeException("超级管理员不可删除");
        UserVO removed = userDb.remove(id);
        if (removed != null) {
            log.info("删除用户成功: {}", removed.getUsername());
        } else {
            throw new RuntimeException("用户不存在");
        }
    }
}
