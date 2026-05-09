package com.shen.examsystem.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 主观题判分服务测试
 */
class SubjectiveGradingServiceTest {

    private final SubjectiveGradingService service = new SubjectiveGradingService();

    private static final String STANDARD_ANSWER =
            "Spring Boot是一个基于Spring框架的快速开发脚手架，核心思想是约定优于配置。";
    private static final double FULL_SCORE = 10.0;

    @Test
    void testHighSimilarityAnswer() {
        // 学生A：表达接近标准答案，应该得 8-9 分
        String studentA = "Spring Boot是基于Spring的，它主要理念就是约定优于配置，能快速开发。";
        double score = service.gradeSubjective(studentA, STANDARD_ANSWER, FULL_SCORE);

        System.out.println("学生A得分: " + score);
        assertTrue(score >= 7.0, "相似答案得分应 >= 7");
    }

    @Test
    void testLowSimilarityAnswer() {
        // 学生B：完全不相关，应该得 0-1 分
        String studentB = "不知道，这题太难了。";
        double score = service.gradeSubjective(studentB, STANDARD_ANSWER, FULL_SCORE);

        System.out.println("学生B得分: " + score);
        assertTrue(score <= 1.0, "无关答案得分应 <= 1");
    }

    @Test
    void testEmptyAnswer() {
        // 空答案应返回 0
        double score = service.gradeSubjective("", STANDARD_ANSWER, FULL_SCORE);
        assertEquals(0.0, score);
    }

    @Test
    void testNullStudentAnswer() {
        // null 学生答案应返回 0
        double score = service.gradeSubjective(null, STANDARD_ANSWER, FULL_SCORE);
        assertEquals(0.0, score);
    }

    @Test
    void testNullStandardAnswer() {
        // null 标准答案应抛出异常
        assertThrows(RuntimeException.class, () -> {
            service.gradeSubjective("学生答案", null, FULL_SCORE);
        });
    }

    @Test
    void testPerfectAnswer() {
        // 完全相同的答案应得满分
        double score = service.gradeSubjective(STANDARD_ANSWER, STANDARD_ANSWER, FULL_SCORE);
        System.out.println("完全相同答案得分: " + score);
        assertEquals(10.0, score, 0.01);
    }

    @Test
    void testPartialMatchAnswer() {
        // 部分匹配
        String studentAnswer = "S，约定优于配置是它的核心。";
        double score = service.gradeSubjective(studentAnswer, STANDARD_ANSWER, FULL_SCORE);
        System.out.println("部分匹配答案得分: " + score);
        assertTrue(score >= 5.0 && score <= 9.5, "部分匹配得分应在 5~9.5 之间");
    }
}
