package com.example.jobpuzzle.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String loginId;

    private String password;

    private String email;

    private String name;

    private String role;

    private Long defaultJobCategoryId;

    private String socialProvider;

    private String socialId;

    private Integer loginFailCount;

    private Boolean isLocked;

    private LocalDateTime lockedAt;

    private String status;

    private LocalDateTime withdrawnAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
