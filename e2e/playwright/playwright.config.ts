import path from 'node:path';

import { defineConfig, devices } from '@playwright/test';

const ciValue = (process.env['CI'] ?? '').trim().toLowerCase();
const isCi = !['', '0', 'false', 'no'].includes(ciValue);

export default defineConfig({
  testDir: path.join(__dirname, 'tests'),
  fullyParallel: false,
  forbidOnly: isCi,
  retries: isCi ? 1 : 0,
  workers: 1,
  reporter: [
    ['list'],
    [
      'html',
      {
        open: 'never',
        outputFolder: path.join(__dirname, 'playwright-report'),
      },
    ],
  ],
  outputDir: path.join(__dirname, 'test-results'),
  use: {
    baseURL: process.env['PLAYWRIGHT_BASE_URL'] ?? 'http://localhost:4200',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
