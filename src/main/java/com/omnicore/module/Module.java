package com.omnicore.module;

public abstract class Module {
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private boolean enabled;
    private int keybind;
    
    public Module(String name, String description, ModuleCategory category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
        this.keybind = -1;
    }
    
    public void toggle() {
        setEnabled(!enabled);
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }
    
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender() {}
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ModuleCategory getCategory() {
        return category;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getKeybind() {
        return keybind;
    }
    
    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }
}
