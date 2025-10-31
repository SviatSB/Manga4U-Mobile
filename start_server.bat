@echo off
echo Starting MangaApp Server...
echo.
echo Make sure you have Python installed and requirements.txt dependencies installed
echo.
echo Installing dependencies...
pip install -r requirements.txt
echo.
echo Starting server...
python server.py
pause
