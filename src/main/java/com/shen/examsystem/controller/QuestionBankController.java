package com.shen.examsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.dto.QuestionExcelDTO;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.listener.QuestionExcelListener;
import com.shen.examsystem.service.QuestionBankService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 题库管理 Controller
 */
@RestController
@RequestMapping("/api/question")
public class QuestionBankController {

    @Autowired
    private QuestionBankService questionBankService;

    /**
     * 分页条件查询题目
     */
    @GetMapping("/page")
    public Result<Page<QuestionBank>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) BigDecimal difficulty) {

        Page<QuestionBank> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(subjectName)) {
            wrapper.like(QuestionBank::getSubjectName, subjectName);
        }
        if (type != null) {
            wrapper.eq(QuestionBank::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(QuestionBank::getDifficulty, difficulty);
        }
        wrapper.orderByDesc(QuestionBank::getCreateTime);

        Page<QuestionBank> result = questionBankService.page(pageParam, wrapper);
        return Result.success(result);
    }

    /**
     * 获取所有科目列表
     */
    @GetMapping("/subjects")
    public Result<List<String>> getSubjects() {
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(QuestionBank::getSubjectName)
               .groupBy(QuestionBank::getSubjectName)
               .orderByAsc(QuestionBank::getSubjectName);
        List<QuestionBank> list = questionBankService.list(wrapper);
        List<String> subjects = list.stream()
                .map(QuestionBank::getSubjectName)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
        return Result.success(subjects);
    }

    /**
     * 新增/修改题目
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody QuestionBank questionBank) {
        questionBankService.saveOrUpdate(questionBank);
        return Result.success();
    }

    /**
     * 删除题目
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionBankService.removeById(id);
        return Result.success();
    }

    /**
     * Excel 批量导入题目
     */
    @PostMapping("/import")
    public Result<String> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        // 使用 EasyExcel 读取并导入
        EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class,
                new QuestionExcelListener(questionBankService)).sheet().doRead();
        return Result.success("导入成功");
    }

    /**
     * 下载 Excel 导入模板
     */
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("题目导入模板", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

        // 写入表头（空数据，仅模板）
        EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class)
                .sheet("题目模板")
                .doWrite(java.util.Collections.emptyList());
    }
}
