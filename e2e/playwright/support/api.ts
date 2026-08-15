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
  sessionCookie: string;
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

/**
 * CSRF is enforced on every mutating request regardless of how the caller authenticates.
 * Playwright's request fixture keeps a cookie jar automatically (like a browser), but it
 * won't echo a cookie back as a header on its own the way Angular's XSRF interceptor does
 * in the real app - callers have to read the seeded cookie and attach it themselves. The
 * token is anonymous/stateless (not tied to any user identity), so reusing the shared
 * context's copy across different simulated users is safe.
 *
 * The seeding GET explicitly clears the Cookie header rather than letting Playwright
 * attach whatever the shared context has accumulated: without this, seeding the token
 * for a second simulated user would ride in on the first user's still-tracked session
 * cookie, answering /auth/me as the wrong identity (harmless for the CSRF value itself,
 * but misleading and masks the same ambient-cookie hazard fixed below in registerUser).
 */
export async function getCsrfToken(request: APIRequestContext): Promise<string> {
  await request.get(`${apiBaseUrl}/auth/me`, { headers: { Cookie: '' } });
  const state = await request.storageState();
  const cookie = state.cookies.find((c) => c.name === 'XSRF-TOKEN');
  if (!cookie) {
    throw new Error('XSRF-TOKEN cookie was not set after GET /auth/me');
  }
  return cookie.value;
}

/**
 * Reads a Set-Cookie value directly off one response, rather than from the shared request
 * context's cookie jar. This test suite registers several distinct users through the same
 * request context to set up test data (e.g. an "owner" and a "member") - since the backend
 * sets a session cookie on register/login, relying on the shared jar would mean each new
 * registration silently overwrites the previous user's session for every later call in the
 * test, authenticating them all as whoever registered last instead of the intended user.
 */
function extractCookieValue(response: APIResponse, cookieName: string): string {
  const prefix = `${cookieName}=`;
  for (const header of response.headersArray()) {
    if (header.name.toLowerCase() !== 'set-cookie') {
      continue;
    }
    const pair = header.value.split(';')[0];
    if (pair.startsWith(prefix)) {
      return decodeURIComponent(pair.slice(prefix.length));
    }
  }
  throw new Error(`Set-Cookie for ${cookieName} not found in response from ${response.url()}`);
}

/**
 * Builds the headers needed to act as a specific registered user on a mutating request:
 * that user's own session cookie (not whatever the shared request context happens to be
 * carrying - see extractCookieValue) plus a valid CSRF cookie/header pair.
 */
export async function authHeaders(
  request: APIRequestContext,
  user: TestUser
): Promise<Record<string, string>> {
  const csrfToken = await getCsrfToken(request);
  return {
    Cookie: `TASKIRA_SESSION=${user.sessionCookie}; XSRF-TOKEN=${csrfToken}`,
    'X-XSRF-TOKEN': csrfToken,
  };
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
  const csrfToken = await getCsrfToken(request);

  // Explicit Cookie header, deliberately carrying only the CSRF cookie: this suite
  // registers several users through the same shared request context (e.g. an "owner"
  // and a "member"), and Playwright's context auto-attaches every cookie it has seen -
  // including a prior user's TASKIRA_SESSION. Without overriding it here, the second
  // registration rides in on the first user's still-valid session; the backend then
  // reuses that existing session rather than starting a new one and never re-sends
  // Set-Cookie for it, which silently authenticates both users under one session.
  const response = await request.post(`${apiBaseUrl}/auth/register`, {
    headers: {
      'X-XSRF-TOKEN': csrfToken,
      Cookie: `XSRF-TOKEN=${csrfToken}`,
    },
    data: {
      firstName,
      lastName,
      email,
      password: testPassword,
      confirmPassword: testPassword,
    },
  });
  const body = await jsonBody(response, 201, `register ${label}`);
  const sessionCookie = extractCookieValue(response, 'TASKIRA_SESSION');

  return {
    id: Number(body.id),
    email,
    firstName,
    lastName,
    password: testPassword,
    sessionCookie,
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
    headers: await authHeaders(request, owner),
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
    headers: await authHeaders(request, owner),
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
    headers: await authHeaders(request, owner),
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
