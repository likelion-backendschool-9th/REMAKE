# Deployment

> Korean version for developers: [`docs/developer/deployment.md`](../developer/deployment.md)

Changes on `dev` are verified in GitHub Actions, transferred to the server over SSH and rsync, and deployed with `deploy/deploy.sh` as a Blue/Green zero-downtime deployment. The backend and the frontend are deployed separately.

## Deployment architecture

```
user → host Nginx (80/443, domain·TLS) → 127.0.0.1:APP_PORT → proxy container ─┬─ /api/ ─┬→ backend-blue:8080
                                                                                │         └→ backend-green:8080
                                                                                └─ rest ──┬→ frontend-blue:3000
                                                                                          └→ frontend-green:3000
                                                                    postgres (shared by both backend slots, no host port)
```

- `backend-blue`/`backend-green` and `frontend-blue`/`frontend-green` are pairs of slots running the same app. While one slot serves traffic, the new version is started in the other.
- Only one host port (`APP_PORT`, the port assigned to the project) is used, and the `proxy` container listens on it. The proxy sends requests starting with `/api/` to the backend and everything else to the frontend. The path is passed unchanged (`/api/items` → backend `/api/items`). The slots and the DB are reachable only through the Docker internal network.
- The server is shared by several teams, so each slot has a memory limit. The backend uses `mem_limit: 2g` and the Java heap may use up to 75% of it. The frontend uses `mem_limit: 1g` and Node automatically sets its heap limit to half of it. During a deploy both slots of that app run. A changed limit takes effect on that app's next deploy, without downtime.
- The server's Docker defaults set no log size limit, so every container has one. When a log file reaches 10MB a new file is started and only the latest 3 are kept (at most 30MB per container). `docker logs` works as before. The setting applies when a container is created. Slots are recreated on every deploy, but `postgres` and `proxy` are not recreated during a deploy, so after changing it, recreate them on the server manually.
- The active slot of each app is the one referenced by `proxy/backend.conf` or `proxy/frontend.conf` on the server. The deploy script rewrites the file of the app being deployed and reloads the proxy to switch. These are server-only files and are never overwritten by the deploy file sync. An app that has never been deployed points to a "not ready" placeholder, and its paths return 503.

## Current state

