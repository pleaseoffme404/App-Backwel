package com.backwell.api_service.common.exception.persistence;

import com.backwell.api_service.common.config.user.UserSessionProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorLogService {
    private final ErrorLogRepository errorLogRepository;
    private final UserSessionProvider sessionProvider;


    public void saveErrorLog(UUID traceId, HttpServletRequest request, Exception ex) {
        try {
            var logBuilder = ErrorLog.builder()
                    .traceId(traceId)
                    .exceptionName(ex.getClass().getName())
                    .message(ex.getMessage())
                    .stackTrace(getStackTraceAsString(ex))
                    .requestUri(request.getRequestURI())
                    .httpMethod(request.getMethod());

            sessionProvider.getCurrentUserSessionOptional().ifPresent(s-> logBuilder.userId(s.uuid()));

            var errorLog = logBuilder.build();
            errorLogRepository.save(errorLog);
        } catch (Exception e) {
            log.error("Fatal Error Saving Error Log with trace Id: `{}`", traceId);
        }
    }

    private String getStackTraceAsString(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
