<#
.SYNOPSIS
Opens every .docx and .xlsx under a folder in Microsoft Word or Excel, records whether it opened and any
sign of repair, and exports the text Office reads from it.

.DESCRIPTION
Each file is opened in its own Word or Excel process, read-only, with alerts off, under a timeout. A
file that hangs (usually behind a dialog) is recorded as a timeout and its process is stopped. Only
Office processes started by this script are stopped, but close Word and Excel before running anyway.

Writes, under -OutDir:
  results.csv        file, app, status, repaired, error, seconds
  text\<path>.txt    the text Office read: every Word story (body, headers, footers, notes, comments,
                     text boxes) and hyperlink addresses; every Excel cell value, comment, shape text, header,
                     footer, and chart title

Signs of repair are heuristic: a workbook whose window caption contains "Repaired" is recorded as
repaired. Word exposes nothing comparable through COM, so a Word file that Word silently repairs can
still show as "ok": open a sample by hand as well.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File open-check.ps1 -InputDir .\office-bundle -OutDir .\office-results
#>
param(
    [Parameter(Mandatory = $true)][string]$InputDir,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = 'Stop'
$InputDir = (Resolve-Path $InputDir).Path
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$OutDir = (Resolve-Path $OutDir).Path
$TextDir = Join-Path $OutDir 'text'
New-Item -ItemType Directory -Force -Path $TextDir | Out-Null

# Runs in a separate process per file, so a hung Office instance can be stopped without affecting the rest.
$worker = {
    param($Path, $TextPath)
    $result = @{ status = 'ok'; repaired = ''; error = '' }
    $sb = New-Object System.Text.StringBuilder
    $app = $null
    try {
        if ($Path -like '*.docx') {
            $app = New-Object -ComObject Word.Application
            $app.Visible = $false
            $app.DisplayAlerts = 0  # wdAlertsNone
            # FileName, ConfirmConversions, ReadOnly, AddToRecentFiles, PasswordDocument (an empty password
            # makes a protected file fail instead of prompting).
            $doc = $app.Documents.Open($Path, $false, $true, $false, '')
            # Every story, including headers, footers, notes, comments, and text boxes, with the hyperlink
            # addresses in each (Document.Hyperlinks covers only the main body).
            foreach ($story in $doc.StoryRanges) {
                $range = $story
                while ($range -ne $null) {
                    [void]$sb.AppendLine("[story $($range.StoryType)]")
                    [void]$sb.AppendLine($range.Text)
                    foreach ($link in $range.Hyperlinks) {
                        try { [void]$sb.AppendLine("[hyperlink] $($link.Address)") } catch { }
                    }
                    $range = $range.NextStoryRange
                }
            }
            $doc.Close($false)
        }
        else {
            $app = New-Object -ComObject Excel.Application
            $app.Visible = $false
            $app.DisplayAlerts = $false
            $app.AskToUpdateLinks = $false
            # Filename, UpdateLinks, ReadOnly
            $wb = $app.Workbooks.Open($Path, 0, $true)
            foreach ($window in $wb.Windows) {
                if ($window.Caption -like '*Repaired*') { $result.repaired = 'yes' }
            }
            foreach ($ws in $wb.Worksheets) {
                [void]$sb.AppendLine("[sheet $($ws.Name)]")
                # Value2 is the stored value. Text is what is displayed, which can be "####" in a narrow
                # column and would hide a numeric SSN from the leak search.
                foreach ($cell in $ws.UsedRange.Cells) {
                    $value = $cell.Value2
                    if ($value -ne $null -and "$value" -ne '') { [void]$sb.AppendLine("$($cell.Address($false, $false))`t$value") }
                }
                foreach ($shape in $ws.Shapes) {
                    try {
                        if ($shape.HasTextFrame) { [void]$sb.AppendLine("[shape] $($shape.TextFrame2.TextRange.Text)") }
                    } catch { }
                }
                foreach ($c in $ws.Comments) { [void]$sb.AppendLine("[comment] $($c.Text())") }
                try {
                    foreach ($c in $ws.CommentsThreaded) { [void]$sb.AppendLine("[threaded] $($c.Text())") }
                } catch { }
                $ps = $ws.PageSetup
                [void]$sb.AppendLine("[header] $($ps.LeftHeader)|$($ps.CenterHeader)|$($ps.RightHeader)")
                [void]$sb.AppendLine("[footer] $($ps.LeftFooter)|$($ps.CenterFooter)|$($ps.RightFooter)")
                foreach ($co in $ws.ChartObjects()) {
                    if ($co.Chart.HasTitle) { [void]$sb.AppendLine("[chart] $($co.Chart.ChartTitle.Text)") }
                }
            }
            $wb.Close($false)
        }
    }
    catch {
        $result.status = 'failed'
        $result.error = ($_.Exception.Message -replace '\s+', ' ')
    }
    finally {
        if ($app -ne $null) {
            try { $app.Quit() } catch { }
            [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($app)
        }
        [System.IO.File]::WriteAllText($TextPath, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))
    }
    return $result
}

$before = @(Get-Process -Name WINWORD, EXCEL -ErrorAction SilentlyContinue | ForEach-Object { $_.Id })
$rows = New-Object System.Collections.Generic.List[object]
$files = Get-ChildItem -Path $InputDir -Recurse -File -Include *.docx, *.xlsx | Sort-Object FullName

foreach ($file in $files) {
    $relative = $file.FullName.Substring($InputDir.Length).TrimStart('\', '/')
    $textPath = Join-Path $TextDir ($relative + '.txt')
    New-Item -ItemType Directory -Force -Path (Split-Path $textPath) | Out-Null
    $app = 'Word'
    if ($file.Extension -eq '.xlsx') { $app = 'Excel' }
    Write-Host "$relative ..." -NoNewline

    $started = Get-Date
    $job = Start-Job -ScriptBlock $worker -ArgumentList $file.FullName, $textPath
    if (Wait-Job $job -Timeout $TimeoutSeconds) {
        $jobErrors = $null
        # The worker's result is its last output; anything it wrote to the error stream is kept, not thrown.
        $r = Receive-Job $job -ErrorAction SilentlyContinue -ErrorVariable jobErrors | Select-Object -Last 1
        if ($r -ne $null -and $r.status) {
            $status = $r.status; $repaired = $r.repaired; $err = $r.error
        }
        else {
            $status = 'failed'; $repaired = ''; $err = ("worker error: " + ($jobErrors -join ' ')) -replace '\s+', ' '
        }
    }
    else {
        $status = 'timeout'; $repaired = ''; $err = "no response in $TimeoutSeconds s (likely a dialog)"
        Stop-Job $job
    }
    Remove-Job $job -Force
    # Stop any Word or Excel this run started that is still running, so instances do not accumulate.
    Get-Process -Name WINWORD, EXCEL -ErrorAction SilentlyContinue |
        Where-Object { $before -notcontains $_.Id } | Stop-Process -Force -ErrorAction SilentlyContinue
    $seconds = [math]::Round(((Get-Date) - $started).TotalSeconds, 1)
    Write-Host " $status"
    $rows.Add([pscustomobject]@{ file = $relative; app = $app; status = $status; repaired = $repaired; error = $err; seconds = $seconds })
}

$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path (Join-Path $OutDir 'results.csv')
$failed = @($rows | Where-Object { $_.status -ne 'ok' -or $_.repaired -eq 'yes' })
Write-Host ""
Write-Host "$($rows.Count) files, $($failed.Count) failed, timed out, or repaired. Results in $OutDir"
