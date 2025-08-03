#!/bin/bash

# Chococar's Inventory Bridge 版本更新腳本
# 用於本地開發時切換不同的 Minecraft 版本

set -e

MC_VERSION=${1:-"1.21.8"}

case "$MC_VERSION" in
  "1.21.4")
    YARN_VERSION="1.21.4+build.1"
    FABRIC_API_VERSION="0.108.0+1.21.4"
    PAPER_VERSION="1.21.4-R0.1-SNAPSHOT"
    DATA_VERSION="4080"
    ;;
  "1.21.5")
    YARN_VERSION="1.21.5+build.1"
    FABRIC_API_VERSION="0.109.0+1.21.5"
    PAPER_VERSION="1.21.5-R0.1-SNAPSHOT"
    DATA_VERSION="4081"
    ;;
  "1.21.6")
    YARN_VERSION="1.21.6+build.1"
    FABRIC_API_VERSION="0.109.5+1.21.6"
    PAPER_VERSION="1.21.6-R0.1-SNAPSHOT"
    DATA_VERSION="4081"
    ;;
  "1.21.7")
    YARN_VERSION="1.21.7+build.1"
    FABRIC_API_VERSION="0.110.0+1.21.7"
    PAPER_VERSION="1.21.7-R0.1-SNAPSHOT"
    DATA_VERSION="4081"
    ;;
  "1.21.8")
    YARN_VERSION="1.21.8+build.1"
    FABRIC_API_VERSION="0.110.5+1.21.8"
    PAPER_VERSION="1.21.8-R0.1-SNAPSHOT"
    DATA_VERSION="4082"
    ;;
  *)
    echo "❌ 不支援的版本: $MC_VERSION"
    echo "支援的版本: 1.21.4, 1.21.5, 1.21.6, 1.21.7, 1.21.8"
    exit 1
    ;;
esac

echo "🔧 正在更新到 Minecraft $MC_VERSION..."

# 更新 gradle.properties
cat > gradle.properties << EOF
# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx4G

# Fabric Properties
minecraft_version=$MC_VERSION
yarn_mappings=$YARN_VERSION
loader_version=0.16.9

# Mod Properties
mod_version=1.0.0-SNAPSHOT
maven_group=site.chococar
archives_base_name=chococars-inventory-bridge

# Dependencies
fabric_version=$FABRIC_API_VERSION
paper_version=$PAPER_VERSION
data_version=$DATA_VERSION

# CI/CD Properties
ci_build=false
EOF

# 更新配置文件中的版本
echo "📝 更新配置文件..."
find . -name "*.yml" -type f -not -path "./.git/*" -exec sed -i.bak "s/minecraftVersion: \".*\"/minecraftVersion: \"$MC_VERSION\"/g" {} \;

# 清理備份文件
find . -name "*.yml.bak" -delete

# 更新常量文件中的數據版本
echo "🔢 更新數據版本..."
find . -name "*.java" -type f -exec sed -i.bak "s/CURRENT_DATA_VERSION = [0-9]*/CURRENT_DATA_VERSION = $DATA_VERSION/g" {} \;
find . -name "*.java.bak" -delete

echo "✅ 版本更新完成！"
echo "📋 當前配置："
echo "   Minecraft: $MC_VERSION"
echo "   Yarn: $YARN_VERSION"
echo "   Fabric API: $FABRIC_API_VERSION"
echo "   Paper: $PAPER_VERSION"
echo "   Data Version: $DATA_VERSION"
echo ""
echo "🚀 現在可以運行 './gradlew build' 來構建專案"