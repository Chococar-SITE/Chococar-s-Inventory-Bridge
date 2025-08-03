# GitHub Actions 工作流程說明

本項目使用 GitHub Actions 自動構建多個 Minecraft 版本的 Fabric 模組和 Paper 插件。

## 工作流程概覽

### 1. Build Workflow (`build.yml`)
**觸發條件：**
- 推送到 `main` 或 `develop` 分支
- 創建 Pull Request 到 `main` 分支
- 創建版本標籤 (格式: `v*`)
- 手動觸發

**功能：**
- 為 Minecraft 1.21.4-1.21.8 的每個版本構建 Fabric 和 Paper JAR
- 總共產生 10 個 JAR 文件 (5個版本 × 2個平台)
- 自動上傳構建產物
- 在創建標籤時自動創建 GitHub Release

### 2. Release Workflow (`release.yml`)
**觸發條件：**
- 手動觸發，需要指定版本號

**功能：**
- 創建並推送版本標籤
- 觸發構建工作流程
- 自動創建 GitHub Release

## 支援的版本矩陣

| Minecraft 版本 | Yarn Mappings | Fabric API | Paper API | Data Version |
|---------------|---------------|------------|-----------|--------------|
| 1.21.4 | 1.21.4+build.1 | 0.108.0+1.21.4 | 1.21.4-R0.1-SNAPSHOT | 4080 |
| 1.21.5 | 1.21.5+build.1 | 0.109.0+1.21.5 | 1.21.5-R0.1-SNAPSHOT | 4081 |
| 1.21.6 | 1.21.6+build.1 | 0.109.5+1.21.6 | 1.21.6-R0.1-SNAPSHOT | 4081 |
| 1.21.7 | 1.21.7+build.1 | 0.110.0+1.21.7 | 1.21.7-R0.1-SNAPSHOT | 4081 |
| 1.21.8 | 1.21.8+build.1 | 0.110.5+1.21.8 | 1.21.8-R0.1-SNAPSHOT | 4082 |

## 使用方法

### 自動構建
推送代碼到 `main` 分支即可觸發自動構建：
```bash
git push origin main
```

### 創建正式版本
1. 前往 GitHub 的 Actions 頁面
2. 選擇 "Create Release" 工作流程
3. 點擊 "Run workflow"
4. 輸入版本號 (例如: `1.0.0`)
5. 選擇是否為預發布版本
6. 點擊 "Run workflow"

### 手動觸發構建
1. 前往 GitHub 的 Actions 頁面
2. 選擇 "Build Multi-Version JAR Files" 工作流程
3. 點擊 "Run workflow"
4. 選擇發布類型 (`snapshot` 或 `release`)
5. 點擊 "Run workflow"

## 構建產物

每次成功構建後，將產生以下文件：

```
artifacts/
├── chococars-inventory-bridge-fabric-1.21.4-{version}/
│   └── chococars-inventory-bridge-fabric-1.21.4-{version}.jar
├── chococar-inventory-bridge-paper-1.21.4-{version}/
│   └── chococars-inventory-bridge-paper-1.21.4-{version}.jar
├── chococars-inventory-bridge-fabric-1.21.5-{version}/
│   └── chococars-inventory-bridge-fabric-1.21.5-{version}.jar
├── chococars-inventory-bridge-paper-1.21.5-{version}/
│   └── chococars-inventory-bridge-paper-1.21.5-{version}.jar
...以此類推
```

## 版本命名規則

- **開發版本**: `1.0.0-SNAPSHOT+mc{minecraft_version}`
- **正式版本**: `{tag_version}` (例如: `1.0.0`)

## 環境變數

工作流程會動態設定以下環境變數：

- `MC_VERSION`: Minecraft 版本
- `YARN_VERSION`: Yarn mappings 版本
- `FABRIC_API_VERSION`: Fabric API 版本
- `PAPER_VERSION`: Paper API 版本
- `DATA_VERSION`: 數據版本號
- `VERSION`: 最終版本號
- `ARTIFACT_NAME`: 構建產物名稱

## 故障排除

### 常見問題

1. **構建失敗 - 依賴解析錯誤**
   - 檢查 `build.yml` 中的版本映射是否正確
   - 確認對應版本的依賴是否可用

2. **JAR 文件未生成**
   - 檢查 Gradle 構建腳本
   - 查看構建日誌中的錯誤訊息

3. **版本號不正確**
   - 確認 `gradle.properties` 中的版本設定
   - 檢查環境變數設定

### 調試建議

1. 在本地使用 `scripts/update-version.bat` 測試版本切換
2. 使用 `build-all-versions.bat` 進行本地多版本構建測試
3. 檢查 Actions 頁面的詳細日誌

## 本地開發

### 切換版本
```bash
# Windows
scripts\update-version.bat 1.21.6

# Linux/macOS  
./scripts/update-version.sh 1.21.6
```

### 本地多版本構建
```bash
# Windows
build-all-versions.bat

# Linux/macOS
./build-all-versions.sh
```

## 貢獻

當添加新的 Minecraft 版本支援時：

1. 更新 `build.yml` 中的版本矩陣
2. 添加對應的依賴版本映射
3. 更新 `scripts/update-version.*` 腳本
4. 測試本地構建
5. 提交 Pull Request