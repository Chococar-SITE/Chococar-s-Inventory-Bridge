#!/usr/bin/env python3
"""
自動獲取最新版本 API 的腳本
支援 Minecraft、Fabric API、Yarn Mappings 和 Paper API 的版本檢測
"""

import json
import requests
import re
import sys
from datetime import datetime
from typing import Dict, List, Optional, Tuple
import argparse

class VersionFetcher:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'ChococarsInventoryBridge/1.0.0 (https://github.com/chococar-site/inventory-bridge)'
        })
    
    def get_minecraft_versions(self) -> List[str]:
        """獲取 Minecraft 版本列表"""
        try:
            response = self.session.get('https://piston-meta.mojang.com/mc/game/version_manifest.json')
            response.raise_for_status()
            data = response.json()
            
            versions = []
            for version in data['versions']:
                if version['type'] == 'release':
                    ver = version['id']
                    # 只保留 1.21.x 版本
                    if re.match(r'^1\.21\.\d+$', ver):
                        versions.append(ver)
            
            # 排序版本（從新到舊）
            versions.sort(key=lambda x: [int(i) for i in x.split('.')], reverse=True)
            return versions
            
        except Exception as e:
            print(f"❌ 獲取 Minecraft 版本失敗: {e}", file=sys.stderr)
            return []
    
    def get_fabric_api_version(self, mc_version: str) -> Optional[str]:
        """獲取指定 Minecraft 版本的最新 Fabric API 版本"""
        try:
            # 使用 CurseForge API 或 Modrinth API
            # 這裡使用 Fabric 官方的版本 API
            response = self.session.get(f'https://meta.fabricmc.net/v2/versions/game/{mc_version}')
            if response.status_code == 404:
                return None
                
            response.raise_for_status()
            
            # 獲取 Fabric API 的最新版本
            api_response = self.session.get('https://api.modrinth.com/v2/project/fabric-api/version')
            api_response.raise_for_status()
            api_data = api_response.json()
            
            # 找到支援該 MC 版本的最新 Fabric API
            for version in api_data:
                if mc_version in version.get('game_versions', []):
                    return version['version_number']
            
            return None
            
        except Exception as e:
            print(f"❌ 獲取 Fabric API 版本失敗 ({mc_version}): {e}", file=sys.stderr)
            return None
    
    def get_yarn_mappings(self, mc_version: str) -> Optional[str]:
        """獲取指定 Minecraft 版本的最新 Yarn Mappings"""
        try:
            response = self.session.get(f'https://meta.fabricmc.net/v2/versions/yarn/{mc_version}')
            if response.status_code == 404:
                return None
                
            response.raise_for_status()
            data = response.json()
            
            if data:
                # 取最新版本
                return data[0]['version']
            
            return None
            
        except Exception as e:
            print(f"❌ 獲取 Yarn Mappings 失敗 ({mc_version}): {e}", file=sys.stderr)
            return None
    
    def get_paper_version(self, mc_version: str) -> Optional[str]:
        """獲取指定 Minecraft 版本的最新 Paper API"""
        try:
            # 檢查 Paper 是否支援該版本
            response = self.session.get('https://api.papermc.io/v2/projects/paper')
            response.raise_for_status()
            data = response.json()
            
            if mc_version not in data.get('versions', []):
                return None
            
            # 獲取該版本的最新構建
            builds_response = self.session.get(f'https://api.papermc.io/v2/projects/paper/versions/{mc_version}')
            builds_response.raise_for_status()
            builds_data = builds_response.json()
            
            latest_build = builds_data['builds'][-1]
            return f"{mc_version}-R0.1-SNAPSHOT"
            
        except Exception as e:
            print(f"❌ 獲取 Paper 版本失敗 ({mc_version}): {e}", file=sys.stderr)
            return None
    
    def get_data_version(self, mc_version: str) -> Optional[int]:
        """獲取 Minecraft 版本對應的數據版本"""
        try:
            response = self.session.get('https://piston-meta.mojang.com/mc/game/version_manifest.json')
            response.raise_for_status()
            manifest = response.json()
            
            # 找到對應版本的詳細信息
            version_info = None
            for version in manifest['versions']:
                if version['id'] == mc_version:
                    version_info = version
                    break
            
            if not version_info:
                return None
            
            # 獲取版本詳細信息
            detail_response = self.session.get(version_info['url'])
            detail_response.raise_for_status()
            detail_data = detail_response.json()
            
            return detail_data.get('worldVersion')
            
        except Exception as e:
            print(f"❌ 獲取數據版本失敗 ({mc_version}): {e}", file=sys.stderr)
            return None
    
    def fetch_all_versions(self, target_versions: List[str] = None) -> Dict:
        """獲取所有版本信息"""
        print("🔍 正在獲取最新版本信息...", file=sys.stderr)
        
        mc_versions = self.get_minecraft_versions()
        if target_versions:
            mc_versions = [v for v in mc_versions if v in target_versions]
        else:
            # 預設只獲取最新的 5 個版本
            mc_versions = mc_versions[:5]
        
        print(f"📋 檢測到的 Minecraft 版本: {', '.join(mc_versions)}", file=sys.stderr)
        
        results = {}
        
        for mc_version in mc_versions:
            print(f"\n🔍 處理 Minecraft {mc_version}...", file=sys.stderr)
            
            fabric_api = self.get_fabric_api_version(mc_version)
            yarn = self.get_yarn_mappings(mc_version)
            paper = self.get_paper_version(mc_version)
            data_version = self.get_data_version(mc_version)
            
            results[mc_version] = {
                'minecraft': mc_version,
                'yarn_mappings': yarn,
                'fabric_api': fabric_api,
                'paper': paper,
                'data_version': data_version,
                'status': 'complete' if all([yarn, fabric_api, paper, data_version]) else 'partial'
            }
            
            print(f"  ✅ Yarn: {yarn}", file=sys.stderr)
            print(f"  ✅ Fabric API: {fabric_api}", file=sys.stderr)
            print(f"  ✅ Paper: {paper}", file=sys.stderr)
            print(f"  ✅ Data Version: {data_version}", file=sys.stderr)
        
        return results

