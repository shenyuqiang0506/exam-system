package com.shen.examsystem.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shen.examsystem.entity.ExamPaper;
import com.shen.examsystem.service.ExamPaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷定时任务
 * 自动归档已结束的试卷
 */
@Component
public class PaperScheduler {

    @Autowired
    private ExamPaperService examPaperService;

    /**
     * 每分钟检查一次，自动归档已结束的试卷
     */
    @Scheduled(fixedRate = 60000)
    public void autoArchiveExpiredPapers() {
        LocalDateTime now = LocalDateTime.now();

        // 查询已结束但未归档的试卷
        LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamPaper::getIsArchived, 0)
               .isNotNull(ExamPaper::getEndTime)
               .lt(ExamPaper::getEndTime, now);

        List<ExamPaper> expiredPapers = examPaperService.list(wrapper);

        if (!expiredPapers.isEmpty()) {
            for (ExamPaper paper : expiredPapers) {
                paper.setIsArchived(1);
            }
            examPaperService.updateBatchById(expiredPapers);
            System.out.println("自动归档试卷数量: " + expiredPapers.size());
        }
    }
}
