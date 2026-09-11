#!/usr/bin/env bash
# Пересборка и перезапуск backend как fat jar (dev-проверки).
#
# Важно: работающий процесс запускается НЕ из target/*.jar, а из его копии
# (/tmp/adisker-run.jar). Иначе следующий `mvn package` перезапишет файл,
# который читает живая JVM (Spring Boot грузит классы лениво), и процесс
# упадёт с ClassNotFoundException: org.apache.catalina.Lifecycle$SingleUse.
set -e
cd /home/bazarbay/adisker-ai/backend
export JAVA_HOME=/opt/jdk/jdk-21.0.4
export PATH="$JAVA_HOME/bin:$PATH"

BUILT_JAR=target/adisker-ai-1.0.0-SNAPSHOT.jar
RUN_JAR=/tmp/adisker-run.jar

echo "[1/5] stop old..."
HOLDER=$(ss -ltnp 2>/dev/null | grep ':8080' | grep -oP 'pid=\K[0-9]+' | head -1 || true)
[ -n "$HOLDER" ] && kill "$HOLDER" 2>/dev/null && echo "  stopped $HOLDER"
for _ in $(seq 1 15); do ss -ltn 2>/dev/null | grep -q ':8080' || break; sleep 1; done
# добить, если ещё держит порт
HOLDER=$(ss -ltnp 2>/dev/null | grep ':8080' | grep -oP 'pid=\K[0-9]+' | head -1 || true)
[ -n "$HOLDER" ] && kill -9 "$HOLDER" 2>/dev/null && sleep 1

echo "[2/5] package..."
mvn -q -DskipTests package >/tmp/adisker-build.log 2>&1 || { tail -20 /tmp/adisker-build.log; exit 1; }

echo "[3/5] freeze jar copy..."
cp -f "$BUILT_JAR" "$RUN_JAR"

echo "[4/5] launch..."
mkdir -p /tmp/adisker-uploads
setsid env DB_HOST=localhost STORAGE_LOCAL_PATH=/tmp/adisker-uploads \
  "$JAVA_HOME/bin/java" -jar "$RUN_JAR" \
  > /tmp/adisker-backend.log 2>&1 &

echo "[5/5] wait for health..."
for _ in $(seq 1 40); do
  if curl -sf --max-time 3 http://localhost:8080/api/actuator/health >/dev/null 2>&1; then
    echo "  UP"; exit 0
  fi
  sleep 2
done
echo "  FAILED — see /tmp/adisker-backend.log"
grep -a -E "APPLICATION FAILED|Caused by|Port 8080" /tmp/adisker-backend.log | tail -5
exit 1
