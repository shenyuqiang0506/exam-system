package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ClassStudent;
import com.shen.examsystem.entity.PaperClass;
import com.shen.examsystem.mapper.ClassStudentMapper;
import com.shen.examsystem.mapper.PaperClassMapper;
import com.shen.examsystem.service.PaperClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaperClassServiceImpl extends ServiceImpl<PaperClassMapper, PaperClass> implements PaperClassService {

    @Autowired
    private ClassStudentMapper classStudentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignClassesToPaper(Long paperId, List<Long> classIds) {
        // 先删除原有分配
        LambdaQueryWrapper<PaperClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperClass::getPaperId, paperId);
        this.remove(wrapper);

        // 重新分配
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                PaperClass pc = new PaperClass();
                pc.setPaperId(paperId);
                pc.setClassId(classId);
                this.save(pc);
            }
        }
    }

    @Override
    public List<Long> getClassIdsByPaper(Long paperId) {
        LambdaQueryWrapper<PaperClass> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperClass::getPaperId, paperId);
        return this.list(wrapper).stream()
                .map(PaperClass::getClassId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasAccess(Long paperId, Long studentId) {
        // 获取试卷分配的班级
        List<Long> classIds = getClassIdsByPaper(paperId);
        if (classIds.isEmpty()) {
            return true; // 未分配班级的试卷，所有学生都可访问
        }

        // 检查学生是否在这些班级中
        LambdaQueryWrapper<ClassStudent> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ClassStudent::getClassId, classIds)
               .eq(ClassStudent::getStudentId, studentId);
        return classStudentMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Long> getAccessiblePaperIds(Long studentId) {
        // 获取学生所在的班级ID
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        List<Long> studentClassIds = classStudentMapper.selectList(csWrapper).stream()
                .map(ClassStudent::getClassId)
                .collect(Collectors.toList());

        if (studentClassIds.isEmpty()) {
            return List.of(); // 学生不在任何班级
        }

        // 获取这些班级关联的试卷ID
        LambdaQueryWrapper<PaperClass> pcWrapper = new LambdaQueryWrapper<>();
        pcWrapper.in(PaperClass::getClassId, studentClassIds);
        return this.list(pcWrapper).stream()
                .map(PaperClass::getPaperId)
                .distinct()
                .collect(Collectors.toList());
    }
}
