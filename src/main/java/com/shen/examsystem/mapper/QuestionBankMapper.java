package com.shen.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shen.examsystem.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题库 Mapper 接口
 */
@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {
}
