import { createHash } from 'node:crypto';

import { expect, type APIRequestContext, type APIResponse, type TestInfo } from '@playwright/test';

const apiBaseUrl = process.env['TASKIRA_API_BASE_URL'] ?? 'http://localhost:8080/api/v1';

export const testPassword = 'Taskira-E2E-Only-42!';

export interface TestUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  token: string;
}

export interface TestProject {
  id: number;
  code: string;
  name: string;
  description: string;
}

export interface TestTicket {
  id: number;
  reference: string;
  title: string;
}

export function testKey(testInfo: TestInfo, label: string): string {
  return createHash('sha256')
    .update(`${testInfo.testId}:${testInfo.retry}:${label}`)
    .digest('hex')
    .slice(0, 10);
}

export async function registerUser(
  request: APIRequestContext,
  testInfo: TestInfo,
  label: string
): Promise<TestUser> {
  const key = testKey(testInfo, label);
  const normalizedLabel = label.toLowerCase().replace(/[^a-z0-9]+/g, '-');
  const firstName = 'E2E';
  const lastName = `${label}-${key}`;
  const email = `${normalizedLabel}.${key}.r${testInfo.retry}@taskira.test`;

  const response = await request.post(`${apiBaseUrl}/auth/register`, {
    data: {
      firstName,
      lastName,
      email,
      password: testPassword,
      confirmPassword: testPassword,
    },
  });
  const body = await jsonBody(response, 201, `register ${label}`);

  return {
    id: Number(body.user.id),
    email,
    firstName,
    lastName,
    password: testPassword,
    token: String(body.accessToken),
  };
}

export async function createProject(
  request: APIRequestContext,
  testInfo: TestInfo,
  owner: TestUser,
  label: string
): Promise<TestProject> {
  const key = testKey(testInfo, label);
  const project = {
    code: `T${key.slice(0, 9).toUpperCase()}`,
    name: `E2E project ${key}`,
    description: `Isolated E2E project ${key}`,
  };
  const response = await request.post(`${apiBaseUrl}/projects`, {
    headers: authHeaders(owner),
    data: project,
  });
  const body = await jsonBody(response, 201, `create project ${label}`);

  return { id: Number(body.id), ...project };
}

export async function addProjectMember(
  request: APIRequestContext,
  owner: TestUser,
  project: TestProject,
  member: TestUser,
  projectRole = 'MEMBER'
): Promise<void> {
  const response = await request.post(`${apiBaseUrl}/projects/${project.id}/members`, {
    headers: authHeaders(owner),
    data: { userId: member.id, projectRole },
  });
  await jsonBody(response, 201, `add ${member.email} to project ${project.id}`);
}

export async function createTicket(
  request: APIRequestContext,
  testInfo: TestInfo,
  owner: TestUser,
  project: TestProject,
  label: string
): Promise<TestTicket> {
  const key = testKey(testInfo, label);
  const title = `E2E ticket ${key}`;
  const response = await request.post(`${apiBaseUrl}/tickets`, {
    headers: authHeaders(owner),
    data: {
      projectId: project.id,
      title,
      description: `Isolated E2E ticket ${key}`,
      type: 'TASK',
      priority: 'MEDIUM',
    },
  });
  const body = await jsonBody(response, 201, `create ticket ${label}`);

  return {
    id: Number(body.id),
    reference: String(body.reference),
    title,
  };
}

export function authHeaders(user: TestUser): Record<string, string> {
  return { Authorization: `Bearer ${user.token}` };
}

export async function jsonBody(
  response: APIResponse,
  expectedStatus: number,
  operation: string
): Promise<any> {
  const text = await response.text();
  expect(response.status(), `${operation} failed: ${text}`).toBe(expectedStatus);
  return text ? JSON.parse(text) : undefined;
}

export async function expectStatus(
  response: APIResponse,
  expectedStatus: number,
  operation: string
): Promise<void> {
  const text = await response.text();
  expect(response.status(), `${operation} failed: ${text}`).toBe(expectedStatus);
}
