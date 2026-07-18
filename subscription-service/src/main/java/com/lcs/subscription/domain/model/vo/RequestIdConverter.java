package com.lcs.subscription.domain.model.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

/**
 * {@link RequestId}(UUID)를 DB 컬럼(VARCHAR)으로 변환하기 위한 JPA 컨버터.
 */
@Converter
public class RequestIdConverter implements AttributeConverter<RequestId, String> {

    @Override
    public String convertToDatabaseColumn(RequestId attribute) {
        return attribute == null ? null : attribute.value().toString();
    }

    @Override
    public RequestId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new RequestId(UUID.fromString(dbData));
    }
}
