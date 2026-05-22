package com.shen.examsystem.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shen.examsystem.common.Result;
import com.shen.examsystem.dto.QuestionExcelDTO;
import com.shen.examsystem.entity.QuestionBank;
import com.shen.examsystem.interceptor.RequireRole;
import com.shen.examsystem.listener.QuestionExcelListener;
import com.shen.examsystem.service.QuestionBankService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 题库管理 Controller
 */
@RestController
@RequestMapping("/api/question")
public class QuestionBankController {

    @Autowired
    private QuestionBankService questionBankService;

    /**
     * 分页条件查询题目（教师只能看自己的题目 + 公共题目）
     */
    @GetMapping("/page")
    public Result<Page<QuestionBank>> page(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) BigDecimal difficulty) {

        Long teacherId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        Page<QuestionBank> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();

        // 教师只能看自己的题目和公共题目
        if ("teacher".equals(role) && teacherId != null) {
            wrapper.and(w -> w
                    .eq(QuestionBank::getTeacherId, teacherId)  // 自己的题目
                    .or()
                    .isNull(QuestionBank::getTeacherId)          // 公共题目
            );
        }
        // 管理员可以看到所有题目

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
    public Result<List<String>> getSubjects(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        LambdaQueryWrapper<QuestionBank> wrapper = new LambdaQueryWrapper<>();

        // 教师只能看到自己和公共题目的科目
        if ("teacher".equals(role) && teacherId != null) {
            wrapper.and(w -> w
                    .eq(QuestionBank::getTeacherId, teacherId)
                    .or()
                    .isNull(QuestionBank::getTeacherId)
            );
        }

        wrapper.select(QuestionBank::getSubjectName)
               .groupBy(QuestionBank::getSubjectName)
               .orderByAsc(QuestionBank::getSubjectName);
        List<QuestionBank> list = questionBankService.list(wrapper);
        List<String> subjects = list.stream()
                .map(QuestionBank::getSubjectName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return Result.success(subjects);
    }

    /**
     * 新增/修改题目
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody QuestionBank questionBank, HttpServletRequest request) {
        // 新增题目时，自动关联教师ID
        if (questionBank.getId() == null) {
            Long teacherId = (Long) request.getAttribute("userId");
            questionBank.setTeacherId(teacherId);
        }
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
    public Result<String> importExcel(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        if (file.isEmpty()) {
            return Result.error("上传文件为空");
        }
        Long teacherId = (Long) request.getAttribute("userId");
        EasyExcel.read(file.getInputStream(), QuestionExcelDTO.class,
                new QuestionExcelListener(questionBankService, teacherId)).sheet().doRead();
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

        EasyExcel.write(response.getOutputStream(), QuestionExcelDTO.class)
                .sheet("题目模板")
                .doWrite(java.util.Collections.emptyList());
    }
}
