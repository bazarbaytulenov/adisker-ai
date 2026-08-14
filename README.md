# Әдіскер-AI

Платформа автоматизации деятельности дошкольных организаций Республики Казахстан.

## Стек

| Слой       | Технология                              |
|------------|-----------------------------------------|
| Backend    | Java 21, Spring Boot 3.3, PostgreSQL 16 |
| Frontend   | React 18, TypeScript, Tailwind CSS      |
| БД миграции| Flyway                                  |
| Auth       | JWT + Refresh Token (rotation)          |
| Деплой     | Docker + Docker Compose                 |

## Быстрый старт

### Требования
- Docker & Docker Compose
- (для разработки) Java 21, Maven 3.9, Node.js 20

### Запуск через Docker Compose

```bash
cd adisker-ai
docker compose up --build
```

После старта:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html

### Разработка — Backend

```bash
cd backend

# Запустить только БД
docker compose up postgres -d

# Запустить Spring Boot (с hot-reload)
mvn spring-boot:run
```

### Разработка — Frontend

```bash
cd frontend
npm install
npm run dev
```

Фронтенд будет на http://localhost:3000, API будет проксироваться на http://localhost:8080.

## Структура проекта

```
adisker-ai/
├── docker-compose.yml
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/kz/adisker/
│       ├── AdiskerApplication.java
│       ├── config/          # SecurityConfig, AuditConfig
│       ├── security/        # JWT, UserPrincipal, AuthFilter
│       ├── common/          # BaseEntity, TenantEntity, DTOs, Exceptions
│       └── module/
│           ├── organization/ # Organization CRUD
│           ├── branch/       # Branch CRUD
│           ├── user/         # User CRUD + Auth
│           ├── group/        # Groups
│           ├── child/        # Children (контингент)
│           ├── attendance/   # Табель посещаемости
│           ├── observation/  # Наблюдение + ИКР
│           ├── plan/         # Перспективный план
│           ├── cyclogram/    # Циклограмма
│           ├── schedule/     # Расписание
│           ├── dailyinfo/    # Информация за день
│           ├── chat/         # Чат воспитатель-родитель
│           └── ...
└── frontend/
    ├── src/
    │   ├── api/        # axios клиент + API функции
    │   ├── components/ # UI компоненты (Button, Table, Modal...)
    │   ├── pages/      # Страницы по роутам
    │   ├── store/      # Zustand (authStore)
    │   └── types/      # TypeScript типы
    └── ...
```

## Роли и доступ

| Роль             | Код              | Описание                           |
|------------------|------------------|------------------------------------|
| Системный админ  | SYSTEM_ADMIN     | Полный технический доступ          |
| Учредитель       | FOUNDER          | Аналитика всех филиалов            |
| Руководитель     | DIRECTOR         | Все модули организации             |
| Методист         | METHODIST        | Образовательные модули             |
| Воспитатель      | EDUCATOR         | Своя группа                        |
| Педагог казахского | KAZ_TEACHER    | Только графа «Қазақ тілі»          |
| Муз. руководитель | MUSIC_TEACHER  | Только графа «Музыка»              |
| Инструктор по физ. | PE_INSTRUCTOR | Только графа «Физическая культура» |
| Медсестра        | NURSE            | Медицинский модуль                 |
| Завхоз           | JANITOR          | Хозяйственный модуль               |
| Бухгалтер        | ACCOUNTANT       | Оплата и начисления                |
| Родитель         | PARENT           | Только свой ребёнок                |

## API Endpoints (основные)

```
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout

GET    /api/organizations
POST   /api/organizations
PUT    /api/organizations/{id}

GET    /api/branches?organizationId=...
POST   /api/branches?organizationId=...

GET    /api/groups?organizationId=...&branchId=...
POST   /api/groups

GET    /api/children?organizationId=...
POST   /api/children

GET    /api/users?organizationId=...
POST   /api/users
GET    /api/users/me
```

Полная документация: http://localhost:8080/api/swagger-ui.html

## Переменные окружения

| Переменная         | Описание                    | Дефолт                |
|--------------------|-----------------------------|-----------------------|
| DB_HOST            | Хост PostgreSQL             | localhost             |
| DB_PORT            | Порт PostgreSQL             | 5432                  |
| DB_NAME            | Имя БД                      | adisker_db            |
| DB_USER            | Пользователь БД             | adisker               |
| DB_PASSWORD        | Пароль БД                   | adisker_pass          |
| JWT_SECRET         | Секрет для подписи токенов  | (обязательно изменить)|
| FRONTEND_URL       | URL фронтенда               | http://localhost:3000 |
| STORAGE_TYPE       | Тип хранилища: local / s3   | local                 |
| GEMINI_API_KEY     | Ключ для AI генерации       | (опционально)         |
