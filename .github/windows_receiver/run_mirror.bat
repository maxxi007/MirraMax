@echo off
REM MirraMax Windows receiver launcher
REM Requires: adb in PATH, ffplay in PATH (ffmpeg build with cuvid/nvdec recommended)

echo Forwarding ports via adb...
adb forward tcp:5004 tcp:5004
adb forward tcp:5005 tcp:5005

echo Starting ffplay receiver (tries NVIDIA hwaccel)
call ffplay_wrapper.bat
pause
