package com.backwell.api_service.common.util;

import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UUIDService {

    private final TimeBasedEpochGenerator generator;

    /**
     * Returns a time-based epoch UUID Object
     * */
    public UUID next() {
        return generator.generate();
    }

    /**
     * Returns a time-based epoch UUID Object
     * */
    public String nextString(){
        return generator.generate().toString();
    }
}
