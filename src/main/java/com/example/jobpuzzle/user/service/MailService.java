package com.example.jobpuzzle.user.service;

import com.example.jobpuzzle.user.repository.EmailVerificationStore;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final EmailVerificationStore emailVerificationStore;

    public MailService(JavaMailSender mailSender, EmailVerificationStore emailVerificationStore) {
        this.mailSender = mailSender;
        this.emailVerificationStore = emailVerificationStore;
    }

    public void sendMail(String to) {
        int code = (int) ( Math.random()*999999 ) + 100000;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("jobpuzzle@naver.com");
        message.setSubject("[JobPuzzle] 이메일 인증 코드");
        message.setText("인증 코드는 [" + code + "]입니다.\n3분 내에 입력해주세요.");
        mailSender.send(message);
        emailVerificationStore.save(to, code);
    }
}