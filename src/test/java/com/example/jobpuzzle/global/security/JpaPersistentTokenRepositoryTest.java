package com.example.jobpuzzle.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class JpaPersistentTokenRepositoryTest {

    @Test
    void removeUserTokensRunsInsideTransaction() throws NoSuchMethodException {
        Method method = JpaPersistentTokenRepository.class
                .getMethod("removeUserTokens", String.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
