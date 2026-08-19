package com.group3.vitamins.qualification.domain;

import com.group3.vitamins.qualification.domain.model.QualificationNameRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QualificationNameRule 전공·자격증 마스터 이름 규칙")
class QualificationNameRuleTest {

    @Test
    @DisplayName("보통 이름은 통과하고 앞뒤 공백은 normalize 가 걷어낸다")
    void plainNamePasses() {
        assertThat(QualificationNameRule.isValid("  컴퓨터공학 ")).isTrue();
        assertThat(QualificationNameRule.normalize("  컴퓨터공학 ")).isEqualTo("컴퓨터공학");
        assertThat(QualificationNameRule.isValid("a".repeat(100))).isTrue();   // 경계 100자 허용
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"정보처리기사, SQLD", "A;B", "컴퓨터공학:학사", "줄\n바꿈", "캐리지\r리턴", "   "})
    @DisplayName("엑셀 구분자(, ; : 줄바꿈)가 든 이름과 빈 이름은 거부한다")
    void separatorCharsRejected(String name) {
        assertThat(QualificationNameRule.isValid(name)).isFalse();
    }

    @Test
    @DisplayName("null · 101자는 거부한다")
    void nullAndTooLongRejected() {
        assertThat(QualificationNameRule.isValid(null)).isFalse();
        assertThat(QualificationNameRule.isValid("a".repeat(101))).isFalse();
    }
}
