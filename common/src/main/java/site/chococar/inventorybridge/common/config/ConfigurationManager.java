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
    private static final int CURRENT_CONFIG_VERSION = 2;
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
                Map<String, Object> loadedConfig = yaml.load(inputStream);
                
                if (loadedConfig == null) {
                    LOGGER.warn("配置文件為空，創建默認配置");
                    createDefaultConfig();
                    return;
                }
                
                // 檢查配置版本
                int configVersion = getConfigVersion(loadedConfig);
                
                if (configVersion < CURRENT_CONFIG_VERSION) {
                    LOGGER.info("檢測到舊版配置文件 (版本 {}，當前版本 {})，開始遷移...", configVersion, CURRENT_CONFIG_VERSION);
                    config = migrateConfig(loadedConfig, configVersion);
                    saveConfig(); // 保存遷移後的配置
                    LOGGER.info("配置文件遷移完成");
                } else if (configVersion > CURRENT_CONFIG_VERSION) {
                    LOGGER.warn("配置文件版本 ({}) 高於當前支持版本 ({})，可能存在兼容性問題", configVersion, CURRENT_CONFIG_VERSION);
                    config = loadedConfig;
                } else {
                    config = loadedConfig;
                    LOGGER.info("配置文件加載成功 (版本 {})", configVersion);
                }
                
            } catch (IOException e) {
                LOGGER.error("加載配置文件失敗", e);
                createDefaultConfig();
            }
        } else {
            createDefaultConfig();
        }
    }
    
    private int getConfigVersion(Map<String, Object> config) {
        Object version = config.get("version");
        if (version instanceof Integer) {
            return (Integer) version;
        }
        // 如果沒有版本號，認為是版本 1
        return 1;
    }
    
    private Map<String, Object> migrateConfig(Map<String, Object> oldConfig, int fromVersion) {
        Map<String, Object> newConfig = createDefaultConfigData();
        
        LOGGER.info("開始從版本 {} 遷移配置到版本 {}", fromVersion, CURRENT_CONFIG_VERSION);
        
        // 遷移舊配置值到新格式
        if (fromVersion == 1) {
            migrateFromV1ToV2(oldConfig, newConfig);
        }
        
        LOGGER.info("配置遷移完成，已保留所有自定義設置");
        return newConfig;
    }
    
    @SuppressWarnings("unchecked")
    private void migrateFromV1ToV2(Map<String, Object> oldConfig, Map<String, Object> newConfig) {
        // 遷移資料庫配置
        migrateSection(oldConfig, newConfig, "database");
        
        // 遷移同步配置
        migrateSection(oldConfig, newConfig, "sync");
        
        // 遷移兼容性配置
        migrateSection(oldConfig, newConfig, "compatibility");
        
        // 記錄遷移的設置數量
        int migratedSettings = 0;
        for (String section : new String[]{"database", "sync", "compatibility"}) {
            if (oldConfig.containsKey(section)) {
                Map<String, Object> oldSection = (Map<String, Object>) oldConfig.get(section);
                migratedSettings += oldSection.size();
            }
        }
        
        LOGGER.info("已遷移 {} 個配置設置", migratedSettings);
    }
    
    @SuppressWarnings("unchecked")
    private void migrateSection(Map<String, Object> oldConfig, Map<String, Object> newConfig, String sectionName) {
        if (oldConfig.containsKey(sectionName) && newConfig.containsKey(sectionName)) {
            Map<String, Object> oldSection = (Map<String, Object>) oldConfig.get(sectionName);
            Map<String, Object> newSection = (Map<String, Object>) newConfig.get(sectionName);
            
            for (Map.Entry<String, Object> entry : oldSection.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // 只遷移新配置中存在的鍵，避免過時的配置項
                if (newSection.containsKey(key)) {
                    newSection.put(key, value);
                    LOGGER.debug("遷移配置: {}.{} = {}", sectionName, key, value);
                } else {
                    LOGGER.debug("跳過已廢棄的配置: {}.{}", sectionName, key);
                }
            }
        }
    }
    
    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                // 寫入配置文件頭部註釋
                writer.write("# Chococar's Inventory Bridge Configuration\n");
                writer.write("# Configuration version: " + CURRENT_CONFIG_VERSION + "\n");
                writer.write("# Do not modify the version number manually!\n");
                writer.write("\n");
                
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
    
    private Map<String, Object> createDefaultConfigData() {
        Map<String, Object> config = new LinkedHashMap<>();
        
        // Version identifier
        config.put("version", CURRENT_CONFIG_VERSION);
        
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
        sync.put("syncOnJoin", false); // 默認關閉自動同步，改為手動
        sync.put("syncOnLeave", false); // 默認關閉自動同步，改為手動
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