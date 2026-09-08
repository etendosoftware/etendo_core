import { chromium } from 'playwright';
import assert from 'node:assert/strict';

const username = process.env.PLATFORM_TEST_USERNAME;
const password = process.env.PLATFORM_TEST_PASSWORD;
assert(username && password, 'Supply test credentials through the environment');
const base = 'http://127.0.0.1:8091/platform/';
const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  const context = await browser.newContext();
  const login = await context.request.post(`${base}login`, { form: { user: username, password } });
  assert.equal(login.status(), 204, 'Canonical login failed');
  const page = await context.newPage();
  const errors = [];
  const failed = [];
  page.on('pageerror', error => errors.push(error.stack?.split('\n').slice(0, 12).join('\n') ?? error.message));
  page.on('response', response => {
    if (response.status() >= 400) {
      const url = new URL(response.url());
      const action = url.searchParams.get('_action');
      failed.push(`${response.status()} ${url.pathname}${action && /^[\w.]+$/.test(action) ? ` action=${action}` : ''}`);
    }
  });
  await page.goto(`${base}shell?windowId=F10D7EFA9C7B30B3AD12697F4552153C`, {
    waitUntil: 'networkidle', timeout: 30000
  });
  const state = await page.evaluate(() => ({
    smartClient: !!window.isc,
    application: !!window.OB?.Application,
    layout: !!window.OB?.Layout,
    gridClass: !!window.isc?.OBViewGrid
  }));
  console.log(JSON.stringify({ errors: errors.slice(0, 15), failed: failed.slice(0, 30), state,
    body: (await page.locator('body').innerText()).slice(0, 800) }, null, 2));
  // Diagnostic only: class presence does not establish widget initialization or CRUD.
  if (errors.length || failed.length) process.exitCode = 1;
} finally {
  await browser.close();
}
