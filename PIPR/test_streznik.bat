@echo off
set URL=http://localhost:8080/data

for /L %%i in (1,1,20) do (
    echo Sending data %%i
    curl -s -X POST %URL% --data-binary "test-podatek-%%i"
)

echo DONE
pause
