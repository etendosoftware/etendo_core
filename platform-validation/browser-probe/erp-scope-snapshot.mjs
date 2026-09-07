import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';

/** Read independent authorization expectations only from an owned Classic copy. */
export function readScopeSnapshot(username) {
  const container = process.env.PLATFORM_COPY_CONTAINER;
  assert(/^etendo-platform-classic-copy-[a-f0-9-]{36}$/.test(container ?? ''),
    'Supply the owned Classic-copy container through PLATFORM_COPY_CONTAINER');
  const inspect = execFileSync('docker', ['inspect', '--format',
    '{{.State.Running}} {{index .Config.Labels "etendo.platform.classic-copy"}}', container],
    { encoding: 'utf8', timeout: 10000 }).trim();
  assert.equal(inspect, 'true true', 'The isolated copy must be running and labeled');
  // Hex encoding keeps the user value out of SQL syntax. No credentials are passed to Docker.
  const userHex = Buffer.from(username, 'utf8').toString('hex');
  const sql = `BEGIN READ ONLY;
    SET LOCAL statement_timeout = '10s';
    WITH target_user AS (
      SELECT ad_user_id FROM ad_user WHERE username=convert_from(decode('${userHex}','hex'),'UTF8')
      AND isactive='Y'
    )
    SELECT json_build_object(
      'products', (SELECT json_agg(json_build_object('id',m_product_id,'client',ad_client_id,
        'organization',ad_org_id,'name',name,'searchKey',value)) FROM m_product),
      'contexts', (SELECT json_agg(json_build_object('role',r.ad_role_id,'client',r.ad_client_id,
        'organization',ro.ad_org_id)) FROM target_user u
        JOIN ad_user_roles ur ON ur.ad_user_id=u.ad_user_id AND ur.isactive='Y'
        JOIN ad_role r ON r.ad_role_id=ur.ad_role_id AND r.isactive='Y'
        JOIN ad_role_orgaccess ro ON ro.ad_role_id=r.ad_role_id AND ro.isactive='Y'
        JOIN ad_org o ON o.ad_org_id=ro.ad_org_id AND o.isactive='Y'),
      'tree', (SELECT json_agg(json_build_object('client',c.ad_client_id,'id',n.node_id,
        'parent',n.parent_id)) FROM ad_treenode n
        JOIN ad_clientinfo c ON c.ad_tree_org_id=n.ad_tree_id));
    ROLLBACK;`;
  return JSON.parse(execFileSync('docker', ['exec', '-i', container, 'psql', '-X', '-qAt',
    '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', 'platform_switch'],
    { input: sql, encoding: 'utf8', timeout: 15000, maxBuffer: 4 * 1024 * 1024 }));
}

/** Resolve each direction independently: ancestors do not grant sibling branches. */
export function expectedProducts(snapshot, context) {
  const grants = snapshot.contexts.filter(c => c.role === context.role).map(c => c.organization);
  const products = snapshot.products.filter(p => p.client === context.client);
  if (grants.includes('0')) return products;
  const tree = snapshot.tree.filter(n => n.client === context.client);
  function closure(direction) {
    const result = new Set(grants);
    let changed;
    do {
      changed = false;
      for (const node of tree) {
        const from = direction === 'down' ? node.parent : node.id;
        const to = direction === 'down' ? node.id : node.parent;
        if (result.has(from) && !result.has(to)) { result.add(to); changed = true; }
      }
    } while (changed);
    return result;
  }
  const visible = new Set(['0', ...closure('down'), ...closure('up')]);
  return products.filter(p => visible.has(p.organization));
}
