import { expect, test } from '@playwright/test';

import { authHeaders, expectStatus, registerUser, testKey, testPassword } from '../support/api';
import { loginThroughUi } from '../support/ui';

const apiBaseUrl = process.env['TASKIRA_API_BASE_URL'] ?? 'http://localhost:8080/api';

test.describe('authentication and authorization', () => {
  test('shows the public login form', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByRole('heading', { name: 'Connexion' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Mot de passe')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Se connecter' })).toBeEnabled();
  });

  test('rejects an invalid .test login', async ({ page }, testInfo) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill(`missing.${testKey(testInfo, 'missing')}@taskira.test`);
    await page.getByLabel('Mot de passe').fill(testPassword);

    const responsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/auth/login'
    );
    await page.getByRole('button', { name: 'Se connecter' }).click();
    expect((await responsePromise).status()).toBe(401);

    await expect(page.getByText('Invalid credentials')).toBeVisible();
    await expect(page).toHaveURL((url) => url.pathname === '/login');
  });

  test('redirects an anonymous visitor away from a protected route', async ({ page }) => {
    await page.goto('/projects?sort=recent');

    await expect(page).toHaveURL(
      (url) =>
        url.pathname === '/login' &&
        url.searchParams.get('redirectTo') === '/projects?sort=recent'
    );
  });

  test('logs in and logs out a user registered through the API', async ({ page, request }, testInfo) => {
    const user = await registerUser(request, testInfo, 'login-owner');

    await loginThroughUi(page, user);
    await expect(page.getByText(user.email)).toBeVisible();
    await expect(page.evaluate(() => localStorage.getItem('taskira_token'))).resolves.toBeTruthy();

    await page.getByRole('button', { name: 'Se déconnecter' }).click();
    await expect(page).toHaveURL((url) => url.pathname === '/login');
    expect(await page.evaluate(() => localStorage.getItem('taskira_token'))).toBeNull();
  });

  test('refuses a USER on the administration API and route', async ({ page, request }, testInfo) => {
    const user = await registerUser(request, testInfo, 'ordinary-user');
    const forbiddenResponse = await request.post(`${apiBaseUrl}/users`, {
      headers: authHeaders(user),
      data: {
        firstName: 'E2E',
        lastName: 'Forbidden',
        email: `forbidden.${testKey(testInfo, 'forbidden')}@taskira.test`,
        password: testPassword,
        globalRole: 'USER',
        active: true,
      },
    });
    await expectStatus(forbiddenResponse, 403, 'USER administration request');

    await loginThroughUi(page, user);
    await page.goto('/users');
    await expect(page).toHaveURL((url) => url.pathname === '/dashboard');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Users' })).toHaveCount(0);
  });
});
