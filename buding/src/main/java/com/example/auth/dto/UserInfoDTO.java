package com.example.auth.dto;

import com.example.auth.entity.SysUser;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 用户信息响应 DTO
 */
@Data
public class UserInfoDTO {
    
    /**
     * 用户基本信息
     */
    private SysUser user;

    /**
     * 权限标识列表 (如: system:user:add)
     */
    private List<String> perms;

    /**
     * 菜单树结构
     */
    private List<Map<String, Object>> menus;
}
