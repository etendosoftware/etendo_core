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
  const endpoint = 'http://127.0.0.1:8093/etendo/org.openbravo.service.datasource/Product';
  const form = { _operationType: 'fetch', windowId: '140', tabId: '180', _startRow: '0', _endRow: '100',
    _sortBy: 'searchKey', _selectedProperties: 'id,name,searchKey,client,organization,productCategory' };
  async function fetch(overrides = {}) {
    const response = await page.request.post(endpoint, { form: { ...form, ...overrides } });
    assert.equal(response.status(), 200);
    const payload = await response.json();
    assert.equal(payload.response.status, 0, 'Original datasource must authorize this ERP session');
    return payload.response;
  }
  const full = await fetch();
  assert(full.data.length > 10 && full.data.length < 100, 'Fixture must exercise multiple complete pages');
  const ids = rows => rows.map(row => row.id);
  assert.equal(new Set(ids(full.data)).size, full.data.length);
  for (const row of full.data) {
    for (const field of ['id', 'name', 'searchKey', 'client', 'organization', 'productCategory']) {
      assert.equal(typeof row[field], 'string', `Selected field ${field} must be returned`);
    }
    assert.equal(row._entityName, 'Product');
    assert.equal(row.$ref, `Product/${row.id}`);
    assert.equal(typeof row._identifier, 'string');
    for (const field of ['client', 'organization', 'productCategory']) {
      assert.equal(typeof row[`${field}$_identifier`], 'string', `Reference identifier ${field} must be returned`);
    }
    assert(!Object.hasOwn(row, 'description'), 'Unselected business fields must not leak into projection');
  }
  const keys = full.data.map(row => row.searchKey);
  assert.deepEqual(keys, [...keys].sort(), 'Fixture search keys must be sorted ascending');
  const descending = await fetch({ _sortBy: '-searchKey' });
  assert.deepEqual(ids(descending.data), ids(full.data).reverse(), 'Descending order must reverse fixture products');
  // Legacy DefaultJsonDataService uses an inclusive end row (end - start + 1).
  const first = await fetch({ _endRow: '4' });
  const second = await fetch({ _startRow: '5', _endRow: '9' });
  assert.equal(first.startRow, 0);
  assert.equal(second.startRow, 5);
  assert.equal(first.endRow, 4);
  assert.equal(second.endRow, 9);
  assert.deepEqual(ids(first.data), ids(full.data.slice(0, 5)));
  assert.deepEqual(ids(second.data), ids(full.data.slice(5, 10)));
  const anonymous = await browser.newContext();
  try {
    const denied = await anonymous.request.post(endpoint, { form, maxRedirects: 0 });
    // The original UI authentication filter returns a login script with HTTP 200.
    assert.equal(denied.status(), 200);
    assert.match(denied.headers()['content-type'], /^application\/javascript/);
    assert.equal((await denied.text()).trim(),
      "window.location.href = 'http://127.0.0.1:8093/etendo/security/Login';",
      'Anonymous response must contain only the legacy login redirect, never Product data');
  } finally { await anonymous.close(); }
  console.log(`PASS: ERP browser session, projection, references, ordering, paging and anonymous denial (${full.data.length} rows)`);
} finally { await browser.close(); }
