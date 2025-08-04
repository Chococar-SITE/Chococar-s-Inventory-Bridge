package site.chococar.inventorybridge.common.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationManager.class);
    private final Path configPath;
    private Map<String, Object> config;
    private final Yaml yaml;
    
    {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        yaml = new Yaml(options);
    }
    
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
        Map<String, Object> config = new LinkedHashMap<>();
        
        // Database configuration
        Map<String, Object> database = new LinkedHashMap<>();
        database.put("host", "localhost");
        database.put("port", 3306);
        database.put("database", "inventory_bridge");
        database.put("username", "minecraft");
        database.put("password", "password");
        database.put("tablePrefix", "ib_");
        database.put("maxPoolSize", 10);
        database.put("connectionTimeout", 30000);
        database.put("useSSL", false);
        config.put("database", database);
        
        // Sync configuration
        Map<String, Object> sync = new LinkedHashMap<>();
        sync.put("enableAutoSync", true);
        sync.put("syncIntervalTicks", 200);
        sync.put("syncOnJoin", true);
        sync.put("syncOnLeave", true);
        sync.put("syncEnderChest", true);
        sync.put("syncExperience", true);
        sync.put("syncHealth", false);
        sync.put("syncHunger", false);
        sync.put("serverId", "server1");
        config.put("sync", sync);
        
        // Compatibility configuration
        Map<String, Object> compatibility = new LinkedHashMap<>();
        compatibility.put("enableLegacySupport", true);
        compatibility.put("convertOldItems", true);
        compatibility.put("preserveCustomModelData", true);
        compatibility.put("handleBundles", true);
        compatibility.put("handleCopperOxidation", true);
        compatibility.put("handleResinItems", true);
        compatibility.put("handleGhastItems", true);
        compatibility.put("handleNewMusicDiscs", true);
        compatibility.put("minecraftVersion", "1.21.8");
        config.put("compatibility", compatibility);
        
        return config;
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