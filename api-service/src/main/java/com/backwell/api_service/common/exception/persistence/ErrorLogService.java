package com.backwell.api_service.common.exception.persistence;

import com.backwell.api_service.common.config.user.UserSessionProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {
    private final ErrorLogRepository errorLogRepository;
    private final UserSessionProvider sessionProvider;



    @Async("taskExecutor")
    @Transactional
    public void saveErrorLog(UUID traceId, HttpServletRequest request, Exception ex) {
        var logBuilder = ErrorLog.builder()
                .traceId(traceId)
                .exceptionName(ex.getClass().getName())
                .message(ex.getMessage())
                .stackTrace(getStackTraceAsString(ex))
                .requestUri(request.getRequestURI())
                .httpMethod(request.getMethod());

        sessionProvider.getCurrentUserSessionOptional().ifPresent(s-> logBuilder.userId(s.uuid()));
        var errorLog = logBuilder.build();

        try {
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Fatal Error Saving Erro Log {}", errorLog.getTraceId());
        }
    }

    private String getStackTraceAsString(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
