package com.group3.vitamins.activitylog.infrastructure.persistence;

import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActivityLogActionConverter")
class ActivityLogActionConverterTest {

    private final ActivityLogActionConverter converter = new ActivityLogActionConverter();

    @ParameterizedTest
    @EnumSource(ActivityLogAction.class)
    @DisplayName("모든 Action을 DB 소문자 값으로 변환하고 다시 되돌린다")
    void roundTrip(ActivityLogAction action) {
        String dbValue = converter.convertToDatabaseColumn(action);

        assertThat(dbValue).isEqualTo(action.name().toLowerCase(Locale.ROOT));
        assertThat(converter.convertToEntityAttribute(dbValue)).isEqualTo(action);
    }

    @Test
    @DisplayName("휴지통 복원은 restore로, 영구삭제는 purge로 저장된다")
    void trashActions() {
        assertThat(converter.convertToDatabaseColumn(ActivityLogAction.RESTORE)).isEqualTo("restore");
        assertThat(converter.convertToDatabaseColumn(ActivityLogAction.PURGE)).isEqualTo("purge");
        assertThat(converter.convertToEntityAttribute("restore")).isEqualTo(ActivityLogAction.RESTORE);
        assertThat(converter.convertToEntityAttribute("purge")).isEqualTo(ActivityLogAction.PURGE);
    }

    @Test
    @DisplayName("알 수 없는 DB 값은 예외를 던진다")
    void unknownDbValue() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("download"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null은 null로 변환한다")
    void nullHandling() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
