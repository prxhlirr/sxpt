param(
    [Parameter(Mandatory = $true)]
    [string]$PlanFile,
    [Parameter(Mandatory = $true)]
    [int]$TaskNumber,
    [Parameter(Mandatory = $true)]
    [string]$OutFile
)

$lines = Get-Content -LiteralPath $PlanFile -Encoding UTF8
$insideFence = $false
$insideTask = $false
$result = [System.Collections.Generic.List[string]]::new()

foreach ($line in $lines) {
    if ($line -match '^```') {
        $insideFence = -not $insideFence
    }

    if (-not $insideFence -and $line -match '^#+\s+Task\s+(\d+)(?:\D|$)') {
        $insideTask = [int]$Matches[1] -eq $TaskNumber
    }

    if ($insideTask) {
        $result.Add($line)
    }
}

if ($result.Count -eq 0) {
    throw "Task $TaskNumber was not found in $PlanFile"
}

$result | Set-Content -LiteralPath $OutFile -Encoding UTF8
Write-Output "wrote $OutFile`: $($result.Count) lines"
