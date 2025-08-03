# Chococar's Inventory Bridge 版本檢查腳本 - PowerShell 版本
# 用於 Windows 環境的快速版本檢查

param(
    [string[]]$Versions = @(),
    [switch]$UpdateGradle,
    [switch]$ShowCurrent,
    [string]$SaveTo = ""
)

# 設定
$UserAgent = "ChococarsInventoryBridge/1.0.0 (https://github.com/chococar-site/inventory-bridge)"

function Invoke-ApiRequest {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 10
    )
    
    try {
        $headers = @{ "User-Agent" = $UserAgent }
        $response = Invoke-RestMethod -Uri $Url -Headers $headers -TimeoutSec $TimeoutSeconds
        return $response
    }
    catch {
        Write-Warning "API 請求失敗: $Url - $($_.Exception.Message)"
        return $null
    }
}

function Get-MinecraftVersions {
    Write-Host "🔍 獲取 Minecraft 版本..." -ForegroundColor Cyan
    
    $manifest = Invoke-ApiRequest -Url "https://piston-meta.mojang.com/mc/game/version_manifest.json"
    if (-not $manifest) {
        return @()
    }
    
    $versions = $manifest.versions | Where-Object { 
        $_.type -eq "release" -and $_.id -match "^1\.21\.\d+$" 
    } | Sort-Object { 
        $parts = $_.id.Split('.')
        [int]$parts[0] * 1000000 + [int]$parts[1] * 1000 + [int]$parts[2]
    } -Descending | Select-Object -First 5 -ExpandProperty id
    
    Write-Host "📋 找到版本: $($versions -join ', ')" -ForegroundColor Green
    return $versions
}

function Get-YarnMappings {
    param([string]$McVersion)
    
    $url = "https://meta.fabricmc.net/v2/versions/yarn/$McVersion"
    $data = Invoke-ApiRequest -Url $url
    
    if ($data -and $data.Count -gt 0) {
        return $data[0].version
    }
    return $null
}

function Get-FabricApiVersion {
    param([string]$McVersion)
    
    # 使用已知版本映射
    $versionMap = @{
        "1.21.4" = "0.108.0+1.21.4"
        "1.21.5" = "0.109.0+1.21.5"
        "1.21.6" = "0.109.5+1.21.6"
        "1.21.7" = "0.110.0+1.21.7"
        "1.21.8" = "0.110.5+1.21.8"
    }
    
    if ($versionMap.ContainsKey($McVersion)) {
        return $versionMap[$McVersion]
    }
    
    # 嘗試從 API 獲取
    try {
        $data = Invoke-ApiRequest -Url "https://api.modrinth.com/v2/project/fabric-api/version"
        foreach ($version in $data) {
            if ($version.game_versions -contains $McVersion) {
                return $version.version_number
            }
        }
    }
    catch {
        Write-Warning "無法從 API 獲取 Fabric API 版本，使用預設值"
    }
    
    return $null
}

function Get-PaperVersion {
    param([string]$McVersion)
    return "$McVersion-R0.1-SNAPSHOT"
}

function Get-DataVersion {
    param([string]$McVersion)
    
    $dataVersionMap = @{
        "1.21.4" = 4080
        "1.21.5" = 4081
        "1.21.6" = 4081
        "1.21.7" = 4081
        "1.21.8" = 4082
    }
    
    if ($dataVersionMap.ContainsKey($McVersion)) {
        return $dataVersionMap[$McVersion]
    }
    return $null
}

function Show-CurrentVersions {
    Write-Host "`n📋 當前專案配置:" -ForegroundColor Yellow
    
    if (Test-Path "gradle.properties") {
        $gradleProps = Get-Content "gradle.properties"
        $mcVersion = ($gradleProps | Where-Object { $_ -match "minecraft_version=" }) -replace "minecraft_version=", ""
        $yarnVersion = ($gradleProps | Where-Object { $_ -match "yarn_mappings=" }) -replace "yarn_mappings=", ""
        $fabricVersion = ($gradleProps | Where-Object { $_ -match "fabric_version=" }) -replace "fabric_version=", ""
        $paperVersion = ($gradleProps | Where-Object { $_ -match "paper_version=" }) -replace "paper_version=", ""
        $dataVersion = ($gradleProps | Where-Object { $_ -match "data_version=" }) -replace "data_version=", ""
        
        Write-Host "  Minecraft: $mcVersion" -ForegroundColor White
        Write-Host "  Yarn: $yarnVersion" -ForegroundColor White
        Write-Host "  Fabric API: $fabricVersion" -ForegroundColor White
        Write-Host "  Paper: $paperVersion" -ForegroundColor White
        Write-Host "  Data Version: $dataVersion" -ForegroundColor White
    }
    else {
        Write-Host "  ❌ gradle.properties 文件不存在" -ForegroundColor Red
    }
    
    if (Test-Path ".github\workflows\build.yml") {
        $buildYml = Get-Content ".github\workflows\build.yml" -Raw
        $workflowVersions = [regex]::Matches($buildYml, '"(1\.21\.\d+)"') | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
        Write-Host "`n📋 工作流程支援版本: $($workflowVersions -join ', ')" -ForegroundColor White
    }
}

