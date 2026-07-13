@echo off
cd /d "%~dp0"
git add .github/workflows/ci-cd.yml .gitignore
git commit -m "fix: CI skip tests"
git push origin master
echo Done
pause
