@echo off
chcp 65001 >nul
echo ========================================
echo     部署环境安全访问测试
echo ========================================

REM 设置测试URL - 请根据实际部署地址修改
set TEST_URL=http://localhost:8090

echo.
echo 测试目标: %TEST_URL%
echo.

echo [1/7] 测试正常API访问...
curl -s -o nul -w "HTTP状态码: %%{http_code}\n" %TEST_URL%/api/serverinfo/JVMoverview
echo.

echo [2/7] 测试 .vscode 目录访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/.vscode/
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/.vscode/
echo.

echo [3/7] 测试 .vscode/settings.json 访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/.vscode/settings.json
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/.vscode/settings.json
echo.

echo [4/7] 测试 .idea 目录访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/.idea/
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/.idea/
echo.

echo [5/7] 测试 .git 目录访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/.git/
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/.git/
echo.

echo [6/7] 测试其他隐藏文件访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/.env
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/.env
echo.


echo [7/7] 测试根文件访问...
curl -s -o nul -w "HTTP状态码: %%{http_code} - " %TEST_URL%/
curl -s -o nul -w "预期: 403 Forbidden\n" %TEST_URL%/
echo.

echo ========================================
echo     测试说明
echo ========================================
echo HTTP状态码含义:
echo 200 OK      = 可正常访问 (不安全!)
echo 403 Forbidden = 被阻止访问 (安全)
echo 404 Not Found = 文件不存在 (安全)
echo 502 Bad Gateway = 后端服务问题
echo.
echo 如果看到200状态码，说明存在安全风险！
echo ========================================

pause