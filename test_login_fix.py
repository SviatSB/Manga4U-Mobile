#!/usr/bin/env python3
"""
Скрипт для перевірки виправлень логіну MangaApp
"""

import os

def check_login_fix():
    """Перевіряє виправлення логіну"""
    print("🔐 Перевірка виправлень логіну MangaApp")
    print("=" * 50)
    
    # Перевірка AccountFragment
    account_file = "app/src/main/java/com/example/mangaapp/fragments/AccountFragment.java"
    if os.path.exists(account_file):
        with open(account_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'authPagerAdapter.setLoginListener(this)' in content:
                print("✅ AccountFragment - правильно налаштований callback для логіну")
            else:
                print("❌ AccountFragment - callback для логіну не налаштований")
                
            if 'authPagerAdapter.setRegisterListener(this)' in content:
                print("✅ AccountFragment - правильно налаштований callback для реєстрації")
            else:
                print("❌ AccountFragment - callback для реєстрації не налаштований")
    
    # Перевірка AuthPagerAdapter
    adapter_file = "app/src/main/java/com/example/mangaapp/adapters/AuthPagerAdapter.java"
    if os.path.exists(adapter_file):
        with open(adapter_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'setLoginListener' in content and 'setRegisterListener' in content:
                print("✅ AuthPagerAdapter - методи для callback наявні")
            else:
                print("❌ AuthPagerAdapter - методи для callback відсутні")
                
            if 'loginFragment.setOnLoginSuccessListener(loginListener)' in content:
                print("✅ AuthPagerAdapter - callback правильно передається до LoginFragment")
            else:
                print("❌ AuthPagerAdapter - callback не передається до LoginFragment")
    
    # Перевірка LoginFragment
    login_file = "app/src/main/java/com/example/mangaapp/fragments/LoginFragment.java"
    if os.path.exists(login_file):
        with open(login_file, 'r', encoding='utf-8') as f:
            content = f.read()
            
            if 'setOnLoginSuccessListener' in content:
                print("✅ LoginFragment - метод setOnLoginSuccessListener наявний")
            else:
                print("❌ LoginFragment - метод setOnLoginSuccessListener відсутній")

def main():
    """Головна функція"""
    check_login_fix()
    
    print("\n🎯 Що було виправлено:")
    print("1. Оновлено AuthPagerAdapter для передачі callback")
    print("2. Налаштовано правильну передачу callback в AccountFragment")
    print("3. Додано логування для діагностики")
    
    print("\n📋 Тепер після успішного логіну:")
    print("- Callback повинен правильно передаватися")
    print("- Екран акаунту повинен показуватися")
    print("- Користувач повинен бачити свій профіль")
    
    print("\n🏁 Перевірка завершена")

if __name__ == "__main__":
    main()

