# platform-portal

PaaS application management portal for workloads deployed by the CI/CD and GitOps project.

## Day 13 Scope

Day 13 creates the runnable portal foundation:

- Spring Boot API on port `8081`.
- React/Vite portal on port `3001`.
- MariaDB local database on host port `3307`.
- API health check at `/api/health`.
- Portal shell that verifies API connectivity.

Application catalog, ArgoCD status, Kubernetes runtime status, deployment requests, audit logs, and Prometheus metrics are added in later days.

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
```

Stop:

```bash
docker compose down
```
