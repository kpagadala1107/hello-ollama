package com.example.mappingagent.service;

import com.example.mappingagent.model.JsonNode;
import com.example.mappingagent.service.LLMService;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
public class EnhancedSimilarityService {

    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    private final LLMService llmService;
    private final Map<String, String> synonymCache = new HashMap<>();

    public EnhancedSimilarityService(LLMService llmService) {
        this.llmService = llmService;
    }

    public Map<String, String> findBestMatches(List<JsonNode> sourceNodes, List<JsonNode> targetNodes) {
        // First try exact matches
        Map<String, String> exactMatches = findExactMatches(sourceNodes, targetNodes);
        
        // Get remaining fields
        List<JsonNode> remainingSources = sourceNodes.stream()
                .filter(n -> !exactMatches.containsKey(n.getName())).toList();


        List<JsonNode> remainingTargets = targetNodes.stream()
                .filter(n -> !exactMatches.containsValue(n.getName())).toList();

        
        // Then try LLM-powered semantic matching
        Map<String, String> semanticMatches = llmService.findSemanticMatches(remainingSources, remainingTargets);

        // Then fall back to string similarity
        Map<String, String> similarityMatches = findSimilarityMatches(remainingSources.stream()
                        .filter(n -> !semanticMatches.containsKey(n.getName())).toList(),
        remainingTargets.stream()
                .filter(n -> !semanticMatches.containsValue(n.getName())).toList()
        );
        
        // Combine all matches
        Map<String, String> allMatches = new HashMap<>();
        allMatches.putAll(exactMatches);
        allMatches.putAll(semanticMatches);
        allMatches.putAll(similarityMatches);
        
        return allMatches;
    }

    private Map<String, String> findExactMatches(List<JsonNode> sourceNodes, List<JsonNode> targetNodes) {
        Map<String, String> matches = new HashMap<>();
        
        Set<String> targetNames = targetNodes.stream()
                .map(JsonNode::getName)
                .collect(Collectors.toSet());
        
        for (JsonNode source : sourceNodes) {
            if (targetNames.contains(source.getName())) {
                matches.put(source.getName(), source.getName());
            }
        }
        
        return matches;
    }

    private Map<String, String> findSimilarityMatches(List<JsonNode> sources, List<JsonNode> targets) {
        Map<String, String> matches = new HashMap<>();
        Map<String, Double> bestScores = new HashMap<>();
        
        for (JsonNode source : sources) {
            for (JsonNode target : targets) {
                double similarity = calculateCombinedSimilarity(source, target);
                
                // Only consider matches above threshold
                if (similarity > 0.6) {
                    if (!bestScores.containsKey(source.getName()) || 
                        bestScores.get(source.getName()) < similarity) {
                        bestScores.put(source.getName(), similarity);
                        matches.put(source.getName(), target.getName());
                    }
                }
            }
        }
        
        return matches;
    }

    protected double calculateCombinedSimilarity(JsonNode source, JsonNode target) {
        double nameSimilarity = calculateNameSimilarity(source.getName(), target.getName());
        double typeSimilarity = source.getType().equals(target.getType()) ? 1.0 : 0.0;
        
        // Weighted combination favoring name similarity
        return 0.7 * nameSimilarity + 0.3 * typeSimilarity;
    }

    private double calculateNameSimilarity(String name1, String name2) {
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);
        
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        if (maxLength == 0) return 1.0;
        
        int distance = levenshteinDistance.apply(normalized1, normalized2);
        return 1.0 - (double) distance / maxLength;
    }

    private String normalizeName(String name) {
        return name.toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }
}