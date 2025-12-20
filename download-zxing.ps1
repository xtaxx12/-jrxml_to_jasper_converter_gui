# Download ZXing libraries for QR code generation
$version = "3.3.0"
$baseUrl = "https://repo1.maven.org/maven2/com/google/zxing"

$files = @(
    @{name="core-$version.jar"; url="$baseUrl/core/$version/core-$version.jar"},
    @{name="javase-$version.jar"; url="$baseUrl/javase/$version/javase-$version.jar"}
)

Write-Host "Downloading ZXing libraries version $version..."

foreach ($file in $files) {
    $outputPath = "lib\$($file.name)"
    Write-Host "Downloading $($file.name)..."
    try {
        Invoke-WebRequest -Uri $file.url -OutFile $outputPath
        Write-Host "  Downloaded successfully to $outputPath"
    } catch {
        Write-Host "  Error downloading $($file.name): $_" -ForegroundColor Red
    }
}

Write-Host "`nDone! ZXing libraries downloaded to lib folder."
