package com.example.mappingagent.model;

import com.example.mappingagent.model.MappingRule;
import lombok.Data;

import java.util.List;

@Data
public class MappingConfiguration {
    private List<MappingRule> rules;
    private String notes;
    private double overallConfidence;

}