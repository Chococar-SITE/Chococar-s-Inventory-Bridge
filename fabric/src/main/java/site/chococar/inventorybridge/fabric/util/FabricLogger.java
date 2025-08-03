package site.chococar.inventorybridge.fabric.util;

import org.slf4j.LoggerFactory;
import site.chococar.inventorybridge.common.util.Logger;

/**
 * Fabric 平台的日誌實現
 */
public class FabricLogger implements Logger {
    private final org.slf4j.Logger logger;
    
    public FabricLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }
    
    @Override
    public void info(String message) {
        logger.info(message);
    }
    
    @Override
    public void warn(String message) {
        logger.warn(message);
    }
    
    @Override
    public void error(String message) {
        logger.error(message);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    @Override
    public void debug(String message) {
        logger.debug(message);
    }
}