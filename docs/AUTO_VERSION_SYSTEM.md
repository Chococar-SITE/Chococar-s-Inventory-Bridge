# 自動版本獲取系統

本文檔說明如何使用 Chococar's Inventory Bridge 的自動版本獲取和更新系統。

## 系統概覽

我們的自動版本系統可以：
- 🔍 自動檢測 Minecraft 1.21.X 的最新版本
- 📡 獲取對應的 Fabric API、Yarn Mappings、Paper API 版本
- 🔄 自動更新 GitHub Actions 工作流程
- 📝 更新配置文件和腳本
- 🤖 創建 GitHub Issues 通知新版本

## 可用的 API 源

### Minecraft 版本
- **Mojang 官方 API**: `https://piston-meta.mojang.com/mc/game/version_manifest.json`
- 獲取所有正式發布版本和數據版本號

### Fabric 生態系統
- **Fabric Meta API**: `https://meta.fabricmc.net/v2/versions/`
- **Yarn Mappings**: `https://meta.fabricmc.net/v2/versions/yarn/{version}`
- **Modrinth API**: `https://api.modrinth.com/v2/project/fabric-api/version`

### Paper 生態系統  
- **Paper API**: `https://api.papermc.io/v2/projects/paper`
- **版本構建**: `https://api.papermc.io/v2/projects/paper/versions/{version}`

## 使用方法

### 1. 自動檢查 (GitHub Actions)

#### 每週自動檢查
系統會在每週一早上 8:00 UTC 自動檢查新版本：

```yaml
schedule:
  - cron: '0 8 * * 1'  # 每週一早上 8:00 UTC
```

#### 手動觸發檢查
1. 前往 GitHub Actions 頁面
2. 選擇 "Auto Update Versions" 工作流程
3. 點擊 "Run workflow"
4. 選擇選項：
   - **強制更新版本**: 即使沒有新版本也強制更新
   - **指定版本**: 只檢查特定版本 (如: `1.21.6 1.21.7`)

#### 快速版本檢查
使用 "Quick Version Check" 工作流程進行快速檢查：
- **current**: 顯示當前配置的版本
- **latest**: 檢查最新可用版本
- **specific**: 檢查指定版本

### 2. 本地腳本

#### Python 腳本 (推薦)
```bash
# 檢查最新版本
python scripts/fetch-latest-versions.py

# 檢查指定版本
python scripts/fetch-latest-versions.py --versions 1.21.6 1.21.7

# 生成 gradle.properties
python scripts/fetch-latest-versions.py --output gradle --save gradle.properties

# 生成工作流程配置
python scripts/fetch-latest-versions.py --output workflow
```

#### Node.js 腳本 (備用)
```bash
# 檢查所有版本
node scripts/check-versions.js

# 檢查指定版本
node scripts/check-versions.js 1.21.6 1.21.7

# 生成 gradle.properties
node scripts/check-versions.js --gradle --save gradle.properties
```

#### PowerShell 腳本 (Windows)
```powershell
# 顯示當前配置
.\scripts\check-versions.ps1 -ShowCurrent

# 檢查最新版本
.\scripts\check-versions.ps1

# 檢查指定版本並更新 gradle.properties
.\scripts\check-versions.ps1 -Versions "1.21.6","1.21.7" -UpdateGradle

# 保存結果到文件
.\scripts\check-versions.ps1 -SaveTo "versions.json"
```

## 自動更新流程

### 1. 版本檢測
- 📡 從各個 API 獲取最新版本信息
- 🔍 比較當前配置與最新版本
- 📊 生成差異報告

### 2. 文件更新
如果發現新版本，系統會自動更新：

#### GitHub Actions 工作流程
- 更新 `.github/workflows/build.yml` 中的版本矩陣
- 更新版本配置的 case 語句

#### 配置文件  
- 更新 `gradle.properties` 預設版本
- 更新 YAML 配置文件中的版本號

#### 腳本文件
- 更新 `scripts/update-version.sh` 和 `.bat`
- 添加新版本的支援

### 3. 提交變更
- 🤖 自動提交變更到 repository
- 📝 生成描述性的提交訊息
- 🏷️ 包含更新的版本信息

### 4. 通知機制
- 📋 創建 GitHub Issue 通知新版本
- ✅ 包含檢查清單供手動驗證
- 🔗 提供相關連結

## 版本映射規則

### 自動版本推斷
系統使用以下規則推斷版本：

