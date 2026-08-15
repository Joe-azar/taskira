import { expect, type Page } from '@playwright/test';

import type { TestUser } from './api';

export async function loginThroughUi(page: Page, user: TestUser): Promise<void> {
  await page.goto('/login');

  const emailInput = page.getByLabel('Email');
  const passwordInput = page.getByLabel('Mot de passe');

  await emailInput.fill(user.email);
  await passwordInput.fill(user.password);

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' &&
      new URL(response.url()).pathname === '/api/v1/auth/login'
  );

  try {
    await page.getByRole('button', { name: 'Se connecter' }).click();
    const loginResponse = await loginResponsePromise;
    expect(loginResponse.status()).toBe(200);
    await expect(page).toHaveURL((url) => url.pathname === '/dashboard');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
  } finally {
    if (await passwordInput.isVisible().catch(() => false)) {
      await passwordInput.fill('').catch(() => undefined);
    }
  }
}

export async function chooseOptionContaining(
  select: ReturnType<Page['getByLabel']>,
  text: string
): Promise<void> {
  const option = select.locator('option').filter({ hasText: text });
  await expect(option).toHaveCount(1);
  const value = await option.getAttribute('value');
  expect(value).not.toBeNull();
  await select.selectOption(value!);
}
