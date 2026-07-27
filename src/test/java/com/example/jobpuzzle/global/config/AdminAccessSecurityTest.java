package com.example.jobpuzzle.global.config;

import com.example.jobpuzzle.admin.controller.AdminController;
import com.example.jobpuzzle.admin.controller.AdminViewController;
import com.example.jobpuzzle.global.security.CustomAccessDeniedHandler;
import com.example.jobpuzzle.global.security.CustomOAuth2UserService;
import com.example.jobpuzzle.global.security.CustomUserDetails;
import com.example.jobpuzzle.global.security.CustomUserDetailsService;
import com.example.jobpuzzle.global.security.OAuth2LoginSuccessHandler;
import com.example.jobpuzzle.user.entity.User;
import com.example.jobpuzzle.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// /admin/**, /api/admin/**가 SecurityConfig의 hasRole("ADMIN") 매처와 CustomAccessDeniedHandler로
// 실제로 보호되는지 검증한다. (로그인 필요 -> 로그인 페이지 리다이렉트, 권한 없음 -> 403/메인 리다이렉트, 관리자 -> 통과)
@WebMvcTest(controllers = {AdminViewController.class, AdminController.class})
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class})
class AdminAccessSecurityTest {

    @Autowired private MockMvc mockMvc;

    // SecurityConfig 생성자가 필요로 하지만 이 테스트 범위와 무관한 의존성들
    @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private PersistentTokenRepository persistentTokenRepository;

    @Test
    void adminPageRedirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void adminPageRedirectsRegularUserToMain() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(UserRole.USER)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void adminPageIsRenderedForAdmin() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("회원 관리")));
    }

    @Test
    void adminApiRedirectsAnonymousUserToLogin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void adminApiReturnsJsonForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_004"));
    }

    @Test
    void adminApiPassesAuthorizationForAdmin() throws Exception {
        // AdminController에 아직 실제 엔드포인트가 없어서 인가를 통과하면 404가 나는 것으로 통과 여부를 확인한다.
        mockMvc.perform(get("/api/admin/users").with(user(UserRole.ADMIN)))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor user(UserRole role) {
        User user = User.createLocalUser("admin-test", "encoded", "admin-test@test.local", "관리자테스트", null);
        ReflectionTestUtils.setField(user, "role", role);
        return SecurityMockMvcRequestPostProcessors.user(new CustomUserDetails(user));
    }
}