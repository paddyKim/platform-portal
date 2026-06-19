# platform-portal

CI/CD portal and application management layer for workloads built, deployed, and operated through the platform project.

## Current Scope

The portal currently provides the Day 13 runtime foundation, the Day 14 read-only application catalog, the Day 15 ArgoCD status view, the Day 16 Kubernetes runtime view, and the Day 17 CI/CD request foundation:

- Spring Boot API on port `8081`.
- React/Vite portal on port `3001`.
- MariaDB local database on host port `3307`.
- API health check at `/api/health`.
- Application catalog API at `/api/applications`.
- Seeded `platform-app` metadata for the `dev` environment.
- Portal UI for catalog list and application detail.
- Local CORS allowance for the portal frontend on `localhost:3001`.
- ArgoCD status API and UI panel for sync, health, operation phase, reconciled time, and image summary.
- Kubernetes runtime API and UI panel for Deployment readiness, Pod status, restart counts, Service metadata, and recent Events.
- CI/CD request API and UI for build/deploy/replica requests.
- Audit event API and UI for portal-owned request history.

Actual CI/CD execution is delegated to the future backend-only `platform-cicd` service. Kafka integration and Prometheus metrics are added in later days.

## Local Run

Prepare a local-only kubeconfig copy if the backend should read ArgoCD status from Docker Compose:

```bash
mkdir -p .local/kube
cp ~/.kube/config .local/kube/config
```

For Colima/k3s, update `.local/kube/config` so the active cluster uses the host-reachable API server:

```text
server: https://host.docker.internal:<k3s-api-port>
insecure-skip-tls-verify: true
```

Do not commit `.local/`; it contains local credentials.

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
curl http://localhost:8081/api/applications/1/environments/dev/runtime
curl http://localhost:8081/api/cicd/requests
curl http://localhost:8081/api/audit-events
```

Stop:

```bash
docker compose down
```
