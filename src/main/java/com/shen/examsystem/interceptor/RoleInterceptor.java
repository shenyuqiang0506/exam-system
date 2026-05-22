package com.shen.examsystem.interceptor;

import cn.hutool.json.JSONUtil;
import com.shen.examsystem.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色权限拦截器
 * 检查 @RequireRole 注解，验证用户角色
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    // 角色映射：字符串 -> 数字
    private static final java.util.Map<String, Integer> ROLE_MAP = java.util.Map.of(
            "student", 0,
            "teacher", 1,
            "admin", 2
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 非控制器方法直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 获取方法上的注解
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        // 如果方法上没有注解，检查类上的注解
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        // 没有注解则放行
        if (requireRole == null) {
            return true;
        }

        // 获取用户角色
        String roleStr = (String) request.getAttribute("role");
        if (roleStr == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(Result.error(401, "未登录")));
            return false;
        }

        // 将字符串角色转换为数字
        Integer userRole = ROLE_MAP.getOrDefault(roleStr, -1);
        int[] allowedRoles = requireRole.value();

        // 检查角色是否在允许列表中
        for (int allowedRole : allowedRoles) {
            if (userRole == allowedRole) {
                return true;
            }
        }

        // 角色不允许
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(Result.error(403, "没有权限访问")));
        return false;
    }
}
