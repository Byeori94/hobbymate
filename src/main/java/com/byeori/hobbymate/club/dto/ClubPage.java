package com.byeori.hobbymate.club.dto;

import java.util.List;

public record ClubPage<T>(
        List<T> content,
        long totalCount,
        int page,
        int pageSize,
        int totalPages,
        int startPage,
        int endPage,
        boolean hasPreviousBlock,
        boolean hasNextBlock) {

    private static final int BLOCK_SIZE = 5;

    public ClubPage {
        content = List.copyOf(content);
    }

    public static int normalizePage(int requestedPage, long totalCount, int pageSize) {
        int totalPages = totalPages(totalCount, pageSize);
        return Math.min(Math.max(requestedPage, 1), totalPages);
    }

    public static <T> ClubPage<T> of(
            List<T> content,
            long totalCount,
            int requestedPage,
            int pageSize) {
        int totalPages = totalPages(totalCount, pageSize);
        int page = normalizePage(requestedPage, totalCount, pageSize);
        int startPage = ((page - 1) / BLOCK_SIZE) * BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages);
        return new ClubPage<>(
                content,
                totalCount,
                page,
                pageSize,
                totalPages,
                startPage,
                endPage,
                startPage > 1,
                endPage < totalPages);
    }

    public int previousBlockPage() {
        return Math.max(1, startPage - 1);
    }

    public int nextBlockPage() {
        return Math.min(totalPages, endPage + 1);
    }

    private static int totalPages(long totalCount, int pageSize) {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
