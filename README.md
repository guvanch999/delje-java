# DELJE — сайт-каталог товаров

Рекламный сайт-каталог: публичная витрина (туркменский и русский языки) и админ-панель
для товаров, категорий и баннеров.

**Стек:** Java 21, Spring Boot 3.5 (Spring MVC, Thymeleaf, Spring Data JPA, Spring Security,
Spring Session JDBC), PostgreSQL 17, Flyway, Maven. Картинки конвертируются в WebP
(Thumbnailator + webp-imageio). Письма с формы контактов уходят через Resend.

## Возможности

- Витрина под префиксами `/tk` и `/ru`; `/` перенаправляет на язык из cookie, `Accept-Language`
  или `tk` по умолчанию. Тексты интерфейса — `messages.properties` (tk) и
  `messages_ru.properties`; контент — колонки `name`/`name_tk` и т.п. с подстановкой
  второго языка, если перевод пустой.
- Главная: слайдер из баннеров, категории, хиты, новинки, блок Instagram.
- Каталог `/{lang}/products`: поиск, категория, цена от/до, 6 вариантов сортировки,
  по 24 товара на странице; карточка `/{lang}/products/{slug}` с галереей и похожими товарами.
- Страницы «О нас» и «Контакты» с формой обратной связи (валидация, ограничение 5 сообщений
  за 10 минут с одного IP, отправка владельцу через Resend).
- SEO: canonical, hreflang, Open Graph, `/sitemap.xml` с альтернативными языками, `/robots.txt`.
- Админка `/admin` (один аккаунт из `.env`): товары с несколькими фото, категории, баннеры
  (показ/скрытие). Сессии хранятся в БД и переживают перезапуск.
- Картинки: товар — превью 400×400 (quality 80) + полное до 1600px (quality 85), баннер —
  до 1920×700. Файлы лежат в `uploads/products/`, в БД — пути `/uploads/products/...`.
- Ежедневный `pg_dump` в 03:00, хранятся 14 дней.

## Структура

```
src/main/java/tm/delje/shop/
  config/       настройки: AppProperties, DataSource из DATABASE_URL, Web, Security
  entity/       Category, Product, ProductImage, Banner
  repository/   Spring Data репозитории
  service/      каталог, товары, категории, баннеры, картинки, почта, бэкапы, импорт
  web/          публичные контроллеры, SEO, обработка ошибок, языки
  web/admin/    контроллеры админки
  dto/          формы и фильтр каталога
  cli/          служебные команды (import, seed, backup, hash-password)
src/main/resources/
  application.yml, messages*.properties
  db/migration/ миграции Flyway
  templates/    Thymeleaf: layouts, fragments, public, admin
  static/       css, js, favicon
deploy/         nginx и systemd для сервера
import/         фото и баннеры для команды import
```

## Локальный запуск

Нужны Java 21 и Docker (для PostgreSQL). Maven ставить не нужно — есть `mvnw`.

```bash
cp .env.example .env        # задать POSTGRES_PASSWORD и такой же пароль в DATABASE_URL
docker compose up -d        # PostgreSQL на 127.0.0.1:5432
./mvnw package              # сборка + тесты -> target/delje.jar
java -jar target/delje.jar hash-password мойпароль   # хэш для ADMIN_PASSWORD_HASH (в одинарных кавычках)
java -jar target/delje.jar import                     # каталог DELJE из import/ (или seed — тестовые данные)
java -jar target/delje.jar
```

Сайт: <http://localhost:3000>, админка: <http://localhost:3000/admin>.
Приложение само читает `.env` из текущей папки. Миграции применяются при старте.

Служебные команды (без запуска веб-сервера):

| Команда | Что делает |
| --- | --- |
| `java -jar delje.jar import` | очищает каталог и загружает товары DELJE из `import/` |
| `java -jar delje.jar seed` | очищает каталог и заполняет тестовыми данными |
| `java -jar delje.jar backup` | делает дамп базы сейчас |
| `java -jar delje.jar hash-password <пароль>` | печатает bcrypt-хэш |

## Переменные окружения

| Переменная | Назначение |
| --- | --- |
| `PORT`, `SERVER_ADDRESS` | порт и адрес HTTP-сервера (на сервере `127.0.0.1`, впереди nginx) |
| `LOG_LEVEL` | уровень логов (`info`) |
| `SITE_URL` | адрес сайта для canonical, hreflang, sitemap (`https://shop.guvanch.site`) |
| `DATABASE_URL` | `postgres://user:password@host:port/db` |
| `POSTGRES_PASSWORD`, `POSTGRES_PORT` | для `docker compose` (контейнер базы) |
| `COOKIE_SECURE` | `true` за HTTPS |
| `ADMIN_LOGIN`, `ADMIN_PASSWORD_HASH` | аккаунт админки; хэш в одинарных кавычках |
| `RESEND_API_KEY`, `ADMIN_EMAIL`, `MAIL_FROM` | отправка писем с формы контактов; домен `MAIL_FROM` должен быть подтверждён в Resend |
| `UPLOADS_DIR`, `BACKUPS_DIR` | папки картинок и дампов |
| `BACKUP_ENABLED`, `PG_DUMP_PATH` | ежедневный бэкап и путь к `pg_dump` |

