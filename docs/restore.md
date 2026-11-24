# Восстановление базы из дампа

Дампы лежат в `backups/db_YYYY-MM-DD.dump` (на сервере — `/opt/delje/backups`),
формат custom (`pg_dump -Fc`). Такой дамп восстанавливается **только** через `pg_restore`,
не через `psql`.

## На сервере

Проще всего выполнить `pg_restore` внутри контейнера базы — пароль там не нужен:

```bash
sudo systemctl stop delje
sudo bash -c 'cd /opt/delje && docker compose exec -T postgres pg_restore -U shop -d shop --clean --if-exists < backups/db_YYYY-MM-DD.dump'
sudo systemctl start delje
```

- `--clean` — сначала удаляет существующие объекты;
- `--if-exists` — не падает, если объектов ещё нет (чистая база).

## Ручной бэкап

```bash
sudo -u delje bash -c 'cd /opt/delje && java -Xmx256m -jar delje.jar backup'
```

Картинки товаров в дамп не входят — каталог `/opt/delje/uploads` нужно копировать отдельно
(например, `rsync` или `tar` вместе с дампом).
