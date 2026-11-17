# Backend Deployment

> Korean version for developers: [`docs/developer/deployment.md`](../developer/deployment.md)

Changes on `dev` are verified in GitHub Actions, transferred to the server over SSH and rsync, and deployed with `deploy/deploy.sh` as a Blue/Green zero-downtime deployment. The frontend is not deployed yet, so only the backend is deployed.

## Deployment architecture

```
user → host Nginx (80/443, domain·TLS) → 127.0.0.1:APP_PORT → proxy container ─┬→ backend-blue:8080
                                                                                └→ backend-green:8080
                                                                    postgres (shared by both slots, no host port)
```

- `backend-blue` and `backend-green` are two slots running the same app. While one slot serves traffic, the new version is started in the other.
- Only one host port (`APP_PORT`, the port assigned to the project) is used, and the `proxy` container listens on it. The slots and the DB are reachable only through the Docker internal network.
- The server is shared by several teams, so each backend slot has a memory limit (`mem_limit: 2g`) and the Java heap may use up to 75% of it. During a deploy both slots run, so the backend can use up to 4GB. A changed limit takes effect on the next deploy, without downtime.
- The active slot is the one referenced by `proxy/upstream.conf` on the server. The deploy script rewrites this file and reloads the proxy to switch. It is a server-only file and is never overwritten by the deploy file sync.

## Current state

- When changes to `backend/`, `compose.prod.yml`, or `deploy/` land on `dev`, the build and tests of `Backend CD` run.
- Actual server deployment runs only after the repository variable `REMAKE_DEPLOY_ENABLED=true` is set. Until a server is chosen, the deploy job is skipped.
- It can also be run manually via `workflow_dispatch`. The same variable decides whether the server deploy actually runs.

## Server preparation

1. Install Docker Engine, the Docker Compose plugin, rsync, and curl on an Ubuntu server. Add the deploy account to the `docker` group so it can run Docker without sudo (`sudo usermod -aG docker <account>`, then log in again).
2. Choose a directory used only by REMAKE and create `.env` inside it. Fill in the DB password using [`deploy/prod.env.example`](../../deploy/prod.env.example) as a reference and restrict the file permission to `600`. The `backend/` and `deploy/` folders in this directory are overwritten by source sync on every deploy, so do not put other files there.
3. Enter the assigned host port as `APP_PORT` in `.env`. Without it, compose does not run and the deploy workflow stops at the server check step.
4. Prepare an SSH key dedicated to GitHub Actions and add its public key to the deploy account's `authorized_keys`. Verify the server host key fingerprint separately, then save the `known_hosts` entry.
5. Under repository **Settings → Secrets and variables → Actions**, set the following.

| Kind | Name | Value |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | Server domain or IPv4 address |
| Secret | `DEPLOY_USER` | SSH deploy account |
| Secret | `DEPLOY_PATH` | Absolute path of the deploy directory on the server, no spaces |
| Secret | `SSH_PRIVATE_KEY` | Full contents of the Actions-only private key |
| Secret | `SSH_KNOWN_HOSTS` | `known_hosts` entry of the verified server host key |
| Variable | `REMAKE_DEPLOY_ENABLED` | `true` once setup is complete |

The deploy workflow never transfers or overwrites the server's `.env` or `proxy/`. DB data is kept in the Docker named volume `remake_postgres-data`. `compose.prod.yml` pins the project name to `remake`, so the same volume is used regardless of the deploy directory's name or location. Do not change this name: a new, empty volume would be created.

## Host Nginx setup (after the domain is decided)

The server also hosts other sites, so do not touch existing configuration; only add a site file for REMAKE. Once the domain and path layout are decided:

1. Create `/etc/nginx/sites-available/remake` and link it into `sites-enabled`. Set `server_name` to the assigned domain and `proxy_pass` to `http://127.0.0.1:<APP_PORT>`. The port must match `APP_PORT` in `.env`.
2. Pass the `Host`, `X-Real-IP`, `X-Forwarded-For`, and `X-Forwarded-Proto` headers. The app needs them to recognize HTTPS requests correctly.
3. Run `sudo nginx -t`, reload, then issue a certificate with `sudo certbot --nginx -d <domain>`. The domain's DNS must point to this server.

Do this once. Later deployments never touch the host Nginx.

## Deploy and rollback

Deployment happens when `Backend CD` runs `deploy/deploy.sh` on the server. It can also be run manually on the server.

```bash
cd <DEPLOY_PATH>
bash deploy/deploy.sh            # deploy (default)
bash deploy/deploy.sh rollback   # switch back to the previous slot
bash deploy/deploy.sh status     # active slot and container status (works during a deploy)
```

Deploy flow:

1. Start `postgres` only if it is not running. If it is already running, leave it untouched.
2. Build and start the new version in the inactive slot, then wait until `/actuator/health/readiness` is healthy.
3. When healthy, rewrite `proxy/upstream.conf` to point to the new slot, validate with `nginx -t`, and reload the proxy.
4. Confirm that the proxy actually routes requests to the new slot, and that the host port (`APP_PORT`) returns a response.
5. Only after these checks pass, stop the previous slot. It waits for in-flight requests to finish (up to 30 seconds), and its container and image are kept for rollback.

If any step fails (new slot not ready, config validation failure, proxy start or reload failure, switch check failure), the script restores `upstream.conf` and the proxy to the previous state and stops. The previous slot is never stopped, so the previous version keeps serving. The cause (`nginx -t` output, recent logs of the new slot) is written to the deploy log. On the first deployment, `blue` is started first and then the proxy. Rollback is possible only to the immediately previous slot.

In the rare case that the restore also fails (for example, reload with the previous config fails), the proxy may still point to the new slot. Stopping either slot could then cause an outage, so the script leaves both the new and the previous slot running and stops with a "manual check required" error. Check `bash deploy/deploy.sh status` and `proxy/upstream.conf`, run `docker compose --env-file .env -f compose.prod.yml exec proxy nginx -s reload` to make the proxy match `upstream.conf`, then stop the unused slot manually.

The proxy re-resolves the slot names (`backend-blue`, `backend-green`) through Docker's internal DNS every 10 seconds. If the active slot container restarts or the server reboots and its IP changes, the proxy follows the new IP automatically without waiting for the next deployment.

## Verification

Run `bash deploy/deploy.sh status` on the server to check the active slot and container status. The deploy job succeeds only when the new slot passes its readiness check. `/actuator` is not exposed externally by the proxy, so check it from inside the server. The check path used by the deploy script, `/__remake/upstream`, can only be called from inside the proxy container and is blocked on the host port (403).

Changing the `postgres` settings in `compose.prod.yml` is not applied by a deployment. This prevents the DB from being recreated during a deploy. If you change DB settings, pick a time when a brief DB restart is acceptable and run `docker compose --env-file .env -f compose.prod.yml up -d postgres` on the server manually.

The production profile does not auto-alter the JPA schema and only runs `validate`, so apply DB migrations first when entities change. During a deployment the old and new versions briefly share the same DB, so make schema changes in backward-compatible steps.

If no server has been chosen yet, do not set `REMAKE_DEPLOY_ENABLED`. At this stage no public service address is created.
