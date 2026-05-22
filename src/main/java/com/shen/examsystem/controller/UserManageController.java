package com.shen.examsystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.ClassInfo;
import com.shen.examsystem.entity.ClassStudent;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.mapper.ClassInfoMapper;
import com.shen.examsystem.mapper.ClassStudentMapper;
import com.shen.examsystem.mapper.ExamPaperMapper;
import com.shen.examsystem.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.shen.examsystem.interceptor.RequireRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理 Controller（教师和管理员可访问）
 */
@RestController
@RequestMapping("/api/admin")
@RequireRole({1, 2})  // 教师和管理员
public class UserManageController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private ClassInfoMapper classInfoMapper;

    @Autowired
    private ClassStudentMapper classStudentMapper;

    @Autowired
    private com.shen.examsystem.service.ExamRecordService examRecordService;

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
     * 获取教师列表（分页）
     */
    @GetMapping("/teachers")
    public Result<Page<SysUser>> getTeacherList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {

        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, 1); // 只查教师

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                             .or().like(SysUser::getRealName, keyword));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 添加教师
     */
    @PostMapping("/teachers")
    public Result<Void> addTeacher(@RequestBody SysUser teacher) {
        // 检查用户名是否已存在
        if (sysUserService.lambdaQuery().eq(SysUser::getUsername, teacher.getUsername()).count() > 0) {
            return Result.error("用户名已存在");
        }
        teacher.setRole(1); // 教师角色
        teacher.setStatus(1); // 正常状态
        sysUserService.register(teacher);
        return Result.success("添加成功", null);
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
        sysUserService.resetPassword(id);
        return Result.success("密码已重置为 123456", null);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        // 检查是否有考试记录
        LambdaQueryWrapper<com.shen.examsystem.entity.ExamRecord> recordWrapper = new LambdaQueryWrapper<>();
        recordWrapper.eq(com.shen.examsystem.entity.ExamRecord::getStudentId, id);
        if (examRecordService.count(recordWrapper) > 0) {
            return Result.error("该用户有考试记录，无法删除");
        }

        // 检查是否在班级中
        LambdaQueryWrapper<com.shen.examsystem.entity.ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(com.shen.examsystem.entity.ClassStudent::getStudentId, id);
        if (classStudentMapper.selectCount(csWrapper) > 0) {
            // 先移除班级关联
            classStudentMapper.delete(csWrapper);
        }

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

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 学生数量
        LambdaQueryWrapper<SysUser> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(SysUser::getRole, 0);
        long studentCount = sysUserService.count(studentWrapper);
        stats.put("studentCount", studentCount);

        // 教师数量
        LambdaQueryWrapper<SysUser> teacherWrapper = new LambdaQueryWrapper<>();
        teacherWrapper.eq(SysUser::getRole, 1);
        long teacherCount = sysUserService.count(teacherWrapper);
        stats.put("teacherCount", teacherCount);

        // 试卷数量
        long paperCount = examPaperMapper.selectCount(null);
        stats.put("paperCount", paperCount);

        return Result.success(stats);
    }

    /**
     * 获取所有班级（管理员用）
     */
    @GetMapping("/classes")
    public Result<List<Map<String, Object>>> getAllClasses() {
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ClassInfo::getCreateTime);
        List<ClassInfo> classes = classInfoMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ClassInfo cls : classes) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cls.getId());
            map.put("className", cls.getClassName());
            map.put("teacherId", cls.getTeacherId());
            map.put("description", cls.getDescription());
            map.put("createTime", cls.getCreateTime());

            // 查询班主任姓名
            if (cls.getTeacherId() != null && cls.getTeacherId() > 0) {
                SysUser teacher = sysUserService.getById(cls.getTeacherId());
                map.put("teacherName", teacher != null ? (teacher.getRealName() != null ? teacher.getRealName() : teacher.getUsername()) : "未指定");
            } else {
                map.put("teacherName", "未指定");
            }

            // 查询学生数量
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, cls.getId());
            long studentCount = classStudentMapper.selectCount(csWrapper);
            map.put("studentCount", studentCount);

            result.add(map);
        }
        return Result.success(result);
    }

    /**
     * 获取教师的班级学生列表
     */
    @GetMapping("/teacher/students")
    public Result<Page<SysUser>> getTeacherStudents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {

        Long teacherId = (Long) request.getAttribute("userId");
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }

        // 获取教师的班级
        LambdaQueryWrapper<ClassInfo> classWrapper = new LambdaQueryWrapper<>();
        classWrapper.eq(ClassInfo::getTeacherId, teacherId);
        List<ClassInfo> classes = classInfoMapper.selectList(classWrapper);

        if (classes.isEmpty()) {
            return Result.success(new Page<>(page, size));
        }

        // 获取班级关联的学生ID
        List<Long> classIds = classes.stream().map(ClassInfo::getId).collect(Collectors.toList());
        LambdaQueryWrapper<com.shen.examsystem.entity.ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.in(com.shen.examsystem.entity.ClassStudent::getClassId, classIds);
        List<Long> studentIds = classStudentMapper.selectList(csWrapper).stream()
                .map(com.shen.examsystem.entity.ClassStudent::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            return Result.success(new Page<>(page, size));
        }

        // 查询学生信息
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getId, studentIds);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                             .or().like(SysUser::getRealName, keyword));
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserService.page(pageParam, wrapper);
        // 清除密码字段
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }
}
