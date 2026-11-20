# Kata 5 — Balanceo de carga dinámico con NGINX y Consul

## Objetivo de la entrega

Configurar **NGINX** como punto de entrada único (`localhost:8080`) con múltiples bloques `upstream` dinámicos que descubren automáticamente las réplicas disponibles mediante registros **DNS SRV** de **Consul** (`:8600`). Cada servicio registra sus réplicas en Consul con un identificador único y un chequeo HTTP en `/health`, permitiendo comprobar en tiempo real el reparto de peticiones, la baja de una réplica y el alta de nuevas réplicas sin editar ni recargar NGINX.

---

## Dependencias y versiones

- **JDK:** OpenJDK 27 (`maven.compiler.source` / `target` 27)
- **Gestión de dependencias:** Apache Maven (`maven-shade-plugin` 3.5.0 en servicios, `maven-jar-plugin` 3.4.2 en clientes)
- **Framework web:** `io.javalin:javalin:7.2.4`
- **Serialización JSON:** `com.google.code.gson:gson:2.14.0`
- **Logging:** `org.slf4j:slf4j-simple:2.0.20`
- **Testing:** `org.junit.jupiter:junit-jupiter:5.11.4` (`scope: test`)
- **Contenedores (Docker / Docker Compose):**
  - `hashicorp/consul:2.1.0`
  - `nginx:stable` (versión >= 1.27.3, requerida para `resolve` en `upstream`)

---

## Estructura de la entrega y clases principales

```text
kata5/
├── load-balancer/
│   ├── compose.yaml          # Contenedores de Consul y NGINX
│   ├── webservice.conf       # Upstreams dinámicos y rutas públicas en NGINX
│   ├── start.sh              # Script de arranque de referencia
│   ├── check                 # URL de consulta de salud en Consul
│   └── rutas.txt             # Chuleta de rutas y comandos de prueba
├── fibonacci-service/
│   └── src/main/java/kata5/  # Main.java, WebService.java (secuencia de Fibonacci)
├── fibonacci-client/
│   └── src/main/java/kata5/  # Main.java (cliente de pruebas para /api/v1/fibonacci/)
├── random-service/
│   └── src/main/java/kata5/  # Main.java, WebService.java (números aleatorios)
├── random-client/
│   └── src/main/java/kata5/  # Main.java (cliente de pruebas para /api/v1/random/)
├── prime-numbers-service/
│   └── src/main/java/kata5/  # Main.java, WebService.java (comprobación de primos)
└── prime-numbers-client/
    └── src/main/java/kata5/  # Main.java (cliente de pruebas para /api/v1/prime_numbers/)
```

- **`kata5.Main` (servicios):** lee los argumentos `id`, `host`, `port` y `consulUrl` e inicia el servicio.
- **`kata5.WebService` (servicios):** levanta Javalin con `/` y `/health`, añade la cabecera `X-Node` con el `id` de la réplica y se registra en Consul vía `PUT /v1/agent/service/register` usando `host.docker.internal` cuando se ejecuta en `localhost`.
- **`kata5.Main` (clientes):** envía peticiones `GET` periódicas a través de NGINX e imprime el valor de `X-Node` y el cuerpo devuelto.

---

## Topología de red y puertos

```text
Clientes Java (:8080) ---> NGINX (Docker :8080 -> :80) ---(DNS :8600)---> Consul (Docker :8500 / :8600)
                                    |                                            ^
                                    +---> Réplicas Java (:8081..:8086) ----------+ (Registro + /health cada 5s)
```

| Componente | Puerto | Descripción |
| :--- | :--- | :--- |
| NGINX (`nginx:stable`) | `127.0.0.1:8080` -> `80` | Punto de entrada público HTTP |
| Consul (`hashicorp/consul:2.1.0`) | `8500` | API HTTP y UI web de Consul |
| Consul DNS | `8600` | Servidor DNS SRV consultado por NGINX (`valid=5s`) |
| `fibonacci-service` | `8081`, `8084` | Réplicas `f1`, `f2` |
| `random-service` | `8082`, `8085` | Réplicas `r1`, `r2` |
| `prime-numbers-service` | `8083`, `8086` | Réplicas `p1`, `p2` |

---

## Correspondencia entre ruta pública, upstream y Consul

| Ruta pública (NGINX) | Upstream en NGINX | Servicio en Consul | Parámetro de consulta |
| :--- | :--- | :--- | :--- |
| `/api/v1/fibonacci/` | `fibonacci_1` | `fibonacci_service_v1` | `?n=<términos>` |
| `/api/v1/random/` | `random_1` | `random_service_v1` | `?n=<cantidad>` |
| `/api/v1/prime_numbers/` | `prime_numbers_1` | `prime_numbers_service_v1` | `?n=<número>` |

---

## Cómo compilar y ejecutar

### 1. Arrancar Consul y NGINX