def generate_github_workflow(versions_data: Dict) -> str:
    """生成 GitHub Workflow 的版本矩陣"""
    
    cases = []
    matrix_versions = []
    
    for mc_version, data in versions_data.items():
        if data['status'] != 'complete':
            continue
            
        matrix_versions.append(f'"{mc_version}"')
        
        case = f'''          "{mc_version}")
            echo "YARN_VERSION={data['yarn_mappings']}" >> $GITHUB_ENV
            echo "FABRIC_API_VERSION={data['fabric_api']}" >> $GITHUB_ENV
            echo "PAPER_VERSION={data['paper']}" >> $GITHUB_ENV
            echo "DATA_VERSION={data['data_version']}" >> $GITHUB_ENV
            ;;'''
        cases.append(case)
    
    matrix_line = f"          - {chr(10)}          - ".join(matrix_versions)
    cases_content = "\n".join(cases)
    
    return f"""        minecraft_version: 
          - {matrix_line}
          
# 在 build.yml 中的 case 語句部分：
        case "$MC_VERSION" in
{cases_content}"""

def generate_gradle_properties(versions_data: Dict, default_version: str = None) -> str:
    """生成 gradle.properties 文件"""
    
    if not default_version:
        default_version = list(versions_data.keys())[0]
    
    data = versions_data[default_version]
    
    return f"""# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx4G

# Fabric Properties (auto-generated on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')})
minecraft_version={default_version}
yarn_mappings={data['yarn_mappings']}
loader_version=0.16.9

# Mod Properties
mod_version=1.0.0-SNAPSHOT
maven_group=site.chococar
archives_base_name=chococars-inventory-bridge

# Dependencies
fabric_version={data['fabric_api']}
paper_version={data['paper']}
data_version={data['data_version']}

# CI/CD Properties
ci_build=false"""

def generate_version_script(versions_data: Dict) -> str:
    """生成版本更新腳本"""
    
    cases = []
    versions_list = []
    
    for mc_version, data in versions_data.items():
        if data['status'] != 'complete':
            continue
            
        versions_list.append(mc_version)
        
        case = f'''  "{mc_version}")
    YARN_VERSION="{data['yarn_mappings']}"
    FABRIC_API_VERSION="{data['fabric_api']}"
    PAPER_VERSION="{data['paper']}"
    DATA_VERSION="{data['data_version']}"
    ;;'''
        cases.append(case)
    
    versions_str = ', '.join(versions_list)
    cases_content = "\n".join(cases)
    
    return f"""#!/bin/bash

# 自動生成的版本更新腳本 - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
# 支援版本: {versions_str}

set -e

MC_VERSION=${{1:-"{versions_list[0]}"}}

case "$MC_VERSION" in
{cases_content}
  *)
    echo "❌ 不支援的版本: $MC_VERSION"
    echo "支援的版本: {versions_str}"
    exit 1
    ;;
esac

# 更新邏輯保持不變...
echo "🔧 正在更新到 Minecraft $MC_VERSION..."
# ... 其餘腳本內容"""

def main():
    parser = argparse.ArgumentParser(description='獲取最新版本 API')
    parser.add_argument('--versions', nargs='+', help='指定要檢查的 Minecraft 版本')
    parser.add_argument('--output', choices=['json', 'workflow', 'gradle', 'script'], 
                       default='json', help='輸出格式')
    parser.add_argument('--save', help='保存到文件')
    
    args = parser.parse_args()
    
    fetcher = VersionFetcher()
    versions_data = fetcher.fetch_all_versions(args.versions)
    
    if args.output == 'json':
        output = json.dumps(versions_data, indent=2, ensure_ascii=False)
    elif args.output == 'workflow':
        output = generate_github_workflow(versions_data)
    elif args.output == 'gradle':
        output = generate_gradle_properties(versions_data)
    elif args.output == 'script':
        output = generate_version_script(versions_data)
    
    if args.save:
        with open(args.save, 'w', encoding='utf-8') as f:
            f.write(output)
        print(f"\n💾 已保存到: {args.save}", file=sys.stderr)
    else:
        if args.output == 'json':
            # 對於 JSON 輸出，只輸出純 JSON（用於管道重定向）
            print(output)
        else:
            print(f"\n📄 輸出結果:")
            print(output)

if __name__ == '__main__':
    main()