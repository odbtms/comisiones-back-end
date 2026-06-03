# Proyecto Comisiones — Guía y estado

> Lee esto primero al retomar. Resume QUÉ es el proyecto, QUÉ está hecho y QUÉ falta.

## 0. DÓNDE RETOMAR (próximos pasos, en orden)

El backend está **completo y compila**; falta probarlo con Docker y subirlo.
Al volver, seguir por acá:

1. **Probar el stack local con Docker**: prender Docker Desktop, luego
   `copy .env.example .env` (editar `DB_PASSWORD`) y `docker compose up --build`.
   Verificar `http://localhost:8080/actuator/health` → `{"status":"UP"}`.
2. **Subir el backend a un repo privado de GitHub** (falta `git init` + primer commit
   + crear repo privado + push). El repo aún NO existe.
3. **Crear el frontend** React + Vite en su propio repo privado (ver sección 6).
4. **Desplegar en Azure** (VM B1s, crédito $100 de estudiante) siguiendo `DEPLOY-azure.md`.

Estado git: rama `master`, **sin commits todavía**.

## 1. Qué es

App personal para **fichar entrada/salida** en el trabajo y **calcular comisiones**,
reemplazando un Excel manual (`planilla mayo.xlsx`, en Descargas). Uso de una sola
persona por ahora (multiusuario queda para más adelante).

Debe funcionar en **PC y 100% en el celular** (responsive), estar **24/7 en la nube
(Oracle Cloud free tier)**.

## 2. Arquitectura

- **Backend** (este repo): Spring Boot 4.0.6, Java 17, Maven. Repo privado propio.
- **Frontend**: React + Vite. **Repo privado SEPARADO** (todavía no creado).
- **DB**: PostgreSQL.
- **3 contenedores Docker separados**: API, DB, front.
- Despliegue final en Oracle Cloud.

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
repository/JornadaRepository  consultas por usuario + rango de fechas
service/CalculoComisionService  las fórmulas + regla de colación
service/Calculo             record con el resultado del cálculo
service/JornadaService      CRUD + resumenMensual (monousuario = id 1)
service/JornadaNoEncontradaException
web/JornadaController       API REST
web/GlobalExceptionHandler  404 (no encontrada) y 400 (validación)
web/CorsConfig              CORS para el front
web/dto/JornadaRequest      entrada del front (con validación)
web/dto/JornadaResponse     datos + valores calculados
web/dto/ResumenMensualResponse  jornadas del mes + totales
```

### Endpoints
| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/jornadas` | crear jornada (201) |
| PUT | `/api/jornadas/{id}` | actualizar |
| GET | `/api/jornadas/{id}` | obtener una |
| DELETE | `/api/jornadas/{id}` | borrar (204) |
| GET | `/api/jornadas/resumen?anio=&mes=` | resumen del mes + totales |

## 5. Cómo correr (local)

### Con Docker (recomendado — igual que en Oracle)
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
Config por env: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JPA_DDL`, `CORS_ORIGINS`.

Archivos Docker: `Dockerfile` (multi-stage build+JRE), `docker-compose.yml`,
`.dockerignore`, `.env.example`.

## 6. Pendiente (roadmap)

### Backend
- [x] **Dockerfile** del backend (multi-stage, imagen JRE liviana).
- [x] **docker-compose** local (api + postgres en contenedores separados).
- [x] Endpoint de **health** (actuator) para Docker / Oracle Cloud.
- [ ] **Probar el stack con Docker** (`docker compose up --build`) — pendiente:
      el daemon de Docker Desktop estaba apagado al dockerizar.
- [ ] **Tests** del cálculo (casos del Excel) y del controller.
- [ ] Revisar si hace falta un endpoint de **listado** sin totales.
- [ ] Manejar conflicto de fecha duplicada (unique constraint) -> 409 amigable.
- [ ] (Más adelante) **multiusuario** + login.

### Frontend (repo separado, NO empezado)
- [ ] Crear proyecto **React + Vite**, repo privado.
- [ ] Pantalla de carga del día (entrada/salida/ventas) y vista mensual tipo planilla.
- [ ] Diseño **responsive / mobile-first**.
- [ ] Consumir la API; configurar URL del back por env.
- [ ] Dockerfile (build estático servido por nginx).

### Infra / Deploy
> Paso a paso detallado en **`DEPLOY-azure.md`**.
- [ ] Crear los **2 repos privados** (back y front) y subir.
- [ ] VM **Standard_B1s** en **Azure** (crédito $100 estudiante) con Ubuntu.
- [x] Contenedores separados (api / db) y red entre ellos (docker-compose).
- [ ] Front como tercer contenedor.
- [ ] Volumen persistente para Postgres; backups.
- [ ] HTTPS / dominio.

## 7. Decisiones tomadas
- Horas: cálculo automático entrada/salida con descuento de 1 h si > 8 h. (No hay
  carga manual de horas.)
- Valores derivados no se guardan en la DB.
- Monousuario por ahora (`usuario_id = 1`), preparado para multiusuario.
