package com.example.jobpuzzle.user.controller;

import com.example.jobpuzzle.global.common.ApiResponse;
import com.example.jobpuzzle.user.dto.JoinRequest;
import com.example.jobpuzzle.user.dto.LoginRequest;
import com.example.jobpuzzle.user.dto.MyInfoUpdateRequest;
import com.example.jobpuzzle.user.dto.UserInfoResponse;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ResponseEntity<ApiResponse<Void>> join(@Valid @RequestBody JoinRequest request) {
        userService.join(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 아이디 중복 확인 (회원가입 화면에서 아이디 입력할 때 실시간으로 호출)
    // GET /api/user/check-id?loginId=xxx -> true(중복) / false(사용 가능)
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Boolean>> checkIdDuplicate(@RequestParam String loginId) {
        return ResponseEntity.ok(ApiResponse.success(userService.checkIdDuplicate(loginId)));
    }

    // 이메일 중복 확인
    // GET /api/user/check-email?email=xxx -> true(중복) / false(사용 가능)
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailDuplicate(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success(userService.checkEmailDuplicate(email)));
    }

    // 아이디/비밀번호 로그인
    // POST /api/user/login { loginId, password, autoLogin }
    // 성공하면 세션에 로그인 상태가 저장되고, 이후 요청부터는 로그인된 상태로 인식됨
    // autoLogin이 true면 자동로그인 쿠키도 함께 발급됨
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        userService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 로그아웃
    // POST /api/user/logout - 세션 + 자동로그인 쿠키/토큰을 모두 정리해서 로그인 상태를 해제함
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        userService.logout(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 내 정보 조회 - 로그인된 사용자만 호출 가능 (프론트에서 로그인 상태 판단할 때도 사용)
    // GET /api/user/me
    // expression="user"로 받는 이유: 아이디/비번 로그인은 CustomUserDetails, 카카오/구글 로그인은 CustomOAuth2User가
    // principal로 들어오는데, 두 타입 다 getUser()를 갖고 있어서 SpEL로 공통 추출함
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMyInfo(user)));
    }

    // 내 정보 수정 - 이름/이메일/기본 관심 직무
    // PUT /api/user/me { name, email, defaultJobCategoryId }
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(@Valid @RequestBody MyInfoUpdateRequest request,
                                                            @AuthenticationPrincipal(expression = "user") User user) {
        userService.updateMyInfo(request, user);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 회원 탈퇴
    // DELETE /api/user/me - 상태를 WITHDRAWN으로 바꾸고 로그인 상태도 함께 해제
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal(expression = "user") User user,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse httpResponse) {
        userService.withdraw(user, httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
