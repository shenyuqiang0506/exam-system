package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.entity.ExamRecord;
import com.shen.examsystem.entity.AnswerDetail;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.entity.PaperClass;
import com.shen.examsystem.mapper.AnswerDetailMapper;
import com.shen.examsystem.mapper.ExamPaperMapper;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.service.ExamPaperService;
import com.shen.examsystem.service.ExamRecordService;
import com.shen.examsystem.service.PaperClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 试卷 Service 实现类
 */
@Service
public class ExamPaperServiceImpl extends ServiceImpl<ExamPaperMapper, ExamPaper> implements ExamPaperService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Autowired
    private ExamRecordService examRecordService;

    @Autowired
    private PaperClassService paperClassService;

    @Autowired
    private AnswerDetailMapper answerDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCreatePaper(ExamPaper examPaper, List<Long> questionIds) {
        // 1. 保存试卷基本信息
        this.save(examPaper);

        // 2. 批量插入试卷-题目关联
        Long paperId = examPaper.getId();
        for (Long qId : questionIds) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(qId);
            paperQuestionMapper.insert(pq);
        }
    }

    @Override
    public void archivePaper(Long paperId) {
        ExamPaper paper = this.getById(paperId);
        if (paper != null) {
            paper.setIsArchived(1);
            this.updateById(paper);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaper(Long paperId) {
        // 1. 删除试卷题目关联
        LambdaQueryWrapper<PaperQuestion> pqWrapper = new LambdaQueryWrapper<>();
        pqWrapper.eq(PaperQuestion::getPaperId, paperId);
        paperQuestionMapper.delete(pqWrapper);

        // 2. 删除试卷班级关联
        LambdaQueryWrapper<com.shen.examsystem.entity.PaperClass> pcWrapper = new LambdaQueryWrapper<>();
        pcWrapper.eq(com.shen.examsystem.entity.PaperClass::getPaperId, paperId);
        paperClassService.remove(pcWrapper);

        // 3. 删除考试记录关联的答题明细
        LambdaQueryWrapper<ExamRecord> erWrapper = new LambdaQueryWrapper<>();
        erWrapper.eq(ExamRecord::getPaperId, paperId);
        List<ExamRecord> records = examRecordService.list(erWrapper);
        for (ExamRecord record : records) {
            LambdaQueryWrapper<AnswerDetail> adWrapper = new LambdaQueryWrapper<>();
            adWrapper.eq(AnswerDetail::getRecordId, record.getId());
            answerDetailMapper.delete(adWrapper);
        }

        // 4. 删除考试记录
        examRecordService.remove(erWrapper);

        // 5. 删除试卷
        this.removeById(paperId);
    }

    @Override
    public List<ExamPaper> listActivePapers() {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getIsArchived, 0);
        return this.list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getActivePapersForStudent(Long studentId) {
        LocalDateTime now = LocalDateTime.now();

        // 获取学生可访问的试卷ID列表
        List<Long> accessiblePaperIds = paperClassService.getAccessiblePaperIds(studentId);

        // 查询活跃的试卷：没有时间限制 OR 当前时间在开始和结束时间之间
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getIsArchived, 0)
               .and(w -> w
                   .isNull(ExamPaper::getStartTime)  // 没有时间限制
                   .or()
                   .le(ExamPaper::getStartTime, now)  // 已开始
                   .ge(ExamPaper::getEndTime, now)    // 未结束
               );

        // 如果学生在某些班级中，只显示分配给这些班级的试卷
        if (!accessiblePaperIds.isEmpty()) {
            wrapper.in(ExamPaper::getId, accessiblePaperIds);
        }

        wrapper.orderByDesc(ExamPaper::getCreateTime);
        List<ExamPaper> papers = this.list(wrapper);
        return buildPaperListWithStatus(papers, studentId);
    }

    @Override
    public java.util.Map<String, Object> getAllPapersForStudent(Long studentId, Integer page, Integer size) {
        // 获取学生可访问的试卷ID列表
        List<Long> accessiblePaperIds = paperClassService.getAccessiblePaperIds(studentId);

        // 使用 MyBatis-Plus 分页
        Page<ExamPaper> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();

        // 如果学生在某些班级中，只显示分配给这些班级的试卷
        if (!accessiblePaperIds.isEmpty()) {
            wrapper.in(ExamPaper::getId, accessiblePaperIds);
        }

        wrapper.orderByDesc(ExamPaper::getCreateTime);
        Page<ExamPaper> pageResult = this.page(pageParam, wrapper);

        // 构建带状态信息的试卷列表
        List<Map<String, Object>> records = buildPaperListWithStatus(pageResult.getRecords(), studentId);

        // 返回分页结果
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", records);
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return result;
    }

    @Override
    public String getPaperStatus(ExamPaper paper) {
        // 没有时间限制的试卷，始终为"进行中"
        if (paper.getStartTime() == null || paper.getEndTime() == null) {
            return "进行中";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(paper.getStartTime())) {
            return "未开始";
        }
        if (now.isAfter(paper.getEndTime())) {
            return "已结束";
        }
        return "进行中";
    }

    @Override
    public long getRemainingSeconds(ExamPaper paper) {
        // 没有时间限制，返回 -1 表示无限制
        if (paper.getEndTime() == null) {
            return -1;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(paper.getEndTime())) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(now, paper.getEndTime());
    }

    /**
     * 构建带状态信息的试卷列表
     */
    private List<Map<String, Object>> buildPaperListWithStatus(List<ExamPaper> papers, Long studentId) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (ExamPaper paper : papers) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", paper.getId());
            map.put("title", paper.getTitle());
            map.put("subjectName", paper.getSubjectName());
            map.put("totalScore", paper.getTotalScore());
            map.put("startTime", paper.getStartTime() != null ? paper.getStartTime().toString() : null);
            map.put("endTime", paper.getEndTime() != null ? paper.getEndTime().toString() : null);
            map.put("isArchived", paper.getIsArchived());  // 归档状态

            // 状态：如果已归档，显示"已归档"，否则根据时间判断
            String status;
            if (paper.getIsArchived() == 1) {
                status = "已归档";
            } else {
                status = getPaperStatus(paper);
            }
            map.put("status", status);

            // 剩余时间
            long remainingSeconds = getRemainingSeconds(paper);
            map.put("remainingSeconds", Integer.valueOf((int) remainingSeconds));

            // 题目数量
            int questionCount = getQuestionCount(paper.getId());
            map.put("questionCount", questionCount);

            // 查询学生答题情况
            ExamRecord record = examRecordService.getStudentRecord(studentId, paper.getId());
            if (record != null) {
                map.put("answeredCount", getAnsweredCount(record.getId()));
                map.put("myScore", record.getTotalScore());
                map.put("recordId", record.getId());
                map.put("recordStatus", record.getStatus());
            } else {
                map.put("answeredCount", 0);
                map.put("myScore", null);
                map.put("recordId", null);
                map.put("recordStatus", null);
            }

            result.add(map);
        }

        return result;
    }

    /**
     * 获取试卷题目数量
     */
    private int getQuestionCount(Long paperId) {
        LambdaQueryWrapper<PaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperQuestion::getPaperId, paperId);
        Long count = paperQuestionMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取已答题数量
     */
    private int getAnsweredCount(Long recordId) {
        if (recordId == null) {
            return 0;
        }
        LambdaQueryWrapper<AnswerDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnswerDetail::getRecordId, recordId)
               .isNotNull(AnswerDetail::getStudentAnswer)
               .ne(AnswerDetail::getStudentAnswer, "");
        Long count = answerDetailMapper.selectCount(wrapper);
        return count != null ? count.intValue() : 0;
    }
}
