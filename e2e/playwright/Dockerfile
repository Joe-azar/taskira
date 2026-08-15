FROM node:24.19.0-bookworm-slim@sha256:3638d9a6fe4030bd716be989438248074489337ba3275657f93595428be4fc03

WORKDIR /workspace

RUN npm install --global npm@12.0.2

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
RUN npx playwright install --with-deps chromium
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl socat \
    && rm -rf /var/lib/apt/lists/*

COPY e2e/playwright /workspace/e2e/playwright
RUN chmod +x /workspace/e2e/playwright/run-in-docker.sh

ENTRYPOINT ["/workspace/e2e/playwright/run-in-docker.sh"]
