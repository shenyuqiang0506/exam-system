package com.shen.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shen.examsystem.entity.ExamPaper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试卷 Mapper 接口
 */
@Mapper
public interface ExamPaperMapper extends BaseMapper<ExamPaper> {
}
