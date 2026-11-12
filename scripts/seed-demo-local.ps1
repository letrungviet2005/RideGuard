param([string]$AdminEmail)
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$config = Join-Path $repo 'apps/backend/.env'
if (!(Test-Path $config)) { throw 'Thiếu apps/backend/.env.' }
Get-Content $config | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        # Empty file values do not erase a password supplied in the process environment.
        if ($Matches[2] -ne '') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
    }
}
if ($env:DB_URL -notin @('jdbc:postgresql://127.0.0.1:5433/rideguard','jdbc:postgresql://localhost:5433/rideguard') -or $env:DB_USER -ne 'rideguard') {
    throw 'Seed chỉ dành cho database/user rideguard local port 5433.'
}
if ($AdminEmail) { $env:DEMO_ADMIN_EMAIL = $AdminEmail }
if (!$env:JAVA_HOME) { $env:JAVA_HOME = (Resolve-Path "$repo/.local/jdk21/jdk-21.0.13+9").Path }
try {
    foreach ($name in @('DB_PASSWORD','DEMO_USER1_PASSWORD','DEMO_USER2_PASSWORD')) {
        if (![Environment]::GetEnvironmentVariable($name)) {
            $secret = Read-Host "Nhập $name (USER demo: 12-128 ký tự; không đổi mật khẩu user đã tồn tại)" -AsSecureString
            $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
            try { [Environment]::SetEnvironmentVariable($name, [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer), 'Process') }
            finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $secret.Dispose() }
        }
    }
    $env:SPRING_PROFILES_ACTIVE = 'local,seed-demo'
    $env:SPRING_FLYWAY_DEFAULT_SCHEMA = 'public'
    $env:DEBUG = 'false'
    $env:LOGGING_LEVEL_ROOT = 'WARN'
    $env:LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC = 'OFF'
    Push-Location $repo
    try {
        & "$repo/apps/backend/mvnw.cmd" -f apps/backend/pom.xml -B -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'Build seed thất bại.' }
        & "$env:JAVA_HOME/bin/java.exe" -jar apps/backend/target/backend-0.1.0-SNAPSHOT.jar '--spring.profiles.active=local,seed-demo'
        if ($LASTEXITCODE -ne 0) { throw 'Seed thất bại. Không ghi nhận thành công; xem thông báo đã che secret.' }
    } finally { Pop-Location }
} finally {
    foreach ($name in @('DB_PASSWORD','DEMO_USER1_PASSWORD','DEMO_USER2_PASSWORD','DEMO_ADMIN_PASSWORD')) {
        Remove-Item "Env:$name" -ErrorAction SilentlyContinue
    }
}
