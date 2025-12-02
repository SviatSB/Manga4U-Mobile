#!/usr/bin/env python3
"""
Скрипт для перевірки структури проекту MangaApp
"""

import os
import sys

def check_file_exists(file_path, description):
    """Перевіряє чи існує файл"""
    if os.path.exists(file_path):
        print(f"✅ {description}: {file_path}")
        return True
    else:
        print(f"❌ {description}: {file_path} - НЕ ЗНАЙДЕНО")
        return False

def check_directory_structure():
    """Перевіряє структуру директорій"""
    print("🔍 Перевірка структури проекту MangaApp")
    print("=" * 50)
    
    files_to_check = [
        ("server.py", "Python сервер"),
        ("requirements.txt", "Python залежності"),
        ("test_server.py", "Тестовий скрипт"),
        ("README.md", "Головний README"),
        ("README_QUICK_START.md", "Швидкий запуск"),
        ("README_ACCOUNT_SYSTEM.md", "Система акаунтів"),
        ("start_server.bat", "Windows скрипт запуску"),
        ("start_server.sh", "Linux/Mac скрипт запуску"),
        ("Android/app/build.gradle.kts", "Android залежності"),
        ("Android/app/src/main/java/com/example/mangaapp/fragments/AccountFragment.java", "AccountFragment"),
        ("Android/app/src/main/java/com/example/mangaapp/api/AccountApiService.java", "AccountApiService"),
        ("Android/app/src/main/java/com/example/mangaapp/models/User.java", "User модель"),
        ("Android/app/src/main/java/com/example/mangaapp/models/RecentManga.java", "RecentManga модель"),
        ("Android/app/src/main/java/com/example/mangaapp/adapters/RecentMangaAdapter.java", "RecentMangaAdapter"),
        ("Android/app/src/main/java/com/example/mangaapp/adapters/AuthPagerAdapter.java", "AuthPagerAdapter"),
        ("Android/app/src/main/res/layout/fragment_account.xml", "Layout акаунта"),
        ("Android/app/src/main/res/layout/item_recent_manga.xml", "Layout манги"),
        ("Android/app/src/main/res/navigation/nav_graph.xml", "Навігаційний граф"),
        ("Android/app/src/main/res/values/strings.xml", "Рядки"),
        ("Android/app/src/main/res/values/colors.xml", "Кольори"),
        ("Android/app/src/main/res/values/styles.xml", "Стилі")
    ]
    
    found_files = 0
    total_files = len(files_to_check)
    
    for file_path, description in files_to_check:
        if check_file_exists(file_path, description):
            found_files += 1
    
    print("\n" + "=" * 50)
    print(f"📊 Результат: {found_files}/{total_files} файлів знайдено")
    
    if found_files == total_files:
        print("🎉 Всі файли на місці! Проект готовий до запуску.")
    else:
        print("⚠️  Деякі файли відсутні. Перевірте структуру проекту.")
    
    return found_files == total_files

def check_python_dependencies():
    """Перевіряє Python залежності"""
    print("\n🐍 Перевірка Python залежностей...")
    
    try:
        import flask
        print("✅ Flask встановлено")
    except ImportError:
        print("❌ Flask НЕ встановлено")
        return False
    
    try:
        import flask_cors
        print("✅ Flask-CORS встановлено")
    except ImportError:
        print("❌ Flask-CORS НЕ встановлено")
        return False
    
    try:
        import jwt
        print("✅ PyJWT встановлено")
    except ImportError:
        print("❌ PyJWT НЕ встановлено")
        return False
    
    return True

def main():
    """Головна функція"""
    print("🚀 Перевірка проекту MangaApp")
    print("=" * 50)
    
    # Перевірка структури
    structure_ok = check_directory_structure()
    
    # Перевірка Python залежностей
    if structure_ok:
        deps_ok = check_python_dependencies()
        
        if deps_ok:
            print("\n🎯 Наступні кроки:")
            print("1. Запустіть сервер: python server.py")
            print("2. Відкрийте Android проект в Android Studio")
            print("3. Синхронізуйте проект з Gradle")
            print("4. Запустіть додаток на пристрої/емуляторі")
        else:
            print("\n📦 Встановіть Python залежності:")
            print("pip install -r requirements.txt")
    else:
        print("\n🔧 Виправте структуру проекту перед продовженням")
    
    print("\n🏁 Перевірка завершена")

if __name__ == "__main__":
    main()

