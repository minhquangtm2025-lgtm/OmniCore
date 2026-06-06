package com.omnicore;

import com.omnicore.command.CommandManager;
import com.omnicore.config.ConfigManager;
import com.omnicore.event.EventManager;
import com.omnicore.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OmniCore implements ClientModInitializer {
    public static final String MOD_ID = "omnicore";
    public static final String MOD_NAME = "OmniCore";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);
    
    private static OmniCore instance;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private EventManager eventManager;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Initializing {} v{} for Minecraft 1.21.11 (Mounts of Mayhem)", MOD_NAME, VERSION);
        
        // Initialize managers
        configManager = new ConfigManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        eventManager = new EventManager();
        
        // Load configuration
        configManager.load();
        
        // Register modules
        moduleManager.registerModules();
        
        // Register commands
        commandManager.registerCommands();
        
        // Register events
        eventManager.registerEvents();
        
        LOGGER.info("{} initialized successfully!", MOD_NAME);
    }
    
    public static OmniCore getInstance() {
        return instance;
    }
    
    public ModuleManager getModuleManager() {
        return moduleManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CommandManager getCommandManager() {
        return commandManager;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
}
