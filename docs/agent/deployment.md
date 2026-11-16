# Backend Deployment Setup

> Korean version for developers: [`docs/developer/deployment.md`](../developer/deployment.md)

As in the Resuit project, changes on `dev` are verified in GitHub Actions, transferred to the server over SSH and rsync, and the backend and PostgreSQL are started with Docker Compose. The frontend is currently empty, so only the backend is deployed.

## Current state

- When backend or deployment config changes land on `dev`, the build and tests of `Backend CD` run.
- Actual server deployment runs only after the repository variable `REMAKE_DEPLOY_ENABLED=true` is set. Until a server is chosen, the deploy job is skipped.
- It can also be run manually via `workflow_dispatch`. The same variable decides whether the server deploy actually runs.

## Server preparation

1. Install Docker Engine, the Docker Compose plugin, and rsync on an Ubuntu server. Grant the deploy account permission to run Docker.
2. Choose a directory used only by REMAKE and create `.env` inside it. Fill in the DB password using [`deploy/prod.env.example`](../../deploy/prod.env.example) as a reference and restrict the file permission to `600`. The `backend/` folder in this directory is overwritten by source sync on every deploy, so do not put other files there.
3. Prepare an SSH key dedicated to GitHub Actions and add its public key to the deploy account's `authorized_keys`. Verify the server host key fingerprint separately, then save the `known_hosts` entry.
4. Under repository **Settings → Secrets and variables → Actions**, set the following.

| Kind | Name | Value |
| --- | --- | --- |
| Secret | `DEPLOY_HOST` | Server domain or IPv4 address |
| Secret | `DEPLOY_USER` | SSH deploy account |
| Secret | `DEPLOY_PATH` | Absolute path of the deploy directory on the server, no spaces |
| Secret | `SSH_PRIVATE_KEY` | Full contents of the Actions-only private key |
| Secret | `SSH_KNOWN_HOSTS` | `known_hosts` entry of the verified server host key |
| Variable | `REMAKE_DEPLOY_ENABLED` | `true` once setup is complete |

The deploy workflow never transfers or overwrites the server's `.env`. DB data is kept in a Docker named volume. The backend listens only on the server's `127.0.0.1:8080`; if external access is needed, proxy it through Nginx or similar once a domain and TLS are decided.

## Verification

Run `docker compose --env-file .env -f compose.prod.yml ps` on the server. The deploy job succeeds only when the backend's `/actuator/health` responds normally. The production profile does not auto-alter the JPA schema and only runs `validate`, so apply DB migrations first when entities change.

If no server has been chosen yet, do not set `REMAKE_DEPLOY_ENABLED`. At this stage no public service address is created.
