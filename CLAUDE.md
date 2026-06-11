# Proyecto Comisiones — Guía y estado

> Lee esto primero al retomar. Resume QUÉ es el proyecto, QUÉ está hecho y QUÉ falta.

## 0. ESTADO ACTUAL + DÓNDE RETOMAR

> **Nota:** se RETOMÓ este backend Spring Boot. El front viejo
> (`comisiones-frontend`, React + Vite + **Supabase**, en `odbtms.github.io/comisiones`)
> queda como está. Se armó un **front nuevo** — **`comisiones-front-v2`**
> (React + Vite, SIN Supabase) — que consume ESTE backend, y **ya está TODO
> desplegado y funcionando en DigitalOcean.**

**Repos:**
- backend: https://github.com/odbtms/comisiones-back-end (privado)
- front nuevo: https://github.com/odbtms/comisiones-front-v2 (privado, ya subido)

### ✅ DESPLEGADO Y FUNCIONANDO (HTTPS desde 2026-06-06)
- **App online:** **https://comisiones.me**  🔒 (front + API en el mismo origen, HTTPS)
  - `http://` redirige a `https://`; `www.comisiones.me` redirige al dominio raíz.
  - Dominio `comisiones.me` (Namecheap, gratis 1 año vía GitHub Student Pack).
- **IP reservada:** **`161.35.252.133`** (fija, NO cambia al apagar/encender el Droplet).
  El registro A de `comisiones.me` apunta a esta IP.
- **Droplet DigitalOcean:** Ubuntu 24.04, 2 GB RAM, swap 2 GB, firewall ufw
  (solo SSH/80/443), Docker 29 + Compose v5.
- **4 contenedores** (`docker-compose.prod.yml`) con `restart: unless-stopped`:
  `comisiones-caddy` (HTTPS, único expuesto :80/:443) · `comisiones-web` (nginx, interno)
  · `comisiones-api` (Spring, interno) · `comisiones-db` (Postgres 16, interno, volumen `db-data`).
  - Caddy saca/renueva el cert de Let's Encrypt solo (volúmenes `caddy-data`/`caddy-config`).
- Probado de punta a punta: crear/leer/borrar día OK, cálculo correcto, persiste. HTTPS verificado.
- **Entrar al server:**
  `ssh -i C:\Users\tomas\.ssh\digitalocean_comisiones root@161.35.252.133`
- **DB_PASSWORD:** está en `~/comisiones-backend/.env` del Droplet (NO se commitea).

> ✅ **Actualizar el código en el Droplet (flujo `git pull` ya configurado, 2026-06-03):**
> los dos dirs (`~/comisiones-backend` y `~/comisiones-front-v2`) **ya son repos git**
> conectados a GitHub vía **deploy keys read-only** (una por repo) + alias en
> `~/.ssh/config` (`github-backend`, `github-front`). Para actualizar:
> ```bash
> ssh -i C:\Users\tomas\.ssh\digitalocean_comisiones root@161.35.252.133
> cd ~/comisiones-backend && git pull        # y/o ~/comisiones-front-v2
> cd ~/comisiones-backend && docker compose -f docker-compose.prod.yml up --build -d
> ```
> El `.env` (con DB_PASSWORD) es **untracked**, así que `git pull`/`reset` NO lo tocan.
> Detalle completo en `DEPLOY-digitalocean.md`.

### Próximos pasos (al retomar)
0. **Encender el Droplet** desde el panel de DigitalOcean (se apagó para no gastar
   crédito). Al prender, Docker arranca solo y los 4 contenedores vuelven solos
   (`restart: unless-stopped`). Verificar: abrir https://comisiones.me.
   - Si no levantan: `ssh ...` → `cd comisiones-backend &&
     docker compose -f docker-compose.prod.yml up -d`.
   - Con **IP reservada** (`161.35.252.133`) la IP ya NO cambia al apagar/encender,
     así que el dominio sigue resolviendo siempre.
