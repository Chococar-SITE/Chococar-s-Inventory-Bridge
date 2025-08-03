package site.chococar.inventorybridge.common.config;

import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ConfigurationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);
    private final Path configPath;
    private Map<String, Object> config;
    private final Yaml yaml = new Yaml();
    
    public ConfigurationManager(Path configPath) {
        this.configPath = configPath;
    }
    
    public void loadConfig() {
        if (Files.exists(configPath)) {
            try (InputStream inputStream = Files.newInputStream(configPath)) {
                config = yaml.load(inputStream);
                LOGGER.info("配置文件加載成功");
            } catch (IOException e) {
                LOGGER.error("加載配置文件失敗", e);
                createDefaultConfig();
            }
        } else {
            createDefaultConfig();
        }
    }
    
    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                yaml.dump(config, writer);
            }
            LOGGER.info("配置文件保存成功");
        } catch (IOException e) {
            LOGGER.error("保存配置文件失敗", e);
        }
    }
    
    private void createDefaultConfig() {
        config = createDefaultConfigData();
        saveConfig();
        LOGGER.info("創建默認配置文件");
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> createDefaultConfigData() {
        return Map.of(
            "database", Map.of(
                "host", "localhost",
                "port", 3306,
                "database", "inventory_bridge",
                "username", "minecraft",
                "password", "password",
                "tablePrefix", "ib_",
                "maxPoolSize", 10,
                "connectionTimeout", 30000,
                "useSSL", false
            ),
            "sync", Map.of(
                "enableAutoSync", true,
                "syncIntervalTicks", 200,
                "syncOnJoin", true,
                "syncOnLeave", true,
                "syncEnderChest", true,
                "syncExperience", true,
                "syncHealth", false,
                "syncHunger", false,
                "serverId", "server1"
            ),
            "compatibility", Map.of(
                "enableLegacySupport", true,
                "convertOldItems", true,
                "preserveCustomModelData", true,
                "handleBundles", true,
                "handleCopperOxidation", true,
                "handleResinItems", true,
                "handleGhastItems", true,
                "handleNewMusicDiscs", true,
                "minecraftVersion", "1.21.8"
            )
        );
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getValue(String path, T defaultValue) {
        String[] keys = path.split("\\.");
        Object current = config;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }
        
        try {
            return (T) current;
        } catch (ClassCastException e) {
            LOGGER.warn("配置值類型不匹配: {}", path);
            return defaultValue;
        }
    }
    
    public String getString(String path, String defaultValue) {
        return getValue(path, defaultValue);
    }
    
    public int getInt(String path, int defaultValue) {
        return getValue(path, defaultValue);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return getValue(path, defaultValue);
    }
    
    public double getDouble(String path, double defaultValue) {
        return getValue(path, defaultValue);
    }
    
    public Map<String, Object> getConfig() {
        return config;
    }
}