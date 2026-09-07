import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

// Development-only source inventory; no runtime dependencies or generated files.
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const modules = [
  'org.openbravo.client.application',
  'org.openbravo.client.kernel',
  'org.openbravo.userinterface.selector'
];
const forbidden = new Set([
  'org.openbravo.model.common.plm.Product',
  'org.openbravo.model.common.enterprise.Warehouse',
  'org.openbravo.model.common.businesspartner.BusinessPartner'
]);

function inspect(source) {
  return source.split(/\r?\n/).flatMap((line, index) => {
    const imported = line.match(/^\s*import\s+(?:static\s+)?(org\.openbravo\.model\.[\w.*]+)\s*;/);
    if (imported) {
      const target = imported[1];
      return [{ line: index + 1, kind: 'model-import', target,
        forbiddenEntity: [...forbidden].some(type => target === type || target.startsWith(`${type}.`)) }];
    }
    const resource = line.match(/createStaticResource\("(web\/js\/[^"\n]+)"/);
    return resource ? [{ line: index + 1, kind: 'global-script', target: resource[1] }] : [];
  });
}

function* javaFiles(directory) {
  for (const entry of readdirSync(directory, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name, 'en'))) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) yield* javaFiles(path);
    else if (entry.isFile() && entry.name.endsWith('.java')) yield path;
  }
}

if (process.argv.includes('--self-test')) {
  const results = inspect('import org.openbravo.model.common.plm.Product;\n'
    + 'import org.openbravo.model.common.enterprise.Organization;\n'
    + 'import static org.openbravo.model.common.enterprise.Warehouse.ENTITY_NAME;\n'
    + 'createStaticResource("web/js/productServices.js", true);\n'
    + '// import org.openbravo.model.common.plm.Product;');
  assert.equal(results.length, 4);
  assert.deepEqual(results.map(item => item.forbiddenEntity), [true, false, true, undefined]);
  assert.equal(results[3].target, 'web/js/productServices.js');
  assert.equal(results[3].line, 4);
  console.log('PASS: UI source inventory matching controls');
} else {
  const findings = [];
  let scannedFiles = 0;
  for (const module of modules) {
    for (const path of javaFiles(join(root, 'modules_core', module, 'src'))) {
      scannedFiles++;
      for (const finding of inspect(readFileSync(path, 'utf8'))) {
        findings.push({ file: relative(root, path), ...finding });
      }
    }
  }
  console.log(JSON.stringify({ scope: 'Direct source references only', scannedFiles,
    forbiddenEntityReferences: findings.filter(item => item.forbiddenEntity).length,
    findings }, null, 2));
}
