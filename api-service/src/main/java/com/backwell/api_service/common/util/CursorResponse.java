package com.backwell.api_service.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Setter
@AllArgsConstructor
public class CursorResponse<T> {
    @Getter
    private List<T> content;
    @Getter
    private boolean hasNext;

    private T nextCursor;

    public Optional<T> getNextCursor(){
        return Optional.ofNullable(nextCursor);
    }
}
