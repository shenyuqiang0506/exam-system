# PTA 风格题目集功能规划

## 需求说明
类似 PTA 程序设计平台，学生端分为：
- **活跃题目集**：正在进行中、可作答的考试
- **所有题目集**：包含未开始、进行中、已结束的所有考试

---

## 一、页面结构设计

### 学生端路由

```
/student
├── /exams              # 考试中心（默认页面）
│   ├── active          # 活跃题目集（Tab）
│   └── all             # 所有题目集（Tab）
├── /exam/:id           # 考试页面
├── /records            # 我的成绩
└── /result/:id         # 成绩详情
```

### 考试中心页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  考试中心                                                     │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐                           │
│  │ 活跃题目集   │  │ 所有题目集   │   ← Tab 切换              │
│  └─────────────┘  └─────────────┘                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 📝 数据结构期末考试                                   │    │
│  │ 科目：数据结构                                        │    │
│  │ 时间：2024-03-20 09:00 ~ 2024-03-20 11:00            │    │
│  │ 状态：🟢 进行中  剩余：01:23:45                       │    │
│  │ 进度：3/10 题  得分：--                               │    │
│  │ ┌─────────────┐                                       │    │
│  │ │  开始考试    │  ← 按钮（根据状态变化）                │    │
│  │ └─────────────┘                                       │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 📝 高等数学期中测试                                   │    │
│  │ 科目：高等数学                                        │    │
│  │ 时间：2024-03-25 14:00 ~ 2024-03-25 16:00            │    │
│  │ 状态：🔵 未开始  开始倒计时：5天                       │    │
│  │ 进度：0/8 题  得分：--                                │    │
│  │ ┌─────────────┐                                       │    │
│  │ │  未开始     │  ← 按钮禁用                           │    │
│  │ └─────────────┘                                       │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、状态定义

### 试卷状态

| 状态 | 条件 | 标签颜色 | 图标 | 按钮状态 |
|------|------|----------|------|----------|
| 未开始 | `NOW < start_time` | 蓝色 `#409eff` | ⏰ | 禁用 |
| 进行中 | `start_time <= NOW <= end_time` | 绿色 `#67c23a` | 🟢 | 可点击 |
| 已结束 | `NOW > end_time` | 红色 `#f56c6c` | 🔴 | 禁用 |
| 已完成 | 学生已交卷 | 灰色 `#909399` | ✅ | 查看成绩 |

### 按钮状态

| 状态 | 按钮文字 | 按钮类型 | 点击行为 |
|------|----------|----------|----------|
| 未开始 | 未开始 | `info` (禁用) | - |
| 进行中 + 未考 | 开始考试 | `primary` | 进入考试 |
| 进行中 + 已开始 | 继续考试 | `warning` | 继续答题 |
| 已结束 + 未考 | 已结束 | `info` (禁用) | - |
| 已结束 + 已考 | 查看成绩 | `success` | 跳转成绩页 |
| 已完成 | 已完成 | `success` (禁用) | - |

---

## 三、API 接口设计

### 现有接口修改

| 接口 | 修改说明 |
|------|----------|
| `GET /api/paper/list-active` | 只返回进行中的试卷 |
| `GET /api/paper/detail/{id}` | 增加时间字段返回 |

### 新增接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /api/paper/active` | GET | 学生获取活跃题目集（进行中） |
| `GET /api/paper/all` | GET | 学生获取所有题目集 |
| `GET /api/paper/{id}/status` | GET | 获取单个试卷状态（含倒计时） |

### 接口返回数据结构

```json
// GET /api/paper/active
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "title": "数据结构期末考试",
      "subjectName": "数据结构",
      "totalScore": 100,
      "startTime": "2024-03-20 09:00:00",
      "endTime": "2024-03-20 11:00:00",
      "status": "进行中",
      "remainingSeconds": 5025,
      "questionCount": 10,
      "answeredCount": 3,
      "myScore": null
    }
  ]
}

// GET /api/paper/all
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "title": "数据结构期末考试",
      "subjectName": "数据结构",
      "totalScore": 100,
      "startTime": "2024-03-20 09:00:00",
      "endTime": "2024-03-20 11:00:00",
      "status": "已结束",
      "remainingSeconds": 0,
      "questionCount": 10,
      "answeredCount": 10,
      "myScore": 85.5
    },
    {
      "id": 1002,
      "title": "高等数学期中测试",
      "subjectName": "高等数学",
      "totalScore": 100,
      "startTime": "2024-03-25 14:00:00",
      "endTime": "2024-03-25 16:00:00",
      "status": "未开始",
      "remainingSeconds": 432000,
      "questionCount": 8,
      "answeredCount": 0,
      "myScore": null
    }
  ]
}
```

