# 试卷时间管理功能规划

## 需求说明
- 教师端：设置试卷的开始时间和结束时间
- 学生端：显示试卷状态（未开始、进行中、已结束）
- 已结束试卷自动归档

---

## 一、数据库修改

### exam_paper 表新增字段

```sql
ALTER TABLE exam_paper ADD COLUMN start_time DATETIME DEFAULT NULL COMMENT '考试开始时间';
ALTER TABLE exam_paper ADD COLUMN end_time DATETIME DEFAULT NULL COMMENT '考试结束时间';
```

### 状态定义

| 状态 | 条件 | 显示 |
|------|------|------|
| 未开始 | `start_time > NOW()` | 蓝色标签 "未开始" |
| 进行中 | `start_time <= NOW() AND end_time >= NOW()` | 绿色标签 "进行中" |
| 已结束 | `end_time < NOW()` | 红色标签 "已结束" |

---

## 二、后端修改

### 1. 实体类修改

**ExamPaper.java**
```java
private LocalDateTime startTime;  // 开始时间
private LocalDateTime endTime;    // 结束时间
```

### 2. Mapper 修改

**ExamPaperMapper.java** - 新增方法
```java
// 查询进行中的试卷（学生端）
List<ExamPaper> selectActivePapers();

// 查询已结束的试卷（用于自动归档）
List<ExamPaper> selectExpiredPapers();
```

### 3. Service 修改

**ExamPaperService.java**
```java
// 获取学生可考的试卷（进行中）
List<ExamPaper> getStudentAvailablePapers(Long studentId);

// 获取所有试卷（教师端，含状态）
List<Map<String, Object>> getTeacherPapersWithStatus();

// 自动归档已结束的试卷
int autoArchiveExpiredPapers();
```

### 4. Controller 修改

**ExamPaperController.java**

```java
// 学生端：获取可考试卷（只返回进行中的）
GET /api/paper/available

// 教师端：获取所有试卷（带状态信息）
GET /api/paper/list-all

// 修改：创建试卷时包含时间
POST /api/paper/manual-create
POST /api/paper/auto-create
```

### 5. 定时任务

**PaperScheduler.java**（新建）
```java
@Component
public class PaperScheduler {
    
    // 每分钟检查一次，自动归档已结束的试卷
    @Scheduled(fixedRate = 60000)
    public void autoArchiveExpiredPapers() {
        // 查询 end_time < NOW() 且 is_archived = 0 的试卷
        // 将其 is_archived 更新为 1
    }
}
```

---

## 三、前端修改

### 1. 教师端 - PaperList.vue

```vue
<template>
  <el-table :data="paperList">
    <el-table-column prop="title" label="试卷名称" />
    <el-table-column prop="subjectName" label="科目" />
    <el-table-column prop="startTime" label="开始时间" />
    <el-table-column prop="endTime" label="结束时间" />
    <el-table-column label="状态">
      <template #default="{ row }">
        <el-tag :type="getStatusType(row)">
          {{ getStatusText(row) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作">
      <!-- 操作按钮 -->
    </el-table-column>
  </el-table>
</template>
```

### 2. 教师端 - 手动/智能组卷

添加时间选择器：
```vue
<el-form-item label="考试时间">
  <el-date-picker
    v-model="examTime"
    type="datetimerange"
    range-separator="至"
    start-placeholder="开始时间"
    end-placeholder="结束时间"
  />
</el-form-item>
```

### 3. 学生端 - StudentDashboard.vue

```vue
<template>
  <el-card v-for="paper in paperList" :key="paper.id">
    <!-- 试卷信息 -->
    <div class="paper-info">
      <p>开始时间：{{ paper.startTime }}</p>
      <p>结束时间：{{ paper.endTime }}</p>
      <el-tag :type="getStatusType(paper)">
        {{ getStatusText(paper) }}
      </el-tag>
    </div>
    
    <!-- 按钮根据状态显示 -->
    <el-button v-if="paper.status === '进行中'" @click="startExam(paper)">
      开始考试
    </el-button>
    <el-button v-else disabled>
      {{ paper.status === '未开始' ? '未开始' : '已结束' }}
    </el-button>
  </el-card>
</template>
```

---

## 四、API 接口变更

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/paper/available` | GET | **新增** - 学生获取可考试卷 |
| `/api/paper/list-all` | GET | **新增** - 教师获取所有试卷（带状态） |
| `/api/paper/manual-create` | POST | 修改 - 增加 startTime、endTime |
| `/api/paper/auto-create` | POST | 修改 - 增加 startTime、endTime |
| `/api/paper/detail/{id}` | GET | 修改 - 返回时间信息 |

---

## 五、实现步骤

### 第一步：数据库修改
```sql
ALTER TABLE exam_paper ADD COLUMN start_time DATETIME DEFAULT NULL;
ALTER TABLE exam_paper ADD COLUMN end_time DATETIME DEFAULT NULL;
```

### 第二步：后端实体类修改
- ExamPaper.java 添加字段

### 第三步：后端接口修改
- ExamPaperController.java
- ExamPaperService.java
- ExamPaperServiceImpl.java

### 第四步：定时任务
- PaperScheduler.java（新建）

### 第五步：前端修改
- PaperList.vue（教师端）
- StudentDashboard.vue（学生端）
- 手动组卷页面
- 智能组卷页面

---

## 六、状态判断逻辑

```javascript
// 前端状态判断
function getStatus(paper) {
  const now = new Date()
  const start = new Date(paper.startTime)
  const end = new Date(paper.endTime)
  
  if (now < start) return '未开始'
  if (now > end) return '已结束'
  return '进行中'
}

// 后端状态判断
public String getStatus(ExamPaper paper) {
    LocalDateTime now = LocalDateTime.now();
    if (paper.getStartTime() == null || paper.getEndTime() == null) {
        return "进行中"; // 兼容旧数据
    }
    if (now.isBefore(paper.getStartTime())) return "未开始";
    if (now.isAfter(paper.getEndTime())) return "已结束";
    return "进行中";
}
```

---

## 七、测试用例

| 场景 | 预期结果 |
|------|----------|
| 教师创建试卷（设置未来时间） | 学生端显示"未开始"，不能考试 |
| 教师创建试卷（当前时间内） | 学生端显示"进行中"，可以考试 |
| 教师创建试卷（过去时间） | 学生端显示"已结束"，不能考试 |
| 试卷结束时间到达 | 自动归档，学生端显示"已结束" |
| 教师手动归档 | 立即归档 |

---

## 八、预计工时

| 任务 | 预计时间 |
|------|----------|
| 数据库修改 | 5分钟 |
| 后端实体类 | 10分钟 |
| 后端接口修改 | 30分钟 |
| 定时任务 | 15分钟 |
| 前端教师端 | 30分钟 |
| 前端学生端 | 20分钟 |
| 测试验证 | 20分钟 |
| **总计** | **约 2 小时** |