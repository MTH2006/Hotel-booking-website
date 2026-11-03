param(
    [int]$Port = 3000
)

$frontendDir = Join-Path $PSScriptRoot "frontend"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       HESTORA - Khởi chạy Website Khách sạn               " -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cyan

# Tự động tìm cổng khả dụng nếu cổng yêu cầu đang bận (ví dụ: bị nginx chiếm)
$selectedPort = $Port
$listener = $null
$maxTries = 30

for ($i = 0; $i -lt $maxTries; $i++) {
    $currentPort = $Port + $i
    $testUrl = "http://localhost:$currentPort/"
    try {
        $tempListener = New-Object System.Net.HttpListener
        $tempListener.Prefixes.Add($testUrl)
        $tempListener.Start()
        $listener = $tempListener
        $selectedPort = $currentPort
        break
    } catch {
        if ($tempListener) {
            try { $tempListener.Close() } catch {}
        }
    }
}

if (-not $listener) {
    Write-Host "[LỖI] Không thể lắng nghe từ cổng $Port đến $($Port + $maxTries)." -ForegroundColor Red
    Write-Host "Bạn có thể chạy bằng Node.js: node .\frontend\server.js" -ForegroundColor Yellow
    exit 1
}

$url = "http://localhost:$selectedPort/"
if ($selectedPort -ne $Port) {
    Write-Host "[THÔNG BÁO] Cổng $Port đang bị chiếm (bởi dịch vụ khác như nginx)." -ForegroundColor Yellow
    Write-Host ">>> Đã tự động chuyển sang cổng khả dụng: $selectedPort" -ForegroundColor Green
}

Write-Host " Thư mục web: $frontendDir" -ForegroundColor Gray
Write-Host " Địa chỉ URL: $url" -ForegroundColor Green
Write-Host " Quản trị:    ${url}hotel-management.html" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

Write-Host ">>> Đang tự động mở trình duyệt web tại $url ..." -ForegroundColor Cyan
Start-Process $url

Write-Host ">>> Máy chủ đang chạy... Nhấn [Ctrl + C] trong cửa sổ này để dừng." -ForegroundColor Green

$mimeTypes = @{
    ".html"  = "text/html; charset=utf-8"
    ".css"   = "text/css; charset=utf-8"
    ".js"    = "application/javascript; charset=utf-8"
    ".json"  = "application/json; charset=utf-8"
    ".png"   = "image/png"
    ".jpg"   = "image/jpeg"
    ".jpeg"  = "image/jpeg"
    ".svg"   = "image/svg+xml"
    ".ico"   = "image/x-icon"
    ".woff2" = "font/woff2"
    ".woff"  = "font/woff"
    ".ttf"   = "font/ttf"
}

try {
    while ($listener.IsListening) {
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        $localPath = $request.Url.LocalPath
        if ($localPath -eq "/" -or [string]::IsNullOrWhiteSpace($localPath)) {
            $localPath = "/index.html"
        }

        $cleanPath = $localPath.TrimStart('/').Replace('/', '\')
        $filePath = Join-Path $frontendDir $cleanPath

        if (Test-Path $filePath -PathType Leaf) {
            $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
            $contentType = if ($mimeTypes.ContainsKey($ext)) { $mimeTypes[$ext] } else { "application/octet-stream" }
            $response.ContentType = $contentType
            $response.AddHeader("Access-Control-Allow-Origin", "*")

            $bytes = [System.IO.File]::ReadAllBytes($filePath)
            $response.ContentLength64 = $bytes.Length
            $response.OutputStream.Write($bytes, 0, $bytes.Length)
            $response.StatusCode = 200
        } else {
            $response.StatusCode = 404
            $notFoundHtml = "<!doctype html><html><head><meta charset='utf-8'><title>404</title></head><body style='font-family:sans-serif;text-align:center;padding:50px;'><h2>404 - Không tìm thấy trang</h2><p>Đường dẫn <code>$localPath</code> không tồn tại trên máy chủ HESTORA.</p><p><a href='/'>← Về Trang chủ</a> | <a href='/hotel-management.html'>Trang Quản trị</a></p></body></html>"
            $notFoundBytes = [System.Text.Encoding]::UTF8.GetBytes($notFoundHtml)
            $response.ContentType = "text/html; charset=utf-8"
            $response.OutputStream.Write($notFoundBytes, 0, $notFoundBytes.Length)
        }
        $response.Close()
    }
} catch [System.Net.HttpListenerException] {
    # Normal on abort
} finally {
    if ($listener -and $listener.IsListening) {
        $listener.Stop()
    }
}
