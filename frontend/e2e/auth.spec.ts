import { expect, test } from '@playwright/test';

test.describe('authentication entry points', () => {
  test('shows the public login form', async ({ page }) => {
    await page.goto('/login');

    await expect(
      page.getByRole('heading', { name: 'Connexion' })
    ).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Mot de passe')).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Se connecter' })
    ).toBeEnabled();
  });

  test('shows the backend error for an invalid login', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Email').fill('invalid@example.test');
    await page.getByLabel('Mot de passe').fill('not-a-real-password');
    const loginResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/auth/login'
    );
    await page.getByRole('button', { name: 'Se connecter' }).click();
    const loginResponse = await loginResponsePromise;
    expect(loginResponse.status()).toBe(401);

    await expect(page.getByText('Invalid credentials')).toBeVisible();
    await expect(page).toHaveURL((url) => url.pathname === '/login');
  });

  test('redirects an anonymous visitor away from a protected route', async ({
    page,
  }) => {
    await page.goto('/projects?sort=recent');

    await expect(page).toHaveURL(
      (url) =>
        url.pathname === '/login' &&
        url.searchParams.get('redirectTo') === '/projects?sort=recent'
    );
    await expect(
      page.getByRole('heading', { name: 'Connexion' })
    ).toBeVisible();
  });
});

test('logs in against the real API when optional credentials are provided', async ({
  page,
}) => {
  const email = process.env['TASKIRA_E2E_EMAIL'];
  const password = process.env['TASKIRA_E2E_PASSWORD'];
  test.skip(
    !email || !password,
    'Set TASKIRA_E2E_EMAIL and TASKIRA_E2E_PASSWORD to run this scenario.'
  );

  await page.goto('/login');
  const emailInput = page.getByLabel('Email');
  const passwordInput = page.getByLabel('Mot de passe');

  try {
    await emailInput.fill(email!);
    await passwordInput.fill(password!);
    await page.getByRole('button', { name: 'Se connecter' }).click();

    await expect(page).toHaveURL((url) => url.pathname === '/dashboard');
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();
    await expect(
      page.getByRole('button', { name: 'Se déconnecter' })
    ).toBeVisible();
  } finally {
    for (const input of [emailInput, passwordInput]) {
      if (await input.isVisible().catch(() => false)) {
        await input.fill('').catch(() => undefined);
      }
    }
  }
});
