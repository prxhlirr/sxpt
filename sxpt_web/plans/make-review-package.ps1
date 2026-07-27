param(
    [Parameter(Mandatory = $true)]
    [string]$OutFile,
    [Parameter(Mandatory = $true)]
    [string[]]$Files,
    [string]$TaskLabel = 'Task review package'
)

$content = [System.Collections.Generic.List[string]]::new()
$content.Add("# $TaskLabel")
$content.Add('')
$content.Add('Git metadata is unavailable in this workspace. This package contains the complete current content of every file changed by the task.')
$content.Add('')

foreach ($file in $Files) {
    $resolved = Resolve-Path -LiteralPath $file -ErrorAction Stop
    $content.Add("## FILE: $resolved")
    $content.Add('')
    $content.Add('```text')
    foreach ($line in Get-Content -LiteralPath $resolved -Encoding UTF8) {
        $content.Add($line)
    }
    $content.Add('```')
    $content.Add('')
}

$content | Set-Content -LiteralPath $OutFile -Encoding UTF8
Write-Output "wrote $OutFile`: $($content.Count) lines"
