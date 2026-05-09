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

    /**
     * 批量处理阈值
     */
    private static final int BATCH_COUNT = 100;

    private final QuestionBankService questionBankService;

    /**
     * 缓存的数据列表
     */
    private List<QuestionBank> cachedDataList = new ArrayList<>(BATCH_COUNT);

    public QuestionExcelListener(QuestionBankService questionBankService) {
        this.questionBankService = questionBankService;
    }

    /**
     * 每读取一行数据时触发
     */
    @Override
    public void invoke(QuestionExcelDTO dto, AnalysisContext context) {
        // 数据转换：DTO -> Entity
        QuestionBank question = convertToEntity(dto);
        cachedDataList.add(question);

        // 达到批量阈值时保存
        if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            cachedDataList = new ArrayList<>(BATCH_COUNT);
        }
    }

    /**
     * 所有数据解析完成后触发
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 保存剩余数据
        saveData();
        log.info("Excel 导入完成，总行数：{}" , context.readRowHolder().getRowIndex());
    }

    /**
     * 批量保存数据
     */
    private void saveData() {
        if (cachedDataList.isEmpty()) {
            return;
        }
        questionBankService.saveBatch(cachedDataList);
        log.info("批量保存 {} 条题目", cachedDataList.size());
    }

    /**
     * DTO 转换为 Entity
     */
    private QuestionBank convertToEntity(QuestionExcelDTO dto) {
        QuestionBank question = new QuestionBank();
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
