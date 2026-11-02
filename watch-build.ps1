$ProjectFolder = $PSScriptRoot
$SourceFolder = Join-Path $ProjectFolder "src"
$TargetFolder = Join-Path $ProjectFolder "target"
$VersionsFolder = Join-Path $ProjectFolder "versions"
$VersionFile = Join-Path $ProjectFolder "version.txt"

New-Item -ItemType Directory -Force -Path $VersionsFolder | Out-Null

# Start at version 1.0 if no version file exists
if (!(Test-Path $VersionFile)) {
    Set-Content -Path $VersionFile -Value "1.0"
}

$CurrentVersion = [version](Get-Content $VersionFile)

function Get-SourceSignature {
    $files = Get-ChildItem $SourceFolder -Recurse -File |
        Sort-Object FullName

    $signature = ($files | ForEach-Object {
        "$($_.FullName):$($_.Length):$($_.LastWriteTimeUtc.Ticks)"
    }) -join "`n"

    return $signature
}

Write-Host "Watching source files for changes..."
Write-Host "Press Ctrl+C to stop."

$PreviousSignature = Get-SourceSignature

while ($true) {
    Start-Sleep -Seconds 2

    $NewSignature = Get-SourceSignature

    if ($NewSignature -ne $PreviousSignature) {

        # Wait for the file save to finish
        Start-Sleep -Seconds 2

        $PreviousSignature = Get-SourceSignature

        Write-Host "Change detected. Building project..."

        Push-Location $ProjectFolder
        mvn clean package
        $BuildResult = $LASTEXITCODE
        Pop-Location

        if ($BuildResult -eq 0) {
            $JarFile = Get-ChildItem $TargetFolder -Filter "*.jar" |
                Where-Object { $_.Name -notlike "original-*" } |
                Select-Object -First 1

            if ($JarFile) {
                $VersionName = "v$($CurrentVersion.Major).$($CurrentVersion.Minor)"
                $NewJarName = "java-devops-project-$VersionName.jar"
                $Destination = Join-Path $VersionsFolder $NewJarName

                Copy-Item $JarFile.FullName $Destination -Force

                Write-Host "Saved JAR version: $Destination"

                $CurrentVersion = [version]::new(
                    $CurrentVersion.Major,
                    ($CurrentVersion.Minor + 1)
                )

                Set-Content -Path $VersionFile -Value $CurrentVersion.ToString()
            }
        }
        else {
            Write-Host "Build failed. No new JAR version saved."
        }
    }
}