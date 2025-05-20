package com.example.mappingagent.service;

import com.example.mappingagent.model.*;
import com.example.mappingagent.service.EnhancedSimilarityService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnhancedMappingService {

    private final EnhancedSimilarityService similarityService;
    private final ObjectMapper objectMapper;

    public EnhancedMappingService(EnhancedSimilarityService similarityService, 
                                ObjectMapper objectMapper) {
        this.similarityService = similarityService;
        this.objectMapper = objectMapper;
    }

    public MappingConfiguration generateMapping(JsonNode sourceSchema, JsonNode targetSchema) {
        List<MappingRule> rules = new ArrayList<>();
        
        // Process root level fields
        if (sourceSchema.getChildren() != null && targetSchema.getChildren() != null) {
            Map<String, String> fieldMatches = similarityService.findBestMatches(
                    sourceSchema.getChildren(), 
                    targetSchema.getChildren()
            );
            
            fieldMatches.forEach((sourceField, targetField) -> {
                JsonNode sourceChild = findChildByName(sourceSchema, sourceField);
                JsonNode targetChild = findChildByName(targetSchema, targetField);
                
                if (sourceChild != null && targetChild != null) {
                    generateRulesForNodes(sourceChild, targetChild, "", "", rules);
                }
            });
        }
        
        // Calculate overall confidence
        double overallConfidence = rules.stream()
                .mapToDouble(MappingRule::getConfidenceScore)
                .average()
                .orElse(0.0);
        
        MappingConfiguration config = new MappingConfiguration();
        config.setRules(rules);
        config.setOverallConfidence(overallConfidence);
        config.setNotes("Generated with LLM-enhanced semantic matching");
        
        return config;
    }

    private void generateRulesForNodes(JsonNode source, JsonNode target, 
                                    String sourcePath, String targetPath, 
                                    List<MappingRule> rules) {
        String currentSourcePath = sourcePath.isEmpty() ? source.getName() : sourcePath + "." + source.getName();
        String currentTargetPath = targetPath.isEmpty() ? target.getName() : targetPath + "." + target.getName();
        
        // Add rule for current node if it's a leaf node
        if (source.getChildren() == null || source.getChildren().isEmpty()) {
            MappingRule rule = new MappingRule();
            rule.setSourcePath(currentSourcePath);
            rule.setTargetPath(currentTargetPath);
            rule.setTransformation("direct");
            
            // Confidence based on similarity score
            double similarity = similarityService.calculateCombinedSimilarity(source, target);
            rule.setConfidenceScore(similarity);
            
            rules.add(rule);
        } else {
            // Recursively process children
            if (source.getChildren() != null && target.getChildren() != null) {
                Map<String, String> childMatches = similarityService.findBestMatches(
                        source.getChildren(), 
                        target.getChildren()
                );
                
                childMatches.forEach((sourceChildName, targetChildName) -> {
                    JsonNode sourceChild = findChildByName(source, sourceChildName);
                    JsonNode targetChild = findChildByName(target, targetChildName);
                    
                    if (sourceChild != null && targetChild != null) {
                        generateRulesForNodes(
                                sourceChild, 
                                targetChild, 
                                currentSourcePath, 
                                currentTargetPath, 
                                rules
                        );
                    }
                });
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

    private JsonNode findChildByName(JsonNode parent, String name) {
        return parent.getChildren().stream()
                .filter(child -> child.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}