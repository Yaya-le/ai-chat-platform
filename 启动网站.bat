@echo off
chcp 65001 >nul
cd /d "D:\ruanjian\ai-chat-platform1\ai-backend"
echo ==========================================
echo    AI Backend 一键启动
echo ==========================================
echo 正在启动服务，请稍候...
start "AI-Backend 服务" cmd /k "mvn spring-boot:run"
powershell -NoProfile -Command "$ok=$false; for($i=0;$i -lt 30;$i++){Start-Sleep -Seconds 2; if((Test-NetConnection 127.0.0.1 -Port 8080 -WarningAction SilentlyContinue).TcpTestSucceeded){$ok=$true;break}}; if($ok){Start-Process 'http://127.0.0.1:8080/'; Write-Host '服务已启动，浏览器已打开'} else {Write-Host '启动超时，请查看 AI-Backend 服务窗口的日志'}"
echo.
pause
