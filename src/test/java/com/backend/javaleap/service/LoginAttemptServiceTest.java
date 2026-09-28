package com.backend.javaleap.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void locksAfterMaxFailuresAndUnlocksOnSuccess() {
        LoginAttemptService svc = new LoginAttemptService();
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS - 1; i++) {
            svc.loginFailed("Admin");
        }
        assertThat(svc.isLocked("admin")).isFalse();
        assertThat(svc.remainingAttempts("admin")).isEqualTo(1);

        svc.loginFailed("admin");
        assertThat(svc.isLocked("ADMIN")).isTrue();

        svc.loginSucceeded("admin");
        assertThat(svc.isLocked("admin")).isFalse();
    }
}
