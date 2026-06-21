package com.shen.examsystem.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.shen.examsystem.dto.QuestionExcelDTO;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 题目 Excel 导入监听器
 * 逐行读取并批量入库
 */
@Slf4j
public class QuestionExcelListener extends AnalysisEventListener<QuestionExcelDTO> {

    private static final int BATCH_COUNT = 100;

    private final QuestionBankService questionBankService;
    private final Long teacherId;

    private List<QuestionBank> cachedDataList = new ArrayList<>(BATCH_COUNT);

    public QuestionExcelListener(QuestionBankService questionBankService, Long teacherId) {
        this.questionBankService = questionBankService;
        this.teacherId = teacherId;
    }

    @Override
    public void invoke(QuestionExcelDTO dto, AnalysisContext context) {
        QuestionBank question = convertToEntity(dto);
        cachedDataList.add(question);

        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList = new ArrayList<>(BATCH_COUNT);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("Excel 导入完成，总行数：{}", context.readRowHolder().getRowIndex());
    }

    private void saveData() {
        if (cachedDataList.isEmpty()) {
            return;
        }
        questionBankService.saveBatch(cachedDataList);
        log.info("批量保存 {} 条题目", cachedDataList.size());
    }

    private QuestionBank convertToEntity(QuestionExcelDTO dto) {
        QuestionBank question = new QuestionBank();
        question.setTeacherId(teacherId);
        question.setSubjectName(dto.getSubjectName());
        question.setType(dto.getType());
        question.setContent(dto.getContent());
        question.setStandardAnswer(dto.getStandardAnswer());
        question.setPointsKeyword(dto.getPointsKeyword());
        question.setScore(dto.getScore());
        question.setDifficulty(dto.getDifficulty());
        return question;
    }
}
