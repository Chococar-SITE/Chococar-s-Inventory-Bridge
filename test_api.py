#!/usr/bin/env python3
"""
簡化的 API 測試腳本
"""

import requests
import sys

def test_fabric_api(mc_version="1.21.8"):
    print(f"🔍 測試 Fabric API 獲取 for {mc_version}")
    session = requests.Session()
    session.headers.update({
        'User-Agent': 'ChococarsInventoryBridge/1.0.0 Test'
    })
    
    try:
        # 直接測試 Modrinth API（不依賴 meta.fabricmc.net）
        import urllib.parse
        
        # 方法1: 使用方括號格式
        game_versions = urllib.parse.quote(f'["{mc_version}"]')
        api_url = f'https://api.modrinth.com/v2/project/fabric-api/version?game_versions={game_versions}'
        
        print(f"📡 請求 Modrinth (方法1): {api_url}")
        api_response = session.get(api_url, timeout=10)
        print(f"📊 Modrinth 狀態碼: {api_response.status_code}")
        
        if api_response.status_code == 200:
            data = api_response.json()
            print(f"📄 找到 {len(data)} 個版本")
            
            if data:
                data.sort(key=lambda x: x['date_published'], reverse=True)
                latest = data[0]['version_number']
                print(f"✅ 最新版本: {latest}")
            else:
                print("⚠️ 方法1沒有找到版本，嘗試方法2...")
                
                # 方法2: 不使用方括號
                api_url2 = f'https://api.modrinth.com/v2/project/fabric-api/version?game_versions={mc_version}'
                print(f"📡 請求 Modrinth (方法2): {api_url2}")
                
                api_response2 = session.get(api_url2, timeout=10)
                print(f"📊 Modrinth 狀態碼 (方法2): {api_response2.status_code}")
                
                if api_response2.status_code == 200:
                    data2 = api_response2.json()
                    print(f"📄 方法2找到 {len(data2)} 個版本")
                    
                    if data2:
                        data2.sort(key=lambda x: x['date_published'], reverse=True)
                        latest = data2[0]['version_number']
                        print(f"✅ 最新版本: {latest}")
                    else:
                        print("⚠️ 方法2也沒有找到版本")
                else:
                    print(f"❌ 方法2請求失敗: {api_response2.text[:200]}")
        else:
            print(f"❌ Modrinth 請求失敗: {api_response.text[:200]}")
            print(f"回應內容: {api_response.text}")
            
    except Exception as e:
        print(f"❌ 錯誤: {e}")

if __name__ == "__main__":
    test_fabric_api()