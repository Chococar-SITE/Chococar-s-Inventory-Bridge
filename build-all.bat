@echo off
chcp 65001 > nul
echo 正在建置 Chococar's Inventory Bridge...
echo.

echo 清理舊的建置文件...
call gradlew clean

echo.
echo 建置共通模組...
call gradlew :common:build

echo.
echo 建置 Fabric 模組...
call gradlew :fabric:build

echo.
echo 建置 Paper 插件...
call gradlew :paper:build

echo.
echo 建置完成！
echo.
echo 輸出文件位置：
echo - Fabric 模組: fabric\build\libs\
echo - Paper 插件: paper\build\libs\
echo.
pause