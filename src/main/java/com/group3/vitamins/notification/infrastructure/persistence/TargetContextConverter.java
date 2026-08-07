package com.group3.vitamins.notification.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * {@code notification.target_context}(JSON 컬럼) ↔ {@code Map<String, Object>} 변환기.
 *
 * <p>이동에 필요한 부가 식별값만 담긴 작은 객체라({@code {"revisionId": 56}}) 통째로 직렬화한다.
 * 이 값으로 검색·정렬하지 않으므로 컬럼 분해는 하지 않는다.
 */
@Converter
public class TargetContextConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("target_context 직렬화 실패: " + attribute, e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(dbData, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalArgumentException("target_context 역직렬화 실패: " + dbData, e);
        }
    }
}
