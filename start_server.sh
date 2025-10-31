#!/bin/bash

echo "Starting MangaApp Server..."
echo ""
echo "Make sure you have Python installed and requirements.txt dependencies installed"
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "Python3 is not installed. Please install Python 3.7+ first."
    exit 1
fi

echo "Installing dependencies..."
pip3 install -r requirements.txt

echo ""
echo "Starting server..."
python3 server.py
