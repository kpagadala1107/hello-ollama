package com.example.mappingagent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class JsonNode {
    private String name;
    private String path;
    private String type; // "object", "array", "string", "number", "boolean", "null"
    private List<JsonNode> children;
    private Map<String, String> metadata;

}