# Experimentation / A-B Testing Platform

**Repository name:** `experimentation-ab-testing-platform`

**GitHub description:** Simple production-grade experimentation and A/B testing platform built with Java 25, Spring Boot 4, WebFlux, R2DBC, PostgreSQL, Flyway, Redis, Kafka KRaft, and Gradle.

## Why this stack

- **Java 25 + Spring Boot 4.0.6**: stable Spring Boot 4 line with first-class Java 25 support.
- **WebFlux + R2DBC**: non-blocking HTTP and PostgreSQL access.
- **PostgreSQL + Flyway**: durable experiment, assignment, event, metric, and outbox storage with repeatable schema migrations.
- **Redis**: fast assignment cache using privacy-safe user hashes.
- **Kafka KRaft**: event streaming for exposure/tracking events without ZooKeeper.
- **Outbox pattern**: HTTP request success does not depend on Kafka availability; unsent events are retried and claimed with `FOR UPDATE SKIP LOCKED` for multi-instance safety.

## Core behavior

1. User requests feature flag assignment.
2. Raw `userId` is HMAC-SHA256 hashed immediately and never stored or emitted.
3. Existing assignment is returned from Redis/PostgreSQL if present.
4. Existing persisted assignments are honored first to avoid churn.
5. New users are deterministically checked against `trafficAllocationBp`; users outside the rollout receive `assigned=false` with reason `OUT_OF_TRAFFIC_ALLOCATION`.
6. Eligible users are assigned by a separate deterministic weighted variant hash.
7. Exposure event is stored and written to the outbox.
8. Outbox publisher sends privacy-safe JSON events to Kafka.
9. Aggregated metrics are stored per experiment, variant, and metric name.
10. Dashboard summary endpoint calculates exposure counts and per-metric conversion rates.

## Quick start

```bash
docker compose up -d
gradle clean test bootRun
```

Default API base URL:

```text
http://localhost:8080
```

Postman collection:

```text
postman/ab-testing-platform.postman_collection.json
```

## Environment variables

| Variable | Default | Description |
|---|---:|---|
| `APP_ADMIN_TOKEN` | `local-admin-token` | Required for experiment admin APIs. |
| `APP_PRIVACY_HMAC_SECRET` | local dev secret | HMAC secret for user hashing. Override in real environments. |
| `POSTGRES_HOST` | `localhost` | PostgreSQL host. |
| `POSTGRES_PORT` | `5432` | PostgreSQL port. |
| `POSTGRES_DB` | `ab_testing` | PostgreSQL database. |
| `POSTGRES_USER` | `ab_user` | PostgreSQL user. |
| `POSTGRES_PASSWORD` | `ab_password` | PostgreSQL password. |
| `REDIS_HOST` | `localhost` | Redis host. |
| `REDIS_PORT` | `6379` | Redis port. |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers. |

## Main endpoints

### Create experiment

```http
POST /api/v1/experiments
X-Admin-Token: local-admin-token
Content-Type: application/json
```

```json
{
  "key": "checkout_button_color",
  "name": "Checkout Button Color",
  "enabled": true,
  "trafficAllocationBp": 10000,
  "variants": [
    { "key": "control", "weight": 5000, "payload": { "color": "blue" } },
    { "key": "green", "weight": 5000, "payload": { "color": "green" } }
  ]
}
```

### Get assignment and emit exposure

```http
GET /api/v1/flags/checkout_button_color/assignment
X-User-Id: user-123
```

### Track conversion/event

```http
POST /api/v1/events
Content-Type: application/json
```

```json
{
  "experimentKey": "checkout_button_color",
  "userId": "user-123",
  "eventName": "purchase",
  "value": 19.99,
  "metadata": { "currency": "EUR" }
}
```

### Read raw metrics

```http
GET /api/v1/metrics/checkout_button_color
```

### Read dashboard summary

```http
GET /api/v1/metrics/checkout_button_color/summary
```

## Acceptance check

Run this twice:

```bash
curl -s -H 'X-User-Id: user-123' \
  http://localhost:8080/api/v1/flags/checkout_button_color/assignment
```

The same user gets the same variant every time. The service first checks Redis and PostgreSQL for an existing assignment, and only computes the deterministic assignment for the first eligible request.

## Production notes

- Replace `APP_PRIVACY_HMAC_SECRET` and `APP_ADMIN_TOKEN` with secrets from Vault, AWS Secrets Manager, Azure Key Vault, or GCP Secret Manager.
- Keep raw user identifiers out of logs, metrics, Kafka payloads, and database tables.
- Use mTLS or OAuth2/OIDC for admin APIs in real production; the local token filter is intentionally lightweight for this simple platform.
- Partition Kafka topics by `experimentKey` or `experimentKey:userHash` depending on downstream ordering needs.
- Keep `status` columns as plain `VARCHAR` and enforce allowed values in application code, not DB enum/check constraints.


## Production hardening added in v1.1

- Correct partial rollout semantics: traffic allocation is now a separate deterministic holdout decision from weighted variant assignment. Variant weights are independent and must sum to 10000 basis points among eligible users.
- Existing assignments are checked before rollout eligibility, so assigned users do not churn if rollout percentage is later reduced.
- Outbox publisher now atomically claims rows with PostgreSQL `FOR UPDATE SKIP LOCKED`, making the scheduler safer in multi-instance deployments.
- Event metadata is sanitized before persistence/Kafka publication to redact obvious PII-like keys such as email, phone, IP, token, cookie, address, session, and raw user identifiers.
- Added `/api/v1/metrics/{experimentKey}/summary` for dashboard-ready exposure counts, event counts, total values, and conversion rates.
- Added no-store response headers for personalized flag assignment responses.
- Added stronger configuration validation for admin token and HMAC secret length.
- New experiments receive a random persisted assignment salt instead of a predictable key-derived salt.
- Added extra unit and integration tests covering rollout holdout, privacy sanitization, and metrics summary.
