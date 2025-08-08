package site.chococar.inventorybridge.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 簡單測試來驗證測試環境
 */
class SimpleTest {
    
    @Test
    @DisplayName("基本測試 - 驗證 JUnit 5 工作正常")
    void testBasicAssertion() {
        assertTrue(true);
        assertEquals(1 + 1, 2);
        assertNotNull("hello");
    }
    
    @Test
    @DisplayName("字符串測試")
    void testStringOperations() {
        String test = "Fabric Test";
        assertEquals(11, test.length());
        assertTrue(test.contains("Fabric"));
    }
}