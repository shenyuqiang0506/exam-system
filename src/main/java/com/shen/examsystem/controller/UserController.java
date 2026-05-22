package com.shen.examsystem.controller;

import com.shen.examsystem.common.JwtUtils;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户 Controller
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        SysUser user = sysUserService.login(loginDTO.getUsername(), loginDTO.getPassword());

        // 生成 JWT Token
        String roleStr = user.getRole() == 2 ? "admin" : (user.getRole() == 1 ? "teacher" : "student");
        String token = jwtUtils.generateToken(user.getId(), roleStr);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        return Result.success("登录成功", data);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody SysUser user) {
        sysUserService.register(user);
        return Result.success("注册成功", null);
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        sysUserService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public Result<SysUser> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return Result.success(user);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UpdateProfileDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (dto.getRealName() != null) user.setRealName(dto.getRealName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        sysUserService.updateById(user);
        return Result.success("更新成功", null);
    }

    /**
     * 修改密码 DTO
     */
    public static class ChangePasswordDTO {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    /**
     * 登录请求 DTO
     */
    public static class LoginDTO {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 更新个人信息 DTO
     */
    public static class UpdateProfileDTO {
        private String realName;
        private String phone;
        private String email;
        private String avatar;

        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
}
