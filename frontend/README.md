# TaskiraWeb

This project uses Angular CLI 21.2.2. The repository is Docker-first: run every command below from the repository root; no host installation of Node, npm, Angular CLI, or a browser is required.

## Development server

Start the complete development stack with hot reload:

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

Open `http://localhost:4200/`. Angular reloads when a source file changes.

## Code scaffolding

With the development stack running, execute the repository-local Angular CLI inside the frontend container:

```powershell
docker compose -f infra/docker-compose.yml exec frontend npm exec ng -- generate component component-name
```

List the available schematics with:

```powershell
docker compose -f infra/docker-compose.yml exec frontend npm exec ng -- generate --help
```

## Build and unit tests

Build the frontend test image once, then run the production build and Vitest suite in isolated containers:

```powershell
docker build -f frontend/Dockerfile -t taskira-frontend-tests frontend
docker run --rm taskira-frontend-tests npm run build
docker run --rm taskira-frontend-tests npm run test:unit
```

## Running end-to-end tests

The Playwright suite expects Taskira to be available on `http://localhost:4200` by default. Run it without installing Node or a browser on the host:

```powershell
docker build -f frontend/Dockerfile.e2e -t taskira-frontend-e2e frontend
docker run --rm `
  --add-host=host.docker.internal:host-gateway `
  -v "${PWD}\frontend\playwright-report:/app/playwright-report" `
  -v "${PWD}\frontend\test-results:/app/test-results" `
  taskira-frontend-e2e
```

`Dockerfile.e2e` pins Node 22.23.2, installs npm 11.9.0, and installs the Chromium version required by the pinned Playwright package. Its local TCP proxies preserve the browser origin `http://localhost:4200` and forward ports 4200 and 8080 to the Docker host, so Angular and the API keep the same URLs and CORS behavior as normal development. Override `TASKIRA_E2E_HOST` if the Docker host has another name. HTML reports and failure screenshots are written to the two ignored host directories mounted above.

The public login and route-guard scenarios need no account. To enable the optional real-login scenario, pass disposable development credentials as environment variables; they are never stored in the repository:

```powershell
docker run --rm `
  --add-host=host.docker.internal:host-gateway `
  -v "${PWD}\frontend\playwright-report:/app/playwright-report" `
  -v "${PWD}\frontend\test-results:/app/test-results" `
  -e TASKIRA_E2E_EMAIL="user@example.test" `
  -e TASKIRA_E2E_PASSWORD="replace-me" `
  taskira-frontend-e2e
```

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
