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
                    # 只保留 1.21.4 以後的版本
                    if re.match(r'^1\.21\.\d+$', ver):
                        # 提取版本號並檢查是否 >= 1.21.4
                        version_parts = [int(x) for x in ver.split('.')]
                        if version_parts >= [1, 21, 4]:
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
            print(f"🔍 從 Modrinth 獲取 Fabric API for {mc_version}...", file=sys.stderr)
            
            # 直接使用 Modrinth API 獲取 Fabric API 版本
            # 不依賴 meta.fabricmc.net，因為它不提供 Fabric API 信息
            import urllib.parse
            
            # 方法1: 使用 game_versions 參數
            game_versions = urllib.parse.quote(f'["{mc_version}"]')
            api_url = f'https://api.modrinth.com/v2/project/fabric-api/version?game_versions={game_versions}'
            
            print(f"📡 請求 Modrinth: {api_url}", file=sys.stderr)
            api_response = self.session.get(api_url, timeout=10)
            print(f"📊 Modrinth 回應狀態: {api_response.status_code}", file=sys.stderr)
            
            if api_response.status_code != 200:
                print(f"📄 Modrinth 回應內容: {api_response.text[:300]}", file=sys.stderr)
            
            api_response.raise_for_status()
            api_data = api_response.json()
            print(f"📄 找到 {len(api_data)} 個版本", file=sys.stderr)
            
            # 如果沒有找到，嘗試不使用方括號
            if not api_data:
                api_url = f'https://api.modrinth.com/v2/project/fabric-api/version?game_versions={mc_version}'
                print(f"📡 重試請求 Modrinth (無方括號): {api_url}", file=sys.stderr)
                
                api_response = self.session.get(api_url, timeout=10)
                print(f"📊 重試回應狀態: {api_response.status_code}", file=sys.stderr)
                
                if api_response.status_code == 200:
                    api_data = api_response.json()
                    print(f"📄 重試找到 {len(api_data)} 個版本", file=sys.stderr)
            
            # 找到最新版本
            if api_data:
                # 按日期排序，取最新的
                api_data.sort(key=lambda x: x['date_published'], reverse=True)
                latest_version = api_data[0]['version_number']
                print(f"✅ 最新 Fabric API: {latest_version}", file=sys.stderr)
                return latest_version
            
            # 如果還是沒找到，使用已知的版本映射作為備用
            print(f"⚠️ Modrinth 沒有找到版本，使用預設值...", file=sys.stderr)
            version_mapping = {
                '1.21.4': '0.119.0+1.21.4',
                '1.21.5': '0.119.2+1.21.5',
                '1.21.6': '0.109.5+1.21.6',  # 需要更新
                '1.21.7': '0.110.0+1.21.7',
                '1.21.8': '0.110.5+1.21.8'
            }
            
            fallback_version = version_mapping.get(mc_version)
            if fallback_version:
                print(f"🔄 使用預設 Fabric API 版本: {fallback_version}", file=sys.stderr)
                return fallback_version
            
            print(f"❌ 找不到 Minecraft {mc_version} 的 Fabric API 版本", file=sys.stderr)
            return None
            
        except Exception as e:
            print(f"❌ 獲取 Fabric API 版本失敗 ({mc_version}): {e}", file=sys.stderr)
            print(f"   詳細錯誤: {type(e).__name__}: {str(e)}", file=sys.stderr)
            if hasattr(e, 'response'):
                print(f"   HTTP 狀態: {e.response.status_code}", file=sys.stderr)
                print(f"   回應內容: {e.response.text[:200]}...", file=sys.stderr)
            
            # 錯誤時也嘗試使用預設值
            version_mapping = {
                '1.21.4': '0.119.0+1.21.4',
                '1.21.5': '0.119.2+1.21.5',
                '1.21.6': '0.109.5+1.21.6',
                '1.21.7': '0.110.0+1.21.7',
                '1.21.8': '0.110.5+1.21.8'
            }
            fallback_version = version_mapping.get(mc_version)
            if fallback_version:
                print(f"🔄 使用預設 Fabric API 版本: {fallback_version}", file=sys.stderr)
                return fallback_version
            
            return None
    
    def get_yarn_mappings(self, mc_version: str) -> Optional[str]:
        """獲取指定 Minecraft 版本的最新 Yarn Mappings"""
        try:
            yarn_url = f'https://meta.fabricmc.net/v2/versions/yarn/{mc_version}'
            print(f"📡 請求 Yarn: {yarn_url}", file=sys.stderr)
            
            response = self.session.get(yarn_url)
            print(f"📊 Yarn 回應狀態: {response.status_code}", file=sys.stderr)
            
            if response.status_code == 404:
                print(f"⚠️ 找不到 {mc_version} 的 Yarn Mappings", file=sys.stderr)
                return None
                
            response.raise_for_status()
            data = response.json()
            print(f"📄 找到 {len(data)} 個 Yarn 版本", file=sys.stderr)
            
            if data:
                # 取最新版本
                latest_yarn = data[0]['version']
                print(f"✅ 最新 Yarn: {latest_yarn}", file=sys.stderr)
                return latest_yarn
            
            return None
            
        except Exception as e:
            print(f"❌ 獲取 Yarn Mappings 失敗 ({mc_version}): {e}", file=sys.stderr)
            print(f"   詳細錯誤: {type(e).__name__}: {str(e)}", file=sys.stderr)
            return None
    
    def get_paper_version(self, mc_version: str) -> Optional[str]:
        """獲取指定 Minecraft 版本的最新 Paper API"""
        try:
            paper_url = 'https://api.papermc.io/v2/projects/paper'
            print(f"📡 請求 Paper 支援版本: {paper_url}", file=sys.stderr)
            
            # 檢查 Paper 是否支援該版本
            response = self.session.get(paper_url)
            print(f"📊 Paper 回應狀態: {response.status_code}", file=sys.stderr)
            
            response.raise_for_status()
            data = response.json()
            
            supported_versions = data.get('versions', [])
            print(f"📄 Paper 支援版本: {supported_versions[-5:]}...（顯示最後5個）", file=sys.stderr)
            
            if mc_version not in supported_versions:
                print(f"⚠️ Paper 尚未支援 {mc_version}", file=sys.stderr)
                return None
            
            # 獲取該版本的最新構建
            builds_url = f'https://api.papermc.io/v2/projects/paper/versions/{mc_version}'
            print(f"📡 請求 Paper 構建: {builds_url}", file=sys.stderr)
            
            builds_response = self.session.get(builds_url)
            print(f"📊 Paper 構建回應狀態: {builds_response.status_code}", file=sys.stderr)
            
            builds_response.raise_for_status()
            builds_data = builds_response.json()
            
            latest_build = builds_data['builds'][-1]
            paper_version = f"{mc_version}-R0.1-SNAPSHOT"
            print(f"✅ Paper 版本: {paper_version} (構建 #{latest_build})", file=sys.stderr)
            
            return paper_version
            
        except Exception as e:
            print(f"❌ 獲取 Paper 版本失敗 ({mc_version}): {e}", file=sys.stderr)
            print(f"   詳細錯誤: {type(e).__name__}: {str(e)}", file=sys.stderr)
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
            
            # 嘗試多個可能的字段名
            data_version = (detail_data.get('worldVersion') or 
                          detail_data.get('dataVersion') or 
                          detail_data.get('protocolVersion'))
            
            # 如果還是沒有，嘗試從已知版本映射獲取
            if not data_version:
                version_mapping = {
                    '1.21.4': 4082,
                    '1.21.5': 4083,
                    '1.21.6': 4083,
                    '1.21.7': 4084,
                    '1.21.8': 4085,
                    '1.21.9': 4086,  # 預估
                    '1.21.10': 4087  # 預估
                }
                data_version = version_mapping.get(mc_version)
                if data_version:
                    print(f"ℹ️ 使用預設數據版本 {mc_version}: {data_version}", file=sys.stderr)
            
            return data_version
            
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
            # 獲取所有 1.21.x 版本（而不是限制為5個）
            mc_versions = mc_versions
        
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