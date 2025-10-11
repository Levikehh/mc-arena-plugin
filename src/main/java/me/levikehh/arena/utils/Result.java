package me.levikehh.arena.utils;

public class Result<T> {
    private final T value;
    private final String error;
    private final boolean success;

    private Result(T value, String error, boolean success) {
        this.value = value;
        this.error = error;
        this.success = success;
    }

    public static <T> Result<T> success(T value) {
        return new Result<>(value, null, true);
    }

    public static <T> Result<T> success() {
        return new Result<>(null, null, true);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(null, error, false);
    }

    public static <T> Result<T> failure(Exception e) {
        return new Result<>(null, e.getMessage(), false);
    }

    public boolean isSuccess() {
        return this.success;
    }

    public boolean isFailure() {
        return !this.success;
    }

    public T getValue() {
        if (!this.success) {
            throw new IllegalStateException("Cannot get value from failed result! Error: " + this.error);
        }

        return this.value;
    }

    public String getError() {
        return this.error;
    }
}
