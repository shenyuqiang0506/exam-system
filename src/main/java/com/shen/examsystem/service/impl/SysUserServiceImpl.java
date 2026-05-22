package com.shen.examsystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.mapper.SysUserMapper;
import com.shen.examsystem.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 Service 实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }

    @Override
    public void register(SysUser user) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        this.save(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("旧密码错误");
        }
        user.setPassword(newPassword);
        this.updateById(user);
    }

    @Override
    public String importStudents(MultipartFile file) {
        List<SysUser> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), StudentImportDTO.class, new ReadListener<StudentImportDTO>() {
                @Override
                public void invoke(StudentImportDTO data, AnalysisContext context) {
                    int rowIndex = context.readRowHolder().getRowIndex() + 1;

                    if (data.getUsername() == null || data.getUsername().isEmpty()) {
                        errors.add("第" + rowIndex + "行: 用户名不能为空");
                        return;
                    }

                    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SysUser::getUsername, data.getUsername());
                    if (SysUserServiceImpl.this.count(wrapper) > 0) {
                        errors.add("第" + rowIndex + "行: 用户名 " + data.getUsername() + " 已存在");
                        return;
                    }

                    SysUser student = new SysUser();
                    student.setUsername(data.getUsername());
                    student.setPassword(data.getPassword() != null && !data.getPassword().isEmpty() ? data.getPassword() : "123456");
                    student.setRealName(data.getRealName());
                    student.setStudentNo(data.getStudentNo());
                    student.setPhone(data.getPhone());
                    student.setEmail(data.getEmail());
                    student.setClassName(data.getClassName());
                    student.setRole(0);
                    student.setStatus(1);
                    students.add(student);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).sheet().doRead();

            if (!students.isEmpty()) {
                this.saveBatch(students);
            }

            StringBuilder result = new StringBuilder();
            result.append("导入完成: 成功 ").append(students.size()).append(" 条");
            if (!errors.isEmpty()) {
                result.append(", 失败 ").append(errors.size()).append(" 条");
                result.append("。失败原因: ").append(String.join("; ", errors));
            }
            return result.toString();

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public List<List<String>> getImportTemplate() {
        List<List<String>> data = new ArrayList<>();
        data.add(List.of("用户名", "密码", "真实姓名", "学号", "手机号", "邮箱", "班级"));
        data.add(List.of("student001", "123456", "张三", "2024001", "13800138001", "zhangsan@example.com", "计算机2401班"));
        data.add(List.of("student002", "123456", "李四", "2024002", "13800138002", "lisi@example.com", "计算机2401班"));
        data.add(List.of("student003", "", "王五", "2024003", "", "", "计算机2402班"));
        return data;
    }

    /**
     * 导入DTO
     */
    public static class StudentImportDTO {
        @ExcelProperty("用户名")
        private String username;

        @ExcelProperty("密码")
        private String password;

        @ExcelProperty("真实姓名")
        private String realName;

        @ExcelProperty("学号")
        private String studentNo;

        @ExcelProperty("手机号")
        private String phone;

        @ExcelProperty("邮箱")
        private String email;

        @ExcelProperty("班级")
        private String className;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }
}
