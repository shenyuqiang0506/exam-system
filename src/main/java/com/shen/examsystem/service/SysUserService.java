package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.SysUser;

/**
 * 用户 Service 接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    SysUser login(String username, String password);

    /**
     * 用户注册
     * @param user 用户信息
     */
    void register(SysUser user);
}
