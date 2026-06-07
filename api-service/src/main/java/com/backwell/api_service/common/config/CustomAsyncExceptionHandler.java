package com.backwell.api_service.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Slf4j
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("Uncaught Async Exception in method {}", method.getName());
        log.error("Caused by: ", ex);

        for  (Object param : params) {
            log.debug("Param: {}", param);
        }
    }
}
