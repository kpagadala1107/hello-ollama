package com.example.mappingagent.service;

import com.example.mappingagent.model.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.mappingagent.model.MappingConfiguration;
import com.example.mappingagent.model.MappingRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MappingService {

    @Autowired
    private SimilarityService similarityService;

    @Autowired
    private ObjectMapper objectMapper;

    public MappingConfiguration generateMapping(JsonNode sourceSchema, JsonNode targetSchema) {
        List<MappingRule> rules = new ArrayList<>();
        
        // First pass: exact name matches
        generateMatchingRules(sourceSchema, targetSchema, rules, "");
        
        // Second pass: similar name matches
        generateSimilarityBasedRules(sourceSchema, targetSchema, rules, "");
        
        // Calculate overall confidence
        double overallConfidence = rules.stream()
                .mapToDouble(MappingRule::getConfidenceScore)
                .average()
                .orElse(0.0);
        
        MappingConfiguration config = new MappingConfiguration();
        config.setRules(rules);
        config.setOverallConfidence(overallConfidence);
        config.setNotes("Automatically generated mapping configuration");
        
        return config;
    }

    private void generateMatchingRules(JsonNode source, JsonNode target, List<MappingRule> rules, String currentPath) {
        String sourcePath = currentPath.isEmpty() ? source.getName() : currentPath + "." + source.getName();
        String targetPath = currentPath.isEmpty() ? target.getName() : currentPath + "." + target.getName();
        
        if (source.getName().equalsIgnoreCase(target.getName()) && 
            source.getType().equals(target.getType())) {
            MappingRule rule = new MappingRule();
            rule.setSourcePath(sourcePath);
            rule.setTargetPath(targetPath);
            rule.setTransformation("direct");
            rule.setConfidenceScore(1.0);
            rules.add(rule);
        }
        
        // Recursively process children
        if (source.getChildren() != null && target.getChildren() != null) {
            Map<String, JsonNode> targetChildren = target.getChildren().stream()
                    .collect(Collectors.toMap(JsonNode::getName, node -> node));
            
            for (JsonNode sourceChild : source.getChildren()) {
                if (targetChildren.containsKey(sourceChild.getName())) {
                    generateMatchingRules(
                            sourceChild, 
                            targetChildren.get(sourceChild.getName()), 
                            rules, 
                            sourcePath
                    );
                }
            }
        }
    }

    private void generateSimilarityBasedRules(JsonNode source, JsonNode target, List<MappingRule> rules, String currentPath) {
        String sourcePath = currentPath.isEmpty() ? source.getName() : currentPath + "." + source.getName();
        String targetPath = currentPath.isEmpty() ? target.getName() : currentPath + "." + target.getName();
        
        double nameSimilarity = similarityService.calculateNameSimilarity(source.getName(), target.getName());
        double structureSimilarity = similarityService.calculateStructureSimilarity(source, target);
        double combinedScore = (nameSimilarity + structureSimilarity) / 2;
        
        if (combinedScore > 0.4 && !rules.stream().anyMatch(r -> r.getTargetPath().equals(targetPath))) {
            MappingRule rule = new MappingRule();
            rule.setSourcePath(sourcePath);
            rule.setTargetPath(targetPath);
            rule.setTransformation("direct");
            rule.setConfidenceScore(combinedScore);
            rules.add(rule);
        }
        
        // Recursively process children
        if (source.getChildren() != null && target.getChildren() != null) {
            for (JsonNode sourceChild : source.getChildren()) {
                for (JsonNode targetChild : target.getChildren()) {
                    generateSimilarityBasedRules(sourceChild, targetChild, rules, sourcePath);
                }
            }
        }
    }

    public JsonNode parseJsonSchema(String json) throws Exception {
        com.fasterxml.jackson.databind.JsonNode rootNode = objectMapper.readTree(json);
        return convertJsonNode(rootNode, "");
    }

    private JsonNode convertJsonNode(com.fasterxml.jackson.databind.JsonNode node, String name) {
        JsonNode result = new JsonNode();
        result.setName(name);
        
        if (node.isObject()) {
            result.setType("object");
            List<JsonNode> children = new ArrayList<>();
            node.fields().forEachRemaining(entry -> {
                children.add(convertJsonNode(entry.getValue(), entry.getKey()));
            });
            result.setChildren(children);
        } else if (node.isArray()) {
            result.setType("array");
            if (!node.isEmpty()) {
                // For arrays, we'll just look at the first element to determine structure
                result.setChildren(List.of(convertJsonNode(node.get(0), "item")));
            }
        } else if (node.isTextual()) {
            result.setType("string");
        } else if (node.isNumber()) {
            result.setType("number");
        } else if (node.isBoolean()) {
            result.setType("boolean");
        } else if (node.isNull()) {
            result.setType("null");
        }
        
        return result;
    }
}