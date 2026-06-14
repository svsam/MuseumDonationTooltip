param(
    [string]$InputPath,
    [string]$OutputPath = "src/main/resources/museum-donatable-items.json"
)

$ErrorActionPreference = "Stop"
$sourceUrl = "https://api.hypixel.net/v2/resources/skyblock/items"

if ($InputPath) {
    $response = Get-Content -Raw -LiteralPath $InputPath | ConvertFrom-Json
} else {
    $response = Invoke-RestMethod -Uri $sourceUrl -Method Get
}

if (-not $response.success -or -not $response.items) {
    throw "Hypixel item resource did not contain a successful items response."
}

$entries = foreach ($item in $response.items) {
    if (-not $item.id -or -not $item.museum_data) {
        continue
    }

    $museumKeys = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    [void]$museumKeys.Add([string]$item.id)

    if ($item.museum_data.armor_set_donation_xp) {
        foreach ($property in $item.museum_data.armor_set_donation_xp.PSObject.Properties) {
            [void]$museumKeys.Add([string]$property.Name)
        }
    }

    $aliases = [System.Collections.Generic.HashSet[string]]::new(
        [System.StringComparer]::OrdinalIgnoreCase
    )
    foreach ($alias in @($item.museum_data.mapped_item_ids)) {
        if ($alias) {
            [void]$aliases.Add([string]$alias)
        }
    }

    [ordered]@{
        id = [string]$item.id
        category = if ($item.museum_data.category) {
            [string]$item.museum_data.category
        } elseif ($item.category) {
            [string]$item.category
        } else {
            "UNKNOWN"
        }
        museumKeys = @($museumKeys | Sort-Object)
        aliases = @($aliases | Sort-Object)
    }
}

$generatedAt = if ($response.lastUpdated) {
    [DateTimeOffset]::FromUnixTimeMilliseconds([long]$response.lastUpdated).ToString("O")
} else {
    [DateTimeOffset]::UtcNow.ToString("O")
}

$registry = [ordered]@{
    schemaVersion = 1
    generatedAt = $generatedAt
    source = $sourceUrl
    items = @($entries | Sort-Object { $_.id })
}

$absoluteOutput = [IO.Path]::GetFullPath($OutputPath)
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($absoluteOutput)) | Out-Null
$json = $registry | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($absoluteOutput, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))

Write-Host "Wrote $($registry.items.Count) museum-donatable entries to $absoluteOutput"
