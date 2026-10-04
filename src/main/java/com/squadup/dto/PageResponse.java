package com.squadup.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Stable JSON form of a Spring Data {@link Page}. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        String sort) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast(), page.getSort().toString());
    }
}
