package com.example.mappingagent.service;

import com.example.mappingagent.model.JsonNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {

    private final ChatClient chatClient;

    public LLMService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Map<String, String> findSemanticMatches(List<JsonNode> sourceFields, List<JsonNode> targetFields) {
        // Prepare the prompt
        String sourceFieldsStr = String.join(", ", sourceFields.stream().map(JsonNode::getName).toList());
        String targetFieldsStr = String.join(", ", targetFields.stream().map(JsonNode::getName).toList());

        String prompt = """
                Analyze these two sets of field names from JSON schemas and suggest the most likely matches 
                based on their semantic meaning. Return only the matching pairs in the format "source:target".
                
                Source fields: %s
                Target fields: %s
                
                Consider:
                1. Synonyms (e.g., "customer" and "client")
                2. Abbreviations (e.g., "addr" and "address")
                3. Different naming conventions (e.g., "first_name" and "firstName")
                4. Related concepts (e.g., "price" and "amount")
                """.formatted(sourceFieldsStr, targetFieldsStr);

        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        assert response != null;
        return  parseLlmResponse(response);

    }

    private Map<String, String> parseLlmResponse(String response) {
        Map<String, String> matches = new HashMap<>();
        String[] lines = response.split("\n");
        
        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    matches.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        
        return matches;
    }
}