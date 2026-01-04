package com.smartcheck.auth.service;

import com.smartcheck.auth.entity.AuthAuditLog;
import com.smartcheck.auth.repository.AuthAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthAuditService {

    private final AuthAuditLogRepository auditLogRepository;

    @Async("authTaskExecutor")
    public CompletableFuture<Void> logAsync(
            String username,
            String action){

        AuthAuditLog log = new AuthAuditLog();
        log.setUsername(username);
        log.setAction(action);
        log.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(log);

        return CompletableFuture.completedFuture(null);
    }
}
