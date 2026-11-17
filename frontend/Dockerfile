# 프론트엔드(Next.js) 운영 이미지. next.config.ts 의 output: "standalone" 결과물로 실행한다.
#
# NEXT_PUBLIC_* 값은 빌드할 때 자바스크립트 번들에 글자 그대로 들어간다.
# 그래서 실행할 때(.env)가 아니라 빌드 인자(--build-arg)로 넣어야 하고, 값이 바뀌면 다시 빌드해야 한다.

FROM node:22-alpine AS base
ENV NEXT_TELEMETRY_DISABLED=1 \
    COREPACK_ENABLE_DOWNLOAD_PROMPT=0
# package.json 의 packageManager(pnpm 버전)를 그대로 쓴다.
RUN corepack enable
WORKDIR /app

# ---- 의존성 설치 --------------------------------------------------------------
FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
# prepare 스크립트(husky 설치)는 git 저장소가 없는 이미지 안에서는 필요 없으므로 건너뛴다.
RUN HUSKY=0 pnpm install --frozen-lockfile

# ---- 빌드 ---------------------------------------------------------------------
FROM base AS build
# 운영 기본값: API는 같은 도메인의 상대 경로로 호출하고(빈 값), MSW 목업은 끈다.
ARG NEXT_PUBLIC_API_BASE_URL=""
ARG NEXT_PUBLIC_API_MOCKING="false"
ARG NEXT_PUBLIC_VAPID_PUBLIC_KEY=""
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL \
    NEXT_PUBLIC_API_MOCKING=$NEXT_PUBLIC_API_MOCKING \
    NEXT_PUBLIC_VAPID_PUBLIC_KEY=$NEXT_PUBLIC_VAPID_PUBLIC_KEY
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# 서비스 워커(/serwist/sw.js)가 빌드 때 미리 생성됐는지 확인한다. (blueprint.md §7)
# 요청할 때 생성되는 라우트로 빌드되면 운영 이미지에서 500 이 나므로, 그런 이미지는 여기서 빌드를 멈춘다.
RUN pnpm build \
 && test -f .next/server/app/serwist/sw.js.body

# ---- 실행 ---------------------------------------------------------------------
FROM node:22-alpine AS runner
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000
WORKDIR /app

# standalone 서버는 public 과 .next/static 을 직접 복사해 줘야 서빙한다.
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
COPY --from=build --chown=node:node /app/public ./public

USER node
EXPOSE 3000
CMD ["node", "server.js"]
