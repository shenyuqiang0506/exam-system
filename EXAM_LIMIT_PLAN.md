# 学生端考试限制功能规划

## 需求说明
学生对同一试卷只能参加一次考试，交卷后不允许再次进入。

---

## 一、数据库分析

### 现有表结构
```sql
-- exam_record 表已支持
student_id    -- 学生ID
paper_id      -- 试卷ID
status        -- 0:考试中, 1:已交卷
```

### 判断逻辑
```sql
-- 检查学生是否已参加过某试卷考试
SELECT COUNT(*) FROM exam_record 
WHERE student_id = ? AND paper_id = ? AND status = 1;
```

---

## 二、后端修改

### 1. ExamRecordService 新增方法

```java
// 检查学生是否已完成某试卷考试
boolean hasCompletedExam(Long studentId, Long paperId);

// 检查学生是否正在考试中（未交卷）
ExamRecord getOngoingExam(Long studentId, Long paperId);
```

### 2. ExamRecordServiceImpl 实现

```java
@Override
public boolean hasCompletedExam(Long studentId, Long paperId) {
    LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ExamRecord::getStudentId, studentId)
           .eq(ExamRecord::getPaperId, paperId)
           .eq(ExamRecord::getStatus, 1); // 已交卷
    return this.count(wrapper) > 0;
}

@Override
public ExamRecord getOngoingExam(Long studentId, Long paperId) {
    LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ExamRecord::getStudentId, studentId)
           .eq(ExamRecord::getPaperId, paperId)
           .eq(ExamRecord::getStatus, 0); // 考试中
    return this.getOne(wrapper);
}
```

### 3. ExamPaperController 修改

```java
// 获取试卷详情时检查是否已考试
@GetMapping("/detail/{id}")
public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
    Long studentId = (Long) request.getAttribute("userId");
    
    // 检查是否已完成考试
    if (examRecordService.hasCompletedExam(studentId, id)) {
        return Result.error(403, "您已完成该试卷考试，不可重复参加");
    }
    
    // 检查是否有正在进行的考试
    ExamRecord ongoing = examRecordService.getOngoingExam(studentId, id);
    if (ongoing != null) {
        // 返回已有记录，前端可选择继续考试
        return Result.success("继续上次考试", ongoing);
    }
    
    // 正常返回试卷详情
    ...
}
```

### 4. ExamRecordController 修改

```java
// 交卷接口增加重复交卷检查
@PostMapping("/submit")
public Result<Long> submit(@RequestBody SubmitDTO submitDTO, HttpServletRequest request) {
    Long studentId = (Long) request.getAttribute("userId");
    
    // 检查是否已交卷
    if (examRecordService.hasCompletedExam(studentId, submitDTO.getPaperId())) {
        return Result.error(403, "您已完成该试卷考试，不可重复交卷");
    }
    
    // 正常交卷逻辑
    ...
}
```

---

## 三、前端修改

### 1. StudentDashboard.vue (考试大厅)

```vue
<template>
  <el-card v-for="paper in paperList" :key="paper.id">
    <!-- 试卷信息 -->
    <div class="paper-info">...</div>
    
    <!-- 按钮状态判断 -->
    <el-button 
      v-if="paper.completed" 
      type="info" 
      disabled
    >
      已完成考试
    </el-button>
    <el-button 
      v-else-if="paper.ongoing" 
      type="warning" 
      @click="continueExam(paper)"
    >
      继续考试
    </el-button>
    <el-button 
      v-else 
      type="primary" 
      @click="startExam(paper)"
    >
      开始考试
    </el-button>
  </el-card>
</template>

<script setup>
// 获取试卷列表时，同时获取考试状态
const loadPapers = async () => {
  const res = await request.get('/api/paper/list-active')
  const papers = res.data
  
  // 获取每张试卷的考试状态
  for (let paper of papers) {
    try {
      const recordRes = await request.get(`/api/record/check/${paper.id}`)
      paper.completed = recordRes.data?.status === 1
      paper.ongoing = recordRes.data?.status === 0
      paper.recordId = recordRes.data?.id
    } catch {
      paper.completed = false
      paper.ongoing = false
    }
  }
  
  paperList.value = papers
}
</script>
```

