package com.omnicore.config;

import com.google.gson.*;
import com.omnicore.OmniCore;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configFile;
    private JsonObject config;

    public ConfigManager() {
        this.configFile = FabricLoader.getInstance().getConfigDir().resolve("omnicore.json");
        this.config = new JsonObject();
    }

    public void load() {
        if (!Files.exists(configFile)) {
            config = new JsonObject();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            config = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            OmniCore.LOGGER.error("Failed to load config: {}", e.getMessage());
            config = new JsonObject();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            OmniCore.LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return config.has(key) ? config.get(key).getAsBoolean() : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        return config.has(key) ? config.get(key).getAsInt() : defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        return config.has(key) ? config.get(key).getAsDouble() : defaultValue;
    }

    public String getString(String key, String defaultValue) {
        return config.has(key) ? config.get(key).getAsString() : defaultValue;
    }

    public void set(String key, boolean value) {
        config.addProperty(key, value);
    }

    public void set(String key, int value) {
        config.addProperty(key, value);
    }

    public void set(String key, double value) {
        config.addProperty(key, value);
    }

    public void set(String key, String value) {
        config.addProperty(key, value);
    }

    public JsonObject getSection(String section) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            return config.getAsJsonObject(section);
        }
        JsonObject obj = new JsonObject();
        config.add(section, obj);
        return obj;
    }
}
