package com.shen.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shen.examsystem.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 根据用户名查询用户（包括已删除的记录）
     * 绕过MyBatis-Plus的逻辑删除过滤
     */
    @Select("SELECT id, username, password, real_name, student_no, phone, email, class_name, avatar, role, status, create_time, update_time, deleted FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsernameIncludeDeleted(@Param("username") String username);
    
    /**
     * 恢复已删除的用户记录
     * 绕过MyBatis-Plus的逻辑删除过滤
     */
    @Update("UPDATE sys_user SET password=#{password}, real_name=#{realName}, phone=#{phone}, email=#{email}, class_name=#{className}, status=#{status}, deleted=0, update_time=NOW() WHERE id=#{id}")
    int restoreDeletedUser(@Param("id") Long id, @Param("password") String password, @Param("realName") String realName, @Param("phone") String phone, @Param("email") String email, @Param("className") String className, @Param("status") Integer status);
}
