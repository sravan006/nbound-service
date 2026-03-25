# nbound-service (RxLink Inbound)

Java **Spring Boot 3** multi-module project implementing the RxLink inbound pipeline:

| Module | Role |
|--------|------|
| **inbound-tcp-receiver** | TCP ingress (ports **7001** / **7002** via config), forwards to the router over HTTP |
| **inbound-router** | `POST /api/v1/route` — data-based routing with **Caffeine (L1)** + **Redis (L2)**, forwards to a sender |
| **inbound-tcp-sender** | `POST /api/v1/send` — applies **Redis orchestration** rules, emits **TCP** to RxClaim (**4111** / **4112** / **4113** via config) |
| **inbound-common** | Shared DTOs |

Infrastructure: **Redis** (routing rules, receiver correlation, sender orchestration, pub/sub `rxlink:cache:invalidate`), **MongoDB** (route/send audit), **Prometheus** metrics via Actuator.

## TCP framing

Inbound TCP uses Spring Integration’s **`ByteArrayLengthHeaderSerializer`**: each message is prefixed with a **4-byte big-endian length** (same on receiver and sender toward RxClaim). Clients must send framed payloads; adjust serializers if you need MLLP or raw streaming.

## Local run

1. Start dependencies:

   ```bash
   docker compose up -d
   ```

2. Optional: seed a Redis route (otherwise the router uses `inbound.router.default-target-url`, default `http://localhost:8083`):

   ```bash
   redis-cli SET 'rxlink:route:MY_APP' 'http://localhost:8083'
   ```

3. Build:

   ```bash
   mvn -DskipTests package
   ```

4. Run three processes (example ports):

   ```bash
   java -jar inbound-router/target/inbound-router-1.0.0-SNAPSHOT.jar
   java -jar inbound-tcp-sender/target/inbound-tcp-sender-1.0.0-SNAPSHOT.jar
   java -jar inbound-tcp-receiver/target/inbound-tcp-receiver-1.0.0-SNAPSHOT.jar
   ```

5. CD-style variants:

   - **Relay (7001):** `INBOUND_TCP_PORT=7001 INBOUND_SOURCE_CHANNEL=RELAY_HEALTH`
   - **Change (7002):** `INBOUND_TCP_PORT=7002 INBOUND_SOURCE_CHANNEL=CHANGE_HEALTH`
   - **Sender 4112:** `RXCLAIM_TCP_PORT=4112 INBOUND_SENDER_INSTANCE_ID=sender-4112`

## HTTP API

- Router: `POST http://localhost:8080/api/v1/route` — body `InboundMessageDto` (`payloadBase64`, optional `correlationId`, `routingKey`, `sourceChannel`, `attributes`).
- Sender: `POST http://localhost:8083/api/v1/send` — same DTO shape.

## Configuration highlights

| Property | Purpose |
|----------|---------|
| `inbound.receiver.router.base-url` | Router HTTP base URL |
| `inbound.router.default-target-url` | Sender base URL when no Redis route exists |
| Redis `rxlink:route:{routingKey}` | Target sender base URL for that key |
| Redis `rxlink:sender:orchestrate:{instance-id}` | JSON `{"prefixBase64":"","suffixBase64":""}` |
| `rxlink:cache:invalidate` | Pub/sub channel; clears L1 route cache (router) and orchestration cache (sender) |

Metrics: `/actuator/prometheus` on each service (receiver HTTP default **8081**, router **8080**, sender **8083**).
