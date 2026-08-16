export const environment = {
  production: true,
  // Relative, not absolute: this build is served by Nginx same-origin with the API
  // (Nginx proxies /api/ to the backend internally), so the browser must call its own
  // origin - an absolute localhost:8080 URL would bypass the proxy and never reach
  // wherever the app is actually deployed.
  apiUrl: '/api/v1',
};
