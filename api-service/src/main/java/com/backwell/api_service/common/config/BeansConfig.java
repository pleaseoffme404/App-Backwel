package com.backwell.api_service.common.config;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqids.Sqids;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;

@Configuration
@RequiredArgsConstructor
public class BeansConfig {

    @Bean
    public TimeBasedEpochGenerator uuidV7Generator() {
        return Generators.timeBasedEpochGenerator();
    }

    @Bean
    public Sqids sqids(
            @Value("${app.sqids.min-length}") int minLength
    ) {
        return Sqids.builder()
                .minLength(minLength)
                .build();
    }

    @Bean
    Supplier<OffsetDateTime> utcNowSupplier() {
        return () -> OffsetDateTime.now(ZoneOffset.UTC);
    }

}