---

## 四、前端组件设计

### 1. ExamCenter.vue（考试中心 - 新建）

```vue
<template>
  <div class="exam-center">
    <!-- 顶部统计 -->
    <div class="stats-bar">
      <el-statistic title="活跃题目集" :value="activeCount" />
      <el-statistic title="待完成" :value="pendingCount" />
      <el-statistic title="已完成" :value="completedCount" />
    </div>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="活跃题目集" name="active">
        <ExamList :papers="activePapers" type="active" />
      </el-tab-pane>
      <el-tab-pane label="所有题目集" name="all">
        <ExamList :papers="allPapers" type="all" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
```

### 2. ExamList.vue（题目集列表 - 新建）

```vue
<template>
  <div class="exam-list">
    <div v-for="paper in papers" :key="paper.id" class="exam-card">
      <div class="exam-card-left">
        <div class="exam-icon" :class="getStatusClass(paper.status)">
          {{ getStatusIcon(paper.status) }}
        </div>
      </div>
      
      <div class="exam-card-body">
        <h3 class="exam-title">{{ paper.title }}</h3>
        <div class="exam-meta">
          <span><el-icon><Collection /></el-icon> {{ paper.subjectName }}</span>
          <span><el-icon><Timer /></el-icon> {{ paper.questionCount }} 题</span>
          <span><el-icon><Trophy /></el-icon> {{ paper.totalScore }} 分</span>
        </div>
        <div class="exam-time">
          <el-icon><Clock /></el-icon>
          {{ paper.startTime }} ~ {{ paper.endTime }}
        </div>
        <div class="exam-status">
          <el-tag :type="getStatusTagType(paper.status)" size="small">
            {{ paper.status }}
          </el-tag>
          <span v-if="paper.status === '进行中'" class="countdown">
            剩余：{{ formatCountdown(paper.remainingSeconds) }}
          </span>
          <span v-if="paper.status === '未开始'" class="countdown">
            {{ formatStartCountdown(paper.remainingSeconds) }} 后开始
          </span>
        </div>
      </div>

      <div class="exam-card-right">
        <!-- 进度 -->
        <div class="exam-progress">
          <el-progress 
            :percentage="getProgress(paper)" 
            :status="getProgressStatus(paper)"
          />
          <span class="progress-text">{{ paper.answeredCount }}/{{ paper.questionCount }}</span>
        </div>
        
        <!-- 得分 -->
        <div v-if="paper.myScore !== null" class="exam-score">
          {{ paper.myScore }} 分
        </div>

        <!-- 操作按钮 -->
        <el-button 
          :type="getButtonType(paper)"
          :disabled="isButtonDisabled(paper)"
          @click="handleAction(paper)"
        >
          {{ getButtonText(paper) }}
        </el-button>
      </div>
    </div>

    <el-empty v-if="papers.length === 0" description="暂无题目集" />
  </div>
</template>
```

### 3. ExamCard.vue（单个题目集卡片 - 可选）

独立的卡片组件，方便复用。

---

## 五、后端接口修改

### ExamPaperController.java

```java
/**
 * 学生获取活跃题目集（进行中）
 */
@GetMapping("/active")
public Result<List<Map<String, Object>>> getActivePapers(HttpServletRequest request) {
    Long studentId = (Long) request.getAttribute("userId");
    List<Map<String, Object>> papers = examPaperService.getActivePapersForStudent(studentId);
    return Result.success(papers);
}

/**
 * 学生获取所有题目集
 */
@GetMapping("/all")
public Result<List<Map<String, Object>>> getAllPapers(HttpServletRequest request) {
    Long studentId = (Long) request.getAttribute("userId");
    List<Map<String, Object>> papers = examPaperService.getAllPapersForStudent(studentId);
    return Result.success(papers);
}
```

### ExamPaperService.java

