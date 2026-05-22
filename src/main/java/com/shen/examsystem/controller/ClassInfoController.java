package com.shen.examsystem.controller;

import com.shen.examsystem.common.Result;
import com.shen.examsystem.entity.ClassInfo;
import com.shen.examsystem.service.ClassInfoService;
import com.shen.examsystem.service.ClassStudentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/class")
public class ClassInfoController {

    @Autowired
    private ClassInfoService classInfoService;

    @Autowired
    private ClassStudentService classStudentService;

    /**
     * 获取教师的班级列表
     */
    @GetMapping("/list")
    public Result<List<ClassInfo>> getClassList(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        List<ClassInfo> classes = classInfoService.getClassesByTeacher(teacherId);
        return Result.success(classes);
    }

    /**
     * 获取学生加入的班级列表
     */
    @GetMapping("/student/list")
    public Result<List<ClassInfo>> getStudentClassList(HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "未登录");
        }
        List<ClassInfo> classes = classInfoService.getClassesByStudent(studentId);
        return Result.success(classes);
    }

    /**
     * 创建班级
     */
    @PostMapping("/create")
    public Result<Void> createClass(@RequestBody ClassInfo classInfo, HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (teacherId == null) {
            return Result.error(401, "未登录");
        }
        classInfo.setTeacherId(teacherId);
        classInfoService.createClass(classInfo);
        return Result.success("创建成功", null);
    }

    /**
     * 更新班级
     */
    @PutMapping("/{id}")
    public Result<Void> updateClass(@PathVariable Long id, @RequestBody ClassInfo classInfo) {
        classInfo.setId(id);
        classInfoService.updateClass(classInfo);
        return Result.success("更新成功", null);
    }

    /**
     * 删除班级
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        classInfoService.deleteClass(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取班级详情（含学生列表）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getClassDetail(@PathVariable Long id) {
        Map<String, Object> detail = classInfoService.getClassDetail(id);
        return Result.success(detail);
    }

    /**
     * 添加学生到班级
     */
    @PostMapping("/{classId}/students")
    public Result<Void> addStudents(@PathVariable Long classId, @RequestBody List<Long> studentIds) {
        classStudentService.addStudentsToClass(classId, studentIds);
        return Result.success("添加成功", null);
    }

    /**
     * 从班级移除学生
     */
    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        classStudentService.removeStudentFromClass(classId, studentId);
        return Result.success("移除成功", null);
    }

    /**
     * 获取班级学生ID列表
     */
    @GetMapping("/{classId}/student-ids")
    public Result<List<Long>> getStudentIds(@PathVariable Long classId) {
        List<Long> studentIds = classStudentService.getStudentIdsByClass(classId);
        return Result.success(studentIds);
    }
}
