param([switch]$ProvisionAdmin)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$config = Join-Path $repo 'apps/backend/.env'
if (!(Test-Path -LiteralPath $config)) {
    throw 'Thiếu apps/backend/.env. Xem docs/backend/postgresql-local.md.'
}
# Chỉ đọc NAME=value, không thực thi nội dung file hoặc in secret.
Get-Content -LiteralPath $config | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
if (!$env:JAVA_HOME) {
    $localJdk = Join-Path $repo '.local/jdk21/jdk-21.0.13+9'
    if (Test-Path "$localJdk/bin/java.exe") { $env:JAVA_HOME = $localJdk }
}
if (!$env:JAVA_HOME -or !(Test-Path "$env:JAVA_HOME/bin/java.exe")) {
    throw 'Đặt JAVA_HOME trỏ tới JDK 21 trước khi chạy.'
}
foreach ($name in @('DB_URL', 'DB_USER', 'JWT_SIGNING_KEY', 'CURSOR_SIGNING_KEY')) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) {
        throw "Thiếu $name trong apps/backend/.env. Xem docs/backend/postgresql-local.md."
    }
}
if (!$env:DB_PASSWORD) {
    $securePassword = Read-Host 'Mật khẩu PostgreSQL của user DB_USER trong .env' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $securePassword.Dispose() }
}
if (!$env:DB_PASSWORD) { throw 'Mật khẩu database không được để trống.' }
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DEBUG = 'false'
$env:LOGGING_LEVEL_ROOT = 'INFO'
$env:COOKIE_SECURE = 'false'
$env:COOKIE_SAME_SITE = 'Lax'
if (!$env:ADMIN_ALLOWED_ORIGINS) { $env:ADMIN_ALLOWED_ORIGINS = 'http://127.0.0.1:5173' }
Push-Location $repo
try {
    if ($ProvisionAdmin) {
        & "$repo/apps/backend/mvnw.cmd" -f apps/backend/pom.xml -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Build backend thất bại.' }
        & "$env:JAVA_HOME/bin/java.exe" -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar '--spring.profiles.active=provision-admin,local'
    } else {
        & "$repo/apps/backend/mvnw.cmd" -f apps/backend/pom.xml spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw "Backend kết thúc với exit code $LASTEXITCODE." }
} finally {
    Pop-Location
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}
