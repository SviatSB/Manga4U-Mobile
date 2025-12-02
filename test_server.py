#!/usr/bin/env python3
"""
Тестовий скрипт для перевірки роботи MangaApp сервера
"""

import requests
import json

# Базовий URL сервера
BASE_URL = "http://localhost:5000"

def test_server_health():
    """Перевірка доступності сервера"""
    try:
        response = requests.get(f"{BASE_URL}/")
        print(f"✅ Сервер доступний: {response.status_code}")
        return True
    except requests.exceptions.ConnectionError:
        print("❌ Сервер недоступний. Переконайтеся, що сервер запущений.")
        return False
    except Exception as e:
        print(f"❌ Помилка підключення: {e}")
        return False

def test_user_registration():
    """Тест реєстрації користувача"""
    print("\n🔐 Тестування реєстрації користувача...")
    
    user_data = {
        "contact": "test@example.com",
        "login": "testuser",
        "password": "password123"
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/auth/register",
            json=user_data,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success"):
                print("✅ Користувача успішно зареєстровано")
                print(f"   Токен: {data.get('token', '')[:20]}...")
                return data.get("token")
            else:
                print(f"❌ Помилка реєстрації: {data.get('message')}")
                return None
        else:
            print(f"❌ HTTP помилка: {response.status_code}")
            print(f"   Відповідь: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Помилка запиту: {e}")
        return None

def test_user_login():
    """Тест входу користувача"""
    print("\n🔑 Тестування входу користувача...")
    
    login_data = {
        "login": "testuser",
        "password": "password123"
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/auth/login",
            json=login_data,
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success"):
                print("✅ Успішний вхід в систему")
                print(f"   Токен: {data.get('token', '')[:20]}...")
                return data.get("token")
            else:
                print(f"❌ Помилка входу: {data.get('message')}")
                return None
        else:
            print(f"❌ HTTP помилка: {response.status_code}")
            print(f"   Відповідь: {response.text}")
            return None
            
    except Exception as e:
        print(f"❌ Помилка запиту: {e}")
        return None

def test_user_profile(token):
    """Тест отримання профілю користувача"""
    print("\n👤 Тестування отримання профілю користувача...")
    
    if not token:
        print("❌ Токен не надано")
        return False
    
    try:
        response = requests.get(
            f"{BASE_URL}/user/profile",
            params={"token": token}
        )
        
        if response.status_code == 200:
            data = response.json()
            print("✅ Профіль користувача отримано")
            print(f"   Логін: {data.get('login')}")
            print(f"   Email: {data.get('email')}")
            print(f"   ID: {data.get('id')}")
            return True
        else:
            print(f"❌ HTTP помилка: {response.status_code}")
            print(f"   Відповідь: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Помилка запиту: {e}")
        return False

def test_reading_progress(token):
    """Тест оновлення прогресу читання"""
    print("\n📚 Тестування оновлення прогресу читання...")
    
    if not token:
        print("❌ Токен не надано")
        return False
    
    progress_data = {
        "mangaId": "test_manga_001",
        "mangaTitle": "Тестова манга",
        "mangaCoverUrl": "https://example.com/cover.jpg",
        "chapterId": "chapter_001",
        "chapterTitle": "Глава 1",
        "currentPage": 5,
        "totalPages": 20
    }
    
    try:
        response = requests.post(
            f"{BASE_URL}/user/reading-progress",
            json=progress_data,
            params={"token": token},
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            data = response.json()
            if data.get("success"):
                print("✅ Прогрес читання оновлено")
                return True
            else:
                print(f"❌ Помилка оновлення: {data.get('message')}")
                return False
        else:
            print(f"❌ HTTP помилка: {response.status_code}")
            print(f"   Відповідь: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Помилка запиту: {e}")
        return False

def test_recent_manga(token):
    """Тест отримання останніх манг"""
    print("\n📖 Тестування отримання останніх манг...")
    
    if not token:
        print("❌ Токен не надано")
        return False
    
    try:
        response = requests.get(
            f"{BASE_URL}/user/recent-manga",
            params={"token": token, "limit": 5}
        )
        
        if response.status_code == 200:
            data = response.json()
            print(f"✅ Отримано {len(data)} останніх манг")
            for i, manga in enumerate(data, 1):
                print(f"   {i}. {manga.get('title')} - {manga.get('chapterTitle')}")
            return True
        else:
            print(f"❌ HTTP помилка: {response.status_code}")
            print(f"   Відповідь: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Помилка запиту: {e}")
        return False

def main():
    """Головна функція тестування"""
    print("🚀 Запуск тестування MangaApp сервера")
    print("=" * 50)
    
    # Перевірка доступності сервера
    if not test_server_health():
        return
    
    # Тестування реєстрації
    token = test_user_registration()
    
    # Якщо реєстрація не вдалася, спробуємо вхід
    if not token:
        token = test_user_login()
    
    # Тестування з отриманим токеном
    if token:
        test_user_profile(token)
        test_reading_progress(token)
        test_recent_manga(token)
    else:
        print("\n❌ Не вдалося отримати токен авторизації")
    
    print("\n" + "=" * 50)
    print("🏁 Тестування завершено")

if __name__ == "__main__":
    main()
