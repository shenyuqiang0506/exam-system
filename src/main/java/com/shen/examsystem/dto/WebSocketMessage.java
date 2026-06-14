package com.shen.examsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * WebSocket 消息 DTO
 * 用于学生端和教师端之间的实时通信
 */
@Data
public class WebSocketMessage {

    /**
     * 消息类型
     */
    public enum MessageType {
        /** 答案自动保存 */
        ANSWER_SAVE,
        /** 切屏上报 */
        SCREEN_SWITCH,
        /** 考试交卷 */
        EXAM_SUBMIT,
        /** 心跳检测 */
        HEARTBEAT,
        /** 考试结束（系统强制） */
        EXAM_END,
        /** 学生上线 */
        STUDENT_ONLINE,
        /** 学生离线 */
        STUDENT_OFFLINE
    }

    /** 消息类型 */
    private MessageType type;

    /** 学生ID */
    private Long studentId;

    /** 学生姓名 */
    private String studentName;

    /** 试卷ID */
    private Long paperId;

    /** 消息数据 */
    private Object data;

    /** 切屏次数 */
    private Integer screenSwitchCount;

    /** 答题进度 (已答/总题数) */
    private Integer answeredCount;

    /** 总题数 */
    private Integer totalQuestions;

    /** 时间戳 */
    private LocalDateTime timestamp;

    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(MessageType type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
}
