package com.shen.examsystem.controller;

import com.shen.examsystem.dto.WebSocketMessage;
import com.shen.examsystem.service.ExamMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @Autowired
    private ExamMonitorService examMonitorService;

    @MessageMapping("/exam/online/{paperId}")
    public void studentOnline(@DestinationVariable Long paperId, WebSocketMessage message) {
        examMonitorService.studentOnline(message.getStudentId(), message.getStudentName(), paperId,
                message.getAnsweredCount(), message.getTotalQuestions());
    }

    @MessageMapping("/exam/offline/{paperId}")
    public void studentOffline(@DestinationVariable Long paperId, WebSocketMessage message) {
        examMonitorService.studentOffline(message.getStudentId(), paperId);
    }

    @MessageMapping("/exam/answer/{paperId}")
    public void saveAnswer(@DestinationVariable Long paperId, WebSocketMessage message) {
        examMonitorService.updateProgress(
                message.getStudentId(), paperId,
                message.getAnsweredCount(), message.getTotalQuestions());
    }

    @MessageMapping("/exam/screen/{paperId}")
    public void screenSwitch(@DestinationVariable Long paperId, WebSocketMessage message) {
        examMonitorService.reportScreenSwitch(message.getStudentId(), paperId);
    }

    @MessageMapping("/exam/heartbeat/{paperId}")
    public void heartbeat(@DestinationVariable Long paperId, WebSocketMessage message) {
        examMonitorService.heartbeat(message.getStudentId(), paperId);
    }
}
