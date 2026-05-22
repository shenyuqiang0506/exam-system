package com.shen.examsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应表: sys_user
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 用户ID (雪花算法) */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码 (BCrypt加密) */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 学号 */
    private String studentNo;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 班级 */
    private String className;

    /** 头像URL */
    private String avatar;

    /** 角色: 0-学生, 1-教师, 2-管理员 */
    private Integer role;

    /** 状态: 0-禁用, 1-正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer deleted;
}
