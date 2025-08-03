@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

:: Chococar's Inventory Bridge 版本更新腳本
:: 用於本地開發時切換不同的 Minecraft 版本

set MC_VERSION=%1
if "%MC_VERSION%"=="" set MC_VERSION=1.21.8

if "%MC_VERSION%"=="1.21.4" (
    set YARN_VERSION=1.21.4+build.1
    set FABRIC_API_VERSION=0.108.0+1.21.4
    set PAPER_VERSION=1.21.4-R0.1-SNAPSHOT
    set DATA_VERSION=4080
) else if "%MC_VERSION%"=="1.21.5" (
    set YARN_VERSION=1.21.5+build.1
    set FABRIC_API_VERSION=0.109.0+1.21.5
    set PAPER_VERSION=1.21.5-R0.1-SNAPSHOT
    set DATA_VERSION=4081
) else if "%MC_VERSION%"=="1.21.6" (
    set YARN_VERSION=1.21.6+build.1
    set FABRIC_API_VERSION=0.109.5+1.21.6
    set PAPER_VERSION=1.21.6-R0.1-SNAPSHOT
    set DATA_VERSION=4081
) else if "%MC_VERSION%"=="1.21.7" (
    set YARN_VERSION=1.21.7+build.1
    set FABRIC_API_VERSION=0.110.0+1.21.7
    set PAPER_VERSION=1.21.7-R0.1-SNAPSHOT
    set DATA_VERSION=4081
) else if "%MC_VERSION%"=="1.21.8" (
    set YARN_VERSION=1.21.8+build.1
    set FABRIC_API_VERSION=0.110.5+1.21.8
    set PAPER_VERSION=1.21.8-R0.1-SNAPSHOT
    set DATA_VERSION=4082
) else (
    echo ❌ 不支援的版本: %MC_VERSION%
    echo 支援的版本: 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8
    exit /b 1
)

echo 🔧 正在更新到 Minecraft %MC_VERSION%...

:: 更新 gradle.properties
echo # Done to increase the memory available to gradle. > gradle.properties
echo org.gradle.jvmargs=-Xmx4G >> gradle.properties
echo. >> gradle.properties
echo # Fabric Properties >> gradle.properties
echo minecraft_version=%MC_VERSION% >> gradle.properties
echo yarn_mappings=%YARN_VERSION% >> gradle.properties
echo loader_version=0.16.9 >> gradle.properties
echo. >> gradle.properties
echo # Mod Properties >> gradle.properties
echo mod_version=1.0.0-SNAPSHOT >> gradle.properties
echo maven_group=site.chococar >> gradle.properties
echo archives_base_name=chococars-inventory-bridge >> gradle.properties
echo. >> gradle.properties
echo # Dependencies >> gradle.properties
echo fabric_version=%FABRIC_API_VERSION% >> gradle.properties
echo paper_version=%PAPER_VERSION% >> gradle.properties
echo data_version=%DATA_VERSION% >> gradle.properties
echo. >> gradle.properties
echo # CI/CD Properties >> gradle.properties
echo ci_build=false >> gradle.properties

echo 📝 更新配置文件...
:: 更新配置文件中的版本號
powershell -Command "(Get-Content -Path 'fabric\src\main\resources\chococars_inventory_bridge.yml') -replace 'minecraftVersion: \".*\"', 'minecraftVersion: \"%MC_VERSION%\"' | Set-Content -Path 'fabric\src\main\resources\chococars_inventory_bridge.yml'"
powershell -Command "(Get-Content -Path 'paper\src\main\resources\config.yml') -replace 'minecraftVersion: \".*\"', 'minecraftVersion: \"%MC_VERSION%\"' | Set-Content -Path 'paper\src\main\resources\config.yml'"

echo ✅ 版本更新完成！
echo 📋 當前配置：
echo    Minecraft: %MC_VERSION%
echo    Yarn: %YARN_VERSION%
echo    Fabric API: %FABRIC_API_VERSION%
echo    Paper: %PAPER_VERSION%
echo    Data Version: %DATA_VERSION%
echo.
echo 🚀 現在可以運行 'gradlew build' 來構建專案
pause