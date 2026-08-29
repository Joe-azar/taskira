FROM node:26.8.1-bookworm-slim@sha256:367679cf9792759492a486e4aa4b421764d71a9546a6dae8aab81a99eb797b3e

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
