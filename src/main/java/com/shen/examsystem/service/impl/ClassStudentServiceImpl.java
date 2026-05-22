package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ClassStudent;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.mapper.ClassStudentMapper;
import com.shen.examsystem.mapper.SysUserMapper;
import com.shen.examsystem.service.ClassStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassStudentServiceImpl extends ServiceImpl<ClassStudentMapper, ClassStudent> implements ClassStudentService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void addStudentToClass(Long classId, Long studentId) {
        // 检查是否已存在
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId)
               .eq(ClassStudent::getStudentId, studentId);
        if (this.count(wrapper) > 0) {
            return; // 已存在，不重复添加
        }

        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        cs.setJoinTime(LocalDateTime.now());
        this.save(cs);
    }

    @Override
    public void addStudentsToClass(Long classId, List<Long> studentIds) {
        for (Long studentId : studentIds) {
            addStudentToClass(classId, studentId);
        }
    }

    @Override
    public void removeStudentFromClass(Long classId, Long studentId) {
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId)
               .eq(ClassStudent::getStudentId, studentId);
        this.remove(wrapper);
    }

    @Override
    public List<Long> getStudentIdsByClass(Long classId) {
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassStudent::getClassId, classId);
        return this.list(wrapper).stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Object> getStudentsByClass(Long classId) {
        List<Long> studentIds = getStudentIdsByClass(classId);
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<SysUser> students = sysUserMapper.selectBatchIds(studentIds);
        return new ArrayList<>(students);
    }
}
