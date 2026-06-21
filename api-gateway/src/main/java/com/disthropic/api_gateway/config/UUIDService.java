package com.disthropic.api_gateway.config;

import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UUIDService {
    private final TimeBasedEpochGenerator timeBasedEpochGenerator;

    @Deprecated
    public UUID next() {
        return timeBasedEpochGenerator.generate();
    }

    @Deprecated
    public String nextString() {
        return timeBasedEpochGenerator.generate().toString();
    }
}
