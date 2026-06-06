#!/usr/bin/env bash
# Backup diario de la base Postgres del proyecto Comisiones (Droplet DigitalOcean).
#
# Hace pg_dump comprimido (gzip) dentro del contenedor comisiones-db, lo guarda en
# ~/comisiones-backups y conserva SOLO los ultimos 7; borra los mas viejos (rotacion).
# Pensado para correr por cron una vez al dia. Ver README/CLAUDE.md para el restore.
set -euo pipefail

BACKUP_DIR="$HOME/comisiones-backups"
CONTAINER="comisiones-db"
DB_USER="${DB_USER:-comisiones}"
DB_NAME="${DB_NAME:-comisiones}"
RETENTION=7                       # cuantos backups conservar

mkdir -p "$BACKUP_DIR"
STAMP="$(date +%F_%H%M%S)"
FILE="$BACKUP_DIR/comisiones-$STAMP.sql.gz"

# Dump comprimido. La conexion local dentro del contenedor no pide password.
docker exec "$CONTAINER" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$FILE"

# Rotacion: dejar los ultimos $RETENTION (por fecha), borrar el resto.
ls -1t "$BACKUP_DIR"/comisiones-*.sql.gz 2>/dev/null | tail -n +$((RETENTION + 1)) | xargs -r rm -f

echo "[$(date '+%F %T')] backup OK -> $FILE ($(du -h "$FILE" | cut -f1)) | total: $(ls -1 "$BACKUP_DIR"/comisiones-*.sql.gz | wc -l) archivos"
