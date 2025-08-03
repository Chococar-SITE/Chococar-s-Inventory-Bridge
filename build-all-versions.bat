@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ╔══════════════════════════════════════════════════════════════╗
echo ║            Chococar's Inventory Bridge 多版本構建            ║
echo ║                    本地測試構建腳本                         ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

set VERSIONS=1.21.4 1.21.5 1.21.6 1.21.7 1.21.8
set BUILD_DIR=builds
set ORIGINAL_DIR=%CD%

:: 創建構建輸出目錄
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
mkdir "%BUILD_DIR%"

echo 🏗️ 開始多版本構建...
echo.

for %%v in (%VERSIONS%) do (
    echo ═══════════════════════════════════════════════════════════════
    echo 🔨 正在構建 Minecraft %%v...
    echo ═══════════════════════════════════════════════════════════════
    
    :: 更新版本
    call scripts\update-version.bat %%v
    if errorlevel 1 (
        echo ❌ 版本更新失敗: %%v
        goto :error
    )
    
    :: 清理之前的構建
    echo 🧹 清理之前的構建...
    call gradlew clean > nul 2>&1
    
    :: 構建 Fabric 模組
    echo 🎯 構建 Fabric 模組...
    call gradlew :fabric:build
    if errorlevel 1 (
        echo ❌ Fabric 構建失敗: %%v
        goto :error
    )
    
    :: 構建 Paper 插件
    echo 🎯 構建 Paper 插件...
    call gradlew :paper:build
    if errorlevel 1 (
        echo ❌ Paper 構建失敗: %%v
        goto :error
    )
    
    :: 複製構建文件
    echo 📦 複製構建文件...
    mkdir "%BUILD_DIR%\%%v"
    mkdir "%BUILD_DIR%\%%v\fabric"
    mkdir "%BUILD_DIR%\%%v\paper"
    
    :: 複製 Fabric JAR
    for %%f in (fabric\build\libs\*.jar) do (
        if not "%%~nf"=="%%~nf-dev" if not "%%~nf"=="%%~nf-sources" (
            copy "%%f" "%BUILD_DIR%\%%v\fabric\chococars-inventory-bridge-fabric-%%v.jar" > nul
            echo ✅ Fabric JAR: chococars-inventory-bridge-fabric-%%v.jar
        )
    )
    
    :: 複製 Paper JAR
    for %%f in (paper\build\libs\*.jar) do (
        if not "%%~nf"=="%%~nf-dev" if not "%%~nf"=="%%~nf-sources" (
            copy "%%f" "%BUILD_DIR%\%%v\paper\chococars-inventory-bridge-paper-%%v.jar" > nul
            echo ✅ Paper JAR: chococars-inventory-bridge-paper-%%v.jar
        )
    )
    
    echo ✅ Minecraft %%v 構建完成
    echo.
)

echo ╔══════════════════════════════════════════════════════════════╗
echo ║                    🎉 構建完成！                           ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.
echo 📊 構建摘要：
echo    支援版本: %VERSIONS%
echo    輸出目錄: %BUILD_DIR%\
echo.
echo 📁 文件結構：
for %%v in (%VERSIONS%) do (
    echo    %BUILD_DIR%\%%v\
    echo    ├── fabric\chococars-inventory-bridge-fabric-%%v.jar
    echo    └── paper\chococars-inventory-bridge-paper-%%v.jar
)
echo.
echo 🚀 所有版本構建成功！
goto :end

:error
echo.
echo ❌ 構建過程中發生錯誤！
echo 請檢查上面的錯誤訊息。
exit /b 1

:end
echo.
echo 按任意鍵退出...
pause > nul