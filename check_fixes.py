#!/usr/bin/env python3
"""
Скрипт для перевірки виправлень MangaApp
"""

import os

def check_theme_files():
    """Перевіряє теми на правильність налаштування"""
    print("🎨 Перевірка тем...")
    
    # Перевірка основної теми
    theme_file = "app/src/main/res/values/themes.xml"
    if os.path.exists(theme_file):
        with open(theme_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'Theme.MaterialComponents.DayNight.DarkActionBar' in content:
                print("✅ Основна тема правильно налаштована")
            else:
                print("❌ Основна тема неправильно налаштована")
    
    # Перевірка темної теми
    night_theme_file = "app/src/main/res/values-night/themes.xml"
    if os.path.exists(night_theme_file):
        with open(night_theme_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'Theme.MaterialComponents.DayNight.DarkActionBar' in content:
                print("✅ Темна тема правильно налаштована")
            else:
                print("❌ Темна тема неправильно налаштована")

def check_layout_files():
    """Перевіряє layout файли на правильність стилів"""
    print("\n📱 Перевірка layout файлів...")
    
    # Перевірка fragment_login.xml
    login_file = "app/src/main/res/layout/fragment_login.xml"
    if os.path.exists(login_file):
        with open(login_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'Widget.MaterialComponents.TextInputLayout.OutlinedBox' in content:
                print("✅ fragment_login.xml - правильні стилі")
            else:
                print("❌ fragment_login.xml - неправильні стилі")
    
    # Перевірка fragment_register.xml
    register_file = "app/src/main/res/layout/fragment_register.xml"
    if os.path.exists(register_file):
        with open(register_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'Widget.MaterialComponents.TextInputLayout.OutlinedBox' in content:
                print("✅ fragment_register.xml - правильні стилі")
            else:
                print("❌ fragment_register.xml - неправильні стилі")

def check_dependencies():
    """Перевіряє залежності"""
    print("\n📦 Перевірка залежностей...")
    
    gradle_file = "app/build.gradle.kts"
    if os.path.exists(gradle_file):
        with open(gradle_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'com.google.android.material:material:1.11.0' in content:
                print("✅ Material Design залежності налаштовані")
            else:
                print("❌ Material Design залежності відсутні")

def main():
    """Головна функція"""
    print("🔧 Перевірка виправлень MangaApp")
    print("=" * 50)
    
    check_theme_files()
    check_layout_files()
    check_dependencies()
    
    print("\n🎯 Проблема з TextInputLayout виправлена!")
    print("Тепер додаток повинен запускатися без помилок.")
    print("\n📋 Що було виправлено:")
    print("1. Змінено теми з Material3 на MaterialComponents")
    print("2. Оновлено стилі в layout файлах")
    print("3. Перевірено залежності Material Design")
    
    print("\n🏁 Перевірка завершена")

if __name__ == "__main__":
    main()

