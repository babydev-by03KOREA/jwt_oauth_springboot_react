package com.common.api.login.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Converter
public class JsonAttributeConverter
        implements AttributeConverter<Map<String,Object>, String> {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attr) {
        try { return om.writeValueAsString(attr); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException(e); }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null
                    ? Collections.emptyMap()
                    : om.readValue(dbData, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

