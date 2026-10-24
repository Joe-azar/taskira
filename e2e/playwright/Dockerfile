FROM node:26.12.0-bookworm-slim@sha256:742a0fe7320e09e0321222b68165e6ac731d30470e7ea7f98f9bfc3a6e1034fe

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
