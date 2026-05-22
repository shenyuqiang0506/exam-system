package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.ClassStudent;

import java.util.List;

public interface ClassStudentService extends IService<ClassStudent> {

    /**
     * 添加学生到班级
     */
    void addStudentToClass(Long classId, Long studentId);

    /**
     * 批量添加学生到班级
     */
    void addStudentsToClass(Long classId, List<Long> studentIds);

    /**
     * 从班级移除学生
     */
    void removeStudentFromClass(Long classId, Long studentId);

    /**
     * 获取班级中的学生ID列表
     */
    List<Long> getStudentIdsByClass(Long classId);

    /**
     * 获取班级中的学生列表（含详细信息）
     */
    List<Object> getStudentsByClass(Long classId);
}
