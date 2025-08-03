# Chococar's Inventory Bridge（巧克車的背包橋接）

一個全面的 Minecraft 1.21.X 背包同步解決方案，支援 Fabric 模組和 Paper 插件環境。此系統讓玩家能夠使用 MySQL 資料庫存儲在多個伺服器間保持背包同步，並具有完整的版本兼容性。

## 功能特色

- **跨伺服器背包同步**：在多個 Minecraft 伺服器間同步玩家背包
- **版本兼容性**：完全支援 Minecraft 1.21.X 版本（1.21 - 1.21.8）
- **物品保存**：保持物品數據包括附魔、自定義模型數據和元數據
- **束包支援**：處理 1.21.2+ 版本引入的束包物品
- **向下兼容**：自動將較新物品轉換為舊版本的兼容替代品
- **MySQL 資料庫**：穩固的資料庫存儲與連接池
- **即時同步**：在玩家加入/離開時自動同步，具可配置選項
- **手動控制**：管理員指令進行手動同步操作
- **全面記錄**：詳細的同步日誌和錯誤追蹤

## 支援的 Minecraft 版本

- **Minecraft 1.21.8**（完整支援）
- 僅限 Java 版
- 需要 Java 21+

![Build Status](https://github.com/chococar-site/inventory-bridge/workflows/Build%20Multi-Version%20JAR%20Files/badge.svg)
![Release](https://img.shields.io/github/v/release/chococar-site/inventory-bridge)
![Downloads](https://img.shields.io/github/downloads/chococar-site/inventory-bridge/total)

## 系統需求

### Fabric 模組
- Fabric Loader 0.16.9+
- Fabric API 0.130.0+
- Minecraft 1.21.8

### Paper 插件
- Paper 1.21.8+
- Java 21+

### 資料庫
- MySQL 8.0+ 或 MariaDB 10.5+

## 下載與安裝

### 📥 下載
從 [GitHub Releases](https://github.com/chococar-site/inventory-bridge/releases) 頁面下載對應您的 Minecraft 版本和平台的 JAR 文件：

- `chococars-inventory-bridge-fabric-{version}.jar` - Fabric 模組
- `chococars-inventory-bridge-paper-{version}.jar` - Paper 插件

### 安裝說明

#### Fabric 模組
1. 安裝 Fabric Loader 和 Fabric API
2. 下載對應版本的 Fabric 模組 JAR 文件
3. 將其放入 `mods` 資料夾
4. 在 `config/chococars_inventory_bridge.yml` 中配置資料庫設定

#### Paper 插件
1. 下載對應版本的 Paper 插件 JAR 文件
2. 將其放入 `plugins` 資料夾
3. 啟動伺服器以生成配置文件
4. 在 `plugins/ChococarsInventoryBridge/config.yml` 中配置資料庫設定

### 🔄 自動化構建
本項目使用 GitHub Actions 自動構建所有支援版本的 JAR 文件。每次發布都包含：
- 1 個 Minecraft 版本 (1.21.8)
- 2 個平台 (Fabric + Paper)
- 總計 2 個 JAR 文件

### 🤖 自動版本更新系統
項目配備了智能版本檢測系統：
- **自動檢測**: 每週自動檢查 Minecraft、Fabric API、Yarn Mappings、Paper API 的最新版本
- **智能更新**: 自動更新 GitHub Actions、配置文件和建置腳本
- **通知機制**: 發現新版本時自動創建 GitHub Issue
- **多平台腳本**: 提供 Python、Node.js、PowerShell 版本檢查工具

詳細說明請參閱：[自動版本系統文檔](docs/AUTO_VERSION_SYSTEM.md)

## 配置說明

### 資料庫配置
```yaml
database:
  host: "localhost"
  port: 3306
  database: "inventory_bridge"
  username: "minecraft"
  password: "password"
  tablePrefix: "ib_"
  maxPoolSize: 10
  connectionTimeout: 30000
  useSSL: false
```

### 同步設定
```yaml
sync:
  enableAutoSync: true
  syncIntervalTicks: 200
  syncOnJoin: true
  syncOnLeave: true
  syncEnderChest: true
  syncExperience: true
  syncHealth: false
  syncHunger: false
  serverId: "server1"
```

### 版本兼容性設定
```yaml
compatibility:
  enableLegacySupport: true
  convertOldItems: true
  preserveCustomModelData: true
  handleBundles: true
  handleCopperOxidation: true
  handleResinItems: true
  minecraftVersion: "1.21.8"
```

## 指令

### Paper 插件指令
- `/inventorybridge reload` - 重新載入配置
- `/inventorybridge sync <load|save>` - 手動同步背包
- `/inventorybridge status` - 檢查同步狀態
- `/inventorybridge info` - 顯示插件資訊

別名：`/ib`、`/invbridge`

## 權限

- `chococars.inventorybridge.admin` - 完全訪問所有指令
- `chococars.inventorybridge.reload` - 重新載入配置
- `chococars.inventorybridge.sync` - 手動背包同步

## 版本兼容性說明

### 1.21.5+ 功能
- **新物品組件**：支援 `blocks_attacks`、`break_sound`、`potion_duration_scale`、`tooltip_display`
- **增強型生怪蛋**：每種生物都有獨特的視覺設計
- **改進的工具提示**：弩顯示所有裝填的彈藥，煙火使用緊湊表示

### 1.21.6+ 功能
- **乾燥惡魂方塊**：可以水化並生成小惡魂
- **韁繩**：16 種顏色變體，可裝備在快樂惡魂上
- **音樂唱片「Tears」**：通過玩家反彈惡魂火球擊殺惡魂獲得

### 1.21.7+ 功能
- **音樂唱片「Lava Chicken」**：通過擊殺騎雞的殭屍寶寶獲得
- **新畫作「Dennis」**：由 Sarah Boeving 創作

### 向下兼容
- 在舊版本中不可用的物品會自動轉換為兼容的替代品
- 在 1.21.6 之前的版本中，乾燥惡魂方塊會變成靈魂沙，韁繩會變成皮革
- 在 1.21.7 之前的版本中，新音樂唱片會變成 13 號唱片

## 資料庫結構

插件會創建以下資料表：
- `ib_inventories` - 玩家背包數據
- `ib_version_mappings` - 物品版本兼容性映射
- `ib_sync_log` - 同步事件日誌

## 效能考量

- 使用 HikariCP 連接池以獲得最佳資料庫效能
- 異步同步操作以防止伺服器延遲
- 可配置的同步間隔以平衡效能和數據一致性
- 高效的 JSON 序列化背包數據

## 故障排除

### 常見問題
1. **資料庫連接失敗**：檢查 MySQL 憑證和伺服器連接性
2. **物品未同步**：驗證伺服器間的伺服器 ID 是否匹配
3. **版本兼容性**：確保所有伺服器使用兼容的 Minecraft 版本

### 日誌記錄
- Fabric 模組日誌記錄到標準 Minecraft 日誌
- Paper 插件日誌記錄到伺服器控制台，前綴為 `[ChococarsInventoryBridge]`
- 資料庫操作會記錄詳細的錯誤訊息

## 開發者 API

插件為開發者提供事件和 API 以整合背包同步：

### Fabric 模組 API
```java
ChococarsInventoryBridgeFabric.getInstance().getSyncManager().manualSync(player, true);
```

### Paper 插件 API
```java
ChococarsInventoryBridgePaper.getInstance().getSyncManager().manualSync(player, true);
```

## 項目結構

```
chococars-inventory-bridge/
├── common/          # 共通程式碼模組
├── fabric/          # Fabric 模組實現
├── paper/           # Paper 插件實現
├── build.gradle     # 主建置腳本
└── settings.gradle  # 項目設定
```

## 授權

此項目使用 MIT 授權 - 詳見 [LICENSE](LICENSE) 文件。

## 支援

如需支援、錯誤報告或功能請求：
- 訪問我們的網站：https://chococar.site
- 在 GitHub 上創建 issue
- 聯繫：chococar

## 更新日誌

### 版本 1.0.0
- 初始發布
- 完整的 Minecraft 1.21.4~8 支援
- 跨伺服器背包同步
- 版本兼容性系統
- 束包和新物品支援
- MySQL 資料庫整合
- 全面的配置選項

## 致謝

感謝 Minecraft 模組和插件社群的持續支援，以及所有為此項目做出貢獻的開發者。