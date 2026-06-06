# Desplegar en DigitalOcean (Droplet + Docker)

Guía para poner la app 24/7 en un Droplet de DigitalOcean usando el crédito del
**GitHub Student Developer Pack** ($200 / 1 año). Arquitectura: un solo Droplet
con 3 contenedores — `db` (Postgres), `api` (Spring Boot) y `web` (nginx que
sirve el front y reenvía `/api` a la API). Solo se expone el puerto 80/443.

```
Internet ──80/443──> [web: nginx] ──/api──> [api: Spring] ──> [db: Postgres]
                         (sirve el front)      (interno)        (interno, con volumen)
```

---

## 0. Antes de empezar
- Cuenta de DigitalOcean con el crédito de estudiante activado
  (https://education.github.com/pack → DigitalOcean).
- Los **dos repos** privados en GitHub:
  - `comisiones-backend` (este)
  - `comisiones-front-v2` (el front nuevo)
- Una clave SSH local para entrar al Droplet (si no tenés: `ssh-keygen -t ed25519`).

---

## 1. Crear el Droplet
1. Panel de DO → **Create → Droplets**.
2. **Region**: la más cercana (ej. *New York* o *San Francisco*).
3. **Image**: Ubuntu 24.04 LTS.
4. **Size**: Basic → Regular → **$6/mes** (1 vCPU, 1 GB RAM). Alcanza para esto.
   - Si Spring se queda corto de RAM, subí al de **$12** (2 GB) o agregá swap (paso 3).
5. **Authentication**: SSH key (pegá tu clave pública).
6. **Hostname**: `comisiones`. Crear.
7. Anotá la **IP pública** del Droplet.

---

## 2. Entrar y preparar el servidor
```bash
ssh root@TU_IP

# Actualizar e instalar Docker (script oficial) + compose plugin
apt update && apt -y upgrade
curl -fsSL https://get.docker.com | sh
docker --version && docker compose version

# Firewall: solo SSH y HTTP/HTTPS
ufw allow OpenSSH
ufw allow 80
ufw allow 443
ufw --force enable
```

### (Opcional pero recomendado en el Droplet de 1 GB) Swap
Evita que el build de Maven/Spring muera por falta de memoria:
```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile
mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

---

## 3. Clonar los repos (privados)
Los dos repos van **lado a lado** en el home, porque el compose del backend
construye el front desde `../comisiones-front-v2`.

Generá una clave en el Droplet y agregala como **Deploy key** en GitHub
(Settings → Deploy keys de cada repo, solo lectura), o usá un Personal Access Token.

```bash
ssh-keygen -t ed25519 -C "droplet-comisiones" -f ~/.ssh/id_ed25519 -N ""
cat ~/.ssh/id_ed25519.pub      # pegá esta clave como Deploy key en AMBOS repos

cd ~
git clone git@github.com:odbtms/comisiones-back-end.git comisiones-backend
git clone git@github.com:odbtms/comisiones-front-v2.git comisiones-front-v2
```
> Ajustá las URLs a los nombres reales de tus repos.

---

## 4. Configurar variables y levantar
```bash
cd ~/comisiones-backend
cp .env.example .env
nano .env          # poné una DB_PASSWORD fuerte; CORS_ORIGINS no importa en prod

docker compose -f docker-compose.prod.yml up --build -d
docker compose -f docker-compose.prod.yml ps      # los 3 contenedores "running/healthy"
```

Verificá:
```bash
curl -fsS http://localhost/api/jornadas/resumen?anio=2026&mes=6   # responde JSON
```
Y desde tu navegador: **http://TU_IP** → debería cargar el front.

---

## 5. Operación diaria
```bash
# Ver logs
docker compose -f docker-compose.prod.yml logs -f api
docker compose -f docker-compose.prod.yml logs -f web

# Reiniciar
docker compose -f docker-compose.prod.yml restart

# Apagar / encender
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

### Actualizar tras un push
```bash
cd ~/comisiones-front-v2 && git pull
cd ~/comisiones-backend  && git pull
docker compose -f docker-compose.prod.yml up --build -d
```

---

## 6. Backups de la base
El volumen `db-data` persiste los datos aunque recrees los contenedores.
Dump periódico (cron):
```bash
docker exec comisiones-db pg_dump -U comisiones comisiones > ~/backup-$(date +%F).sql
```

---

## 7. Dominio + HTTPS (✅ HECHO 2026-06-06)

> **Estado:** activo en **https://comisiones.me**. Dominio `comisiones.me` (Namecheap,
> gratis 1 año vía GitHub Student Pack) + **IP reservada** DigitalOcean `161.35.252.133`
> (registro A `@` → esa IP; CNAME `www` → `comisiones.me`). **Caddy** delante de nginx
> saca y renueva el cert de Let's Encrypt solo. Config real: ver `Caddyfile` y el
> servicio `caddy` en `docker-compose.prod.yml`. Lo de abajo queda como referencia.

