

C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe -Command "$ts=Get-Date -Format 'yyyyMMdd_HHmmss'; Rename-Item 'sokoban-android\build\outputs\apk\debug\sokoban-android-debug.apk' ('apk_' + $ts + '.apk')"

ren  sokoban-android\build\outputs\apk\debug\sokoban-android-debug.apk  "app_%timestamp%.apk"
del C:\Users\ShibuPC\Downloads\5\*.apk
copy "sokoban-android\build\outputs\apk\debug\*.apk" C:\Users\ShibuPC\Downloads\5\ /y
