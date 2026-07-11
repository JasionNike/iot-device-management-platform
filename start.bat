@chcp 65001 >nul
@echo off
cd /d "%~dp0"
title IoT Platform Launcher
setlocal

echo ============================================================
echo   IoT Device Management Platform Launcher (11 steps)
echo   Ports: Gateway=8080, Backend=8081, Notification=8082, Nacos=8848
echo ============================================================
echo   Working dir: %CD%
echo.

echo [0/11] Stopping old processes...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING" 2^>nul') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081.*LISTENING" 2^>nul') do taskkill /F /PID %%a >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8082.*LISTENING" 2^>nul') do taskkill /F /PID %%a >nul 2>&1
if exist "D:\soft\Redis-x64-5.0.9\redis-cli.exe" "D:\soft\Redis-x64-5.0.9\redis-cli.exe" shutdown >nul 2>&1
taskkill /F /IM erl.exe >nul 2>&1
timeout /t 2 /nobreak >nul
echo        Done
echo.

echo [1/11] Redis...
if not exist "D:\soft\Redis-x64-5.0.9\redis-server.exe" (echo        WARNING: Redis not found & goto skip_redis)
"D:\soft\Redis-x64-5.0.9\redis-cli.exe" ping >nul 2>&1
if %errorlevel% equ 0 (echo        Redis already running & goto skip_redis)
start "Redis" /MIN "D:\soft\Redis-x64-5.0.9\redis-server.exe"
timeout /t 3 /nobreak >nul
"D:\soft\Redis-x64-5.0.9\redis-cli.exe" ping >nul 2>&1
if %errorlevel% equ 0 (echo        Redis started) else (echo        WARNING: Redis may not have started)
:skip_redis
echo.

echo [2/11] RabbitMQ...
set RMQ_BAT=D:\softPackage1\RabbitMQ Server\rabbitmq_server-3.12.14\sbin\rabbitmq-server.bat
netstat -ano 2>nul | findstr ":5672 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (echo        RabbitMQ already running & goto skip_rabbit)
if not exist "%RMQ_BAT%" (echo        WARNING: RabbitMQ not found & goto skip_rabbit)
powershell -Command "Start-Process -FilePath '%RMQ_BAT%' -WindowStyle Minimized" >nul 2>&1
timeout /t 20 /nobreak >nul
netstat -ano 2>nul | findstr ":5672 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (echo        RabbitMQ started) else (echo        WARNING: RabbitMQ may still be starting)
:skip_rabbit
echo.

echo [3/11] Nacos Server...
set NACOS_DIR=D:\softPackage1\nacos-server-3.1.1\nacos
set JDK17=D:\soft\JDK\jdk17
netstat -ano 2>nul | findstr ":8848 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (echo        Nacos already running & goto skip_nacos)
if not exist "%NACOS_DIR%\target\nacos-server.jar" (echo        INFO: Nacos not found - using local config & goto skip_nacos)
if not exist "%JDK17%\bin\java.exe" (echo        WARNING: Java 17 not found, Nacos needs JDK17+ & goto skip_nacos)
echo        Starting Nacos with JDK17...
start "Nacos" /MIN "%JDK17%\bin\java.exe" -Xms512m -Xmx512m -Xmn256m -Dnacos.standalone=true -Dnacos.deployment.type=merged -Dnacos.home="%NACOS_DIR%" -jar "%NACOS_DIR%\target\nacos-server.jar" --spring.config.additional-location="file:%NACOS_DIR%/conf/"
timeout /t 12 /nobreak >nul
netstat -ano 2>nul | findstr ":8848 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (echo        Nacos started on :8848) else (echo        WARNING: Nacos may still be starting)
:skip_nacos
echo.

echo [4/11] MySQL...
set MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.1\bin\mysql.exe
if not exist "%MYSQL_EXE%" (echo        FAIL: MySQL not found & goto end)
"%MYSQL_EXE%" -u root -p123456 iot_platform -e "SELECT 1" >nul 2>&1
if errorlevel 1 (echo        FAIL: MySQL connection failed & goto end)
echo        MySQL OK
echo.

echo [5/11] Java...
java -version >nul 2>&1
if %errorlevel% neq 0 (echo        FAIL: Java not found & goto end)
echo        Java OK
echo.

echo [6/11] Database check...
"%MYSQL_EXE%" -u root -p123456 iot_platform -e "SELECT COUNT(*) FROM t_product" >nul 2>&1
if errorlevel 1 (
    echo        First run - initializing database...
    "%MYSQL_EXE%" -u root -p123456 --default-character-set=utf8mb4 < "%~dp0sql\schema.sql" 2>nul
    "%MYSQL_EXE%" -u root -p123456 --default-character-set=utf8mb4 < "%~dp0sql\data.sql" 2>nul
    echo        Database initialized
) else (echo        Database OK)
echo.

