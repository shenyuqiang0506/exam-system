package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    /**
     * 修改密码
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 重置密码为默认密码
     * @param userId 用户ID
     */
    void resetPassword(Long userId);

    /**
     * 批量导入学生
     * @param file Excel文件
     * @return 导入结果
     */
    String importStudents(MultipartFile file);

    /**
     * 下载导入模板
     * @return 模板数据
     */
    List<List<String>> getImportTemplate();
}