```java
// 获取学生可考的活跃题目集
List<Map<String, Object>> getActivePapersForStudent(Long studentId);

// 获取学生所有题目集
List<Map<String, Object>> getAllPapersForStudent(Long studentId);

// 获取试卷状态
String getPaperStatus(ExamPaper paper);

// 获取剩余时间（秒）
long getRemainingSeconds(ExamPaper paper);
```

### ExamPaperServiceImpl.java

```java
@Override
public List<Map<String, Object>> getActivePapersForStudent(Long studentId) {
    LocalDateTime now = LocalDateTime.now();
    
    // 查询进行中的试卷
    LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ExamPaper::getIsArchived, 0)
           .le(ExamPaper::getStartTime, now)
           .ge(ExamPaper::getEndTime, now);
    
    List<ExamPaper> papers = this.list(wrapper);
    return buildPaperListWithStatus(papers, studentId);
}

@Override
public List<Map<String, Object>> getAllPapersForStudent(Long studentId) {
    // 查询所有未归档的试卷
    LambdaQueryWrapper<ExamPaper> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ExamPaper::getIsArchived, 0)
           .orderByDesc(ExamPaper::getCreateTime);
    
    List<ExamPaper> papers = this.list(wrapper);
    return buildPaperListWithStatus(papers, studentId);
}

private List<Map<String, Object>> buildPaperListWithStatus(List<ExamPaper> papers, Long studentId) {
    List<Map<String, Object>> result = new ArrayList<>();
    LocalDateTime now = LocalDateTime.now();
    
    for (ExamPaper paper : papers) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", paper.getId());
        map.put("title", paper.getTitle());
        map.put("subjectName", paper.getSubjectName());
        map.put("totalScore", paper.getTotalScore());
        map.put("startTime", paper.getStartTime());
        map.put("endTime", paper.getEndTime());
        
        // 计算状态
        String status = getPaperStatus(paper);
        map.put("status", status);
        
        // 计算剩余时间
        long remainingSeconds = getRemainingSeconds(paper);
        map.put("remainingSeconds", remainingSeconds);
        
        // 查询题目数量
        int questionCount = getQuestionCount(paper.getId());
        map.put("questionCount", questionCount);
        
        // 查询学生答题情况
        ExamRecord record = examRecordService.getStudentRecord(studentId, paper.getId());
        if (record != null) {
            map.put("answeredCount", getAnsweredCount(record.getId()));
            map.put("myScore", record.getTotalScore());
        } else {
            map.put("answeredCount", 0);
            map.put("myScore", null);
        }
        
        result.add(map);
    }
    
    return result;
}
```

---

## 六、文件清单

### 前端（新建）

| 文件 | 说明 |
|------|------|
| `views/student/ExamCenter.vue` | 考试中心（主页面） |
| `components/ExamCard.vue` | 题目集卡片组件（可选） |

### 前端（修改）

| 文件 | 修改内容 |
|------|----------|
| `router/index.js` | 添加考试中心路由 |
| `views/student/StudentLayout.vue` | 侧边栏菜单更新 |
| `views/student/StudentDashboard.vue` | 替换为 ExamCenter |
| `views/student/ExamList.vue` | 可能删除或重构 |

### 后端（修改）

| 文件 | 修改内容 |
|------|----------|
| `ExamPaperService.java` | 新增接口方法 |
| `ExamPaperServiceImpl.java` | 实现业务逻辑 |
| `ExamPaperController.java` | 新增接口 |

---

## 七、实现步骤

### 第一步：后端接口
1. 修改 ExamPaperService 接口
2. 实现 ExamPaperServiceImpl
3. 修改 ExamPaperController

### 第二步：前端页面
1. 创建 ExamCenter.vue
2. 创建 ExamCard.vue（可选）
3. 修改路由配置
4. 修改侧边栏菜单

### 第三步：测试验证
1. 测试活跃题目集显示
2. 测试所有题目集显示
3. 测试状态切换
4. 测试按钮状态

---

## 八、预计工时

| 任务 | 预计时间 |
|------|----------|
| 后端接口修改 | 45分钟 |
| 前端 ExamCenter.vue | 40分钟 |
| 前端 ExamCard.vue | 20分钟 |
| 路由和菜单修改 | 15分钟 |
| 测试验证 | 20分钟 |
| **总计** | **约 2.5 小时** |