package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.PaperQuestion;
import com.shen.examsystem.mapper.PaperQuestionMapper;
import com.shen.examsystem.service.PaperQuestionService;
import org.springframework.stereotype.Service;

/**
 * 试卷-题目关联 Service 实现类
 */
@Service
public class PaperQuestionServiceImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements PaperQuestionService {
}
