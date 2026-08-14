import { expect, test } from '@playwright/test';

import { createProject, registerUser, testKey } from '../support/api';
import { chooseOptionContaining, loginThroughUi } from '../support/ui';

test.describe('projects and members', () => {
  test('creates, updates, and archives a project', async ({ page, request }, testInfo) => {
    const owner = await registerUser(request, testInfo, 'project-owner');
    const key = testKey(testInfo, 'project-ui');
    const initialName = `E2E UI project ${key}`;
    const updatedName = `E2E updated project ${key}`;
    const initialCode = `U${key.slice(0, 9).toUpperCase()}`;
    const updatedCode = `V${key.slice(0, 9).toUpperCase()}`;

    await loginThroughUi(page, owner);
    await page.goto('/projects');
    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible();

    await page.getByLabel('Nom', { exact: true }).fill(initialName);
    await page.getByLabel('Clé projet').fill(initialCode);
    await page.getByLabel('Description', { exact: true }).fill(`Initial description ${key}`);
    const createResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === '/api/projects'
    );
    await page.getByRole('button', { name: 'Créer le projet' }).click();
    expect((await createResponsePromise).status()).toBe(201);
    await expect(page.getByText('Projet créé avec succès.')).toBeVisible();

    await page.getByRole('link', { name: initialName }).click();
    await expect(page.getByRole('heading', { name: initialName })).toBeVisible();

    await page.getByRole('button', { name: 'Modifier le projet' }).click();
    const editForm = page.locator('form.edit-form');
    await editForm.getByLabel('Nom *').fill(updatedName);
    await editForm.getByLabel('Code *').fill(updatedCode);
    await editForm.getByLabel('Description', { exact: true }).fill(`Updated description ${key}`);
    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PUT' &&
        /^\/api\/projects\/\d+$/.test(new URL(response.url()).pathname)
    );
    await editForm.getByRole('button', { name: 'Enregistrer', exact: true }).click();
    expect((await updateResponsePromise).status()).toBe(200);
    await expect(page.getByRole('heading', { name: updatedName })).toBeVisible();
    await expect(page.getByText('Projet modifié avec succès.')).toBeVisible();

    page.once('dialog', (dialog) => dialog.accept());
    const archiveResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'PATCH' &&
        /\/api\/projects\/\d+\/archive$/.test(new URL(response.url()).pathname)
    );
    await page.getByRole('button', { name: 'Archiver' }).click();
    expect((await archiveResponsePromise).status()).toBe(200);
    await expect(page.getByText('Projet archivé avec succès.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Créer un ticket' })).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Ajouter un membre' })).toHaveCount(0);
  });

  test('adds and removes a project member', async ({ page, request }, testInfo) => {
    const owner = await registerUser(request, testInfo, 'member-owner');
    const member = await registerUser(request, testInfo, 'member-user');
    const project = await createProject(request, testInfo, owner, 'member-project');

    await loginThroughUi(page, owner);
    await page.goto(`/projects/${project.id}`);
    await expect(page.getByRole('heading', { name: project.name })).toBeVisible();

    await chooseOptionContaining(page.getByLabel('Utilisateur'), member.email);
    await page.getByLabel('Rôle projet').selectOption('MEMBER');
    const addResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' &&
        new URL(response.url()).pathname === `/api/projects/${project.id}/members`
    );
    await page.getByRole('button', { name: 'Ajouter le membre' }).click();
    expect((await addResponsePromise).status()).toBe(201);
    await expect(page.getByText('Membre ajouté avec succès.')).toBeVisible();

    const memberArticle = page.locator('article.member-item').filter({ hasText: member.email });
    await expect(memberArticle).toBeVisible();
    page.once('dialog', (dialog) => dialog.accept());
    const removeResponsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === 'DELETE' &&
        new URL(response.url()).pathname ===
          `/api/projects/${project.id}/members/${member.id}`
    );
    await memberArticle.getByRole('button', { name: 'Retirer' }).click();
    expect((await removeResponsePromise).status()).toBe(204);
    await expect(page.getByText('Membre retiré avec succès.')).toBeVisible();
    await expect(memberArticle).toHaveCount(0);
  });
});
