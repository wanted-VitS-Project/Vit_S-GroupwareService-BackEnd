package com.group3.vitamins.issue.application.command;

public record PatchField<T>(
        boolean present,
        T value
) {

    public static <T> PatchField<T> absent() {
        return new PatchField<>(false, null);
    }

    public static <T> PatchField<T> present(T value) {
        return new PatchField<>(true, value);
    }
}
