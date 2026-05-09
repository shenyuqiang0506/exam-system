package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.ExamPaper;

import java.util.List;

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
}
