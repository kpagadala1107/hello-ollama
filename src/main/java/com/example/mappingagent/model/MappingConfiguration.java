package com.example.mappingagent.model;

import lombok.Data;
import java.util.List;

@Data
public class MappingConfiguration {
    private List<MappingRule> rules;
    private String notes;
    private double overallConfidence;

}