1. ~~**Commitear y pushear** los cambios del backend~~ ✅ HECHO (2026-06-03,
   commit `2af8c30`): `docker-compose.prod.yml`, `DEPLOY-digitalocean.md`, `CLAUDE.md`.
2. ~~**Flujo `git pull` en el Droplet**~~ ✅ HECHO (2026-06-03): deploy keys
   read-only por repo + `~/.ssh/config` (alias `github-backend`/`github-front`),
   los dos dirs convertidos a repos git (`git init` + `reset --hard origin/main`,
   `.env` preservado). `git pull` probado en ambos. Ver bloque ✅ de arriba.
3. ~~**HTTPS + dominio**~~ ✅ HECHO (2026-06-06): IP reservada `161.35.252.133` +
   dominio `comisiones.me` (Namecheap/Student Pack) + **Caddy** delante de nginx
   sacando el cert de Let's Encrypt solo. El login ya viaja cifrado. Ver `Caddyfile`.
4. ~~**Backups** de Postgres con `pg_dump` (cron)~~ ✅ HECHO (2026-06-06): `backup-db.sh`
   + cron diario 03:30 en el Droplet, dump comprimido a `~/comisiones-backups`, rota
   y conserva los últimos 7. Restore/descarga abajo. (Opcional: bajarte copias a tu PC.)
5. **(login)** Registrá TU cuenta primero (heredás la jornada de prueba existente)
   antes de compartir la app. Opcional: recuperación de contraseña / verificación email.

## 1. Qué es

App personal para **fichar entrada/salida** en el trabajo y **calcular comisiones**,
reemplazando un Excel manual (`planilla mayo.xlsx`, en Descargas). Uso de una sola
persona por ahora (multiusuario queda para más adelante).

Debe funcionar en **PC y 100% en el celular** (responsive), estar **24/7 en la nube
(DigitalOcean, crédito GitHub Student)**.

## 2. Arquitectura

- **Backend** (este repo): Spring Boot 4.0.6, Java 17, Maven. Repo privado propio.
- **Frontend nuevo**: `comisiones-front-v2` (React + Vite, SIN Supabase). **Repo
  privado SEPARADO.** Consume este backend por REST.
- **DB**: PostgreSQL.
- **3 contenedores Docker separados**: `web` (nginx+front), `api`, `db`. En prod
  nginx sirve el front y hace `proxy_pass /api` → API (mismo origen, sin CORS).
- Despliegue final en **DigitalOcean** (Droplet + `docker-compose.prod.yml`).

## 3. Reglas de cálculo (verificadas contra el Excel)

Por día:
```
horas       = (salida - entrada)        # si supera 8 h, se descuenta 1 h (colación)
pagoBase    = horas * valorHora
ventasNetas = ROUND(ventasBrutas / 1.19, 0)   # saca el IVA (19%)
comision    = ventasNetas * 0.03              # 3%
total       = ROUND(pagoBase + comision, 0)
```
- Si **no asistió** (feriado / no fui): el día paga **0**.
- **Turno partido (✅ 2026-06-11):** un mismo día puede cargarse VARIAS veces (ej:
  10–14 y luego 15–17). Cada tramo es una jornada separada; el resumen mensual suma
  todo (horas, pago, comisión, total). "Días trabajados" cuenta fechas distintas,
  no tramos. ⚠️ La colación (>8 h descuenta 1 h) se aplica **por tramo**, así que
  dos tramos cortos que sumados pasen de 8 h no descuentan.
- Defaults configurables en `application.properties` / env:
  `valorHora=3098`, `iva=0.19`, `comision=0.03`, colación `umbral=8h` / `descuento=1h`.
- Los valores derivados **NO se persisten**: se calculan al vuelo en cada respuesta,
  así nunca quedan desincronizados si cambia una regla.
