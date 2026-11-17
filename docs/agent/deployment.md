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
- The active slot of each app is the one referenced by `proxy/backend.conf` or `proxy/frontend.conf` on the server. The deploy script rewrites the file of the app being deployed and reloads the proxy to switch. These are server-only files and are never overwritten by the deploy file sync. An app that has never been deployed points to a "not ready" placeholder, and its paths return 503.

## Current state

- When changes to `backend/` land on `dev`, `Backend CD` runs; changes to `frontend/` run `Frontend CD`. Changes to the shared deploy files (`compose.prod.yml`, `deploy/`) run both.
- Each CD first runs its checks (backend `./gradlew build`; frontend `pnpm lint`, `pnpm typecheck`, `pnpm build`) and deploys to the server only if they pass.
- Actual server deployment runs only after the repository variable `REMAKE_DEPLOY_ENABLED=true` is set. Until a server is chosen, the deploy job is skipped.
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

## Host Nginx setup (after the domain is decided)

The server also hosts other sites, so do not touch existing configuration; only add a site file for REMAKE. Once the domain is decided:

1. Create `/etc/nginx/sites-available/remake` and link it into `sites-enabled`. Set `server_name` to the assigned domain and `proxy_pass` to `http://127.0.0.1:<APP_PORT>`. The port must match `APP_PORT` in `.env`. The inner proxy does the path routing, so the host Nginx forwards every path unchanged.
2. Pass the `Host`, `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto` headers. The app needs them to recognize HTTPS requests correctly.
3. Do not enable caching (`proxy_cache`). The service worker (`/serwist/sw.js`) carries `s-maxage` of 30 days; if a shared cache keeps it, the old service worker is still served after a deploy.
4. Run `sudo nginx -t`, reload, then issue a certificate with `sudo certbot --nginx -d <domain>`. The domain's DNS must point to this server. PWA and Web Push work only over HTTPS.

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

If any step fails (new slot not ready, config validation failure, proxy start or reload failure, switch check failure), the script restores `proxy/<app>.conf` and the proxy to the previous state and stops. The previous slot is never stopped, so the previous version keeps serving. The proxy is never stopped, because the other app may be serving. The cause (`nginx -t` output, recent logs of the new slot) is written to the deploy log. On an app's first deployment, `blue` is started first. Rollback is possible only to the immediately previous slot.

In the rare case that the restore also fails (for example, reload with the previous config fails), the proxy may still point to the new slot. Stopping either slot could then cause an outage, so the script leaves both the new and the previous slot running and stops with a "manual check required" error. Check `bash deploy/deploy.sh status` and `proxy/<app>.conf`, run `docker compose --env-file .env -f compose.prod.yml exec proxy nginx -s reload` to make the proxy match the slot files, then stop the unused slot manually.

The proxy re-resolves the slot names (`backend-blue`, `frontend-green`, and so on) through Docker's internal DNS every 10 seconds. If an active slot container restarts or the server reboots and its IP changes, the proxy follows the new IP automatically without waiting for the next deployment.

## Verification

Run `bash deploy/deploy.sh status` on the server to check the active slot of each app and the container status. A deploy job succeeds only when the new slot passes its health check. `/actuator` is not exposed externally by the proxy, so check it from inside the server. The check paths used by the deploy script, `/__remake/upstream/backend` and `/__remake/upstream/frontend`, can only be called from inside the proxy container and are blocked on the host port (403).

Changing the `postgres` settings in `compose.prod.yml` is not applied by a deployment. This prevents the DB from being recreated during a deploy. If you change DB settings, pick a time when a brief DB restart is acceptable and run `docker compose --env-file .env -f compose.prod.yml up -d postgres` on the server manually.

The production profile does not auto-alter the JPA schema and only runs `validate`, so apply DB migrations first when entities change. During a deployment the old and new versions briefly share the same DB, so make schema changes in backward-compatible steps. The backend and the frontend are also deployed separately, so between the two deploys a new frontend may briefly run against the previous backend (or the reverse).

If no server has been chosen yet, do not set `REMAKE_DEPLOY_ENABLED`. At this stage no public service address is created.
