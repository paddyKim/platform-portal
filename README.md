# platform-portal

PaaS application management portal for workloads deployed by the CI/CD and GitOps project.

## Current Scope

The portal currently provides the Day 13 runtime foundation, the Day 14 read-only application catalog, and the Day 15 ArgoCD status view:

- Spring Boot API on port `8081`.
- React/Vite portal on port `3001`.
- MariaDB local database on host port `3307`.
- API health check at `/api/health`.
- Application catalog API at `/api/applications`.
- Seeded `platform-app` metadata for the `dev` environment.
- Portal UI for catalog list and application detail.
- Local CORS allowance for the portal frontend on `localhost:3001`.
- ArgoCD status API and UI panel for sync, health, operation phase, reconciled time, and image summary.

Kubernetes runtime status, deployment requests, audit logs, and Prometheus metrics are added in later days.

## Local Run

```bash
docker compose up --build -d
```

Open:

```text
http://localhost:3001
```

Verify API:

```bash
curl http://localhost:8081/api/health
curl http://localhost:8081/actuator/health
curl http://localhost:8081/api/applications
curl http://localhost:8081/api/applications/1/environments/dev/status
```

Stop:

```bash
docker compose down
```
