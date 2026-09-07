'use strict';
const byId = id => document.getElementById(id);
const status = message => { byId('status').textContent = message; };

async function call(method, parameters = {}) {
  const token = byId('token').value;
  if (!token) throw new Error('Enter an access token.');
  const response = await fetch(`requests?${new URLSearchParams(parameters)}`, {
    method,
    headers: { Authorization: `Bearer ${token}`, Accept: 'application/json' },
    cache: 'no-store',
    credentials: 'omit'
  });
  if (!response.ok) throw new Error(`Request rejected (HTTP ${response.status}).`);
  return (await response.json()).response.data;
}

async function load() {
  const records = await call('GET');
  byId('rows').replaceChildren();
  for (const record of records) {
    const row = document.createElement('tr');
    const title = document.createElement('td');
    title.textContent = record.title;
    const action = document.createElement('td');
    const edit = document.createElement('button');
    edit.type = 'button';
    edit.textContent = 'Edit';
    edit.addEventListener('click', () => {
      byId('record-id').value = record.id;
      byId('title').value = record.title;
      byId('title').focus();
    });
    action.append(edit);
    row.append(title, action);
    byId('rows').append(row);
  }
  status(`${records.length} request(s) loaded.`);
}

byId('load').addEventListener('click', () => load().catch(error => status(error.message)));
byId('new').addEventListener('click', () => {
  byId('editor').reset();
  byId('record-id').value = '';
});
byId('editor').addEventListener('submit', async event => {
  event.preventDefault();
  const id = byId('record-id').value;
  try {
    await call(id ? 'PUT' : 'POST', { title: byId('title').value, ...(id ? { id } : {}) });
    byId('editor').reset();
    byId('record-id').value = '';
    await load();
  } catch (error) { status(error.message); }
});
