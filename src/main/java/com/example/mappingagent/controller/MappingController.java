package com.example.mappingagent.controller;

import com.example.mappingagent.model.JsonNode;
import com.example.mappingagent.model.MappingConfiguration;
import com.example.mappingagent.service.MappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mapping")
public class MappingController {

    @Autowired
    private MappingService mappingService;

    @PostMapping("/generate")
    public MappingConfiguration generateMapping(
            @RequestBody MappingRequest request) throws Exception {
        JsonNode sourceSchema = mappingService.parseJsonSchema(request.getSourceJson());
        JsonNode targetSchema = mappingService.parseJsonSchema(request.getTargetJson());
        return mappingService.generateMapping(sourceSchema, targetSchema);
    }

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