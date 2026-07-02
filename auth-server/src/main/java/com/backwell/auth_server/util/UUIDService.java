package com.backwell.auth_server.util;

import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UUIDService {
    private final TimeBasedEpochGenerator generator;

    public UUID next() {
        return generator.generate();
    }

    public String nextString() {
        return generator.generate().toString();
    }
}
