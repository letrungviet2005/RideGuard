package com.rideguard.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ResponseViews {
  private final ObjectMapper mapper;

  public ResponseViews(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public <T> T one(Object value, Class<T> type) {
    return mapper.convertValue(value, type);
  }

  public record Page<T>(List<T> items, String nextCursor) {}

  public <T> Page<T> page(Object value, Class<T> type) {
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) value;
    @SuppressWarnings("unchecked")
    List<Object> rows = (List<Object>) map.get("items");
    return new Page<>(
        rows.stream().map(r -> one(r, type)).toList(), (String) map.get("next_cursor"));
  }
}
