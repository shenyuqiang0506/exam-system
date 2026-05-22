package com.shen.examsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.shen.examsystem.mapper")
@EnableScheduling
public class ExamSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamSystemApplication.class, args);
    }

}
