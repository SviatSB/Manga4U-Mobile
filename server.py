from flask import Flask, request, jsonify
from flask_cors import CORS
import sqlite3
import jwt
import datetime
import hashlib
import os

app = Flask(__name__)
app.config['SECRET_KEY'] = 'your-secret-key-here'
CORS(app)

# Створення бази даних
def init_db():
    conn = sqlite3.connect('manga_app.db')
    cursor = conn.cursor()
    
    # Таблиця користувачів
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            login TEXT UNIQUE NOT NULL,
            email TEXT,
            phone TEXT,
            password_hash TEXT NOT NULL,
            avatar_url TEXT,
            registration_date INTEGER,
            last_login_date INTEGER
        )
    ''')
    
    # Таблиця історії читання
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS reading_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            manga_id TEXT NOT NULL,
            manga_title TEXT NOT NULL,
            manga_cover_url TEXT,
            chapter_id TEXT NOT NULL,
            chapter_title TEXT,
            current_page INTEGER DEFAULT 0,
            total_pages INTEGER DEFAULT 0,
            last_read_time INTEGER,
            FOREIGN KEY (user_id) REFERENCES users (id)
        )
    ''')
    
    conn.commit()
    conn.close()

# Ініціалізація бази даних
init_db()

@app.route('/auth/register', methods=['POST'])
def register():
    data = request.get_json()
    
    login = data.get('login')
    contact = data.get('contact')
    password = data.get('password')
    
    if not all([login, contact, password]):
        return jsonify({'success': False, 'message': 'Всі поля обов\'язкові'}), 400
    
    # Хешування пароля
    password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    try:
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        # Перевірка чи існує користувач
        cursor.execute('SELECT id FROM users WHERE login = ?', (login,))
        if cursor.fetchone():
            return jsonify({'success': False, 'message': 'Користувач з таким логіном вже існує'}), 400
        
        # Визначення типу контакту
        email = None
        phone = None
        if '@' in contact:
            email = contact
        else:
            phone = contact
        
        # Створення користувача
        now = int(datetime.datetime.now().timestamp())
        cursor.execute('''
            INSERT INTO users (login, email, phone, password_hash, registration_date, last_login_date)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (login, email, phone, password_hash, now, now))
        
        user_id = cursor.lastrowid
        
        # Створення токена
        token = jwt.encode({
            'user_id': user_id,
            'login': login,
            'exp': datetime.datetime.utcnow() + datetime.timedelta(days=30)
        }, app.config['SECRET_KEY'], algorithm='HS256')
        
        # Отримання створеного користувача
        cursor.execute('SELECT * FROM users WHERE id = ?', (user_id,))
        user_data = cursor.fetchone()
        
        user = {
            'id': user_data[0],
            'login': user_data[1],
            'email': user_data[2],
            'phone': user_data[3],
            'avatar_url': user_data[4],
            'registration_date': user_data[5],
            'last_login_date': user_data[6]
        }
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': 'Користувач успішно створено',
            'token': token,
            'user': user
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

@app.route('/auth/login', methods=['POST'])
def login():
    data = request.get_json()
    
    login = data.get('login')
    password = data.get('password')
    
    if not all([login, password]):
        return jsonify({'success': False, 'message': 'Всі поля обов\'язкові'}), 400
    
    password_hash = hashlib.sha256(password.encode()).hexdigest()
    
    try:
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        # Пошук користувача
        cursor.execute('SELECT * FROM users WHERE (login = ? OR email = ?) AND password_hash = ?', 
                      (login, login, password_hash))
        user_data = cursor.fetchone()
        
        if not user_data:
            return jsonify({'success': False, 'message': 'Невірний логін або пароль'}), 401
        
        # Оновлення часу останнього входу
        now = int(datetime.datetime.now().timestamp())
        cursor.execute('UPDATE users SET last_login_date = ? WHERE id = ?', (now, user_data[0]))
        
        user = {
            'id': user_data[0],
            'login': user_data[1],
            'email': user_data[2],
            'phone': user_data[3],
            'avatar_url': user_data[4],
            'registration_date': user_data[5],
            'last_login_date': now
        }
        
        # Створення токена
        token = jwt.encode({
            'user_id': user['id'],
            'login': user['login'],
            'exp': datetime.datetime.utcnow() + datetime.timedelta(days=30)
        }, app.config['SECRET_KEY'], algorithm='HS256')
        
        conn.commit()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': 'Успішний вхід',
            'token': token,
            'user': user
        })
        
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

@app.route('/user/profile', methods=['GET'])
def get_user_profile():
    token = request.args.get('token')
    
    if not token:
        return jsonify({'success': False, 'message': 'Токен не надано'}), 401
    
    try:
        # Декодування токена
        payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
        user_id = payload['user_id']
        
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        cursor.execute('SELECT * FROM users WHERE id = ?', (user_id,))
        user_data = cursor.fetchone()
        
        if not user_data:
            return jsonify({'success': False, 'message': 'Користувача не знайдено'}), 404
        
        user = {
            'id': user_data[0],
            'login': user_data[1],
            'email': user_data[2],
            'phone': user_data[3],
            'avatar_url': user_data[4],
            'registration_date': user_data[5],
            'last_login_date': user_data[6]
        }
        
        conn.close()
        
        return jsonify(user)
        
    except jwt.ExpiredSignatureError:
        return jsonify({'success': False, 'message': 'Токен застарів'}), 401
    except jwt.InvalidTokenError:
        return jsonify({'success': False, 'message': 'Невірний токен'}), 401
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

@app.route('/user/profile', methods=['PUT'])
def update_user_profile():
    token = request.args.get('token')
    
    if not token:
        return jsonify({'success': False, 'message': 'Токен не надано'}), 401
    
    try:
        payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
        user_id = payload['user_id']
        
        data = request.get_json()
        
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        # Оновлення профілю
        if 'email' in data:
            cursor.execute('UPDATE users SET email = ? WHERE id = ?', (data['email'], user_id))
        if 'phone' in data:
            cursor.execute('UPDATE users SET phone = ? WHERE id = ?', (data['phone'], user_id))
        if 'avatar_url' in data:
            cursor.execute('UPDATE users SET avatar_url = ? WHERE id = ?', (data['avatar_url'], user_id))
        
        conn.commit()
        
        # Отримання оновленого профілю
        cursor.execute('SELECT * FROM users WHERE id = ?', (user_id,))
        user_data = cursor.fetchone()
        
        user = {
            'id': user_data[0],
            'login': user_data[1],
            'email': user_data[2],
            'phone': user_data[3],
            'avatar_url': user_data[4],
            'registration_date': user_data[5],
            'last_login_date': user_data[6]
        }
        
        conn.close()
        
        return jsonify(user)
        
    except jwt.ExpiredSignatureError:
        return jsonify({'success': False, 'message': 'Токен застарів'}), 401
    except jwt.InvalidTokenError:
        return jsonify({'success': False, 'message': 'Невірний токен'}), 401
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

@app.route('/user/recent-manga', methods=['GET'])
def get_recent_manga():
    token = request.args.get('token')
    limit = int(request.args.get('limit', 10))
    
    if not token:
        return jsonify({'success': False, 'message': 'Токен не надано'}), 401
    
    try:
        payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
        user_id = payload['user_id']
        
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT * FROM reading_history 
            WHERE user_id = ? 
            ORDER BY last_read_time DESC 
            LIMIT ?
        ''', (user_id, limit))
        
        history_data = cursor.fetchall()
        
        recent_manga = []
        for row in history_data:
            manga = {
                'mangaId': row[2],
                'title': row[3],
                'coverUrl': row[4],
                'chapterId': row[5],
                'chapterTitle': row[6],
                'currentPage': row[7],
                'totalPages': row[8],
                'lastReadTime': row[9]
            }
            recent_manga.append(manga)
        
        conn.close()
        
        return jsonify(recent_manga)
        
    except jwt.ExpiredSignatureError:
        return jsonify({'success': False, 'message': 'Токен застарів'}), 401
    except jwt.InvalidTokenError:
        return jsonify({'success': False, 'message': 'Невірний токен'}), 401
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

