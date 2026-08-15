import { expect, test } from '@playwright/test';

import {
  addProjectMember,
  createProject,
  createTicket,
  registerUser,
  testKey,
} from '../support/api';
import { chooseOptionContaining, loginThroughUi } from '../support/ui';

test.describe('tickets and comments', () => {
  test('creates and updates a ticket, then changes status and assignee', async ({
    page,
    request,
  }, testInfo) => {
    const owner = await registerUser(request, testInfo, 'ticket-owner');
    const member = await registerUser(request, testInfo, 'ticket-assignee');
    const project = await createProject(request, testInfo, owner, 'ticket-project');
    await addProjectMember(request, owner, project, member);
    const key = testKey(testInfo, 'ticket-ui');
    const initialTitle = `E2E UI ticket ${key}`;
    const updatedTitle = `E2E updated ticket ${key}`;

    await loginThroughUi(page, owner);
    await page.goto(`/projects/${project.id}`);
    await expect(page.getByRole('heading', { name: project.name })).toBeVisible();

    await page.getByLabel('Titre', { exact: true }).fill(initialTitle);
    await page.getByLabel('Type', { exact: true }).selectOption('BUG');
    await page.getByLabel('Priorité', { exact: true }).selectOption('HIGH');
    await page.getByLabel('Description', { exact: true }).fill(`Initial ticket ${key}`);
    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/v1/tickets'
    );
    await page.getByRole('button', { name: 'Créer le ticket' }).click();
    expect((await createResponsePromise).status()).toBe(201);
    await expect(page.getByText('Ticket créé avec succès.')).toBeVisible();

    await page.getByRole('link', { name: initialTitle }).click();
    await expect(
      page.locator('.page-header').getByText(initialTitle, { exact: true })
    ).toBeVisible();

    await page.getByRole('button', { name: 'Modifier', exact: true }).click();
    await page.getByLabel('Titre *').fill(updatedTitle);
    await page.getByLabel('Description', { exact: true }).fill(`Updated ticket ${key}`);
    await page.getByLabel('Type *').selectOption('FEATURE');
    await page.getByLabel('Priorité *').selectOption('CRITICAL');
    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /^\/api\/v1\/tickets\/\d+$/.test(new URL(response.url()).pathname)
    );
    await page.getByRole('button', { name: 'Enregistrer', exact: true }).click();
    expect((await updateResponsePromise).status()).toBe(200);
    await expect(page.getByText('Ticket mis à jour avec succès.')).toBeVisible();
    await expect(
      page.locator('.page-header').getByText(updatedTitle, { exact: true })
    ).toBeVisible();

    await page.getByLabel('Statut').selectOption('IN_PROGRESS');
    const statusResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PATCH' &&
        /\/api\/v1\/tickets\/\d+\/status$/.test(new URL(response.url()).pathname)
    );
    await page.getByRole('button', { name: 'Mettre à jour le statut' }).click();
    const statusResponse = await statusResponsePromise;
    expect(statusResponse.status()).toBe(200);
    await expect(page.getByText('Statut mis à jour avec succès.')).toBeVisible();
    // The response body is read via the UI, not response.json(): the app refetches
    // the ticket immediately after this PATCH succeeds, and that follow-up request
    // races Playwright's out-of-process CDP call for the PATCH response body, which
    // is sometimes already evicted by the time it resolves (a documented Playwright/
    // CDP limitation, not an application bug - see the "Response body is not
    // available for a response that was navigated away from" error it raises).
    await expect(page.locator('.page-header').locator('app-status-badge')).toHaveText(
      'In progress'
    );

    await chooseOptionContaining(page.getByLabel('Membre du projet'), member.email);
    const assigneeResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PATCH' &&
        /\/api\/v1\/tickets\/\d+\/assignee$/.test(new URL(response.url()).pathname)
    );
    await page.getByRole('button', { name: /Mettre à jour l.assignation/ }).click();
    const assigneeResponse = await assigneeResponsePromise;
    expect(assigneeResponse.status()).toBe(200);
    await expect(page.getByText('Assignation mise à jour avec succès.')).toBeVisible();
    // Same reasoning as the status check above: assert the UI after the app's own
    // reload settles, rather than reading the PATCH response body via CDP. Angular's
    // [ngValue] binding encodes the <select>'s DOM value as "<index>: <value>", so
    // assert on the selected option's visible label instead of the raw value.
    await expect(
      page.getByLabel('Membre du projet').locator('option:checked')
    ).toHaveText(new RegExp(member.email));
  });

  test('creates, updates, and deletes a comment', async ({ page, request }, testInfo) => {
    const owner = await registerUser(request, testInfo, 'comment-owner');
    const project = await createProject(request, testInfo, owner, 'comment-project');
    const ticket = await createTicket(request, testInfo, owner, project, 'comment-ticket');
    const key = testKey(testInfo, 'comment-ui');
    const initialComment = `E2E initial comment ${key}`;
    const updatedComment = `E2E updated comment ${key}`;

    await loginThroughUi(page, owner);
    await page.goto(`/tickets/${ticket.id}`);
    await expect(page.getByRole('heading', { name: ticket.reference })).toBeVisible();

    await page.getByLabel('Commentaire').fill(initialComment);
    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === `/api/v1/tickets/${ticket.id}/comments`
    );
    await page.getByRole('button', { name: 'Ajouter le commentaire' }).click();
    expect((await createResponsePromise).status()).toBe(201);
    await expect(page.getByText('Commentaire ajouté avec succès.')).toBeVisible();

    let commentArticle = page.locator('article.comment-item').filter({ hasText: owner.email });
    await expect(commentArticle).toBeVisible();
    await expect(commentArticle.getByText(initialComment, { exact: true })).toBeVisible();
    await commentArticle.getByRole('button', { name: 'Modifier' }).click();
    await commentArticle.locator('textarea').fill(updatedComment);
    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /^\/api\/v1\/comments\/\d+$/.test(new URL(response.url()).pathname)
    );
    await commentArticle.getByRole('button', { name: 'Enregistrer' }).click();
    expect((await updateResponsePromise).status()).toBe(200);

    commentArticle = page.locator('article.comment-item').filter({ hasText: owner.email });
    await expect(commentArticle).toBeVisible();
    await expect(commentArticle.getByText(updatedComment, { exact: true })).toBeVisible();
    page.once('dialog', (dialog) => dialog.accept());
    const deleteResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'DELETE' &&
        /^\/api\/v1\/comments\/\d+$/.test(new URL(response.url()).pathname)
    );
    await commentArticle.getByRole('button', { name: 'Supprimer' }).click();
    expect((await deleteResponsePromise).status()).toBe(204);
    await expect(commentArticle).toHaveCount(0);
  });
});
