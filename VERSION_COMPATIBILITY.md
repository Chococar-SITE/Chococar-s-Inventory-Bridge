# 版本兼容性參考

本文檔詳細列出了 Chococar's Inventory Bridge 如何處理 Minecraft 1.21.X 系列各版本間的物品兼容性。

## 版本更新摘要

### 1.21.4 (基礎版本)
**重要新增：淺橡木和樹脂**
- 所有淺橡木木材變體（原木、木板、階梯、半磚等）
- 樹脂物品（樹脂塊、樹脂磚）
- 嘎嘎怪相關物品（嘎嘎怪之心、嘎嘎怪生怪蛋）
- 新物品模型定義格式

### 1.21.5
**重要新增：物品組件系統**
- 新物品組件：`blocks_attacks`、`break_sound`、`potion_duration_scale`、`tooltip_display`
- 增強型生怪蛋（每種生物獨特視覺）
- 改進的工具提示系統

### 1.21.6
**重要新增：惡魂相關內容**
- 乾燥惡魂方塊（可水化並生成小惡魂）
- 16 種顏色韁繩（可裝備在快樂惡魂上）
- 音樂唱片「Tears」
- 對話系統和路標系統

### 1.21.7
**重要新增：新音樂和畫作**
- 音樂唱片「Lava Chicken」
- 新畫作「Dennis」
- 數據包和資源包格式版本更新

### 1.21.8
**最新穩定版本**
- 主要針對圖形和穩定性問題的修復
- 特別是 Intel 整合顯卡的問題
- 與 1.21.7 伺服器兼容
- 目前支援的最新版本

## 物品轉換映射表

### 惡魂相關物品 (1.21.6+)
| 原始物品 | 向下兼容替代 |
|---------|-------------|
| `minecraft:dried_ghast` | `minecraft:soul_sand` |
| `minecraft:*_harness` (16種顏色) | `minecraft:leather` |
| `minecraft:music_disc_tears` | `minecraft:music_disc_13` |

### 新音樂唱片 (1.21.7+)
| 原始物品 | 向下兼容替代 |
|---------|-------------|
| `minecraft:music_disc_lava_chicken` | `minecraft:music_disc_13` |

## 版本檢查邏輯

系統會根據目標版本自動判斷是否需要轉換物品：

1. **1.21.2 之前**：束包物品不可用
2. **1.21.4 之前**：樹脂、淺橡木、嘎嘎怪物品不可用
3. **1.21.6 之前**：惡魂相關物品不可用
4. **1.21.7 之前**：新音樂唱片不可用

## 配置選項

在配置文件中，您可以控制各類物品的處理：

```yaml
compatibility:
  enableLegacySupport: true          # 啟用舊版支援
  convertOldItems: true              # 轉換舊物品
  preserveCustomModelData: true      # 保留自定義模型數據
  handleBundles: true                # 處理束包物品
  handleCopperOxidation: true        # 處理銅製品氧化
  handleResinItems: true             # 處理樹脂物品
  handleGhastItems: true             # 處理惡魂相關物品
  handleNewMusicDiscs: true          # 處理新音樂唱片
```

## 注意事項

1. **數據版本**：每個版本都有對應的數據版本號，系統會記錄並處理
2. **組件系統**：1.21.5+ 引入的新組件系統需要特殊處理
3. **格式更新**：1.21.7 的數據包和資源包格式更新可能影響自定義內容
4. **伺服器兼容性**：某些版本間不完全兼容，需要注意版本匹配

這個兼容性系統確保玩家在不同版本的伺服器間移動時，背包內容都能正確保存和載入。