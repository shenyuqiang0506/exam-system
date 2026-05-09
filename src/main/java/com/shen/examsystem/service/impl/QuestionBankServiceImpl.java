package com.shen.examsystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.mapper.QuestionBankMapper;
import com.shen.examsystem.service.QuestionBankService;
import org.springframework.stereotype.Service;

/**
 * 题库 Service 实现类
 */
@Service
public class QuestionBankServiceImpl extends ServiceImpl<QuestionBankMapper, QuestionBank> implements QuestionBankService {
}