Desde `load-balancer/`:

```bash
docker compose up -d --wait
```

Si se modifica `load-balancer/webservice.conf` con el contenedor en marcha:

```bash
docker compose restart nginx
```

### 2. Compilar y ejecutar tests (opcional si se ejecuta desde IntelliJ)

En cada carpeta de servicio:

```bash
mvn test
mvn clean package
```

### 3. Arrancar al menos dos réplicas por servicio

Cada réplica recibe `ID host port consulUrl`:

```bash
# Fibonacci
java -jar fibonacci-service/target/fibonacci-service-1.0-SNAPSHOT.jar f1 localhost 8081 http://localhost:8500
java -jar fibonacci-service/target/fibonacci-service-1.0-SNAPSHOT.jar f2 localhost 8084 http://localhost:8500

# Random
java -jar random-service/target/random-service-1.0-SNAPSHOT.jar r1 localhost 8082 http://localhost:8500
java -jar random-service/target/random-service-1.0-SNAPSHOT.jar r2 localhost 8085 http://localhost:8500

# Prime numbers
java -jar prime-numbers-service/target/prime-numbers-service-1.0-SNAPSHOT.jar p1 localhost 8083 http://localhost:8500
java -jar prime-numbers-service/target/prime-numbers-service-1.0-SNAPSHOT.jar p2 localhost 8086 http://localhost:8500
```

### 4. Ejecutar los clientes

Ejecutar `kata5.Main` en `fibonacci-client`, `random-client` o `prime-numbers-client` para observar las respuestas y la cabecera `X-Node` en consola.

---

## Comprobación de reparto, baja y alta de réplicas

### Verificar estado en Consul

- Interfaz web: `http://localhost:8500`
- API de salud por servicio:

```bash
curl -s http://localhost:8500/v1/health/service/fibonacci_service_v1
curl -s http://localhost:8500/v1/health/service/random_service_v1
curl -s http://localhost:8500/v1/health/service/prime_numbers_service_v1
```

### Verificar reparto con curl

```bash
curl -i "http://localhost:8080/api/v1/fibonacci/?n=10"
curl -i "http://localhost:8080/api/v1/random/?n=5"
curl -i "http://localhost:8080/api/v1/prime_numbers/?n=7"
```

### Verificar baja y alta en vivo

1. Con las dos réplicas en marcha y el cliente ejecutándose, comprobar que `X-Node` alterna entre ambas réplicas (por ejemplo `p1` y `p2`).
2. Detener una réplica (por ejemplo `p1`). Tras el chequeo de Consul (`Interval: 5s`) y la expiración de caché DNS en NGINX (`valid=5s`), todas las peticiones pasan a ser atendidas por `p2`, mientras los demás upstreams (`fibonacci_1`, `random_1`) siguen respondiendo con normalidad.
3. Volver a arrancar `p1` (o incorporar `p3` en un puerto libre). Sin editar la configuración ni recargar NGINX, vuelve a recibir tráfico en unos segundos.

---

## Repeticiones de la kata

- **iteration-1 (`develop`, completada):** Despliegue inicial en el puerto público `8080` con tres servicios independientes (`fibonacci-service`, `random-service`, `prime-numbers-service`), réplicas `f1`/`f2`, `r1`/`r2`, `p1`/`p2`, sus respectivos clientes y configuración dinámica en NGINX + Consul.
- **iteration-2 (`iteration-2`, en progreso):** Repetición con variaciones pequeñas: cambio de dominio (`temperature-service` y `distance-service`), nuevos identificadores de réplica (`t1`/`t2`, `d1`/`d2`) y cambio del puerto público de NGINX a `8090:80`.
- **iteration-3 (`iteration-3`, pendiente):** Repetición con variación pequeña (pendiente de realizar).

---

## Evidencias de verificación

### Pruebas unitarias (JUnit 5)

- `fibonacci-service` (`kata5.WebServiceTest`): 3 tests ejecutados, 3 superados, 0 fallidos (`n=0`, `n=1`, `n=7`).
- `random-service` (`kata5.WebServiceTest`): 2 tests ejecutados, 2 superados, 0 fallidos (`n=0`, rango `[0, 100)`).
- `prime-numbers-service` (`kata5.WebServiceTest`): 3 tests ejecutados, 3 superados, 0 fallidos (negativos/0/1, primos y compuestos).

### Reparto en vivo con NGINX y Consul

Ejemplo de salida observada en `prime-numbers-client` con `p1` y `p2` activos:

```text
Node: p1 | Body: false
Node: p2 | Body: true
Node: p1 | Body: true
Node: p2 | Body: false
```

*(Pendiente añadir evidencias de capturas/salidas de baja y alta en `iteration-2` e `iteration-3`).*

---

## Vídeo explicativo

- **Enlace:** Pendiente (máximo 7 minutos).
