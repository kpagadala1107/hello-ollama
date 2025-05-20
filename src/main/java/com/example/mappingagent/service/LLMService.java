package com.example.mappingagent.service;

import com.example.mappingagent.model.JsonNode;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.model:gpt-4}")
    private String model;

    public Map<String, String> findSemanticMatches(List<JsonNode> sourceFields, List<JsonNode> targetFields) {
        if (openAiApiKey == null || openAiApiKey.isEmpty()) {
            return Map.of(); // Fall back to non-LLM matching
        }

        OpenAiService service = new OpenAiService(openAiApiKey, Duration.ofSeconds(60));

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

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(List.of(new ChatMessage("user", prompt)))
                .temperature(0.2)
                .maxTokens(500)
                .build();

        ChatCompletionResult result = service.createChatCompletion(request);
        String response = result.getChoices().get(0).getMessage().getContent();

        return parseLlmResponse(response);
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