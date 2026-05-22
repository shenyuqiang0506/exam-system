package com.shen.examsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class UserManageController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取学生列表（分页）
     */
    @GetMapping("/students")
    public Result<Page<SysUser>> getStudentList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {

        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, 0); // 只查学生

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                             .or().like(SysUser::getRealName, keyword));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 禁用/启用用户
     */
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.success("操作成功", null);
    }

    /**
     * 重置密码
     */
    @PutMapping("/users/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword("123456"); // 重置为默认密码
        sysUserService.updateById(user);
        return Result.success("密码已重置为 123456", null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        sysUserService.removeById(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取所有学生（用于添加到班级）
     */
    @GetMapping("/students/all")
    public Result<List<SysUser>> getAllStudents() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, 0)
               .eq(SysUser::getStatus, 1);
        List<SysUser> students = sysUserService.list(wrapper);
        return Result.success(students);
    }

    /**
     * 批量导入学生
     */
    @PostMapping("/students/import")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        try {
            String result = sysUserService.importStudents(file);
            return Result.success(result, null);
        } catch (Exception e) {
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 下载导入模板
     */
    @GetMapping("/students/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生导入模板", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                .head(sysUserService.getImportTemplate())
                .sheet("学生列表")
                .doWrite(List.of());
    }
}
