import { chromium } from 'playwright';
import assert from 'node:assert/strict';

const username = process.env.PLATFORM_TEST_USERNAME;
const password = process.env.PLATFORM_TEST_PASSWORD;
assert(username && password, 'Supply test credentials through the process environment');
const browser = await chromium.launch({ channel: 'chrome', headless: true });
try {
  const page = await browser.newPage();
  await page.goto('http://127.0.0.1:8093/etendo/');
  await page.locator('#user').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#buttonOK').click();
  await page.waitForURL(url => !url.pathname.includes('/security/Login'), { timeout: 30000 });
  const response = await page.request.post('http://127.0.0.1:8093/etendo/org.openbravo.service.datasource/Product', {
    form: { _operationType: 'fetch', windowId: '140', tabId: '180', _startRow: '0', _endRow: '100',
      _sortBy: 'searchKey', _selectedProperties: 'id,name,searchKey,client,organization,productCategory' }
  });
  assert.equal(response.status(), 200);
  const payload = await response.json();
  assert.equal(payload.response.status, 0, 'Original datasource must authorize this ERP session');
  assert(payload.response.data.length > 0, 'Original datasource must return persisted products');
  assert(payload.response.data.every(row => row.id && row.name && row._entityName === 'Product'));
  console.log(`PASS: Original ERP browser login and session-authenticated Product fetch (${payload.response.data.length} rows)`);
} finally { await browser.close(); }
