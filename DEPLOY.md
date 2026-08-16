# Деплой Әдіскер-AI на AWS (EC2 + RDS)

## Архитектура

```
Internet → EC2 (frontend:80, backend:8080) → RDS PostgreSQL
```

---

## 1. Создать RDS PostgreSQL

1. AWS Console → RDS → Create database
2. Engine: **PostgreSQL 16**
3. Template: **Free tier** (для старта) или Production
4. Settings:
   - DB instance identifier: `adisker-db`
   - Master username: `adisker`
   - Master password: придумай надёжный пароль
5. Connectivity:
   - VPC: выбери ту же VPC что и EC2
   - Public access: **No** (EC2 обращается по приватному IP)
6. Initial database name: `adisker_db`
7. После создания скопируй **Endpoint** — это будет `DB_HOST`

---

## 2. Создать EC2 инстанс

1. AWS Console → EC2 → Launch instance
2. AMI: **Ubuntu 24.04 LTS**
3. Instance type: **t3.small** (минимум для prod)
4. Key pair: создай или выбери существующий
5. Security Group — открой порты:
   - `22` — SSH (только твой IP)
   - `80` — HTTP
   - `443` — HTTPS
   - `8080` — Backend API (можно закрыть если nginx проксирует)
6. Storage: минимум **20 GB**

---

## 3. Настроить Security Groups

RDS Security Group — добавь правило:
- Type: PostgreSQL (5432)
- Source: Security Group EC2 инстанса

---

## 4. Подключиться к EC2 и установить Docker

```bash
ssh -i your-key.pem ubuntu@your-ec2-ip

# Установить Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu
newgrp docker

# Установить Docker Compose
sudo apt-get install -y docker-compose-plugin
docker compose version
```

---

## 5. Задеплоить приложение

```bash
# Клонировать репозиторий
git clone https://github.com/твой-username/adisker-ai.git
cd adisker-ai

# Создать .env файл
cp .env.example .env
nano .env
```

Заполни `.env`:
```
DB_HOST=your-rds-endpoint.rds.amazonaws.com
DB_PORT=5432
DB_NAME=adisker_db
DB_USER=adisker
DB_PASSWORD=твой_пароль_rds

JWT_SECRET=$(openssl rand -base64 64)

FRONTEND_URL=http://your-ec2-ip
INVITE_URL=http://your-ec2-ip/invite

STORAGE_TYPE=local
STORAGE_LOCAL_PATH=/var/adisker/uploads
```

```bash
# Собрать и запустить
docker compose up --build -d

# Проверить статус
docker compose ps
docker compose logs backend --tail=50
```

---

## 6. Проверить работу

```bash
# Health check backend
curl http://localhost:8080/api/actuator/health

# Логи
docker compose logs -f backend
```

Браузер:
- Frontend: `http://your-ec2-ip`
- Swagger: `http://your-ec2-ip:8080/api/swagger-ui.html`

---

## 7. Настроить домен и HTTPS (опционально)

```bash
# Установить Certbot
sudo apt install certbot python3-certbot-nginx -y

# Получить сертификат
sudo certbot --nginx -d your-domain.com

# Сертификаты будут в /etc/letsencrypt/live/your-domain.com/
```

---

## Переменные окружения

| Переменная | Обязательно | Описание |
|---|---|---|
| DB_HOST | ✅ | RDS endpoint |
| DB_PORT | | Порт PostgreSQL (default: 5432) |
| DB_NAME | ✅ | Имя БД |
| DB_USER | ✅ | Пользователь БД |
| DB_PASSWORD | ✅ | Пароль БД |
| JWT_SECRET | ✅ | Секрет JWT (min 32 символа) |
| FRONTEND_URL | ✅ | URL фронтенда |
| INVITE_URL | ✅ | URL для инвайтов |
| STORAGE_TYPE | | `local` или `s3` (default: local) |
| GEMINI_API_KEY | | API ключ для AI функций |

---

## Обновление приложения

```bash
cd adisker-ai
git pull
docker compose up --build -d
```
