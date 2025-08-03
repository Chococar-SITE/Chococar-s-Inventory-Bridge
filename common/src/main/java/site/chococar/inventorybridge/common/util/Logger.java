package site.chococar.inventorybridge.common.util;

/**
 * 抽象日誌介面，讓不同平台實現自己的日誌邏輯
 */
public interface Logger {
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);
    void debug(String message);
}