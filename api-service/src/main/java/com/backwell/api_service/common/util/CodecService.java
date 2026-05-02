package com.backwell.api_service.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.sqids.Sqids;

import java.util.List;

@Component
@RequiredArgsConstructor
@Deprecated
public class CodecService {
    private final Sqids sqids;

    public String encode(List<Long> ids) {
        return sqids.encode(ids);
    }

    public List<Long> decode(String str, int length) {
        if (StringUtils.hasText(str)) throw new IllegalArgumentException("Encoded SString must have text.");

        List<Long> ids = sqids.decode(str);
        if(ids.size() != length) {
            throw new IllegalArgumentException("Decoded ids length mismatch");
        }
        return ids;
    }
}
