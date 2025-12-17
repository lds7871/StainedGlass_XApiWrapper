@echo off
chcp 65001 >nul
cd /d %~dp0

echo 正在从 GitHub 同步项目...

git fetch origin
git pull origin main

echo 同步完成！
pause
