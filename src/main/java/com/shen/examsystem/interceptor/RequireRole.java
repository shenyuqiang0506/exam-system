package com.shen.examsystem.interceptor;

import java.lang.annotation.*;

/**
 * 角色权限注解
 * 标注在 Controller 方法上，限制访问角色
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /**
     * 允许的角色值
     * 0-学生, 1-教师, 2-管理员
     */
    int[] value();
}