@app.route('/user/reading-progress', methods=['POST'])
def update_reading_progress():
    token = request.args.get('token')
    
    if not token:
        return jsonify({'success': False, 'message': 'Токен не надано'}), 401
    
    try:
        payload = jwt.decode(token, app.config['SECRET_KEY'], algorithms=['HS256'])
        user_id = payload['user_id']
        
        data = request.get_json()
        manga_id = data.get('mangaId')
        chapter_id = data.get('chapterId')
        current_page = data.get('currentPage', 0)
        total_pages = data.get('totalPages', 0)
        manga_title = data.get('mangaTitle', 'Назва манги')
        manga_cover_url = data.get('mangaCoverUrl', '')
        chapter_title = data.get('chapterTitle', 'Глава')
        
        conn = sqlite3.connect('manga_app.db')
        cursor = conn.cursor()
        
        # Перевірка чи існує запис
        cursor.execute('''
            SELECT id FROM reading_history 
            WHERE user_id = ? AND manga_id = ? AND chapter_id = ?
        ''', (user_id, manga_id, chapter_id))
        
        existing_record = cursor.fetchone()
        now = int(datetime.datetime.now().timestamp())
        
        if existing_record:
            # Оновлення існуючого запису
            cursor.execute('''
                UPDATE reading_history 
                SET current_page = ?, total_pages = ?, last_read_time = ?, 
                    manga_title = ?, manga_cover_url = ?, chapter_title = ?
                WHERE id = ?
            ''', (current_page, total_pages, now, manga_title, manga_cover_url, 
                  chapter_title, existing_record[0]))
        else:
            # Створення нового запису
            cursor.execute('''
                INSERT INTO reading_history 
                (user_id, manga_id, manga_title, manga_cover_url, chapter_id, chapter_title, 
                 current_page, total_pages, last_read_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (user_id, manga_id, manga_title, manga_cover_url, chapter_id, 
                  chapter_title, current_page, total_pages, now))
        
        conn.commit()
        conn.close()
        
        return jsonify({'success': True, 'message': 'Прогрес оновлено'})
        
    except jwt.ExpiredSignatureError:
        return jsonify({'success': False, 'message': 'Токен застарів'}), 401
    except jwt.InvalidTokenError:
        return jsonify({'success': False, 'message': 'Невірний токен'}), 401
    except Exception as e:
        return jsonify({'success': False, 'message': f'Помилка сервера: {str(e)}'}), 500

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)