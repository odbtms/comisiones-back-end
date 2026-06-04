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

### ✅ DESPLEGADO Y FUNCIONANDO (2026-06-03)
- **App online:** **http://159.223.161.106**  (front + API en el mismo origen)
- **Droplet DigitalOcean:** Ubuntu 24.04, 2 GB RAM, swap 2 GB, firewall ufw
  (solo SSH/80/443), Docker 29 + Compose v5.
- **3 contenedores** (`docker-compose.prod.yml`) con `restart: unless-stopped`:
  `comisiones-web` (nginx, único expuesto :80) · `comisiones-api` (Spring, interno)
  · `comisiones-db` (Postgres 16, interno, volumen `db-data`).
- Probado de punta a punta: crear/leer/borrar día OK, cálculo correcto, persiste.
- **Entrar al server:**
  `ssh -i C:\Users\tomas\.ssh\digitalocean_comisiones root@159.223.161.106`
- **DB_PASSWORD:** está en `~/comisiones-backend/.env` del Droplet (NO se commitea).

> ✅ **Actualizar el código en el Droplet (flujo `git pull` ya configurado, 2026-06-03):**
> los dos dirs (`~/comisiones-backend` y `~/comisiones-front-v2`) **ya son repos git**
> conectados a GitHub vía **deploy keys read-only** (una por repo) + alias en
> `~/.ssh/config` (`github-backend`, `github-front`). Para actualizar:
> ```bash
> ssh -i C:\Users\tomas\.ssh\digitalocean_comisiones root@159.223.161.106
> cd ~/comisiones-backend && git pull        # y/o ~/comisiones-front-v2
> cd ~/comisiones-backend && docker compose -f docker-compose.prod.yml up --build -d
> ```
> El `.env` (con DB_PASSWORD) es **untracked**, así que `git pull`/`reset` NO lo tocan.
> Detalle completo en `DEPLOY-digitalocean.md`.

### Próximos pasos (al retomar)
0. **Encender el Droplet** desde el panel de DigitalOcean (se apagó para no gastar
   crédito). Al prender, Docker arranca solo y los 3 contenedores vuelven solos
   (`restart: unless-stopped`). Verificar: abrir http://159.223.161.106.
   - Si no levantan: `ssh ...` → `cd comisiones-backend &&
     docker compose -f docker-compose.prod.yml up -d`.
   - **OJO:** al apagar/encender, DigitalOcean **puede cambiar la IP pública**
     (salvo IP reservada). Si cambió, usar la nueva IP.
1. ~~**Commitear y pushear** los cambios del backend~~ ✅ HECHO (2026-06-03,
   commit `2af8c30`): `docker-compose.prod.yml`, `DEPLOY-digitalocean.md`, `CLAUDE.md`.
2. ~~**Flujo `git pull` en el Droplet**~~ ✅ HECHO (2026-06-03): deploy keys
   read-only por repo + `~/.ssh/config` (alias `github-backend`/`github-front`),
   los dos dirs convertidos a repos git (`git init` + `reset --hard origin/main`,
   `.env` preservado). `git pull` probado en ambos. Ver bloque ✅ de arriba.
3. **HTTPS + dominio** (hoy es http://IP) — ⚠️ AHORA MÁS URGENTE: con el login,
   las contraseñas viajan sin cifrar por HTTP. Poner Caddy delante (paso 7 de la guía).
4. **Backups** de Postgres con `pg_dump` (cron) — ver paso 6 de la guía.
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
- Defaults configurables en `application.properties` / env:
  `valorHora=3098`, `iva=0.19`, `comision=0.03`, colación `umbral=8h` / `descuento=1h`.
- Los valores derivados **NO se persisten**: se calculan al vuelo en cada respuesta,
  así nunca quedan desincronizados si cambia una regla.
- Nota pendiente: la regla es "**más de** 8 h". En el Excel hubo un día de 8 h exactas
  cargado como 7. Si se quiere que 8 exactas también descuenten, bajar el umbral.

## 4. Estructura del backend (hecho)

```
domain/Jornada              entidad JPA (un día); guarda solo datos de entrada;
                            unique (usuario_id, fecha)
domain/Usuario              cuenta (email único, password BCrypt, nombre)
repository/JornadaRepository  consultas por usuario + rango de fechas
repository/UsuarioRepository  buscar por email
service/CalculoComisionService  las fórmulas + regla de colación
service/Calculo             record con el resultado del cálculo
service/JornadaService      CRUD + resumenMensual (recibe el usuarioId del token)
service/AuthService         registro + login (hash BCrypt, emite el JWT)
service/JornadaNoEncontradaException · EmailYaRegistradoException
security/SecurityConfig     Spring Security stateless + CORS + 401 JSON
security/JwtService         firma/valida los JWT (HS256, JJWT)
security/JwtAuthFilter      lee "Authorization: Bearer" en cada request
security/AuthUser           principal autenticado (id, email, nombre)
web/JornadaController       API REST (usa @AuthenticationPrincipal)
web/AuthController          /api/auth/register · /login · /me
web/GlobalExceptionHandler  404, 400 (validación), 409 (dup), 401 (credenciales)
web/dto/JornadaRequest/Response/ResumenMensualResponse
web/dto/RegisterRequest · LoginRequest · AuthResponse
```

### Endpoints
**Auth (públicos):**
| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/auth/register` | crear cuenta (201) → `{token, email, nombre}` |
| POST | `/api/auth/login` | iniciar sesión → `{token, ...}` (401 si falla) |
| GET | `/api/auth/me` | datos del usuario del token |

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
> ⚠️ **HTTP plano:** el login viaja sin cifrar; activar HTTPS (Caddy) pendiente.

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
- [x] Manejar conflicto de fecha duplicada (unique constraint) -> 409 amigable.
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
- [x] **Stack levantado y verificado** en el Droplet (http://159.223.161.106).
- [x] Volumen persistente para Postgres (`db-data`).
- [ ] **Backups** de Postgres con `pg_dump` (cron) — FALTA.
- [x] **Flujo `git pull` en el Droplet** — deploy keys + `~/.ssh/config` + repos git
      (2026-06-03). Actualizar: `git pull` y `docker compose -f docker-compose.prod.yml up --build -d`.
- [ ] **HTTPS / dominio** (Caddy delante, ver guía) — FALTA.
- [x] **Commitear** cambios del backend (compose prod + guía + este CLAUDE.md) — commit `2af8c30`.

## 7. Decisiones tomadas
- Horas: cálculo automático entrada/salida con descuento de 1 h si > 8 h. (No hay
  carga manual de horas.)
- Valores derivados no se guardan en la DB.
- **Multiusuario con login** (email + contraseña, JWT). Cada usuario ve solo sus
  jornadas. Registro abierto. HTTPS pendiente (hoy el login va por HTTP plano).
