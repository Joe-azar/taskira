FROM node:26.11.1-bookworm-slim@sha256:86f07bc9c5dce4578cf37e5a418b7bfc7f817cda25cde66e2b66e95ed86c4567

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