- When changes to `backend/` land on `dev`, `Backend CD` runs; changes to `frontend/` run `Frontend CD`. Changes to the shared deploy files (`compose.prod.yml`, `deploy/`) run both.
- Each CD first runs its checks (backend `./gradlew build`; frontend `pnpm lint`, `pnpm typecheck`, `pnpm build`) and deploys to the server only if they pass.
- PRs that change the deploy files (`compose.prod.yml`, `deploy/`) are checked by `Deploy CI` before merge: the shell scripts in `deploy/` (`bash -n`, `shellcheck`), `compose.prod.yml` with the empty values of `deploy/prod.env.example` filled in (`docker compose config`), and the proxy Nginx configuration with `nginx -t` run the same way `deploy/deploy.sh` runs it on the server. A required variable (`${VAR:?...}`) added to `compose.prod.yml` but missing from `deploy/prod.env.example` fails this check. Details: [`ci.md`](ci.md#deploy-file-and-workflow-checks-deploy-ci).
- Development server: <https://remake.newlecture.com> (HTTP requests are redirected to HTTPS).
- Server deployment has been enabled since 2026-11-17 (repository variable `REMAKE_DEPLOY_ENABLED=true`). When changes land on `dev`, the changed app is deployed to the development server automatically according to the rules above.
- It can also be run manually via `workflow_dispatch`. The same variable decides whether the server deploy actually runs.

## Server preparation

1. Install Docker Engine, the Docker Compose plugin, rsync, and curl on an Ubuntu server. Add the deploy account to the `docker` group so it can run Docker without sudo (`sudo usermod -aG docker <account>`, then log in again).
2. Choose a directory used only by REMAKE and create `.env` inside it. Fill in the DB password using [`deploy/prod.env.example`](../../deploy/prod.env.example) as a reference and restrict the file permission to `600`. The `backend/`, `frontend/`, and `deploy/` folders in this directory are overwritten by source sync on every deploy, so do not put other files there.
3. Enter the assigned host port as `APP_PORT` in `.env`. Without it, compose does not run and the deploy workflow stops at the server check step.
4. Prepare an SSH key dedicated to GitHub Actions and add its public key to the deploy account's `authorized_keys`. Verify the server host key fingerprint separately, then save the `known_hosts` entry.
5. Under repository **Settings → Environments**, create the `production` environment and restrict its **Deployment branches** to the `dev` branch only.
6. Set the following. Both CD workflows use the same values. Store the deploy secrets as secrets of the `production` environment, not as repository secrets. Only the deploy jobs of the two CD workflows use this environment, so other branches and other workflows cannot read the server access key. `REMAKE_DEPLOY_ENABLED` is read in the deploy job's `if` condition, which is evaluated before environment values are available, so keep it as a repository variable (**Settings → Secrets and variables → Actions**).

| Kind | Location | Name | Value |
| --- | --- | --- | --- |
| Secret | `production` environment | `DEPLOY_HOST` | Server domain or IPv4 address |
| Secret | `production` environment | `DEPLOY_USER` | SSH deploy account |
| Secret | `production` environment | `DEPLOY_PATH` | Absolute path of the deploy directory on the server, no spaces |
| Secret | `production` environment | `SSH_PRIVATE_KEY` | Full contents of the Actions-only private key |
| Secret | `production` environment | `SSH_KNOWN_HOSTS` | `known_hosts` entry of the verified server host key |
| Variable | Repository | `REMAKE_DEPLOY_ENABLED` | `true` once setup is complete |

The deploy workflows never transfer or overwrite the server's `.env` or `proxy/`. DB data is kept in the Docker named volume `remake_postgres-data`. `compose.prod.yml` pins the project name to `remake`, so the same volume is used regardless of the deploy directory's name or location. Do not change this name: a new, empty volume would be created.

Frontend `NEXT_PUBLIC_*` values are embedded in the bundle when the image is built, not at runtime. `frontend/Dockerfile` sets the defaults: the API base URL is a same-domain relative path (empty value) and MSW mocking is off. Only the per-server value `NEXT_PUBLIC_VAPID_PUBLIC_KEY` (Web Push public key) goes in the server's `.env`. A changed value takes effect when the next frontend deploy rebuilds the image.

## Host Nginx setup (one time)

The server also hosts other sites, so do not touch existing configuration; only add a site file for REMAKE. The development server was set up with the following steps on 2026-11-17.

1. Create `/etc/nginx/sites-available/remake` and link it into `sites-enabled`. Set `server_name` to the assigned domain and `proxy_pass` to `http://127.0.0.1:<APP_PORT>`. The port must match `APP_PORT` in `.env`. The inner proxy does the path routing, so the host Nginx forwards every path unchanged. Write only the HTTP (port 80) `server` block at first. certbot adds the 443 configuration and the HTTP-to-HTTPS redirect in step 5.
2. Pass the `Host`, `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto` headers. The app needs them to recognize HTTPS requests correctly.
3. Set `proxy_buffering off`. The frontend (Next.js) streams pages in parts; if the host Nginx collects the response before sending it, streaming does not work. The inner proxy has the same setting on the frontend path.
4. Do not enable caching (`proxy_cache`). The service worker (`/serwist/sw.js`) carries `s-maxage` of 30 days; if a shared cache keeps it, the old service worker is still served after a deploy.
5. Reload only after `sudo nginx -t` passes. The Nginx is shared with other sites, so a configuration that fails validation also blocks their reloads. If it fails, remove the `sites-enabled/remake` link. Then issue a certificate with `sudo certbot --nginx -d <domain>`. The domain's DNS must point to this server. PWA and Web Push work only over HTTPS. The server's certbot timer renews the certificate automatically.

Do this once. Later deployments never touch the host Nginx.

## Deploy and rollback

Deployment happens when `Backend CD` or `Frontend CD` runs `deploy/deploy.sh` on the server. It can also be run manually on the server.

```bash
cd <DEPLOY_PATH>
bash deploy/deploy.sh backend             # deploy the backend (deploy may be omitted)
bash deploy/deploy.sh frontend            # deploy the frontend
bash deploy/deploy.sh frontend rollback   # switch the frontend back to the previous slot (same for backend)
bash deploy/deploy.sh status              # active slot per app and container status (works during a deploy)
```

Deploy flow. Only the app being deployed changes; the other app's slots are never touched.

1. (Backend only) Start `postgres` only if it is not running. If it is already running, leave it untouched.
2. Build and start the new version in that app's inactive slot, then wait until its health check (backend `/actuator/health/readiness`, frontend `/`) is healthy.
3. When healthy, rewrite `proxy/<app>.conf` to point to the new slot, validate with `nginx -t`, and reload the proxy.
4. Confirm that the proxy actually routes requests to the new slot, and that the app's path on the host port (`APP_PORT`) responds (backend `/api/`, frontend `/`). Only 5xx or a connection failure counts as failure; a 4xx such as 404 means the request reached the app and passes.
5. Only after these checks pass, stop the previous slot. Its container and image are kept for rollback. The backend waits up to 30 seconds for in-flight requests to finish.

There is only one proxy, so deploys and rollbacks run one at a time regardless of the app. If another deploy is in progress, the script waits for it to finish (up to 15 minutes) and then continues. So when a shared-file change starts both CD workflows at once, both apps are deployed in turn.

If any step fails (new slot not ready, config validation failure, proxy start or reload failure, switch check failure), the script restores `proxy/<app>.conf` and the proxy to the previous state and stops. The previous slot is never stopped, so the previous version keeps serving. The proxy is never stopped, because the other app may be serving. The cause (`nginx -t` output, recent logs of the new slot) is written to the deploy log. On an app's first deployment, `blue` is started first. Rollback is possible only to the immediately previous slot. Even after a rollback, the next run of that app's CD deploys the latest code on `dev`.

In the rare case that the restore also fails (for example, reload with the previous config fails), the proxy may still point to the new slot. Stopping either slot could then cause an outage, so the script leaves both the new and the previous slot running and stops with a "manual check required" error. Check `bash deploy/deploy.sh status` and `proxy/<app>.conf`, run `docker compose --env-file .env -f compose.prod.yml exec proxy nginx -s reload` to make the proxy match the slot files, then stop the unused slot manually.

The proxy re-resolves the slot names (`backend-blue`, `frontend-green`, and so on) through Docker's internal DNS every 10 seconds. If an active slot container restarts or the server reboots and its IP changes, the proxy follows the new IP automatically without waiting for the next deployment.

## Verification

Run `bash deploy/deploy.sh status` on the server to check the active slot of each app and the container status. A deploy job succeeds only when the new slot passes its health check. `/actuator` is not exposed externally by the proxy, so check it from inside the server. The check paths used by the deploy script, `/__remake/upstream/backend` and `/__remake/upstream/frontend`, can only be called from inside the proxy container and are blocked on the host port (403).

Changing the `postgres` settings in `compose.prod.yml` is not applied by a deployment. This prevents the DB from being recreated during a deploy. If you change DB settings, pick a time when a brief DB restart is acceptable and run `docker compose --env-file .env -f compose.prod.yml up -d postgres` on the server manually.

The DB schema is managed by Flyway. Change tables only through versioned SQL files (`V<number>__<description>.sql`) in `backend/src/main/resources/db/migration`; JPA only runs `validate` (checks that entities match the tables) in both the production and local profiles. When a new slot starts, Flyway runs the files not yet applied, in order. If a migration fails or a table required by an entity is missing, the new slot never becomes healthy, so the switch does not happen and the previous version keeps serving. PostgreSQL runs DDL inside transactions, so the changes in a failed file are rolled back together (statements that run outside a transaction, such as `CREATE INDEX CONCURRENTLY`, are the exception). During a deployment the old and new versions briefly share the same DB, so make schema changes in backward-compatible steps. The backend and the frontend are also deployed separately, so between the two deploys a new frontend may briefly run against the previous backend (or the reverse).

To pause deployment (server maintenance, freezing the version for a demo, or waiting for a fix PR after a rollback), set `REMAKE_DEPLOY_ENABLED` to `false`. The CD workflows then only run their checks and skip the server deploy, and the server keeps running the last deployed version.

## What the app needs because of the deployment setup

These are not team rules; they are conditions that came from implementing the deployment. The app must meet them for the current deployment setup to work. The numeric limits (1MB, 60 seconds) are Nginx defaults left as they are; if you need more, tell the deployment owner so they can be adjusted.

| Condition | Why | If not met | How |
| --- | --- | --- | --- |
| The health check path responds without authentication | The Docker health check and the switch check in `deploy/deploy.sh` call `/actuator/health/readiness` without credentials. Once Spring Security is added, every path requires authentication by default. | The new slot never becomes healthy and every deployment fails (the previous version keeps serving). | Permit `/actuator/health/**` without authentication in the Security configuration. External users cannot reach it because the proxy returns 404 for `/actuator`, and the production profile exposes only `health`. |
| Add new environment variables to `compose.prod.yml` too | The server's `.env` is only used by compose to fill `${...}` in the configuration file. The container receives only the values listed under `environment` in `compose.prod.yml`. | The app works locally and fails to start only in production. | Add a new setting (token secret, external API key, etc.) to the backend `environment` and to `deploy/prod.env.example`, and tell the deployment owner to put the real value in the server's `.env`. |
| Do not store files inside the container | Every deployment replaces the container. The request body size is limited to 1MB in both the proxy and the host Nginx. | Stored files are lost after a redeploy. Requests over 1MB get 413. | Keep files that must persist, such as uploads, in a volume or external storage (decide with the deployment owner when introducing them). |
| Expect two versions to run together during a deployment | From the moment the new slot starts until the switch and the old slot stops, both versions usually run for a few tens of seconds. State kept only in server memory is lost at the switch. | Scheduled jobs such as `@Scheduled` can run twice. If login state is kept in an in-memory server session, every user is logged out on every deployment. | Make scheduled jobs safe to run twice or prevent duplicate runs. Do not keep login state in an in-memory server session. |
| Each request finishes within 60 seconds | The proxy and the host Nginx wait up to 60 seconds for a response. During a deployment the old version waits at most 30 seconds for in-flight requests before stopping. | Long requests are cut off with 504 or interrupted by a deployment. | For long-running work such as OCR, do not wait for completion inside the request; accept the job and let the client poll its status. |
| Register the development server domain with external services | The app runs behind `https://remake.newlecture.com`. It recognizes that the original request was HTTPS from the headers passed by the host Nginx (`server.forward-headers-strategy: native`). | External integrations such as social login fail only in production. | Register social login redirect URIs and similar addresses as `https://remake.newlecture.com/...`. |
| Write logs to the console (standard output) | `docker logs` and the log size limit (10MB × 3) apply only to console output. | Log files pile up inside the container without a size limit and are lost on redeploy. | Keep Spring's default (console output) and do not add a log file configuration. |
