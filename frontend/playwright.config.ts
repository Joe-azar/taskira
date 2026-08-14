import { defineConfig, devices } from '@playwright/test';

const ciValue = (process.env['CI'] ?? '').trim().toLowerCase();
const isCi = !['', '0', 'false', 'no'].includes(ciValue);

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  workers: isCi ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  outputDir: 'test-results',
  use: {
    baseURL:
      process.env['PLAYWRIGHT_BASE_URL'] ?? 'http://localhost:4200',
    screenshot: 'only-on-failure',
    trace: 'off',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
