package com.example.jobpuzzle.user.controller;

import com.example.jobpuzzle.user.dto.JoinRequest;
import com.example.jobpuzzle.user.dto.LoginRequest;
import com.example.jobpuzzle.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 회원가입
    // POST /api/user/join { loginId, password, email, name }
    @PostMapping("/join")
    public ResponseEntity<Void> join(@RequestBody JoinRequest request) {
        userService.join(request);
        return ResponseEntity.ok().build();
    }

    // 아이디 중복 확인 (회원가입 화면에서 아이디 입력할 때 실시간으로 호출)
    // GET /api/user/check-id?loginId=xxx -> true(중복) / false(사용 가능)
    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkIdDuplicate(@RequestParam String loginId) {
        return ResponseEntity.ok(userService.checkIdDuplicate(loginId));
    }

    // 이메일 중복 확인
    // GET /api/user/check-email?email=xxx -> true(중복) / false(사용 가능)
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        return ResponseEntity.ok(userService.checkEmailDuplicate(email));
    }

    // 아이디/비밀번호 로그인
    // POST /api/user/login { loginId, password }
    // 성공하면 세션에 로그인 상태가 저장되고, 이후 요청부터는 로그인된 상태로 인식됨
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        userService.login(request, httpRequest);
        return ResponseEntity.ok().build();
    }
}
