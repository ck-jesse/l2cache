package com.github.l2cache.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author chenck
 * @date 2020/7/8 16:19
 */
public class ObjectMapperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Object convert to json
     */
    public static String toJson(Object value) {
        if (null == value) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("convert to json error", e);
        }
    }

    /**
     * json convert to Object
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (null == json || json.trim().length() == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("convert to " + clazz.getName() + " error", e);
        }
    }
}
