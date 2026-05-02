package com.backwell.auth_server.service;

import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UUIDGeneratorService {
    private final TimeBasedEpochGenerator generator;

    public UUID generate() {
        return generator.generate();
    }
}