```javascript
// Yarn Mappings: 通常是 {mc_version}+build.X
"1.21.8" → "1.21.8+build.1"
"1.21.4" → "1.21.4+build.8"

// Fabric API: 有特定的版本號模式
"1.21.8" → "0.130.0+1.21.8"

// Paper: 固定格式
"1.21.8" → "1.21.8"

// Data Version: 手動維護的映射表
"1.21.8" → 4082
```

### 已知版本映射
```json
{
  "1.21.4": {
    "yarn": "1.21.4+build.8",
    "fabric_api": "0.119.0+1.21.4", 
    "paper": "1.21.4",
    "data_version": 4080
  },
  "1.21.8": {
    "yarn": "1.21.8+build.1",
    "fabric_api": "0.130.0+1.21.8",
    "paper": "1.21.8", 
    "data_version": 4082
  }
}
```

## 錯誤處理

### API 失敗處理
- 🔄 自動重試機制
- ⏰ 設定合理的超時時間
- 🛡️ 降級到已知版本映射

### 版本不完整處理
- ⚠️ 標記為 `partial` 狀態
- 📝 記錄缺失的依賴
- 🚫 不包含在自動更新中

### 網路問題處理
- 🔌 檢測網路連接
- 📋 使用本地快取 (如果可用)
- 💾 保存最後已知的良好狀態

## 配置選項

### 環境變數
```bash
# 自定義 User-Agent
export USER_AGENT="YourProject/1.0.0"

# API 超時設定 (秒)
export API_TIMEOUT=15

# 最大重試次數
export MAX_RETRIES=3
```

### 腳本參數
```bash
# Python 腳本
--versions        # 指定版本列表
--output         # 輸出格式 (json|workflow|gradle|script)
--save          # 保存到文件
--timeout       # API 超時時間

# PowerShell 腳本  
-Versions       # 指定版本陣列
-UpdateGradle   # 自動更新 gradle.properties
-ShowCurrent    # 顯示當前配置
-SaveTo         # 保存路徑
```

## 監控和通知

### GitHub Issues
新版本檢測後會自動創建 Issue：
- 📋 包含新版本列表
- ✅ 提供驗證檢查清單
- 🔗 相關工作流程連結
- 🏷️ 自動添加標籤

### Actions 摘要
每次運行都會生成摘要：
- 📊 版本比較結果
- 🔄 更新的文件列表
- ⚠️ 需要手動檢查的項目

## 最佳實踐

### 定期檢查
- 🗓️ 設定每週自動檢查
- 📱 關注 GitHub 通知
- 🔍 定期手動驗證

### 測試新版本
- 🧪 在測試環境先驗證
- 📋 檢查相容性問題
- 🔧 更新兼容性映射

### 維護版本映射
- 📝 更新已知版本映射
- 🛠️ 調整推斷規則
- 📚 更新文檔

## 故障排除

### 常見問題

1. **API 請求失敗**
   ```bash
   # 檢查網路連接
   curl -I https://piston-meta.mojang.com/mc/game/version_manifest.json
   
   # 檢查用戶代理
   curl -H "User-Agent: YourProject/1.0.0" https://api.modrinth.com/v2/project/fabric-api/version
   ```

2. **版本映射錯誤**
   ```bash
   # 手動檢查特定版本
   python scripts/fetch-latest-versions.py --versions 1.21.6
   
   # 驗證 API 回應
   curl "https://meta.fabricmc.net/v2/versions/yarn/1.21.6"
   ```

3. **工作流程更新失敗**
   ```bash
   # 檢查權限
   git status
   git diff
   
   # 手動提交
   git add .
   git commit -m "手動更新版本"
   ```

### 調試模式
```bash
# 啟用詳細輸出
python scripts/fetch-latest-versions.py --verbose

# 保存調試信息
node scripts/check-versions.js --debug > debug.log 2>&1
```

## 擴展系統

### 添加新的 API 源
```python
def get_custom_api_version(mc_version):
    try:
        response = requests.get(f"https://api.example.com/versions/{mc_version}")
        return response.json()['version']
    except Exception as e:
        logger.warning(f"Custom API failed: {e}")
        return None
```

### 自定義版本規則
```javascript
// 添加到版本映射邏輯
const customVersionMap = {
    "1.21.9": {
        "fabric_api": "0.111.0+1.21.9",
        "data_version": 4083
    }
};
```

這個系統設計為可擴展和可維護的，當新的 Minecraft 版本發布時，它會自動檢測並更新所有相關配置。