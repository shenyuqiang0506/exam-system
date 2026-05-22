package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.ExamPaper;

import java.util.List;
import java.util.Map;

/**
 * 试卷 Service 接口
 */
public interface ExamPaperService extends IService<ExamPaper> {

    /**
     * 手动组卷：保存试卷并关联题目
     * @param examPaper 试卷基本信息
     * @param questionIds 题目ID列表
     */
    void manualCreatePaper(ExamPaper examPaper, List<Long> questionIds);

    /**
     * 归档试卷
     * @param paperId 试卷ID
     */
    void archivePaper(Long paperId);

    /**
     * 获取可考试卷列表 (未归档)
     * @return 试卷列表
     */
    List<ExamPaper> listActivePapers();

    /**
     * 获取学生可考的活跃题目集（进行中）
     * @param studentId 学生ID
     * @return 试卷列表（含状态信息）
     */
    List<Map<String, Object>> getActivePapersForStudent(Long studentId);

    /**
     * 获取学生所有题目集（分页）
     * @param studentId 学生ID
     * @param page 页码
     * @param size 每页大小
     * @return 包含分页信息的结果
     */
    java.util.Map<String, Object> getAllPapersForStudent(Long studentId, Integer page, Integer size);

    /**
     * 获取试卷状态
     * @param paper 试卷
     * @return 状态：未开始、进行中、已结束
     */
    String getPaperStatus(ExamPaper paper);

    /**
     * 删除试卷（同时删除关联的题目）
     * @param paperId 试卷ID
     */
    void deletePaper(Long paperId);

    /**
     * 获取剩余时间（秒）
     * @param paper 试卷
     * @return 剩余秒数
     */
    long getRemainingSeconds(ExamPaper paper);
}
