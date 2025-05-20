package com.example.mappingagent.controller;

import com.example.mappingagent.model.JsonNode;
import com.example.mappingagent.model.MappingConfiguration;
import com.example.mappingagent.service.EnhancedMappingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mapping")
public class EnhancedMappingController {

    private final EnhancedMappingService mappingService;

    public EnhancedMappingController(EnhancedMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @PostMapping("/generate-enhanced")
    public MappingConfiguration generateEnhancedMapping(
            @RequestBody MappingRequest request) throws Exception {
        JsonNode sourceSchema = mappingService.parseJsonSchema(request.getSourceJson());
        JsonNode targetSchema = mappingService.parseJsonSchema(request.getTargetJson());
        return mappingService.generateMapping(sourceSchema, targetSchema);
    }

    // ... (keep the existing MappingRequest class)
    public static class MappingRequest {
        private String sourceJson;
        private String targetJson;

        // Getters and setters
        public String getSourceJson() {
            return sourceJson;
        }

        public void setSourceJson(String sourceJson) {
            this.sourceJson = sourceJson;
        }

        public String getTargetJson() {
            return targetJson;
        }

        public void setTargetJson(String targetJson) {
            this.targetJson = targetJson;
        }
    }
}