## Развёртывание на сервере (shop.guvanch.site)

Схема: nginx (TLS) → приложение на `127.0.0.1:3000` (systemd) → PostgreSQL в Docker
на `127.0.0.1:5432`. Инструкции для Ubuntu/Debian.

### 1. Пакеты

```bash
sudo apt install openjdk-21-jre-headless docker.io docker-compose-v2 nginx certbot python3-certbot-nginx
# pg_dump должен быть не старше сервера базы (17): на Ubuntu 26.04 подходит пакет из системы
sudo apt install postgresql-client-18
# на старых Ubuntu — postgresql-client-17 из репозитория PGDG (apt.postgresql.org)
```

На сервере с 1 ГБ памяти нужен swap (1–2 ГБ); размер heap в `delje.service` рассчитан на такой сервер.

### 2. Пользователь и файлы

```bash
sudo useradd --system --home /opt/delje --shell /usr/sbin/nologin delje
sudo mkdir -p /opt/delje/uploads/products /opt/delje/backups
# скопировать с локальной машины: target/delje.jar, docker-compose.yml, import/ (если нужен импорт)
sudo cp delje.jar docker-compose.yml /opt/delje/
sudo cp .env.example /opt/delje/.env && sudo nano /opt/delje/.env
sudo chown -R delje:delje /opt/delje && sudo chmod 600 /opt/delje/.env
```

Для продакшена в `.env`:

```
SERVER_ADDRESS=127.0.0.1
SITE_URL=https://shop.guvanch.site
COOKIE_SECURE=true
DATABASE_URL=postgres://shop:<пароль>@127.0.0.1:5432/shop
UPLOADS_DIR=/opt/delje/uploads
BACKUPS_DIR=/opt/delje/backups
BACKUP_ENABLED=true
MAIL_FROM=DELJE <noreply@shop.guvanch.site>
```

На сервере из секретов остаётся только этот `.env` (chmod 600). Копии ключей и паролей
храните локально, не в папках на сервере.

### 3. База

```bash
sudo bash -c 'cd /opt/delje && docker compose up -d'
```

### 4. Перенос данных со старой версии (Node.js)

Новая версия работает на той же схеме базы: при первом старте Flyway помечает
существующие таблицы как версию 1 и добавляет только таблицы сессий. Таблица
`SequelizeMeta` остаётся и не мешает.

```bash
# дамп старой базы (пример для старого docker compose)
docker compose -f /путь/к/старому/docker-compose.yml exec -T postgres pg_dump -U shop -d shop -Fc > /tmp/old.dump
# восстановление в новую базу
docker compose exec -T postgres pg_restore -U shop -d shop --no-owner < /tmp/old.dump
# картинки: в БД пути /uploads/products/..., файлы копируются как есть
sudo rsync -a /путь/к/старому/uploads/ /opt/delje/uploads/ && sudo chown -R delje:delje /opt/delje/uploads
rm /tmp/old.dump
```

Если старый `ADMIN_PASSWORD_HASH` (bcrypt) перенести в новый `.env`, пароль админа останется прежним.

Для чистой установки вместо переноса (нужна папка `/opt/delje/import`):

```bash
sudo -u delje bash -c 'cd /opt/delje && java -Xmx384m -jar delje.jar import'
```

Приложение само читает `.env` из текущей папки, поэтому служебные команды запускаются
из `/opt/delje`. Подключать `.env` через `source` не нужно (значение `MAIL_FROM` содержит `<`).

### 5. Сервис

```bash
sudo cp deploy/systemd/delje.service /etc/systemd/system/
sudo systemctl daemon-reload && sudo systemctl enable --now delje
journalctl -u delje -f            # логи
curl -I http://127.0.0.1:3000/tk  # проверка
```

### 6. nginx и HTTPS

```bash
sudo cp deploy/nginx/shop.guvanch.site.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/shop.guvanch.site.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d shop.guvanch.site
```

DNS-запись `A` для `shop.guvanch.site` должна указывать на сервер.

### Обновление версии

```bash
sudo cp delje.jar /opt/delje/delje.jar && sudo chown delje:delje /opt/delje/delje.jar
sudo systemctl restart delje
```

Миграции применяются автоматически при старте.

## Бэкапы

При `BACKUP_ENABLED=true` каждый день в 03:00 создаётся `backups/db_YYYY-MM-DD.dump`
(`pg_dump -Fc`), дампы старше 14 дней удаляются. Ошибки пишутся в лог и не влияют на сайт.
Восстановление — [docs/restore.md](docs/restore.md).

## Тесты

```bash
./mvnw test
```