- Nota pendiente: la regla es "**más de** 8 h". En el Excel hubo un día de 8 h exactas
  cargado como 7. Si se quiere que 8 exactas también descuenten, bajar el umbral.

## 4. Estructura del backend (hecho)

```
domain/Jornada              entidad JPA (un TRAMO de un día); guarda solo datos de
                            entrada. SIN unique (usuario_id, fecha): se permiten
                            varios tramos por día (turno partido) y todo se suma.
domain/Usuario              cuenta (email único, password BCrypt, nombre)
repository/JornadaRepository  consultas por usuario + rango de fechas
repository/UsuarioRepository  buscar por email
service/CalculoComisionService  las fórmulas + regla de colación
service/Calculo             record con el resultado del cálculo
service/JornadaService      CRUD + resumenMensual (recibe el usuarioId del token)
service/AuthService         registro + login (hash BCrypt, emite el JWT)
service/AdminService        panel admin: lista usuarios + stats + eliminar (solo admin)
service/EmailDominioValidador  chequea por DNS que el dominio del email reciba mail
service/excepciones: JornadaNoEncontrada · EmailYaRegistrado · AccesoDenegado ·
                     UsuarioNoEncontrado · OperacionNoPermitida · EmailInvalido
security/SecurityConfig     Spring Security stateless + CORS + 401 JSON
security/JwtService         firma/valida los JWT (HS256, JJWT)
security/JwtAuthFilter      lee "Authorization: Bearer" en cada request
security/AuthUser           principal autenticado (id, email, nombre)
web/JornadaController       API REST (usa @AuthenticationPrincipal)
web/AuthController          /api/auth/register · /login · /me
web/AdminController         /api/admin/usuarios (403 si no es admin)
web/GlobalExceptionHandler  404, 400 (validación), 409 (dup), 401, 403 (no admin)
web/dto/JornadaRequest/Response/ResumenMensualResponse
web/dto/RegisterRequest · LoginRequest · AuthResponse (incluye id) · AdminUsuarioResponse
```

### Endpoints
**Auth (públicos):**
| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/auth/register` | crear cuenta (201) → `{id, token, email, nombre}` |
| POST | `/api/auth/login` | iniciar sesión → `{id, token, ...}` (401 si falla) |
| GET | `/api/auth/me` | datos del usuario del token (`{id, email, nombre}`) |

**Admin (requiere token; solo el usuario admin, id 1 por defecto):**
| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/admin/usuarios` | lista usuarios + stats (jornadas, total acum., última fecha). 403 si no es admin |
| DELETE | `/api/admin/usuarios/{id}` | borra usuario + sus jornadas. 403 no-admin · 400 si es la cuenta admin · 404 inexistente |

> **Panel admin (✅ desplegado 2026-06-06):** el usuario admin (id 1, configurable
> con `app.admin.usuario-id`) ve en su perfil un apartado "Administración" con la
> lista de usuarios y sus estadísticas, y puede **eliminar** usuarios (no a sí mismo).
> La autorización la valida `AdminService` (403 vía `AccesoDenegadoException`); el
> front muestra el botón solo si el `id` del token es el admin.

> **Verificación de email en el registro (✅ 2026-06-06):** además del formato
> (`@Email`), `EmailDominioValidador` consulta DNS (MX, fallback A, vía DNS público
> 8.8.8.8/1.1.1.1) para confirmar que el dominio pueda recibir correo; rechaza
> dominios falsos/typos con 400. Configurable con `app.registro.validar-dominio`.
> NO envía mails ni prueba el buzón exacto (un typo registrado como `gmial.com`,
> que tiene MX, pasaría). Para prueba real de propiedad haría falta confirmación
> por email (pendiente).

