package com.backwell.auth_server.init;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class InitializerOrchestrator implements SmartInitializingSingleton {
    private final List<ApplicationInitializer> initializers;

    @Override
    public void afterSingletonsInstantiated() {
        log.info("Starting Initializer Orchestrator");
        for (ApplicationInitializer initializer : initializers) {
            log.info("Initializing Application Initializer: {}", initializer.getClass().getSimpleName());
            initializer.initialize();
        }
        log.info("Finished Initializer Orchestrator");
    }
}