echo [7/11] Building (Maven multi-module)...
echo        This may take 1-2 minutes on first build...
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (echo        FAIL: Build failed & goto end)
echo        Build OK
echo.

echo [8/11] Starting Backend (IoT Core :8081)...
cd backend
start "IoT-Backend" /MIN java -Dfile.encoding=UTF-8 -jar target\iot-platform-service-1.0.0-SNAPSHOT.jar
cd ..
echo        Waiting for startup...
set COUNT=0
:wait_backend
timeout /t 2 /nobreak >nul
set /a COUNT=%COUNT%+1
powershell -Command "try{$r=Invoke-WebRequest 'http://localhost:8081/api/device/dashboard' -UseBasicParsing -TimeoutSec 3;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 goto backend_ready
if %COUNT% lss 20 goto wait_backend
echo        WARNING: Backend startup taking longer
goto skip_backend_wait
:backend_ready
echo        Backend started OK on :8081
:skip_backend_wait
echo.

echo [9/11] Starting Notification Stub (:8082)...
cd mock-stubs
start "IoT-Notification" /MIN java -Dfile.encoding=UTF-8 -jar target\iot-notification-stub-1.0.0-SNAPSHOT.jar
cd ..
echo        Waiting for startup...
set COUNT=0
:wait_notify
timeout /t 2 /nobreak >nul
set /a COUNT=%COUNT%+1
powershell -Command "try{$r=Invoke-WebRequest 'http://localhost:8082/api/notification/health' -UseBasicParsing -TimeoutSec 3;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 goto notify_ready
if %COUNT% lss 10 goto wait_notify
echo        WARNING: Notification Stub startup taking longer
goto skip_notify_wait
:notify_ready
echo        Notification Stub started OK on :8082
:skip_notify_wait
echo.

echo [10/11] Starting Gateway (API Gateway :8080)...
cd gateway
start "IoT-Gateway" /MIN java -Dfile.encoding=UTF-8 -jar target\iot-gateway-1.0.0-SNAPSHOT.jar
cd ..
echo        Waiting for startup...
set COUNT=0
:wait_gateway
timeout /t 2 /nobreak >nul
set /a COUNT=%COUNT%+1
powershell -Command "try{$r=Invoke-WebRequest 'http://localhost:8080/actuator/health' -UseBasicParsing -TimeoutSec 3;exit 0}catch{exit 1}" >nul 2>&1
if %errorlevel% equ 0 goto gateway_ready
if %COUNT% lss 10 goto wait_gateway
echo        WARNING: Gateway startup taking longer
goto skip_gateway_wait
:gateway_ready
echo        Gateway started OK on :8080
:skip_gateway_wait
echo.

echo [11/11] Verifying...
echo.
echo ============================================================
echo   All services started!
echo   Gateway       : http://localhost:8080
echo   Backend       : http://localhost:8081
echo   Notification  : http://localhost:8082
echo   Nacos Console : http://localhost:8848/nacos
echo   Actuator      : http://localhost:8080/actuator/health
echo   Dashboard     : http://localhost:8080/api/device/dashboard
echo ============================================================
echo.

echo Quick health check:
powershell -Command "try{$r=Invoke-WebRequest 'http://localhost:8080/api/device/dashboard' -UseBasicParsing -TimeoutSec 5;Write-Host 'PASS: Gateway -> Backend OK'}catch{Write-Host 'WARN: check failed'}"
powershell -Command "try{$r=Invoke-WebRequest 'http://localhost:8082/api/notification/health' -UseBasicParsing -TimeoutSec 3;Write-Host 'PASS: Notification Stub OK'}catch{Write-Host 'WARN: check failed'}"
echo.

echo ============================================================
echo   设备模拟器(DeviceSimulator)已自动启动:
echo     - 200台MOCK设备正在上报遥测数据
echo     - 告警事件随机触发中(约5%%概率超阈值)
echo     - 打开浏览器查看实时数据变化
echo ============================================================
echo.

echo Tip: Use Bearer Token to access protected APIs
echo   curl -H "Authorization: Bearer iot-platform-demo-token-2024" http://localhost:8080/api/device/list
echo.

:: 优先打开Gateway欢迎页（避免中文路径问题）
start "" "http://localhost:8080/"

:: If Nacos is running, auto-open console in browser
netstat -ano 2>nul | findstr ":8848 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    start "" "http://localhost:8848/nacos"
    echo Nacos Console opened in browser
)

:end
echo Press any key to close...
pause >nul
