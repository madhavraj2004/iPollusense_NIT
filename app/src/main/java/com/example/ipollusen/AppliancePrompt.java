package com.example.ipollusen;

import java.util.List;

public class AppliancePrompt {
    private String name;
    private String description;
    private String type;
    private List<String> validRange; // For simplicity, using List<String> to accommodate different types

    public AppliancePrompt(String name, String description, String type, List<String> validRange) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.validRange = validRange;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public List<String> getValidRange() {
        return validRange;
    }
}
