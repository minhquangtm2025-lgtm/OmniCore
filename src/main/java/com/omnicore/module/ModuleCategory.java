package com.omnicore.module;

public enum ModuleCategory {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    SURVIVAL("Survival"),
    BUILDING("Building"),
    UTILITY("Utility"),
    AI("AI");
    
    private final String name;
    
    ModuleCategory(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
}