**Jornadas (requieren `Authorization: Bearer <token>`):** cada usuario ve y toca
solo SUS jornadas (se filtran por el `usuarioId` del token).
| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/jornadas` | crear jornada (201) |
| PUT | `/api/jornadas/{id}` | actualizar (404 si no es tuya) |
| GET | `/api/jornadas/{id}` | obtener una |
| DELETE | `/api/jornadas/{id}` | borrar (204) |
| GET | `/api/jornadas/resumen?anio=&mes=` | resumen del mes + totales |

> **Login multiusuario (✅ desplegado 2026-06-03):** registro abierto, login con
> **email + contraseña** (BCrypt), JWT HS256 (`JWT_SECRET` en `.env`, 7 días).
> El front guarda el token en localStorage y hace auto-logout en 401.
> ⚠️ **El primer registro hereda los datos de `usuario_id=1`** (auto-increment).
> Por eso **registrá TU cuenta primero** antes de pasarle la app a un compañero.
> ✅ **HTTPS activo** (Caddy + Let's Encrypt, 2026-06-06): el login ya viaja cifrado.

> ⚠️ **GOTCHA CORS (resuelto 2026-06-06):** aunque front y API comparten origen
> (nginx hace el proxy), el navegador **igual manda el header `Origin` en los POST**
> (login, etc.). Si `CORS_ORIGINS` no incluye el dominio real, Spring Security
> responde **`403 "Invalid CORS request"`** en el preflight `OPTIONS` y el login
> falla (NO es pérdida de datos). **`CORS_ORIGINS` debe coincidir con el dominio
> servido.** En el `.env` del Droplet está como
> `CORS_ORIGINS=https://comisiones.me,https://www.comisiones.me`. **Si algún día
> cambia el dominio, actualizá esta variable** (en `.env`, untracked) y recreá el
> api: `docker compose -f docker-compose.prod.yml up -d api`.

## 5. Cómo correr (local)

### Con Docker (recomendado — igual que en DigitalOcean)
```bash
cp .env.example .env       # completar credenciales (DB_PASSWORD, etc.)
docker compose up --build  # levanta postgres + api en contenedores separados
```
- API en http://localhost:8080  ·  health en `/actuator/health`
- La DB NO publica puerto hacia afuera (solo la usa la API).