Lo más simple es poner **Caddy** adelante (saca el certificado de Let's Encrypt solo):

1. Apuntá un dominio (o subdominio) a la IP del Droplet (registro A).
2. En `docker-compose.prod.yml`, sacá el `ports: 80:80` del servicio `web`
   (que quede interno) y agregá un servicio `caddy`:
   ```yaml
   caddy:
     image: caddy:2
     restart: unless-stopped
     ports: ["80:80", "443:443"]
     volumes:
       - ./Caddyfile:/etc/caddy/Caddyfile
       - caddy-data:/data
     networks: [comisiones-net]
   ```
   `Caddyfile`:
   ```
   tu-dominio.com {
       reverse_proxy web:80
   }
   ```
   y sumá `caddy-data:` a `volumes:`.
3. `docker compose -f docker-compose.prod.yml up -d`. Caddy gestiona el TLS solo.

---

## Flujo `git pull` en el Droplet (✅ configurado 2026-06-03)

En vez de copiar archivos a mano (tar+scp), los dos dirs del Droplet
(`~/comisiones-backend` y `~/comisiones-front-v2`) son **repos git** conectados a
GitHub con **deploy keys read-only** (una por repo: GitHub no deja reusar la misma
deploy key en dos repos).

### Cómo quedó montado (no hace falta repetir, queda como referencia)
1. **Dos deploy keys** en `~/.ssh` del Droplet, sin passphrase:
   ```bash
   ssh-keygen -t ed25519 -N "" -C "deploy-backend@droplet" -f ~/.ssh/deploy_backend
   ssh-keygen -t ed25519 -N "" -C "deploy-front@droplet"   -f ~/.ssh/deploy_front
   ```
   La `.pub` de cada una se agregó en **Settings → Deploy keys** del repo
   correspondiente (sin "Allow write access").
2. **`~/.ssh/config`** con un alias por repo (para que cada uno use su llave):
   ```
   Host github-backend
       HostName github.com
       User git
       IdentityFile ~/.ssh/deploy_backend
       IdentitiesOnly yes
   Host github-front
       HostName github.com
       User git
       IdentityFile ~/.ssh/deploy_front
       IdentitiesOnly yes
   ```
3. **Convertir cada dir copiado en repo git** sin perder el `.env` (que es untracked):
   ```bash
   # los dirs venían con dueño de Windows (del tar); git como root los rechaza:
   chown -R root:root ~/comisiones-backend ~/comisiones-front-v2
   git config --global --add safe.directory /root/comisiones-backend
   git config --global --add safe.directory /root/comisiones-front-v2

   cd ~/comisiones-backend
   git init -b main
   git remote add origin git@github-backend:odbtms/comisiones-back-end.git
   git fetch --depth=1 origin main
   git reset --hard origin/main          # NO toca el .env (untracked)
   git branch --set-upstream-to=origin/main main

   cd ~/comisiones-front-v2
   git init -b main
   git remote add origin git@github-front:odbtms/comisiones-front-v2.git
   git fetch --depth=1 origin main
   git reset --hard origin/main
   git branch --set-upstream-to=origin/main main
   ```

### Actualizar la app de ahora en más
```bash
ssh -i C:\Users\tomas\.ssh\digitalocean_comisiones root@159.223.161.106
cd ~/comisiones-backend  && git pull
cd ~/comisiones-front-v2 && git pull
# rebuild solo lo que cambió y relevantar:
cd ~/comisiones-backend
docker compose -f docker-compose.prod.yml up --build -d
```

---

## Notas
- La API **no** publica su puerto: solo nginx la alcanza por la red interna del compose.
- Postgres tampoco se expone. Si querés conectarte con un cliente, hacelo por túnel SSH.
- El Droplet de $6 con swap aguanta bien un uso monousuario.
