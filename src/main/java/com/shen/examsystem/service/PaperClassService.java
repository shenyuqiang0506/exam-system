package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.PaperClass;

import java.util.List;

public interface PaperClassService extends IService<PaperClass> {

    /**
     * 为试卷分配班级
     * @param paperId 试卷ID
     * @param classIds 班级ID列表
     */
    void assignClassesToPaper(Long paperId, List<Long> classIds);

    /**
     * 获取试卷分配的班级ID列表
     * @param paperId 试卷ID
     * @return 班级ID列表
     */
    List<Long> getClassIdsByPaper(Long paperId);

    /**
     * 检查学生是否有权限参加某试卷考试
     * @param paperId 试卷ID
     * @param studentId 学生ID
     * @return true=有权限
     */
    boolean hasAccess(Long paperId, Long studentId);

    /**
     * 获取学生可访问的试卷ID列表
     * @param studentId 学生ID
     * @return 试卷ID列表
     */
    List<Long> getAccessiblePaperIds(Long studentId);
}
