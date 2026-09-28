@echo off
chcp 65001 >nul
cd /d "D:\ruanjian\ai-chat-platform1"
echo ============================================
echo    推送到 Gitee 仓库
echo    蘑菇嘎达 / ai-chat-platform1
echo ============================================
echo.
set /p TOKEN=请输入你的Gitee私人令牌（粘贴后按回车）: 
if "%TOKEN%"=="" (
  echo.
  echo 令牌不能为空！
  pause
  exit /b
)
git push "https://mushroom-gada:%TOKEN%@gitee.com/mushroom-gada/ai-chat-platform1.git" main
if %errorlevel%==0 (
  echo.
  echo ============================================
  echo   ✅ 推送成功！刷新 Gitee 页面就能看到文件列表
  echo ============================================
) else (
  echo.
  echo   ❌ 推送失败，请检查令牌是否正确或已过期
)
pause
