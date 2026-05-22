package com.shen.examsystem.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shen.examsystem.entity.ClassInfo;
import com.shen.examsystem.entity.ClassStudent;
import com.shen.examsystem.entity.SysUser;
import com.shen.examsystem.mapper.ClassInfoMapper;
import com.shen.examsystem.mapper.ClassStudentMapper;
import com.shen.examsystem.mapper.SysUserMapper;
import com.shen.examsystem.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 Service 实现类
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private ClassInfoMapper classInfoMapper;

    @Autowired
    private ClassStudentMapper classStudentMapper;

    @Override
    public SysUser login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 检查用户状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // BCrypt 密码验证
        if (!passwordEncoder.matches(password, user.getPassword())) {
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
        // 设置默认值
        if (user.getRole() == null) {
            user.setRole(0); // 默认学生
        }
        if (user.getStatus() == null) {
            user.setStatus(1); // 默认正常
        }
        // BCrypt 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        this.save(user);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        // 加密新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importStudents(MultipartFile file) {
        List<SysUser> students = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), StudentImportDTO.class, new ReadListener<StudentImportDTO>() {
                @Override
                public void invoke(StudentImportDTO data, AnalysisContext context) {
                    int rowIndex = context.readRowHolder().getRowIndex() + 1;

                    // 学号必填
                    if (data.getStudentNo() == null || data.getStudentNo().isEmpty()) {
                        errors.add("第" + rowIndex + "行: 学号不能为空");
                        return;
                    }

                    // 用户名 = 学号
                    String username = data.getStudentNo();

                    // 检查学号是否已存在（数据库中）
                    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SysUser::getUsername, username);
                    if (SysUserServiceImpl.this.count(wrapper) > 0) {
                        errors.add("第" + rowIndex + "行: 学号 " + username + " 已存在于数据库");
                        return;
                    }

                    // 检查本次导入中是否有重复学号
                    boolean duplicate = students.stream()
                            .anyMatch(s -> username.equals(s.getUsername()));
                    if (duplicate) {
                        errors.add("第" + rowIndex + "行: 学号 " + username + " 在本次导入中重复");
                        return;
                    }

                    // 初始密码 = 学号后6位（如果学号不足6位则用学号本身）
                    String rawPassword = username.length() > 6
                        ? username.substring(username.length() - 6)
                        : username;

                    SysUser student = new SysUser();
                    student.setUsername(username);
                    student.setPassword(passwordEncoder.encode(rawPassword));
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

            // 批量保存学生并关联班级
            if (!students.isEmpty()) {
                this.saveBatch(students);

                // 处理班级关联
                for (SysUser student : students) {
                    if (student.getClassName() != null && !student.getClassName().isEmpty()) {
                        // 查找或创建班级
                        ClassInfo classInfo = findOrCreateClass(student.getClassName());
                        // 建立学生-班级关联
                        ClassStudent cs = new ClassStudent();
                        cs.setClassId(classInfo.getId());
                        cs.setStudentId(student.getId());
                        classStudentMapper.insert(cs);
                    }
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("导入完成: 成功 ").append(students.size()).append(" 条");
            if (!errors.isEmpty()) {
                result.append(", 失败 ").append(errors.size()).append(" 条");
                result.append("。失败原因: ").append(String.join("; ", errors));
            }
            if (!students.isEmpty()) {
                result.append("。初始密码为学号后6位");
            }
            return result.toString();

        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    /**
     * 查找或创建班级
     */
    private ClassInfo findOrCreateClass(String className) {
        LambdaQueryWrapper<ClassInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassInfo::getClassName, className);
        ClassInfo classInfo = classInfoMapper.selectOne(wrapper);

        if (classInfo == null) {
            // 班级不存在，自动创建
            classInfo = new ClassInfo();
            classInfo.setClassName(className);
            classInfo.setDescription("导入学生时自动创建");
            classInfo.setTeacherId(0L); // 默认无教师
            classInfoMapper.insert(classInfo);
        }
        return classInfo;
    }

    @Override
    public List<List<String>> getImportTemplate() {
        List<List<String>> data = new ArrayList<>();
        // 表头：只填学号、姓名、班级等信息，不需要填用户名和密码
        data.add(List.of("学号", "真实姓名", "班级", "手机号", "邮箱"));
        // 示例数据
        data.add(List.of("2024001", "张三", "计算机2401班", "13800138001", "zhangsan@example.com"));
        data.add(List.of("2024002", "李四", "计算机2401班", "13800138002", "lisi@example.com"));
        data.add(List.of("2024003", "王五", "计算机2402班", "", ""));
        return data;
    }

    /**
     * 导入DTO
     */
    public static class StudentImportDTO {
        @ExcelProperty("学号")
        private String studentNo;

        @ExcelProperty("真实姓名")
        private String realName;

        @ExcelProperty("班级")
        private String className;

        @ExcelProperty("手机号")
        private String phone;

        @ExcelProperty("邮箱")
        private String email;

        // 保留 username 和 password 字段以兼容旧代码，但不作为 Excel 列
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
    }
}