### Sin Docker (necesita PostgreSQL en localhost:5432)
```bash
./mvnw spring-boot:run
```
Config por env: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JPA_DDL`, `CORS_ORIGINS`,
`JWT_SECRET` (secreto del login; en dev hay default, en prod va por `.env`).

Archivos Docker: `Dockerfile` (multi-stage build+JRE), `docker-compose.yml` (dev:
api+db), `docker-compose.prod.yml` (prod: web+api+db), `.dockerignore`, `.env.example`.

## 6. Pendiente (roadmap)

### Backend
- [x] **Dockerfile** del backend (multi-stage, imagen JRE liviana).
- [x] **docker-compose** local (api + postgres en contenedores separados).
- [x] Endpoint de **health** (actuator) para Docker / Oracle Cloud.
- [ ] **Probar el stack con Docker** (`docker compose up --build`) — pendiente:
      el daemon de Docker Desktop estaba apagado al dockerizar.
- [ ] **Tests** del cálculo (casos del Excel) y del controller.
- [ ] Revisar si hace falta un endpoint de **listado** sin totales.
- [x] ~~Manejar conflicto de fecha duplicada (unique constraint) -> 409 amigable.~~
      **REVERTIDO (2026-06-11):** se quitó la unique (usuario_id, fecha) para permitir
      turnos partidos (varios tramos por día). ⚠️ En el Droplet hay que dropear la
      constraint a mano una vez (ver abajo); con `ddl-auto=update` Hibernate no la borra.
- [x] **Multiusuario + login** (Spring Security + JWT, email+password BCrypt).
      Cada usuario ve solo sus jornadas. Desplegado y verificado 2026-06-03.

### Frontend nuevo — `comisiones-front-v2` (React + Vite, repo separado)
- [x] Crear proyecto **React + Vite** (Vite 7, NO 8 por Smart App Control).
- [x] Pantalla de carga del día (entrada/salida/ventas) y vista mensual.
- [x] Diseño **responsive / mobile-first** (criterio de animación de Emil Kowalski).
- [x] Consumir la API por rutas relativas `/api` (proxy en dev, nginx en prod).
- [x] Dockerfile (build estático servido por nginx + proxy a la API).
- [x] Subido a repo privado: https://github.com/odbtms/comisiones-front-v2

### Infra / Deploy
> Paso a paso detallado en **`DEPLOY-digitalocean.md`**.
- [x] **Backend** subido a repo privado: https://github.com/odbtms/comisiones-back-end
- [x] `docker-compose.prod.yml` (db + api interna + web nginx con proxy).
- [x] Subir el repo privado del **front** (`comisiones-front-v2`).
- [x] **Droplet** en DigitalOcean (Ubuntu 24.04, 2 GB, swap, ufw, Docker 29).
- [x] Contenedores separados (web / api / db) y red entre ellos (docker-compose).
- [x] Front como tercer contenedor (nginx que sirve build + proxy `/api`).
- [x] **Stack levantado y verificado** en el Droplet (https://comisiones.me).
- [x] Volumen persistente para Postgres (`db-data`).
- [x] **Backups** de Postgres — `backup-db.sh` + cron diario 03:30, dump comprimido
      a `~/comisiones-backups`, rotación de 7 días (2026-06-06). Ver bloque "Backups" abajo.
- [x] **Flujo `git pull` en el Droplet** — deploy keys + `~/.ssh/config` + repos git
      (2026-06-03). Actualizar: `git pull` y `docker compose -f docker-compose.prod.yml up --build -d`.
- [x] **IP reservada** DigitalOcean (`161.35.252.133`) — IP fija que no cambia al reiniciar (2026-06-06).
- [x] **HTTPS / dominio** — dominio `comisiones.me` (Namecheap/Student Pack) + **Caddy** delante
      de nginx con cert Let's Encrypt auto-renovable (2026-06-06). Ver `Caddyfile`.
- [x] **Commitear** cambios del backend (compose prod + guía + este CLAUDE.md) — commit `2af8c30`.

### Backups (✅ 2026-06-06)
- **Script:** `backup-db.sh` (en el repo y en `~/comisiones-backend` del Droplet).
  Hace `pg_dump` comprimido a `~/comisiones-backups/comisiones-FECHA.sql.gz`,
  conserva los **últimos 7** y borra los viejos (rotación). Un dump pesa ~1–4 KB.
- **Cron:** diario a las **03:30** (hora del server). Log en `~/comisiones-backups/backup.log`.
  Ver: `crontab -l`. Correr a mano: `~/comisiones-backend/backup-db.sh`.
- **Restaurar** un backup (⚠️ sobre una base vacía/recién creada idealmente):
  ```bash
  gunzip -c ~/comisiones-backups/comisiones-FECHA.sql.gz \
    | docker exec -i comisiones-db psql -U comisiones -d comisiones
  ```
- **Bajar copias a tu PC** (off-site, gratis) desde PowerShell en Windows:
  ```powershell
  scp -i C:\Users\tomas\.ssh\digitalocean_comisiones `
    root@161.35.252.133:/root/comisiones-backups/comisiones-*.sql.gz `
    C:\Users\tomas\Downloads\
  ```
  > El dump diario vive en el mismo Droplet (cubre borrados/errores). Para cubrir
  > que explote el Droplet entero, bajate de vez en cuando una copia con el `scp` de arriba.

## 7. Decisiones tomadas
- Horas: cálculo automático entrada/salida con descuento de 1 h si > 8 h. (No hay
  carga manual de horas.)
- Valores derivados no se guardan en la DB.
- **Multiusuario con login** (email + contraseña, JWT). Cada usuario ve solo sus
  jornadas. Registro abierto. HTTPS activo (Caddy + Let's Encrypt en `comisiones.me`).
