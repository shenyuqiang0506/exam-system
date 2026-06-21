package com.shen.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shen.examsystem.entity.ClassStudent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ClassStudentMapper extends BaseMapper<ClassStudent> {
    
    /**
     * 查询班级学生关联（包括已删除的记录）
     */
    @Select("SELECT id, class_id, student_id, create_time, update_time, deleted FROM class_student WHERE class_id = #{classId} AND student_id = #{studentId} LIMIT 1")
    ClassStudent selectByClassAndStudentIncludeDeleted(@Param("classId") Long classId, @Param("studentId") Long studentId);
    
    /**
     * 恢复已删除的班级学生关联
     */
    @Update("UPDATE class_student SET deleted=0, update_time=NOW() WHERE id=#{id}")
    int restoreDeletedClassStudent(@Param("id") Long id);
}
