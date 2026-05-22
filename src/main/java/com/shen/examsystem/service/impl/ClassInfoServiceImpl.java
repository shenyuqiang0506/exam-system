package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ClassInfo;
import com.shen.examsystem.entity.ClassStudent;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.mapper.ClassInfoMapper;
import com.shen.examsystem.mapper.ClassStudentMapper;
import com.shen.examsystem.mapper.SysUserMapper;
import com.shen.examsystem.service.ClassInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassInfoServiceImpl extends ServiceImpl<ClassInfoMapper, ClassInfo> implements ClassInfoService {

    @Autowired
    private ClassStudentMapper classStudentMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<ClassInfo> getClassesByTeacher(Long teacherId) {
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassInfo::getTeacherId, teacherId)
               .orderByDesc(ClassInfo::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<ClassInfo> getClassesByStudent(Long studentId) {
        // 查询学生所在的班级ID
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getStudentId, studentId);
        List<ClassStudent> relations = classStudentMapper.selectList(wrapper);

        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> classIds = relations.stream()
                .map(ClassStudent::getClassId)
                .collect(Collectors.toList());

        return this.listByIds(classIds);
    }

    @Override
    public void createClass(ClassInfo classInfo) {
        this.save(classInfo);
    }

    @Override
    public void updateClass(ClassInfo classInfo) {
        this.updateById(classInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClass(Long classId) {
        // 删除班级学生关联
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId);
        classStudentMapper.delete(wrapper);

        // 删除班级
        this.removeById(classId);
    }

    @Override
    public Map<String, Object> getClassDetail(Long classId) {
        ClassInfo classInfo = this.getById(classId);
        if (classInfo == null) {
            throw new RuntimeException("班级不存在");
        }

        // 获取班级学生列表
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId);
        List<ClassStudent> relations = classStudentMapper.selectList(wrapper);

        List<Long> studentIds = relations.stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toList());

        List<SysUser> students = new ArrayList<>();
        if (!studentIds.isEmpty()) {
            students = sysUserMapper.selectBatchIds(studentIds);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("classInfo", classInfo);
        result.put("students", students);
        result.put("studentCount", students.size());
        return result;
    }
}