### 2. TakeExam.vue (考试页面)

```vue
<script setup>
// 进入页面时检查是否可以考试
onMounted(async () => {
  try {
    const res = await request.get(`/api/paper/detail/${route.params.paperId}`)
    
    if (res.code === 403) {
      // 已完成考试，提示并返回
      ElMessage.warning(res.message)
      router.push('/student/exams')
      return
    }
    
    if (res.data?.status === 0) {
      // 有正在进行的考试，继续
      ElMessage.info('继续上次考试')
      recordId.value = res.data.id
    }
    
    // 正常加载试卷
    loadExamData()
  } catch (err) {
    ElMessage.error('无法进入考试')
    router.push('/student/exams')
  }
})
</script>
```

### 3. 新增接口：检查考试状态

```java
// ExamRecordController.java

@GetMapping("/check/{paperId}")
public Result<ExamRecord> checkExamStatus(@PathVariable Long paperId, HttpServletRequest request) {
    Long studentId = (Long) request.getAttribute("userId");
    
    // 查询该学生对该试卷的考试记录
    LambdaQueryWrapper<ExamRecord> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(ExamRecord::getStudentId, studentId)
           .eq(ExamRecord::getPaperId, paperId)
           .orderByDesc(ExamRecord::getCreateTime)
           .last("LIMIT 1");
    
    ExamRecord record = examRecordService.getOne(wrapper);
    return Result.success(record); // 可能为 null
}
```

---

## 四、API 接口变更

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/record/check/{paperId}` | GET | 新增：检查学生对某试卷的考试状态 |
| `/api/paper/detail/{id}` | GET | 修改：增加已考试检查 |
| `/api/record/submit` | POST | 修改：增加重复交卷检查 |

---

## 五、状态流转图

```
学生点击试卷
      │
      ▼
┌─────────────────┐
│ 检查考试记录     │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
 有记录     无记录
    │         │
    ▼         ▼
┌───────┐  ┌───────┐
│status │  │创建新  │
│判断   │  │记录    │
└───┬───┘  └───┬───┘
    │          │
 ┌──┴──┐       ▼
 ▼     ▼    开始考试
0     1
 │     │
 ▼     ▼
继续  提示已完成
考试  返回列表
```

---

## 六、测试用例

### 测试场景 1：首次考试
1. 学生登录，进入考试大厅
2. 点击"开始考试"
3. 完成答题，点击"交卷"
4. 预期：交卷成功，跳转成绩页

### 测试场景 2：重复考试
1. 学生登录，进入考试大厅
2. 找到已完成的试卷
3. 预期：按钮显示"已完成考试"，不可点击

### 测试场景 3：中途退出
1. 学生开始考试，未交卷关闭页面
2. 重新进入同一试卷
3. 预期：提示"继续上次考试"，恢复之前进度

### 测试场景 4：直接访问 URL
1. 学生已完成考试
2. 直接访问 `/student/exam/:paperId`
3. 预期：提示"已完成考试"，跳转回列表

---

## 七、实现优先级

| 优先级 | 任务 | 预计时间 |
|--------|------|----------|
| P0 | 后端：新增检查接口 | 30分钟 |
| P0 | 后端：修改交卷接口 | 20分钟 |
| P0 | 前端：考试大厅状态显示 | 40分钟 |
| P1 | 前端：考试页面检查 | 30分钟 |
| P1 | 前端：继续考试功能 | 30分钟 |
| P2 | 测试与修复 | 30分钟 |

**总计：约 3 小时**

---

## 八、注意事项

1. **并发问题**：同一学生可能同时打开多个标签页，需要在交卷时做幂等检查
2. **考试中断**：网络断开、浏览器崩溃等情况，应允许继续考试
3. **时间计算**：继续考试时，剩余时间应从原始开始时间计算
4. **数据一致性**：交卷失败时，不应标记为已完成