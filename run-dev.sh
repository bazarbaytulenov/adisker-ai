#!/usr/bin/env bash
#
# run-dev.sh — запуск Әдіскер-AI в режиме разработки.
#
#   Postgres  → Docker-контейнер adisker-postgres (localhost:5432)
#   Backend   → Spring Boot на http://localhost:8080/api  (JDK 21)
#   Frontend  → Vite dev-сервер на http://localhost:3000
#
# Использование:
#   ./run-dev.sh          # запустить всё
#   ./run-dev.sh stop     # остановить backend и frontend
#   ./run-dev.sh logs     # смотреть логи (tail -f)
#
set -euo pipefail

# ─── Пути и настройки ─────────────────────────────────────────────────────────
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

JAVA_HOME_21="/opt/jdk/jdk-21.0.4"

PG_CONTAINER="adisker-postgres"
PG_IMAGE="postgres:16-alpine"
DB_NAME="adisker_db"
DB_USER="adisker"
DB_PASSWORD="adisker_pass"
DB_PORT="5432"

BACKEND_LOG="/tmp/adisker-backend.log"
FRONTEND_LOG="/tmp/adisker-frontend.log"
BACKEND_PIDFILE="/tmp/adisker-backend.pid"
FRONTEND_PIDFILE="/tmp/adisker-frontend.pid"

# ─── Цвета ────────────────────────────────────────────────────────────────────
green() { printf '\033[32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[33m%s\033[0m\n' "$1"; }
red() { printf '\033[31m%s\033[0m\n' "$1"; }

# ─── Остановка ────────────────────────────────────────────────────────────────
stop_services() {
  for pidfile in "$BACKEND_PIDFILE" "$FRONTEND_PIDFILE"; do
    if [[ -f "$pidfile" ]]; then
      pid="$(cat "$pidfile")"
      if kill -0 "$pid" 2>/dev/null; then
        # убиваем всё дерево процессов (mvn/vite порождают дочерние)
        pkill -P "$pid" 2>/dev/null || true
        kill "$pid" 2>/dev/null || true
        yellow "Остановлен процесс $pid ($(basename "$pidfile"))"
      fi
      rm -f "$pidfile"
    fi
  done
  green "Backend и frontend остановлены. Postgres ($PG_CONTAINER) оставлен работать."
}

# ─── Логи ─────────────────────────────────────────────────────────────────────
show_logs() {
  green "Логи (Ctrl+C для выхода):"
  tail -f "$BACKEND_LOG" "$FRONTEND_LOG"
}

# ─── Postgres ─────────────────────────────────────────────────────────────────
start_postgres() {
  if docker ps --format '{{.Names}}' | grep -qx "$PG_CONTAINER"; then
    green "Postgres уже запущен ($PG_CONTAINER)."
  elif docker ps -a --format '{{.Names}}' | grep -qx "$PG_CONTAINER"; then
    yellow "Запускаю существующий контейнер $PG_CONTAINER..."
    docker start "$PG_CONTAINER" >/dev/null
    green "Postgres запущен."
  else
    yellow "Создаю новый контейнер Postgres..."
    docker run -d \
      --name "$PG_CONTAINER" \
      -e POSTGRES_DB="$DB_NAME" \
      -e POSTGRES_USER="$DB_USER" \
      -e POSTGRES_PASSWORD="$DB_PASSWORD" \
      -p "$DB_PORT:5432" \
      -v adisker_pgdata:/var/lib/postgresql/data \
      "$PG_IMAGE" >/dev/null
    green "Postgres создан и запущен."
  fi

  # Ждём готовности БД
  yellow "Ожидание готовности Postgres..."
  for _ in $(seq 1 30); do
    if docker exec "$PG_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
      green "Postgres готов."
      return 0
    fi
    sleep 1
  done
  red "Postgres не ответил за 30 секунд."; exit 1
}

# ─── Backend ──────────────────────────────────────────────────────────────────
start_backend() {
  if [[ ! -d "$JAVA_HOME_21" ]]; then
    red "JDK 21 не найден по пути $JAVA_HOME_21"
    red "Укажи корректный путь в переменной JAVA_HOME_21 в этом скрипте."
    exit 1
  fi

  yellow "Запуск backend (Spring Boot, JDK 21)..."
  (
    cd "$BACKEND_DIR"
    export JAVA_HOME="$JAVA_HOME_21"
    export PATH="$JAVA_HOME/bin:$PATH"
    export DB_HOST="localhost"
    export DB_PORT="$DB_PORT"
    export STORAGE_LOCAL_PATH="${STORAGE_LOCAL_PATH:-$ROOT_DIR/.dev-uploads}"
    mkdir -p "$STORAGE_LOCAL_PATH"
    nohup mvn -DskipTests spring-boot:run > "$BACKEND_LOG" 2>&1 &
    echo $! > "$BACKEND_PIDFILE"
  )
  green "Backend стартует (PID $(cat "$BACKEND_PIDFILE")). Лог: $BACKEND_LOG"

  # Ждём health=UP
  yellow "Ожидание старта backend..."
  for _ in $(seq 1 60); do
    if curl -sf http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
      green "Backend поднят: http://localhost:8080/api (health = UP)"
      return 0
    fi
    sleep 2
  done
  red "Backend не поднялся за отведённое время. Смотри лог: $BACKEND_LOG"
  exit 1
}

# ─── Frontend ─────────────────────────────────────────────────────────────────
start_frontend() {
  yellow "Запуск frontend (Vite)..."
  (
    cd "$FRONTEND_DIR"
    if [[ ! -d node_modules ]]; then
      yellow "node_modules не найдены — устанавливаю зависимости..."
      npm install
    fi
    nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
    echo $! > "$FRONTEND_PIDFILE"
  )
  sleep 4
  green "Frontend запущен (PID $(cat "$FRONTEND_PIDFILE")): http://localhost:3000  | Лог: $FRONTEND_LOG"
}

# ─── main ─────────────────────────────────────────────────────────────────────
case "${1:-start}" in
  stop)  stop_services ;;
  logs)  show_logs ;;
  start)
    start_postgres
    start_backend
    start_frontend
    echo
    green "════════════════════════════════════════════════"
    green " Әдіскер-AI запущен в режиме разработки"
    green "  Frontend:  http://localhost:3000"
    green "  Backend:   http://localhost:8080/api"
    green "  Swagger:   http://localhost:8080/api/swagger-ui.html"
    green "════════════════════════════════════════════════"
    echo "  Логи:      ./run-dev.sh logs"
    echo "  Остановка: ./run-dev.sh stop"
    ;;
  *)
    red "Неизвестная команда: $1"
    echo "Использование: ./run-dev.sh [start|stop|logs]"
    exit 1
    ;;
esac