function Test-AllVersions {
    param([string[]]$TargetVersions)
    
    Write-Host "`n🚀 開始版本檢查..." -ForegroundColor Cyan
    
    if ($TargetVersions.Count -eq 0) {
        $TargetVersions = Get-MinecraftVersions
    }
    
    $results = @{}
    
    foreach ($mcVersion in $TargetVersions) {
        Write-Host "`n🔍 檢查 Minecraft $mcVersion..." -ForegroundColor Yellow
        
        $yarn = Get-YarnMappings -McVersion $mcVersion
        $fabricApi = Get-FabricApiVersion -McVersion $mcVersion
        $paper = Get-PaperVersion -McVersion $mcVersion
        $dataVersion = Get-DataVersion -McVersion $mcVersion
        
        $status = if ($yarn -and $fabricApi -and $paper -and $dataVersion) { "complete" } else { "partial" }
        
        $results[$mcVersion] = @{
            minecraft = $mcVersion
            yarn_mappings = $yarn
            fabric_api = $fabricApi
            paper = $paper
            data_version = $dataVersion
            status = $status
        }
        
        Write-Host "  ✅ Yarn: $(if($yarn) { $yarn } else { '❌ 未找到' })" -ForegroundColor $(if($yarn) { 'Green' } else { 'Red' })
        Write-Host "  ✅ Fabric API: $(if($fabricApi) { $fabricApi } else { '❌ 未找到' })" -ForegroundColor $(if($fabricApi) { 'Green' } else { 'Red' })
        Write-Host "  ✅ Paper: $paper" -ForegroundColor Green
        Write-Host "  ✅ Data Version: $(if($dataVersion) { $dataVersion } else { '❌ 未知' })" -ForegroundColor $(if($dataVersion) { 'Green' } else { 'Red' })
        Write-Host "  📊 狀態: $status" -ForegroundColor $(if($status -eq 'complete') { 'Green' } else { 'Yellow' })
    }
    
    return $results
}

function Update-GradleProperties {
    param(
        [hashtable]$VersionsData,
        [string]$DefaultVersion = ""
    )
    
    if (-not $DefaultVersion) {
        $DefaultVersion = ($VersionsData.Keys | Sort-Object { [version]$_ } -Descending)[0]
    }
    
    $data = $VersionsData[$DefaultVersion]
    if ($data.status -ne "complete") {
        Write-Error "版本 $DefaultVersion 數據不完整"
        return
    }
    
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $content = @"
# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx4G

# Fabric Properties (auto-updated $timestamp)
minecraft_version=$DefaultVersion
yarn_mappings=$($data.yarn_mappings)
loader_version=0.16.9

# Mod Properties
mod_version=1.0.0-SNAPSHOT
maven_group=site.chococar
archives_base_name=chococars-inventory-bridge

# Dependencies
fabric_version=$($data.fabric_api)
paper_version=$($data.paper)
data_version=$($data.data_version)

# CI/CD Properties
ci_build=false
"@
    
    Set-Content -Path "gradle.properties" -Value $content -Encoding UTF8
    Write-Host "✅ gradle.properties 已更新為 $DefaultVersion" -ForegroundColor Green
}

# 主邏輯
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║            Chococar's Inventory Bridge 版本檢查             ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

if ($ShowCurrent) {
    Show-CurrentVersions
    return
}

$results = Test-AllVersions -TargetVersions $Versions

# 顯示摘要
$completeVersions = $results.Keys | Where-Object { $results[$_].status -eq "complete" }
$totalVersions = $results.Keys.Count

Write-Host "`n📊 檢查摘要:" -ForegroundColor Yellow
Write-Host "  總版本: $totalVersions" -ForegroundColor White
Write-Host "  完整支援: $($completeVersions.Count)" -ForegroundColor Green
Write-Host "  完整版本: $($completeVersions -join ', ')" -ForegroundColor Green

# 輸出結果
if ($SaveTo) {
    $jsonOutput = $results | ConvertTo-Json -Depth 3
    Set-Content -Path $SaveTo -Value $jsonOutput -Encoding UTF8
    Write-Host "💾 結果已保存到: $SaveTo" -ForegroundColor Green
}
else {
    Write-Host "`n📄 詳細結果:" -ForegroundColor Yellow
    $results | ConvertTo-Json -Depth 3 | Write-Host
}

# 更新 gradle.properties
if ($UpdateGradle -and $completeVersions.Count -gt 0) {
    $latestVersion = $completeVersions | Sort-Object { [version]$_ } -Descending | Select-Object -First 1
    Update-GradleProperties -VersionsData $results -DefaultVersion $latestVersion
}

Write-Host "`n🎉 版本檢查完成！" -ForegroundColor Green