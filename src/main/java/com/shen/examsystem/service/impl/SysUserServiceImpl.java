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
        // 使用数组包装，使其可以在内部类中修改
        final List<SysUser>[] studentsArray = new List[]{new ArrayList<>()};
        final List<String>[] errorsArray = new List[]{new ArrayList<>()};

        try {
            EasyExcel.read(file.getInputStream(), StudentImportDTO.class, new ReadListener<StudentImportDTO>() {
                @Override
                public void invoke(StudentImportDTO data, AnalysisContext context) {
                    int rowIndex = context.readRowHolder().getRowIndex() + 1;
                    List<SysUser> students = studentsArray[0];
                    List<String> errors = errorsArray[0];

                    // 跳过第一行（表头）
                    if (rowIndex == 1) {
                        System.out.println("=== 跳过表头行 ===");
                        return;
                    }

                    // 学号必填
                    if (data.getStudentNo() == null || data.getStudentNo().trim().isEmpty()) {
                        errors.add("第" + rowIndex + "行: 学号不能为空");
                        return;
                    }

                    String studentNo = data.getStudentNo().trim();
                    
                    // 验证学号格式
                    if (!studentNo.matches("^[0-9a-zA-Z]+$")) {
                        errors.add("第" + rowIndex + "行: 学号格式错误，只能包含数字或字母，当前值: " + studentNo);
                        return;
                    }

                    // 用户名 = 学号
                    String username = studentNo;

                    // 检查学号是否已存在（只查未删除的记录）
                    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(SysUser::getUsername, username);
                    long count = SysUserServiceImpl.this.count(wrapper);
                    
                    if (count > 0) {
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

                    // 初始密码 = 学号后6位
                    String rawPassword = username.length() > 6
                        ? username.substring(username.length() - 6)
                        : username;

                    SysUser student = new SysUser();
                    student.setUsername(username);
                    student.setPassword(passwordEncoder.encode(rawPassword));
                    student.setRealName(data.getRealName());
                    student.setStudentNo(studentNo);
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

            List<SysUser> students = studentsArray[0];
            List<String> errors = errorsArray[0];
            List<SysUser> successList = new ArrayList<>();

            // 批量保存新学生并关联班级
            if (!students.isEmpty()) {
                // 逐个插入，捕获唯一索引冲突
                for (SysUser student : students) {
                    try {
                        this.save(student);
                        successList.add(student);
                        System.out.println("=== 学号 " + student.getUsername() + " 插入成功 ===");
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                            // 唯一索引冲突，记录已存在（可能是已删除的记录）
                            System.out.println("=== 学号 " + student.getUsername() + " 已存在（可能已删除），尝试恢复 ===");
                            
                            // 使用自定义方法查询所有记录（包括已删除的）
                            SysUser existingUser = SysUserServiceImpl.this.getBaseMapper().selectByUsernameIncludeDeleted(student.getUsername());
                            
                            if (existingUser != null) {
                                System.out.println("=== 找到记录，ID: " + existingUser.getId() + ", deleted: " + existingUser.getDeleted() + " ===");
                                
                                // 使用自定义方法恢复已删除的记录
                                int updateCount = SysUserServiceImpl.this.getBaseMapper().restoreDeletedUser(
                                    existingUser.getId(),
                                    student.getPassword(),
                                    student.getRealName(),
                                    student.getPhone(),
                                    student.getEmail(),
                                    student.getClassName(),
                                    1 // status = 1
                                );
                                
                                System.out.println("=== 更新记录数: " + updateCount + " ===");
                                
                                if (updateCount > 0) {
                                    student.setId(existingUser.getId()); // 设置ID用于班级关联
                                    successList.add(student);
                                    System.out.println("=== 学号 " + student.getUsername() + " 恢复成功 ===");
                                } else {
                                    errors.add("学号 " + student.getUsername() + " 恢复失败");
                                }
                            } else {
                                System.out.println("=== 未找到记录 ===");
                                errors.add("学号 " + student.getUsername() + " 插入失败: " + e.getMessage());
                            }
                        } else {
                            errors.add("学号 " + student.getUsername() + " 插入失败: " + e.getMessage());
                        }
                    }
                }

                // 处理班级关联
                for (SysUser student : successList) {
                    if (student.getClassName() != null && !student.getClassName().isEmpty()) {
                        ClassInfo classInfo = findOrCreateClass(student.getClassName());
                        
                        // 检查是否已存在班级关联（包括已删除的）
                        ClassStudent existingCs = classStudentMapper.selectByClassAndStudentIncludeDeleted(classInfo.getId(), student.getId());
                        
                        if (existingCs != null) {
                            // 已存在关联，恢复它
                            if (existingCs.getDeleted() != null && existingCs.getDeleted() == 1) {
                                classStudentMapper.restoreDeletedClassStudent(existingCs.getId());
                                System.out.println("=== 恢复班级关联: 班级" + classInfo.getId() + " - 学生" + student.getId() + " ===");
                            }
                            // 如果未删除，则跳过（已存在）
                        } else {
                            // 不存在关联，创建新的
                            ClassStudent cs = new ClassStudent();
                            cs.setClassId(classInfo.getId());
                            cs.setStudentId(student.getId());
                            classStudentMapper.insert(cs);
                            System.out.println("=== 创建班级关联: 班级" + classInfo.getId() + " - 学生" + student.getId() + " ===");
                        }
                    }
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("导入完成: 成功 ").append(successList.size()).append(" 条");
            if (!errors.isEmpty()) {
                result.append(", 失败 ").append(errors.size()).append(" 条");
                result.append("。失败原因: ").append(String.join("; ", errors));
            }
            if (!successList.isEmpty()) {
                result.append("。初始密码为学号后6位");
            }
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("导入失败: " + e.getMessage());
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
        // 表头（第一行）
        data.add(List.of("学号", "真实姓名", "班级", "手机号", "邮箱"));
        // 示例数据（纵向排列，每行一个学生）
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
