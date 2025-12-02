#!/usr/bin/env python3
"""
Скрипт для перевірки Android проекту MangaApp
"""

import os
import sys

def check_android_files():
    """Перевіряє наявність всіх необхідних Android файлів"""
    print("🔍 Перевірка Android проекту MangaApp")
    print("=" * 50)
    
    files_to_check = [
        ("app/src/main/java/com/example/mangaapp/fragments/LoginFragment.java", "LoginFragment"),
        ("app/src/main/java/com/example/mangaapp/fragments/RegisterFragment.java", "RegisterFragment"),
        ("app/src/main/java/com/example/mangaapp/fragments/AccountFragment.java", "AccountFragment"),
        ("app/src/main/java/com/example/mangaapp/adapters/AuthPagerAdapter.java", "AuthPagerAdapter"),
        ("app/src/main/java/com/example/mangaapp/adapters/RecentMangaAdapter.java", "RecentMangaAdapter"),
        ("app/src/main/java/com/example/mangaapp/api/AccountApiService.java", "AccountApiService"),
        ("app/src/main/java/com/example/mangaapp/models/User.java", "User модель"),
        ("app/src/main/java/com/example/mangaapp/models/RecentManga.java", "RecentManga модель"),
        ("app/src/main/res/layout/fragment_login.xml", "Layout входу"),
        ("app/src/main/res/layout/fragment_register.xml", "Layout реєстрації"),
        ("app/src/main/res/layout/fragment_account.xml", "Layout акаунта"),
        ("app/src/main/res/layout/item_recent_manga.xml", "Layout манги"),
        ("app/src/main/res/navigation/nav_graph.xml", "Навігаційний граф"),
        ("app/src/main/res/values/strings.xml", "Рядки"),
        ("app/src/main/res/values/colors.xml", "Кольори"),
        ("app/src/main/res/values/themes.xml", "Теми"),
        ("app/src/main/res/values/styles.xml", "Стилі"),
        ("app/build.gradle.kts", "Gradle залежності")
    ]
    
    found_files = 0
    total_files = len(files_to_check)
    
    for file_path, description in files_to_check:
        if os.path.exists(file_path):
            print(f"✅ {description}: {file_path}")
            found_files += 1
        else:
            print(f"❌ {description}: {file_path} - НЕ ЗНАЙДЕНО")
    
    print("\n" + "=" * 50)
    print(f"📊 Результат: {found_files}/{total_files} файлів знайдено")
    
    if found_files == total_files:
        print("🎉 Всі Android файли на місці! Проект готовий до компіляції.")
    else:
        print("⚠️  Деякі файли відсутні. Перевірте структуру проекту.")
    
    return found_files == total_files

def check_layout_files():
    """Перевіряє layout файли на наявність необхідних ID"""
    print("\n🎨 Перевірка layout файлів...")
    
    # Перевірка fragment_login.xml
    login_file = "app/src/main/res/layout/fragment_login.xml"
    if os.path.exists(login_file):
        with open(login_file, 'r', encoding='utf-8') as f:
            content = f.read()
            required_ids = ['btn_login', 'login_progress', 'login_error']
            missing_ids = [id for id in required_ids if f'android:id="@+id/{id}"' not in content]
            
            if not missing_ids:
                print("✅ fragment_login.xml - всі необхідні ID присутні")
            else:
                print(f"❌ fragment_login.xml - відсутні ID: {missing_ids}")
    
    # Перевірка fragment_register.xml
    register_file = "app/src/main/res/layout/fragment_register.xml"
    if os.path.exists(register_file):
        with open(register_file, 'r', encoding='utf-8') as f:
            content = f.read()
            required_ids = ['btn_register', 'register_progress', 'register_error']
            missing_ids = [id for id in required_ids if f'android:id="@+id/{id}"' not in content]
            
            if not missing_ids:
                print("✅ fragment_register.xml - всі необхідні ID присутні")
            else:
                print(f"❌ fragment_register.xml - відсутні ID: {missing_ids}")

def main():
    """Головна функція"""
    print("🚀 Перевірка Android проекту MangaApp")
    print("=" * 50)
    
    # Перевірка Android файлів
    android_ok = check_android_files()
    
    # Перевірка layout файлів
    if android_ok:
        check_layout_files()
        
        print("\n🎯 Наступні кроки:")
        print("1. Відкрийте Android проект в Android Studio")
        print("2. Синхронізуйте проект з Gradle")
        print("3. Перевірте, чи немає помилок компіляції")
        print("4. Запустіть додаток на пристрої/емуляторі")
    else:
        print("\n🔧 Виправте структуру Android проекту перед продовженням")
    
    print("\n🏁 Перевірка завершена")

if __name__ == "__main__":
    main()

