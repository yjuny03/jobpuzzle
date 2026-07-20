package com.example.jobpuzzle.user.repository;

import java.time.LocalDateTime;

public record VerificationCode(int code, LocalDateTime expireAt) {
}
