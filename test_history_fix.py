#!/usr/bin/env python3
"""
Скрипт для перевірки виправлень історії MangaApp
"""

import os

def check_history_fix():
    """Перевіряє виправлення історії"""
    print("📚 Перевірка виправлень історії MangaApp")
    print("=" * 50)
    
    # Перевірка AccountApiService
    api_file = "app/src/main/java/com/example/mangaapp/api/AccountApiService.java"
    if os.path.exists(api_file):
        with open(api_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'public int getTotalPages()' in content:
                print("✅ AccountApiService - getTotalPages повертає int")
            else:
                print("❌ AccountApiService - getTotalPages не повертає int")
                
            if 'updateReadingProgress' in content:
                print("✅ AccountApiService - метод updateReadingProgress наявний")
            else:
                print("❌ AccountApiService - метод updateReadingProgress відсутній")
    
    # Перевірка TestHistoryFragment
    test_file = "app/src/main/java/com/example/mangaapp/fragments/TestHistoryFragment.java"
    if os.path.exists(test_file):
        print("✅ TestHistoryFragment - створено для тестування")
    else:
        print("❌ TestHistoryFragment - не створено")
    
    # Перевірка layout тестування
    test_layout = "app/src/main/res/layout/fragment_test_history.xml"
    if os.path.exists(test_layout):
        print("✅ Layout для тестування історії створено")
    else:
        print("❌ Layout для тестування історії не створено")
    
    # Перевірка навігації
    nav_file = "app/src/main/res/navigation/nav_graph.xml"
    if os.path.exists(nav_file):
        with open(nav_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'nav_test_history' in content:
                print("✅ Навігація - тестовий фрагмент додано")
            else:
                print("❌ Навігація - тестовий фрагмент не додано")
    
    # Перевірка AccountFragment
    account_file = "app/src/main/java/com/example/mangaapp/fragments/AccountFragment.java"
    if os.path.exists(account_file):
        with open(account_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'testHistory()' in content:
                print("✅ AccountFragment - метод testHistory додано")
            else:
                print("❌ AccountFragment - метод testHistory не додано")
                
            if 'btn_test_history' in content:
                print("✅ AccountFragment - кнопка тестування додана")
            else:
                print("❌ AccountFragment - кнопка тестування не додана")

def main():
    """Головна функція"""
    check_history_fix()
    
    print("\n🎯 Що було виправлено:")
    print("1. Виправлено getTotalPages() в AccountApiService")
    print("2. Створено TestHistoryFragment для тестування")
    print("3. Додано кнопку тестування в AccountFragment")
    print("4. Додано тестовий фрагмент до навігації")
    
    print("\n📋 Як тестувати історію:")
    print("1. Увійдіть в акаунт")
    print("2. Натисніть 'Тестувати історію'")
    print("3. Перевірте, чи з'явилася манга в історії")
    print("4. Якщо так - історія працює!")
    
    print("\n🔧 Якщо історія не працює:")
    print("- Перевірте логи сервера")
    print("- Перевірте, чи правильно налаштований API")
    print("- Перевірте, чи зберігаються дані в базі")
    
    print("\n🏁 Перевірка завершена")

if __name__ == "__main__":
    main()
