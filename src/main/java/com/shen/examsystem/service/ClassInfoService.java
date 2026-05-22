package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.ClassInfo;

import java.util.List;
import java.util.Map;

public interface ClassInfoService extends IService<ClassInfo> {

    /**
     * 获取教师的班级列表
     */
    List<ClassInfo> getClassesByTeacher(Long teacherId);

    /**
     * 获取学生加入的班级列表
     */
    List<ClassInfo> getClassesByStudent(Long studentId);

    /**
     * 创建班级
     */
    void createClass(ClassInfo classInfo);

    /**
     * 更新班级
     */
    void updateClass(ClassInfo classInfo);

    /**
     * 删除班级
     */
    void deleteClass(Long classId);

    /**
     * 获取班级详情（含学生列表）
     */
    Map<String, Object> getClassDetail(Long classId);
}
