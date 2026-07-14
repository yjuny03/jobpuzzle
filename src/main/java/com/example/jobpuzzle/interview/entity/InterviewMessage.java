package com.example.jobpuzzle.interview.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "interview_message")
public class InterviewMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    private Long questionId;

    private String sender;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String messageText;

    private LocalDateTime createdAt;

}
