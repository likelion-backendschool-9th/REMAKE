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

The deploy workflow never transfers or overwrites the server's `.env` or `proxy/`. DB data is kept in a Docker named volume.

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
bash deploy/deploy.sh status     # active slot and container status
```

Deploy flow:

1. Build and start the new version in the inactive slot, then wait until `/actuator/health/readiness` is healthy.
2. When healthy, rewrite `proxy/upstream.conf` to point to the new slot, validate with `nginx -t`, and reload the proxy.
3. Confirm that the proxy returns a response.
4. Stop the previous slot after in-flight requests finish (up to 30 seconds). Its container and image are kept for rollback.

If the new slot does not become ready, or the check after switching fails, the script does not switch (or reverts) and stops; the previous version keeps serving. On the first deployment, `blue` is started first and then the proxy. Rollback is possible only to the immediately previous slot.

## Verification

Run `bash deploy/deploy.sh status` on the server to check the active slot and container status. The deploy job succeeds only when the new slot passes its readiness check. `/actuator` is not exposed externally by the proxy, so check it from inside the server.

The production profile does not auto-alter the JPA schema and only runs `validate`, so apply DB migrations first when entities change. During a deployment the old and new versions briefly share the same DB, so make schema changes in backward-compatible steps.

If no server has been chosen yet, do not set `REMAKE_DEPLOY_ENABLED`. At this stage no public service address is created.
