package com.byeori.hobbymate.common.exception;

public class CategoryManagementException extends RuntimeException {

    private final String fieldName;
    private final boolean notFound;

    public CategoryManagementException(String message) {
        this(null, message, false);
    }

    public CategoryManagementException(String fieldName, String message) {
        this(fieldName, message, false);
    }

    private CategoryManagementException(String fieldName, String message, boolean notFound) {
        super(message);
        this.fieldName = fieldName;
        this.notFound = notFound;
    }

    public static CategoryManagementException notFound() {
        return new CategoryManagementException(
                null,
                "카테고리를 찾을 수 없습니다.",
                true);
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isNotFound() {
        return notFound;
    }
}
