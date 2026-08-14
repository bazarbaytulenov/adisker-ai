# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# ── Stage 2: Serve ────────────────────────────────────────────────────────────
FROM nginx:alpine

# envsubst входит в nginx:alpine через пакет gettext
RUN apk add --no-cache gettext

COPY --from=build /app/dist /usr/share/nginx/html

# Кладём шаблон конфига — envsubst подставит переменные при старте
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80

# envsubst заменяет ${BACKEND_URL} в шаблоне и запускает nginx
CMD ["/bin/sh", "-c", "envsubst '${BACKEND_URL}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
