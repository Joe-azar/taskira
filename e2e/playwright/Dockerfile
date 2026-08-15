FROM node:26.7.0-bookworm-slim@sha256:cd565714d4da3e84bfd341e31448f81d47c6362198f152345297c9c1154e6341

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
