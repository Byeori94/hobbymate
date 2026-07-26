package com.byeori.hobbymate.admin.dto;

import java.util.List;

public record AdminPage<T>(
        List<T> content,
        long totalCount,
        int page,
        int totalPages,
        int startPage,
        int endPage,
        boolean hasPreviousBlock,
        boolean hasNextBlock) {

    public static final int PAGE_SIZE = 10;
    private static final int BLOCK_SIZE = 5;

    public static int normalizePage(int requestedPage, long totalCount) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        return Math.min(Math.max(requestedPage, 1), totalPages);
    }

    public static <T> AdminPage<T> of(List<T> content, long totalCount, int requestedPage) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int page = normalizePage(requestedPage, totalCount);
        int startPage = ((page - 1) / BLOCK_SIZE) * BLOCK_SIZE + 1;
        int endPage = Math.min(startPage + BLOCK_SIZE - 1, totalPages);
        return new AdminPage<>(
                List.copyOf(content),
                totalCount,
                page,
                totalPages,
                startPage,
                endPage,
                startPage > 1,
                endPage < totalPages);
    }

    public int offset() {
        return (page - 1) * PAGE_SIZE;
    }

    public int previousBlockPage() {
        return Math.max(1, startPage - 1);
    }

    public int nextBlockPage() {
        return Math.min(totalPages, endPage + 1);
    }
}
