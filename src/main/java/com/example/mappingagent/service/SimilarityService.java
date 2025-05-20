package com.example.mappingagent.service;

import com.example.mappingagent.model.JsonNode;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SimilarityService {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    public double calculateNameSimilarity(String name1, String name2) {
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);
        
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance.apply(normalized1, normalized2);
        return 1.0 - (double) distance / maxLength;
    }

    public double calculateStructureSimilarity(JsonNode node1, JsonNode node2) {
        if (!node1.getType().equals(node2.getType())) {
            return 0.0;
        }
        
        if (node1.getChildren()!= null && node1.getChildren().isEmpty() && node2.getChildren() != null && node2.getChildren().isEmpty()) {
            return 1.0;
        }
        
        Map<String, JsonNode> children1 = indexChildren(node1);
        Map<String, JsonNode> children2 = indexChildren(node2);
        
        double totalScore = 0.0;
        int comparisons = 0;
        
        for (Map.Entry<String, JsonNode> entry : children1.entrySet()) {
            if (children2.containsKey(entry.getKey())) {
                totalScore += calculateNameSimilarity(entry.getKey(), entry.getKey());
                totalScore += calculateStructureSimilarity(entry.getValue(), children2.get(entry.getKey()));
                comparisons += 2;
            }
        }
        
        return comparisons > 0 ? totalScore / comparisons : 0.0;
    }

    private Map<String, JsonNode> indexChildren(JsonNode node) {
        Map<String, JsonNode> index = new HashMap<>();
        if (node.getChildren() != null) {
            for (JsonNode child : node.getChildren()) {
                index.put(child.getName(), child);
            }
        }

        return index;
    }

    private String normalizeName(String name) {
        return name.toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}