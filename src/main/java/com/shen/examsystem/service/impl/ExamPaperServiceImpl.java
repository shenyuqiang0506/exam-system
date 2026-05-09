package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.mapper.ExamPaperMapper;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷 Service 实现类
 */
@Service
public class ExamPaperServiceImpl extends ServiceImpl<ExamPaperMapper, ExamPaper> implements ExamPaperService {

    @Autowired
    private PaperQuestionMapper paperQuestionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualCreatePaper(ExamPaper examPaper, List<Long> questionIds) {
        // 1. 保存试卷基本信息 (雪花算法自动生成ID)
        this.save(examPaper);

        // 2. 批量插入试卷-题目关联
        Long paperId = examPaper.getId();
        List<PaperQuestion> relations = questionIds.stream()
                .map(qId -> {
                    PaperQuestion pq = new PaperQuestion();
                    pq.setPaperId(paperId);
                    pq.setQuestionId(qId);
                    return pq;
                })
                .collect(Collectors.toList());

        // 批量插入
        for (PaperQuestion pq : relations) {
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
    public List<ExamPaper> listActivePapers() {
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getIsArchived, 0);
        return this.list(wrapper);
    }
}
