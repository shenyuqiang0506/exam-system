-- =====================================================
-- 在线考试系统 数据库初始化脚本
-- MySQL 8.0+
-- =====================================================

SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;
SET CHARACTER_SET_RESULTS = utf8mb4;

CREATE DATABASE IF NOT EXISTS exam_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE exam_system;

-- -------------------------------------------------
-- 1. 用户表 (sys_user)
-- -------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT       NOT NULL COMMENT '用户ID (雪花算法)',
    username        VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password        VARCHAR(255) NOT NULL COMMENT '密码 (BCrypt加密)',
    real_name       VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    student_no      VARCHAR(50)  DEFAULT NULL COMMENT '学号',
    phone           VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    class_name      VARCHAR(100) DEFAULT NULL COMMENT '班级',
    avatar          LONGTEXT      DEFAULT NULL COMMENT '头像(base64或URL)',
    role            TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0-学生, 1-教师, 2-管理员',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- -------------------------------------------------
-- 2. 题库表 (question_bank)
-- -------------------------------------------------
DROP TABLE IF EXISTS question_bank;
CREATE TABLE question_bank (
    id               BIGINT        NOT NULL COMMENT '题目ID (雪花算法)',
    teacher_id       BIGINT        DEFAULT NULL COMMENT '创建教师ID (NULL表示公共题目)',
    subject_name     VARCHAR(50)   NOT NULL COMMENT '科目名称',
    type             TINYINT       NOT NULL COMMENT '题型: 1-单选, 2-多选, 3-判断, 4-主观题',
    content          TEXT          NOT NULL COMMENT '题目正文 (含选项JSON)',
    standard_answer  TEXT          NOT NULL COMMENT '标准答案',
    points_keyword   VARCHAR(1000) DEFAULT NULL COMMENT '给分关键词 (逗号分隔, 仅主观题)',
    score            DECIMAL(5,1)  NOT NULL DEFAULT 5.0 COMMENT '题目默认分值',
    difficulty       DECIMAL(2,1)  NOT NULL DEFAULT 0.5 COMMENT '难度系数 (0.1~1.0)',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    KEY idx_teacher_id (teacher_id),
    KEY idx_subject_type (subject_name, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库表';

-- -------------------------------------------------
-- 3. 试卷表 (exam_paper)
-- -------------------------------------------------
DROP TABLE IF EXISTS exam_paper;
CREATE TABLE exam_paper (
    id                 BIGINT        NOT NULL COMMENT '试卷ID (雪花算法)',
    title              VARCHAR(200)  NOT NULL COMMENT '试卷名称',
    subject_name       VARCHAR(50)   NOT NULL COMMENT '科目名称',
    total_score        DECIMAL(6,1)  NOT NULL DEFAULT 100.0 COMMENT '试卷总分',
    target_difficulty  DECIMAL(2,1)  NOT NULL DEFAULT 0.5 COMMENT '期望平均难度系数',
    start_time         DATETIME      DEFAULT NULL COMMENT '考试开始时间',
    end_time           DATETIME      DEFAULT NULL COMMENT '考试结束时间',
    is_archived        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-正常, 1-已归档',
    create_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    KEY idx_subject (subject_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷表';

-- -------------------------------------------------
-- 4. 试卷-题目关联表 (paper_question)
-- -------------------------------------------------
DROP TABLE IF EXISTS paper_question;
CREATE TABLE paper_question (
    id          BIGINT NOT NULL COMMENT '关联ID (雪花算法)',
    paper_id    BIGINT NOT NULL COMMENT '试卷ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_paper_question (paper_id, question_id),
    KEY idx_paper_id (paper_id),
    KEY idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷题目关联表';

-- -------------------------------------------------
-- 5. 考试记录表 (exam_record)
-- -------------------------------------------------
DROP TABLE IF EXISTS exam_record;
CREATE TABLE exam_record (
    id               BIGINT        NOT NULL COMMENT '记录ID (雪花算法)',
    student_id       BIGINT        NOT NULL COMMENT '学生用户ID',
    paper_id         BIGINT        NOT NULL COMMENT '试卷ID',
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-考试中, 1-已交卷/已批阅',
    total_score      DECIMAL(6,1)  DEFAULT NULL COMMENT '最终总得分',
    objective_score  DECIMAL(6,1)  DEFAULT NULL COMMENT '客观题得分',
    subjective_score DECIMAL(6,1)  DEFAULT NULL COMMENT '主观题得分',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    KEY idx_student_paper (student_id, paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考试记录表';

-- -------------------------------------------------
-- 6. 答题明细表 (answer_detail)
-- -------------------------------------------------
DROP TABLE IF EXISTS answer_detail;
CREATE TABLE answer_detail (
    id            BIGINT        NOT NULL COMMENT '明细ID (雪花算法)',
    record_id     BIGINT        NOT NULL COMMENT '考试记录ID',
    question_id   BIGINT        NOT NULL COMMENT '题目ID',
    student_answer TEXT         DEFAULT NULL COMMENT '学生作答内容',
    score         DECIMAL(5,1)  DEFAULT NULL COMMENT '该题最终得分',
    ai_reason     TEXT          DEFAULT NULL COMMENT '主观题算法判分依据/评语',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    KEY idx_record_question (record_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题明细表';

-- -------------------------------------------------
-- 7. 班级表 (class_info)
-- -------------------------------------------------
DROP TABLE IF EXISTS class_info;
CREATE TABLE class_info (
    id              BIGINT       NOT NULL COMMENT '班级ID (雪花算法)',
    class_name      VARCHAR(100) NOT NULL COMMENT '班级名称',
    teacher_id      BIGINT       DEFAULT NULL COMMENT '教师ID (NULL表示未指定)',
    description     VARCHAR(500) DEFAULT NULL COMMENT '班级描述',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级表';

-- -------------------------------------------------
-- 8. 班级-学生关联表 (class_student)
-- -------------------------------------------------
DROP TABLE IF EXISTS class_student;
CREATE TABLE class_student (
    id              BIGINT   NOT NULL COMMENT '关联ID (雪花算法)',
    class_id        BIGINT   NOT NULL COMMENT '班级ID',
    student_id      BIGINT   NOT NULL COMMENT '学生ID',
    join_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_class_student (class_id, student_id),
    KEY idx_class_id (class_id),
    KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级学生关联表';

-- -------------------------------------------------
-- 9. 试卷-班级关联表 (paper_class)
-- -------------------------------------------------
DROP TABLE IF EXISTS paper_class;
CREATE TABLE paper_class (
    id              BIGINT   NOT NULL COMMENT '关联ID (雪花算法)',
    paper_id        BIGINT   NOT NULL COMMENT '试卷ID',
    class_id        BIGINT   NOT NULL COMMENT '班级ID',
    create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_paper_class (paper_id, class_id),
    KEY idx_paper_id (paper_id),
    KEY idx_class_id (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试卷班级关联表';

-- =====================================================
-- 测试数据
-- 注意：密码已使用 BCrypt 加密
-- 原始密码: teacher/123456, student/123456, admin/admin123
-- =====================================================

-- 用户测试数据
INSERT INTO sys_user (id, username, password, real_name, role, status) VALUES
(1001, 'teacher', '$2a$10$hsdPocSo4CC3kMJr.lv/9ucScFWHD7paQA9g/S6/GkW4Th2MXgbz.', '张老师', 1, 1),
(1002, 'student', '$2a$10$hsdPocSo4CC3kMJr.lv/9ucScFWHD7paQA9g/S6/GkW4Th2MXgbz.', '李同学', 0, 1),
(1003, 'admin', '$2a$10$I9ur6Vv2/TeEJlIvleiwvOLJlPWUnm9xD5mO04iButAgAlt1tgOVm', '系统管理员', 2, 1),
(1004, 'student2', '$2a$10$hsdPocSo4CC3kMJr.lv/9ucScFWHD7paQA9g/S6/GkW4Th2MXgbz.', '王同学', 0, 1),
(1005, 'student3', '$2a$10$hsdPocSo4CC3kMJr.lv/9ucScFWHD7paQA9g/S6/GkW4Th2MXgbz.', '赵同学', 0, 1);

-- 班级测试数据
INSERT INTO class_info (id, class_name, teacher_id, description) VALUES
(2001, 'Java程序设计2024春季班', 1001, 'Java程序设计课程班级'),
(2002, '数据结构2024春季班', 1001, '数据结构课程班级');

-- 班级学生关联测试数据
INSERT INTO class_student (id, class_id, student_id) VALUES
(3001, 2001, 1002),
(3002, 2001, 1004),
(3003, 2002, 1002),
(3004, 2002, 1005);

-- =====================================================
-- 题库样本数据 (100 条)
-- 科目: Java程序设计(30) / 数据结构(25) / 计算机网络(25) / 高等数学(20)
-- 题型: 1-单选 2-多选 3-判断 4-主观
-- =====================================================
INSERT INTO exam_system.question_bank (id, subject_name, type, content, standard_answer, points_keyword, score,
                                       difficulty)
VALUES

-- ==================== Java程序设计 - 单选题 (10条) ====================
(1000001, 'Java程序设计', 1,
 '{"title":"下列哪个关键字用于声明一个类不能被继承？","options":[{"value":"A","label":"abstract"},{"value":"B","label":"final"},{"value":"C","label":"static"},{"value":"D","label":"synchronized"}]}',
 'B', NULL, 5.0, 0.3),
(1000002, 'Java程序设计', 1,
 '{"title":"Java中 int 类型占用的字节数是？","options":[{"value":"A","label":"1字节"},{"value":"B","label":"2字节"},{"value":"C","label":"4字节"},{"value":"D","label":"8字节"}]}',
 'C', NULL, 5.0, 0.2),
(1000003, 'Java程序设计', 1,
 '{"title":"下列哪个不是Java的基本数据类型？","options":[{"value":"A","label":"int"},{"value":"B","label":"char"},{"value":"C","label":"String"},{"value":"D","label":"boolean"}]}',
 'C', NULL, 5.0, 0.2),
(1000004, 'Java程序设计', 1,
 '{"title":"关于ArrayList和LinkedList，描述正确的是？","options":[{"value":"A","label":"ArrayList随机访问快，LinkedList插入删除快"},{"value":"B","label":"LinkedList随机访问快，ArrayList插入删除快"},{"value":"C","label":"两者性能完全相同"},{"value":"D","label":"ArrayList不支持动态扩容"}]}',
 'A', NULL, 5.0, 0.5),
(1000005, 'Java程序设计', 1,
 '{"title":"Java中抛出受检异常必须使用哪个关键字声明？","options":[{"value":"A","label":"throws"},{"value":"B","label":"throw"},{"value":"C","label":"try"},{"value":"D","label":"catch"}]}',
 'A', NULL, 5.0, 0.4),
(1000006, 'Java程序设计', 1,
 '{"title":"以下哪个是Java中的函数式接口注解？","options":[{"value":"A","label":"@Override"},{"value":"B","label":"@FunctionalInterface"},{"value":"C","label":"@Deprecated"},{"value":"D","label":"@SuppressWarnings"}]}',
 'B', NULL, 5.0, 0.5),
(1000007, 'Java程序设计', 1,
 '{"title":"HashMap在Java 8中，链表长度超过多少时转为红黑树？","options":[{"value":"A","label":"4"},{"value":"B","label":"6"},{"value":"C","label":"8"},{"value":"D","label":"16"}]}',
 'C', NULL, 5.0, 0.6),
(1000008, 'Java程序设计', 1,
 '{"title":"以下哪个是Java集合框架的根接口？","options":[{"value":"A","label":"Collection"},{"value":"B","label":"Iterable"},{"value":"C","label":"List"},{"value":"D","label":"Set"}]}',
 'B', NULL, 5.0, 0.6),
(1000009, 'Java程序设计', 1,
 '{"title":"volatile关键字在Java中的主要作用是？","options":[{"value":"A","label":"保证原子性"},{"value":"B","label":"保证可见性和有序性"},{"value":"C","label":"实现线程同步"},{"value":"D","label":"防止死锁"}]}',
 'B', NULL, 5.0, 0.7),
(1000010, 'Java程序设计', 1,
 '{"title":"String.intern()方法的作用是？","options":[{"value":"A","label":"将字符串转为大写"},{"value":"B","label":"返回字符串在常量池中的引用"},{"value":"C","label":"反转字符串"},{"value":"D","label":"去除首尾空格"}]}',
 'B', NULL, 5.0, 0.7),

-- ==================== Java程序设计 - 多选题 (5条) ====================
(1000011, 'Java程序设计', 2,
 '{"title":"以下哪些是Java面向对象的三大特性？","options":[{"value":"A","label":"封装"},{"value":"B","label":"继承"},{"value":"C","label":"多态"},{"value":"D","label":"抽象"}]}',
 'ABC', NULL, 8.0, 0.3),
(1000012, 'Java程序设计', 2,
 '{"title":"以下哪些集合是线程安全的？","options":[{"value":"A","label":"Vector"},{"value":"B","label":"ArrayList"},{"value":"C","label":"Hashtable"},{"value":"D","label":"ConcurrentHashMap"}]}',
 'ACD', NULL, 8.0, 0.6),
(1000013, 'Java程序设计', 2,
 '{"title":"以下哪些属于运行时异常 RuntimeException？","options":[{"value":"A","label":"NullPointerException"},{"value":"B","label":"IOException"},{"value":"C","label":"ArrayIndexOutOfBoundsException"},{"value":"D","label":"ClassCastException"}]}',
 'ACD', NULL, 8.0, 0.5),
(1000014, 'Java程序设计', 2,
 '{"title":"以下哪些是Java 8引入的新特性？","options":[{"value":"A","label":"Lambda表达式"},{"value":"B","label":"Stream API"},{"value":"C","label":"泛型"},{"value":"D","label":"Optional类"}]}',
 'ABD', NULL, 8.0, 0.5),
(1000015, 'Java程序设计', 2,
 '{"title":"关于Java接口，以下哪些说法正确？","options":[{"value":"A","label":"接口可以包含默认方法"},{"value":"B","label":"接口不能被实例化"},{"value":"C","label":"一个类可以实现多个接口"},{"value":"D","label":"接口变量默认是public static final"}]}',
 'ABCD', NULL, 8.0, 0.5),

-- ==================== Java程序设计 - 判断题 (8条) ====================
(1000016, 'Java程序设计', 3, 'Java中的==比较引用类型时，比较的是对象的内容', '错误', NULL, 5.0, 0.3),
(1000017, 'Java程序设计', 3, 'final修饰的变量只能赋值一次', '正确', NULL, 5.0, 0.4),
(1000018, 'Java程序设计', 3, 'Java中子类可以继承父类的private成员变量', '错误', NULL, 5.0, 0.4),
(1000019, 'Java程序设计', 3, 'StringBuilder是线程安全的，StringBuffer不是线程安全的', '错误', NULL, 5.0, 0.5),
(1000020, 'Java程序设计', 3, '接口中可以定义static方法（Java 8及以后）', '正确', NULL, 5.0, 0.5),
(1000021, 'Java程序设计', 3, 'Java的垃圾回收器可以回收所有不再被引用的对象', '正确', NULL, 5.0, 0.5),
(1000022, 'Java程序设计', 3, '抽象类可以有构造方法', '正确', NULL, 5.0, 0.5),
(1000023, 'Java程序设计', 3, 'Java中的int和Integer是同一类型', '错误', NULL, 5.0, 0.4),

-- ==================== Java程序设计 - 主观题 (7条) ====================
(1000024, 'Java程序设计', 4, '请简述Java中多态的概念及其实现方式',
 '多态是指同一行为具有多个不同表现形式的能力。实现方式：1.方法重载（编译时多态）；2.方法重写+向上转型（运行时多态）。运行时多态通过父类引用指向子类对象实现，调用方法时根据实际对象类型决定执行哪个方法（动态分派）。',
 '多态,方法重载,方法重写,向上转型', 10.0, 0.5),
(1000025, 'Java程序设计', 4, '简述Java中HashMap的工作原理，包括put和get的过程',
 'HashMap基于数组+链表+红黑树实现。put时：计算key的hash值，对数组长度取模得到下标，若该位置为空直接插入；若有冲突则以链表追加；链表长度超过8时转为红黑树。get时：同样计算下标，遍历链表或红黑树找到key相等的节点返回value。默认初始容量16，负载因子0.75，超出时扩容为2倍。',
 'hash,数组,链表,红黑树,负载因子,扩容', 15.0, 0.7),
(1000026, 'Java程序设计', 4, '请解释Java中的垃圾回收机制，常见的GC算法有哪些？',
 'Java GC自动回收不再被引用的对象。判断可回收使用可达性分析（从GC Roots出发无法到达则可回收）。常见算法：1.标记-清除：标记可回收对象后清除，产生碎片；2.复制算法：将存活对象复制到新区域，适用新生代；3.标记-整理：标记后将存活对象移到一端，适用老年代；4.分代收集：新生代和老年代分别采用不同算法。',
 'GC,可达性分析,标记清除,复制算法,标记整理,分代', 15.0, 0.7),
(1000027, 'Java程序设计', 4, '什么是Java中的线程安全？如何实现线程安全？',
 '线程安全是指多个线程并发访问同一资源时程序能正确执行。实现方式：1.使用synchronized关键字加锁；2.使用ReentrantLock等显式锁；3.使用volatile保证可见性；4.使用线程安全集合类如ConcurrentHashMap；5.使用ThreadLocal实现线程局部变量；6.使用AtomicInteger等原子类。',
 '线程安全,synchronized,volatile,锁,原子类', 10.0, 0.6),
(1000028, 'Java程序设计', 4, '请简述Spring框架中IOC和DI的概念',
 'IOC控制反转是指将对象的创建和管理权交给Spring容器，而不是由程序代码直接控制。DI依赖注入是IOC的具体实现方式，即容器在创建对象时自动将其依赖的对象注入进来。注入方式有：构造器注入、Setter注入、字段注入。好处：降低代码耦合度，提高可测试性和可维护性。',
 'IOC,控制反转,DI,依赖注入,Spring容器,解耦', 10.0, 0.6),
(1000029, 'Java程序设计', 4, '请解释Java中的反射机制及其应用场景',
 'Java反射机制允许程序在运行时获取类的信息（类名、字段、方法等）并动态调用。核心类：Class、Method、Field、Constructor。应用场景：1.框架设计（Spring、Hibernate大量使用）；2.动态代理；3.单元测试框架；4.序列化/反序列化；5.IDE代码补全。缺点：性能较低，破坏封装性。',
 '反射,Class,Method,Field,动态调用,框架', 10.0, 0.7),
(1000030, 'Java程序设计', 4, '比较进程和线程的区别，以及Java中创建线程的方式',
 '进程是操作系统资源分配的基本单位，线程是CPU调度的基本单位。区别：进程有独立内存空间，线程共享进程内存；进程切换开销大，线程切换开销小。Java创建线程的方式：1.继承Thread类；2.实现Runnable接口；3.实现Callable接口（有返回值）；4.使用线程池ExecutorService。',
 '进程,线程,Thread,Runnable,Callable,线程池', 10.0, 0.5),

-- ==================== 数据结构 - 单选题 (8条) ====================
(1000031, '数据结构', 1,
 '{"title":"以下哪种排序算法的平均时间复杂度为O(n log n)？","options":[{"value":"A","label":"冒泡排序"},{"value":"B","label":"插入排序"},{"value":"C","label":"快速排序"},{"value":"D","label":"选择排序"}]}',
 'C', NULL, 5.0, 0.4),
(1000032, '数据结构', 1,
 '{"title":"栈（Stack）的特点是？","options":[{"value":"A","label":"先进先出FIFO"},{"value":"B","label":"先进后出FILO"},{"value":"C","label":"随机访问"},{"value":"D","label":"双端访问"}]}',
 'B', NULL, 5.0, 0.2),
(1000033, '数据结构', 1,
 '{"title":"哈希表在理想情况下查找的时间复杂度是？","options":[{"value":"A","label":"O(1)"},{"value":"B","label":"O(n)"},{"value":"C","label":"O(log n)"},{"value":"D","label":"O(n^2)"}]}',
 'A', NULL, 5.0, 0.3),
(1000034, '数据结构', 1,
 '{"title":"图的深度优先搜索DFS使用的辅助数据结构是？","options":[{"value":"A","label":"队列"},{"value":"B","label":"栈"},{"value":"C","label":"堆"},{"value":"D","label":"链表"}]}',
 'B', NULL, 5.0, 0.5),
(1000035, '数据结构', 1,
 '{"title":"二分查找的前提条件是？","options":[{"value":"A","label":"数据无序存储"},{"value":"B","label":"数据有序存储"},{"value":"C","label":"数据量较小"},{"value":"D","label":"数据存储在链表中"}]}',
 'B', NULL, 5.0, 0.3),
(1000036, '数据结构', 1,
 '{"title":"以下哪种树保证查找、插入、删除操作的时间复杂度都是O(log n)？","options":[{"value":"A","label":"普通二叉树"},{"value":"B","label":"完全二叉树"},{"value":"C","label":"红黑树"},{"value":"D","label":"满二叉树"}]}',
 'C', NULL, 5.0, 0.6),
(1000037, '数据结构', 1,
 '{"title":"链表相比数组，其优势在于？","options":[{"value":"A","label":"随机访问速度快"},{"value":"B","label":"插入和删除操作效率高"},{"value":"C","label":"内存利用率更高"},{"value":"D","label":"缓存命中率高"}]}',
 'B', NULL, 5.0, 0.3),
(1000038, '数据结构', 1,
 '{"title":"一棵有n个节点的完全二叉树，其高度为？","options":[{"value":"A","label":"floor(log2 n)"},{"value":"B","label":"floor(log2 n)+1"},{"value":"C","label":"n/2"},{"value":"D","label":"n-1"}]}',
 'B', NULL, 5.0, 0.5),

-- ==================== 数据结构 - 多选题 (5条) ====================
(1000039, '数据结构', 2,
 '{"title":"以下哪些排序算法是稳定排序？","options":[{"value":"A","label":"冒泡排序"},{"value":"B","label":"快速排序"},{"value":"C","label":"归并排序"},{"value":"D","label":"插入排序"}]}',
 'ACD', NULL, 8.0, 0.6),
(1000040, '数据结构', 2,
 '{"title":"以下哪些是树的遍历方式？","options":[{"value":"A","label":"前序遍历"},{"value":"B","label":"中序遍历"},{"value":"C","label":"后序遍历"},{"value":"D","label":"层次遍历"}]}',
 'ABCD', NULL, 8.0, 0.3),
(1000041, '数据结构', 2,
 '{"title":"以下哪些属于图的存储方式？","options":[{"value":"A","label":"邻接矩阵"},{"value":"B","label":"邻接表"},{"value":"C","label":"十字链表"},{"value":"D","label":"散列表"}]}',
 'ABC', NULL, 8.0, 0.5),
(1000042, '数据结构', 2,
 '{"title":"以下哪些数据结构可以用来实现队列？","options":[{"value":"A","label":"数组"},{"value":"B","label":"链表"},{"value":"C","label":"两个栈"},{"value":"D","label":"双端队列"}]}',
 'ABCD', NULL, 8.0, 0.5),
(1000043, '数据结构', 2,
 '{"title":"关于堆（Heap）数据结构，以下哪些说法正确？","options":[{"value":"A","label":"大根堆中父节点的值大于等于子节点"},{"value":"B","label":"堆是一棵完全二叉树"},{"value":"C","label":"堆排序的时间复杂度为O(n log n)"},{"value":"D","label":"堆支持O(1)时间的最值查找"}]}',
 'ABCD', NULL, 8.0, 0.6),

-- ==================== 数据结构 - 判断题 (7条) ====================
(1000044, '数据结构', 3, '队列（Queue）是一种先进后出的数据结构', '错误', NULL, 5.0, 0.2),
(1000045, '数据结构', 3, '二叉搜索树的中序遍历结果是有序的', '正确', NULL, 5.0, 0.4),
(1000046, '数据结构', 3, '图的广度优先搜索BFS使用队列作为辅助数据结构', '正确', NULL, 5.0, 0.4),
(1000047, '数据结构', 3, '完全二叉树一定是满二叉树', '错误', NULL, 5.0, 0.4),
(1000048, '数据结构', 3, '归并排序在最坏情况下时间复杂度仍为O(n log n)', '正确', NULL, 5.0, 0.5),
(1000049, '数据结构', 3, '哈希表的最坏情况查找时间复杂度是O(n)', '正确', NULL, 5.0, 0.6),
(1000050, '数据结构', 3, '双向链表任意位置的插入和删除操作时间复杂度都是O(1)', '错误', NULL, 5.0, 0.5),

-- ==================== 数据结构 - 主观题 (5条) ====================
(1000051, '数据结构', 4, '请比较快速排序和归并排序的异同，分析各自适用场景',
 '快速排序：原地排序，空间O(log n)，平均时间O(n log n)，最坏O(n^2)，不稳定。归并排序：需额外O(n)空间，时间始终O(n log n)，稳定排序。快速排序适合内存充足且对稳定性无要求的场景，实际中因缓存友好性更快；归并排序适合要求稳定性或外部排序的场景。',
 '快速排序,归并排序,时间复杂度,稳定性,空间复杂度', 15.0, 0.6),
(1000052, '数据结构', 4, '请解释什么是AVL树，它与普通二叉搜索树相比有什么优势？',
 'AVL树是自平衡二叉搜索树，任意节点的左右子树高度差不超过1。普通BST在极端情况下（如有序插入）会退化为链表，查找效率降至O(n)；AVL树通过旋转操作（左旋、右旋、左右旋、右左旋）保持平衡，确保查找、插入、删除操作的时间复杂度都是O(log n)。',
 'AVL,平衡,旋转,BST,log n', 10.0, 0.7),
(1000053, '数据结构', 4, '请简述Dijkstra最短路径算法的基本思想',
 'Dijkstra用于求单源最短路径。基本思想：维护已确定最短距离的节点集合S和未确定集合。初始化源点距离为0，其余为无穷。每次从未确定集合中选取距离最小的节点u加入S，然后用u更新其邻接节点的距离（松弛操作）。重复直到所有节点加入S。时间O(V^2)，使用优先队列可优化到O((V+E)log V)。不适用于有负权边的图。',
 'Dijkstra,最短路径,松弛,优先队列', 10.0, 0.8),
(1000054, '数据结构', 4, '请解释哈希冲突的概念及常见解决方法',
 '哈希冲突是指不同的键通过哈希函数映射到相同的桶位置。解决方法：1.链地址法（拉链法）：同一桶位置的元素以链表存储，Java HashMap采用此法；2.开放地址法：冲突时寻找下一个空闲位置，包括线性探测、二次探测；3.再哈希法：使用另一个哈希函数重新计算；4.建立公共溢出区。链地址法最常用；开放地址法空间利用率高但性能随负载因子增大而降低。',
 '哈希冲突,链地址法,开放地址,拉链法', 10.0, 0.6),
(1000055, '数据结构', 4, '请介绍动态规划的基本思想，并举例说明',
 '动态规划将问题分解为重叠子问题，通过存储子问题的解避免重复计算。基本要素：最优子结构、重叠子问题。解题步骤：定义状态、找状态转移方程、确定初始值、确定计算顺序。经典例子：斐波那契数列f(n)=f(n-1)+f(n-2)、0-1背包问题、最长公共子序列LCS。DP与递归+备忘录本质相同，但DP自底向上，避免递归栈溢出。',
 '动态规划,状态转移,最优子结构,重叠子问题', 15.0, 0.7),

-- ==================== 计算机网络 - 单选题 (8条) ====================
(1000056, '计算机网络', 1,
 '{"title":"HTTP协议默认使用的端口号是？","options":[{"value":"A","label":"80"},{"value":"B","label":"443"},{"value":"C","label":"8080"},{"value":"D","label":"21"}]}',
 'A', NULL, 5.0, 0.2),
(1000057, '计算机网络', 1,
 '{"title":"OSI参考模型共分为几层？","options":[{"value":"A","label":"4层"},{"value":"B","label":"5层"},{"value":"C","label":"7层"},{"value":"D","label":"9层"}]}',
 'C', NULL, 5.0, 0.2),
(1000058, '计算机网络', 1,
 '{"title":"TCP和UDP的主要区别是？","options":[{"value":"A","label":"TCP无连接，UDP面向连接"},{"value":"B","label":"TCP面向连接提供可靠传输，UDP无连接不保证可靠"},{"value":"C","label":"TCP传输速度比UDP快"},{"value":"D","label":"UDP只能用于局域网"}]}',
 'B', NULL, 5.0, 0.3),
(1000059, '计算机网络', 1,
 '{"title":"DNS的主要作用是？","options":[{"value":"A","label":"分配IP地址"},{"value":"B","label":"将域名解析为IP地址"},{"value":"C","label":"路由数据包"},{"value":"D","label":"加密网络通信"}]}',
 'B', NULL, 5.0, 0.2),
(1000060, '计算机网络', 1,
 '{"title":"以下哪个协议工作在传输层？","options":[{"value":"A","label":"HTTP"},{"value":"B","label":"IP"},{"value":"C","label":"TCP"},{"value":"D","label":"以太网"}]}',
 'C', NULL, 5.0, 0.3),
(1000061, '计算机网络', 1,
 '{"title":"HTTPS默认端口是？","options":[{"value":"A","label":"80"},{"value":"B","label":"443"},{"value":"C","label":"8443"},{"value":"D","label":"22"}]}',
 'B', NULL, 5.0, 0.3),
(1000062, '计算机网络', 1,
 '{"title":"TCP三次握手中，第二次握手服务器发送的标志位是？","options":[{"value":"A","label":"SYN=1"},{"value":"B","label":"ACK=1"},{"value":"C","label":"SYN=1 ACK=1"},{"value":"D","label":"FIN=1 ACK=1"}]}',
 'C', NULL, 5.0, 0.5),
(1000063, '计算机网络', 1,
 '{"title":"以下哪种是私有IP地址？","options":[{"value":"A","label":"8.8.8.8"},{"value":"B","label":"192.168.1.1"},{"value":"C","label":"220.181.38.148"},{"value":"D","label":"114.114.114.114"}]}',
 'B', NULL, 5.0, 0.3),

-- ==================== 计算机网络 - 多选题 (5条) ====================
(1000064, '计算机网络', 2,
 '{"title":"以下哪些属于应用层协议？","options":[{"value":"A","label":"HTTP"},{"value":"B","label":"FTP"},{"value":"C","label":"TCP"},{"value":"D","label":"SMTP"}]}',
 'ABD', NULL, 8.0, 0.4),
(1000065, '计算机网络', 2,
 '{"title":"关于TCP连接，以下正确的是？","options":[{"value":"A","label":"建立连接需要三次握手"},{"value":"B","label":"关闭连接需要四次挥手"},{"value":"C","label":"TCP提供全双工通信"},{"value":"D","label":"TCP不保证数据顺序"}]}',
 'ABC', NULL, 8.0, 0.5),
(1000066, '计算机网络', 2,
 '{"title":"以下哪些是TCP的可靠性保证机制？","options":[{"value":"A","label":"确认应答ACK"},{"value":"B","label":"超时重传"},{"value":"C","label":"流量控制"},{"value":"D","label":"拥塞控制"}]}',
 'ABCD', NULL, 8.0, 0.6),
(1000067, '计算机网络', 2,
 '{"title":"以下哪些是IPv4私有地址段？","options":[{"value":"A","label":"10.0.0.0/8"},{"value":"B","label":"172.16.0.0/12"},{"value":"C","label":"192.168.0.0/16"},{"value":"D","label":"224.0.0.0/4"}]}',
 'ABC', NULL, 8.0, 0.5),
(1000068, '计算机网络', 2,
 '{"title":"以下哪些是HTTP请求方法？","options":[{"value":"A","label":"GET"},{"value":"B","label":"POST"},{"value":"C","label":"DELETE"},{"value":"D","label":"PUT"}]}',
 'ABCD', NULL, 8.0, 0.4),

-- ==================== 计算机网络 - 判断题 (7条) ====================
(1000069, '计算机网络', 3, 'UDP协议是面向连接的可靠传输协议', '错误', NULL, 5.0, 0.2),
(1000070, '计算机网络', 3, 'ARP协议用于将IP地址解析为MAC地址', '正确', NULL, 5.0, 0.4),
(1000071, '计算机网络', 3, 'HTTP/2支持多路复用，可在一个TCP连接上并行传输多个请求', '正确', NULL, 5.0, 0.6),
(1000072, '计算机网络', 3, 'IP协议工作在传输层', '错误', NULL, 5.0, 0.3),
(1000073, '计算机网络', 3, 'Cookie是存储在服务器端的会话信息', '错误', NULL, 5.0, 0.4),
(1000074, '计算机网络', 3, 'HTTPS在HTTP的基础上增加了SSL/TLS加密层', '正确', NULL, 5.0, 0.3),
(1000075, '计算机网络', 3, 'TCP四次挥手中，主动关闭方需等待2MSL后才能完全关闭连接', '正确', NULL, 5.0, 0.7),

-- ==================== 计算机网络 - 主观题 (5条) ====================
(1000076, '计算机网络', 4, '请详细描述TCP的三次握手过程，并解释为什么需要三次而不是两次',
 '三次握手：1.客户端发送SYN=1，seq=x，进入SYN_SENT状态；2.服务器发送SYN=1，ACK=1，ack=x+1，seq=y，进入SYN_RCVD状态；3.客户端发送ACK=1，ack=y+1，连接建立。需要三次：两次握手只能保证客户端发送和服务器接收正常，无法保证服务器发送和客户端接收正常；三次握手确认双方的收发能力都正常，且防止历史失效连接请求到达服务器造成资源浪费。',
 '三次握手,SYN,ACK,seq,双向通信', 15.0, 0.5),
(1000077, '计算机网络', 4, '请解释HTTP和HTTPS的区别以及HTTPS的工作原理',
 'HTTP明文传输不安全；HTTPS在HTTP基础上加了SSL/TLS加密。HTTPS工作原理：1.客户端发起HTTPS请求；2.服务器返回证书（含公钥）；3.客户端验证证书；4.客户端生成对称密钥，用服务器公钥加密发送；5.服务器用私钥解密获取对称密钥；6.双方用对称密钥加密通信。区别：HTTPS有身份验证、数据加密、防篡改；HTTP端口80，HTTPS端口443。',
 'HTTPS,SSL,TLS,证书,加密,公钥,私钥', 10.0, 0.5),
(1000078, '计算机网络', 4, '请解释什么是CDN及其工作原理',
 'CDN通过在全球部署边缘节点服务器，将内容缓存到离用户最近的节点，减少延迟。工作原理：1.用户请求域名，DNS解析返回最优CDN节点IP；2.用户请求CDN边缘节点；3.若节点有缓存则直接返回；4.若无缓存则回源到源服务器获取内容，缓存后返回；5.后续请求直接从边缘节点获取。优点：加速访问、减轻源服务器压力、防DDoS。',
 'CDN,边缘节点,缓存,DNS,回源', 10.0, 0.6),
(1000079, '计算机网络', 4, '请解释跨域问题及常见解决方案',
 '跨域是指浏览器的同源策略（协议、域名、端口全部相同才是同源）限制了不同源之间的资源访问。常见解决方案：1.CORS：服务器在响应头添加Access-Control-Allow-Origin等字段；2.JSONP：利用script标签不受同源限制，只支持GET；3.代理服务器：前端请求同源代理，代理转发到目标服务器；4.Nginx反向代理。推荐使用CORS方案，安全且灵活。',
 '跨域,同源策略,CORS,JSONP,代理', 10.0, 0.6),
(1000080, '计算机网络', 4, '请解释TCP的拥塞控制机制',
 'TCP拥塞控制防止发送方发送过多数据导致网络拥塞。主要机制：1.慢启动：初始拥塞窗口cwnd=1，每次ACK确认cwnd翻倍，直到达到慢启动阈值ssthresh；2.拥塞避免：超过ssthresh后cwnd每轮次+1线性增长；3.快速重传：连续收到3个重复ACK时立即重传，不等超时；4.快速恢复：快速重传后，ssthresh=cwnd/2，cwnd=ssthresh进入拥塞避免。超时时cwnd重置为1重新慢启动。',
 '拥塞控制,慢启动,拥塞避免,快速重传,快速恢复,cwnd', 15.0, 0.8),

-- ==================== 高等数学 - 单选题 (6条) ====================
(1000081, '高等数学', 1,
 '{"title":"函数y=sin(x)的导数是？","options":[{"value":"A","label":"cos(x)"},{"value":"B","label":"-cos(x)"},{"value":"C","label":"tan(x)"},{"value":"D","label":"-sin(x)"}]}',
 'A', NULL, 5.0, 0.2),
(1000082, '高等数学', 1,
 '{"title":"∫x dx 的不定积分是？","options":[{"value":"A","label":"x + C"},{"value":"B","label":"x^2 + C"},{"value":"C","label":"x^2/2 + C"},{"value":"D","label":"2x + C"}]}',
 'C', NULL, 5.0, 0.2),
(1000083, '高等数学', 1,
 '{"title":"lim(x→0) sin(x)/x 的极限值是？","options":[{"value":"A","label":"0"},{"value":"B","label":"1"},{"value":"C","label":"无穷大"},{"value":"D","label":"不存在"}]}',
 'B', NULL, 5.0, 0.3),
(1000084, '高等数学', 1,
 '{"title":"以下哪个函数在x=0处不可导？","options":[{"value":"A","label":"y=x^2"},{"value":"B","label":"y=|x|"},{"value":"C","label":"y=sin(x)"},{"value":"D","label":"y=e^x"}]}',
 'B', NULL, 5.0, 0.4),
(1000085, '高等数学', 1,
 '{"title":"函数y=ln(x)的导数是？","options":[{"value":"A","label":"1/x"},{"value":"B","label":"x"},{"value":"C","label":"e^x"},{"value":"D","label":"1/ln(x)"}]}',
 'A', NULL, 5.0, 0.2),
(1000086, '高等数学', 1,
 '{"title":"等差数列首项a1=1，公差d=2，前10项之和S10=？","options":[{"value":"A","label":"90"},{"value":"B","label":"100"},{"value":"C","label":"110"},{"value":"D","label":"120"}]}',
 'B', NULL, 5.0, 0.3),

-- ==================== 高等数学 - 多选题 (4条) ====================
(1000087, '高等数学', 2,
 '{"title":"关于连续函数，以下正确的是？","options":[{"value":"A","label":"连续函数一定可导"},{"value":"B","label":"可导函数一定连续"},{"value":"C","label":"闭区间上连续函数一定有最大值和最小值"},{"value":"D","label":"介值定理对连续函数成立"}]}',
 'BCD', NULL, 8.0, 0.6),
(1000088, '高等数学', 2,
 '{"title":"以下哪些积分公式是正确的？","options":[{"value":"A","label":"∫e^x dx = e^x + C"},{"value":"B","label":"∫1/x dx = ln|x| + C"},{"value":"C","label":"∫cos(x) dx = sin(x) + C"},{"value":"D","label":"∫sin(x) dx = cos(x) + C"}]}',
 'ABC', NULL, 8.0, 0.4),
(1000089, '高等数学', 2,
 '{"title":"关于泰勒展开，以下说法正确的是？","options":[{"value":"A","label":"e^x在x=0处展开为1+x+x^2/2!+..."},{"value":"B","label":"sin(x)在x=0处展开为x-x^3/3!+x^5/5!-..."},{"value":"C","label":"cos(x)在x=0处展开为1-x^2/2!+x^4/4!-..."},{"value":"D","label":"ln(1+x)在x=0处展开为x-x^2/2+x^3/3-..."}]}',
 'ABCD', NULL, 8.0, 0.7),
(1000090, '高等数学', 2,
 '{"title":"以下哪些是无穷小量的性质？","options":[{"value":"A","label":"有限个无穷小的和仍为无穷小"},{"value":"B","label":"无穷小与有界函数的乘积是无穷小"},{"value":"C","label":"无穷小的倒数是无穷大"},{"value":"D","label":"两个无穷小的商一定是无穷小"}]}',
 'ABC', NULL, 8.0, 0.6),

-- ==================== 高等数学 - 判断题 (6条) ====================
(1000091, '高等数学', 3, '函数在某点可导，则在该点一定连续', '正确', NULL, 5.0, 0.3),
(1000092, '高等数学', 3, '函数在某点连续，则在该点一定可导', '错误', NULL, 5.0, 0.3),
(1000093, '高等数学', 3, '若f(x)在[a,b]上连续且f(a)*f(b)<0，则在(a,b)内至少存在一个零点', '正确', NULL, 5.0, 0.4),
(1000094, '高等数学', 3, '两个发散级数的和一定是发散的', '错误', NULL, 5.0, 0.5),
(1000095, '高等数学', 3, '洛必达法则只能用于求0/0型或无穷/无穷型的极限', '正确', NULL, 5.0, 0.5),
(1000096, '高等数学', 3, '偏导数存在的二元函数一定是连续的', '错误', NULL, 5.0, 0.6),

-- ==================== 高等数学 - 主观题 (4条) ====================
(1000097, '高等数学', 4, '请解释函数极限的概念，并说明左极限、右极限与极限存在的关系',
 '函数极限：当x趋近于x0时，若f(x)无限趋近于某确定值A，则A为f(x)在x0处的极限。形式化定义（ε-δ语言）：对任意ε>0，存在δ>0，使得0<|x-x0|<δ时|f(x)-A|<ε。极限存在的充要条件：左极限=右极限=A。若左右极限不相等则极限不存在。',
 '极限,左极限,右极限,充要条件,ε-δ', 10.0, 0.6),
(1000098, '高等数学', 4, '请陈述微积分基本定理（牛顿-莱布尼茨公式）并说明其意义',
 '微积分基本定理：若f(x)在[a,b]上连续，F(x)是f(x)的任意原函数，则定积分∫[a,b]f(x)dx=F(b)-F(a)。意义：将求定积分转化为求原函数，大大简化了积分的计算；揭示了微分（求导）与积分互为逆运算的关系，是微积分的核心定理。',
 '牛顿莱布尼茨,原函数,定积分,微积分基本定理', 15.0, 0.8),
(1000099, '高等数学', 4, '请解释函数极值和最值的概念，以及求函数极值的方法',
 '极值：函数在某点附近的局部最大或最小值。极大值：f(x0)在某邻域内最大；极小值反之。最值：函数在整个区间上的最大/最小值（全局）。求极值方法：1.令f(x)=0，求驻点；2.二阶导判别：f(x0)=0且f(x0)>0为极小值，<0为极大值；3.一阶导变号法。闭区间上求最值需比较所有极值和端点值。',
 '极值,最值,驻点,二阶导数,极大值,极小值', 10.0, 0.6),
(1000100, '高等数学', 4, '请解释级数收敛的概念，以及常见的判断级数收敛的方法',
 '级数∑an收敛是指部分和Sn随n趋于无穷时趋近于某有限值S。判断方法：1.比值审敛法：计算|a(n+1)/an|的极限ρ，ρ<1收敛，ρ>1发散；2.根值审敛法：计算|an|^(1/n)的极限ρ，同上判断；3.比较审敛法：与已知级数比较；4.交错级数莱布尼茨法：若|an|单调递减趋于0则收敛；5.p级数：p>1收敛，p≤1发散。',
 '级数,收敛,比值审敛,根值审敛,p级数', 10.0, 0.7);;
