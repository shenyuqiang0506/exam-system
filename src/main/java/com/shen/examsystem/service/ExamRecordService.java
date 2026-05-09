package com.shen.examsystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shen.examsystem.entity.ExamRecord;

import java.util.List;
import java.util.Map;

/**
 * 考试记录 Service 接口
 */
public interface ExamRecordService extends IService<ExamRecord> {

    /**
     * 根据试卷ID查询所有考试记录
     */
    List<ExamRecord> listByPaperId(Long paperId);

    /**
     * 提交试卷
     * @param studentId 学生ID
     * @param paperId 试卷ID
     * @param answers 答案列表 (questionId, answer)
     * @return 记录ID
     */
    Long submitPaper(Long studentId, Long paperId, List<?> answers);

    /**
     * 获取我的考试记录
     */
    List<Map<String, Object>> getMyRecords(Long studentId);

    /**
     * 获取记录详情
     */
    Map<String, Object> getRecordDetail(Long recordId);
}
