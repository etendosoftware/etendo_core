import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { chromium } from 'playwright';

const properties = readFileSync(process.argv[2], 'utf8');
const property = name => {
  const line = properties.split(/\r?\n/).find(value => value.startsWith(`${name}=`));
  if (!line) throw new Error(`Missing fixture property: ${name}`);
  return line.slice(name.length + 1).trim();
};
const base = process.argv[3] || 'http://127.0.0.1:8092/platform/';
const address = new URL(base);
assert.equal(address.hostname, '127.0.0.1', 'Browser probe is restricted to loopback');
const browser = await chromium.launch({ channel: 'chrome', headless: true });
const page = await browser.newPage();
const errors = [];
page.on('pageerror', error => errors.push(error.message));
const original = `Browser request ${Date.now()}`;
const updated = `${original} updated`;
try {
  await page.goto(base);
  await page.getByLabel('Access token').fill(property('platform.validation.token'));
  await page.getByRole('button', { name: 'Load requests', exact: true }).click();
  await page.getByRole('status').filter({ hasText: 'loaded.' }).waitFor();
  await page.getByLabel('Title', { exact: true }).fill(original);
  await page.getByRole('button', { name: 'Save request', exact: true }).click();
  const row = page.getByRole('row').filter({ hasText: original });
  await row.waitFor();
  await row.getByRole('button', { name: 'Edit', exact: true }).click();
  await page.getByLabel('Title', { exact: true }).fill(updated);
  await page.getByRole('button', { name: 'Save request', exact: true }).click();
  await page.getByRole('cell', { name: updated, exact: true }).waitFor();
  await page.reload();
  await page.getByLabel('Access token').fill(property('platform.validation.readOnlyToken'));
  await page.getByRole('button', { name: 'Load requests', exact: true }).click();
  await page.getByRole('cell', { name: updated, exact: true }).waitFor();
  await page.getByRole('row').filter({ hasText: updated }).getByRole('button', { name: 'Edit' }).click();
  await page.getByLabel('Title', { exact: true }).fill(`${original} forbidden`);
  await page.getByRole('button', { name: 'Save request', exact: true }).click();
  await page.getByRole('status').filter({ hasText: 'HTTP 403' }).waitFor();
  await page.getByRole('button', { name: 'Load requests', exact: true }).click();
  await page.getByRole('status').filter({ hasText: 'loaded.' }).waitFor();
  assert.equal(await page.getByRole('cell', { name: updated, exact: true }).count(), 1);
  assert.equal(await page.getByRole('cell', { name: `${original} forbidden`, exact: true }).count(), 0);
  assert.deepEqual(errors, [], 'Browser JavaScript must not fail');
  console.log('PASS: Browser UI loaded, created, updated, reloaded and denied a read-only write');
  console.log(`Retained test-owned record: ${updated}`);
} finally {
  await page.getByLabel('Access token').fill('').catch(() => {});
  await browser.close();
}
