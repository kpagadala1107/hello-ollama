package com.example.mappingagent.model;

import lombok.Data;

@Data
public class MappingRule {
    private String sourcePath;
    private String targetPath;
    private String transformation;
    private double confidenceScore;


}