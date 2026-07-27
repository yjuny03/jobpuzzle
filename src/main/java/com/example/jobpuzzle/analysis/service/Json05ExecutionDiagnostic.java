package com.example.jobpuzzle.analysis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 개발/ai-e2e에서만 JSON-05 경계의 stack trace를 남긴다. 입력·응답 데이터는 인자로 받지 않는다.
@Component
public class Json05ExecutionDiagnostic {
    private static final Logger log = LoggerFactory.getLogger(Json05ExecutionDiagnostic.class);
    private final boolean enabled;

    public Json05ExecutionDiagnostic(@Value("${app.ai.json05-diagnostic.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public void checkpoint(String phase) {
        if (enabled) log.info("JSON05_DIAGNOSTIC checkpoint={}", phase);
    }

    public void failure(String phase, Throwable throwable) {
        if (!enabled) return;
        Throwable root = rootCause(throwable);
        StackTraceElement frame = firstApplicationFrame(throwable);
        log.error("JSON05_DIAGNOSTIC phase={} exceptionClass={} rootCauseClass={} firstApplicationFrame={}",
                phase, safeClass(throwable), safeClass(root), frame == null ? "none" : frame.toString(), throwable);
    }

    private Throwable rootCause(Throwable value) { Throwable current = value; while (current != null && current.getCause() != null) current = current.getCause(); return current; }
    private StackTraceElement firstApplicationFrame(Throwable value) { for (Throwable current = value; current != null; current = current.getCause()) for (StackTraceElement frame : current.getStackTrace()) if (frame.getClassName().startsWith("com.example.jobpuzzle.")) return frame; return null; }
    private String safeClass(Throwable value) { return value == null ? "none" : value.getClass().getName(); }
}
