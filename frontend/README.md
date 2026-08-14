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

Run the isolated Playwright stack from the repository root without installing Node or a browser on the host:

```powershell
& .\e2e\playwright\run.ps1
```

The command builds a dedicated Angular, Spring Boot, PostgreSQL, and Playwright stack. It publishes no host port, stores PostgreSQL data only in a container `tmpfs`, and always runs `docker compose down --volumes --remove-orphans` in a `finally` block. The Playwright runner exposes separate local TCP proxies on ports 4200 and 8080 inside its own container, preserving the application origins `http://localhost:4200` and `http://localhost:8080` without weakening CORS.

The suite creates disposable users with reserved `.test` email addresses through the public API. It does not use development accounts or personal credentials. Reports, traces, and failure screenshots are written to the ignored directories `e2e/playwright/playwright-report/` and `e2e/playwright/test-results/`.

To inspect or control the Compose invocation directly, use `e2e/playwright/compose.e2e.yml` with an explicit project name and finish with `down --volumes --remove-orphans` for that same project.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
