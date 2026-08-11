package com.group3.vitamins.bidding.bidnotice.application.command;

// PATCH 요청에서 필드 생략과 명시적인 null 전달을 구분합니다.
public record PatchField<T>(boolean present, T value) {

    // 요청에 필드가 없을 때 기존 값을 유지하도록 표시합니다.
    public static <T> PatchField<T> absent() {
        return new PatchField<>(false, null);
    }

    // 요청에 필드가 있을 때 null을 포함한 전달값을 표시합니다.
    public static <T> PatchField<T> of(T value) {
        return new PatchField<>(true, value);
    }

    // 필드가 생략됐으면 기존 값을, 전달됐으면 요청값을 반환합니다.
    public T resolve(T currentValue) {
        return present ? value : currentValue;
    }
}
