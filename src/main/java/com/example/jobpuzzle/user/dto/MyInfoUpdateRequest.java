package com.example.jobpuzzle.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 내 정보 수정 요청 - 이름/이메일은 부분 수정이 아니라 항상 전체를 다시 받아서 덮어씀 (UserService.updateMyInfo 참고)
@Getter
@NoArgsConstructor
public class MyInfoUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이내로 입력해주세요.")
    private String name;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이내로 입력해주세요.")
    private String email;

    // 소셜 로그인 회원은 보내더라도 무시됨 (UserService.updateMyInfo 참고)
    @Size(min = 4, max = 50, message = "아이디는 4~50자로 입력해주세요.")
    private String loginId;

    private Long defaultJobCategoryId;
}
