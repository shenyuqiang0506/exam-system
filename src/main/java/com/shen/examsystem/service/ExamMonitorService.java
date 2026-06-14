package com.shen.examsystem.service;

import com.shen.examsystem.dto.WebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 考试监控服务
 * 管理在线学生状态和消息推送
 */
@Service
public class ExamMonitorService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 在线学生状态存储
     * Key: paperId_studentId
     * Value: 学生状态信息
     */
    private final ConcurrentHashMap<String, StudentStatus> onlineStudents = new ConcurrentHashMap<>();

    /**
     * 学生状态内部类
     */
    public static class StudentStatus {
        private Long studentId;
        private String studentName;
        private Long paperId;
        private int screenSwitchCount = 0;
        private int answeredCount = 0;
        private int totalQuestions = 0;
        private long lastHeartbeat;

        public StudentStatus(Long studentId, String studentName, Long paperId) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.paperId = paperId;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        // Getters and Setters
        public Long getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public Long getPaperId() { return paperId; }
        public int getScreenSwitchCount() { return screenSwitchCount; }
        public void setScreenSwitchCount(int count) { this.screenSwitchCount = count; }
        public int getAnsweredCount() { return answeredCount; }
        public void setAnsweredCount(int count) { this.answeredCount = count; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int count) { this.totalQuestions = count; }
        public long getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(long time) { this.lastHeartbeat = time; }
    }

    /**
     * 学生上线
     */
    public void studentOnline(Long studentId, String studentName, Long paperId, int answeredCount, int totalQuestions) {
        String key = paperId + "_" + studentId;
        StudentStatus status = new StudentStatus(studentId, studentName, paperId);
        status.setAnsweredCount(answeredCount);
        status.setTotalQuestions(totalQuestions);
        onlineStudents.put(key, status);

        // 通知教师端
        WebSocketMessage msg = new WebSocketMessage(WebSocketMessage.MessageType.STUDENT_ONLINE);
        msg.setStudentId(studentId);
        msg.setStudentName(studentName);
        msg.setPaperId(paperId);
        msg.setAnsweredCount(answeredCount);
        msg.setTotalQuestions(totalQuestions);
        broadcastToTeacher(paperId, msg);
    }

    /**
     * 学生离线
     */
    public void studentOffline(Long studentId, Long paperId) {
        String key = paperId + "_" + studentId;
        onlineStudents.remove(key);

        // 通知教师端
        WebSocketMessage msg = new WebSocketMessage(WebSocketMessage.MessageType.STUDENT_OFFLINE);
        msg.setStudentId(studentId);
        msg.setPaperId(paperId);
        broadcastToTeacher(paperId, msg);
    }

    /**
     * 更新学生答题进度
     */
    public void updateProgress(Long studentId, Long paperId, int answeredCount, int totalQuestions) {
        String key = paperId + "_" + studentId;
        StudentStatus status = onlineStudents.get(key);
        if (status != null) {
            status.setAnsweredCount(answeredCount);
            status.setTotalQuestions(totalQuestions);
            status.setLastHeartbeat(System.currentTimeMillis());
        }

        // 通知教师端
        WebSocketMessage msg = new WebSocketMessage(WebSocketMessage.MessageType.ANSWER_SAVE);
        msg.setStudentId(studentId);
        msg.setPaperId(paperId);
        msg.setAnsweredCount(answeredCount);
        msg.setTotalQuestions(totalQuestions);
        broadcastToTeacher(paperId, msg);
    }

    /**
     * 学生切屏上报
     */
    public void reportScreenSwitch(Long studentId, Long paperId) {
        String key = paperId + "_" + studentId;
        StudentStatus status = onlineStudents.get(key);
        if (status != null) {
            status.setScreenSwitchCount(status.getScreenSwitchCount() + 1);
            status.setLastHeartbeat(System.currentTimeMillis());

            // 通知教师端
            WebSocketMessage msg = new WebSocketMessage(WebSocketMessage.MessageType.SCREEN_SWITCH);
            msg.setStudentId(studentId);
            msg.setStudentName(status.getStudentName());
            msg.setPaperId(paperId);
            msg.setScreenSwitchCount(status.getScreenSwitchCount());
            broadcastToTeacher(paperId, msg);
        }
    }

    /**
     * 学生心跳
     */
    public void heartbeat(Long studentId, Long paperId) {
        String key = paperId + "_" + studentId;
        StudentStatus status = onlineStudents.get(key);
        if (status != null) {
            status.setLastHeartbeat(System.currentTimeMillis());
        }
    }

    /**
     * 向教师端推送消息
     */
    public void broadcastToTeacher(Long paperId, WebSocketMessage message) {
        messagingTemplate.convertAndSend("/topic/monitor/" + paperId, message);
    }

    /**
     * 向学生端推送考试结束消息
     */
    public void broadcastExamEnd(Long paperId) {
        WebSocketMessage msg = new WebSocketMessage(WebSocketMessage.MessageType.EXAM_END);
        msg.setPaperId(paperId);
        messagingTemplate.convertAndSend("/topic/exam/" + paperId, msg);
    }

    /**
     * 获取某试卷的在线学生列表
     */
    public List<StudentStatus> getOnlineStudents(Long paperId) {
        List<StudentStatus> result = new ArrayList<>();
        for (StudentStatus status : onlineStudents.values()) {
            if (status.getPaperId().equals(paperId)) {
                result.add(status);
            }
        }
        return result;
    }

    /**
     * 获取在线学生数量
     */
    public int getOnlineCount(Long paperId) {
        return (int) onlineStudents.values().stream()
                .filter(s -> s.getPaperId().equals(paperId))
                .count();
    }

    /**
     * 检查学生是否在线
     */
    public boolean isStudentOnline(Long studentId, Long paperId) {
        String key = paperId + "_" + studentId;
        return onlineStudents.containsKey(key);
    }
}
