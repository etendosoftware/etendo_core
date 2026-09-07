import { readdirSync, existsSync } from 'node:fs';
import { join, resolve, delimiter } from 'node:path';
import { spawnSync } from 'node:child_process';
import assert from 'node:assert/strict';

// Read-only JDK bytecode inventory. Never deploys, changes a database or loads application classes.
const prefixes = ['org.openbravo.client.application.', 'org.openbravo.client.kernel.',
  'org.openbravo.userinterface.selector.', 'org.openbravo.service.datasource.', 'org.openbravo.base.weld.'];
const entryPoints = ['org.openbravo.erpCommon.security.Login',
  'org.openbravo.base.secureApp.HttpSecureAppServlet', 'org.openbravo.dal.core.DalContextListener'];
const forbidden = ['org.openbravo.model.common.plm.Product',
  'org.openbravo.model.common.enterprise.Warehouse', 'org.openbravo.model.common.businesspartner.BusinessPartner'];

function classes(directory, prefix = '') {
  return readdirSync(directory, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name, 'en'))
    .flatMap(entry => entry.isDirectory() ? classes(join(directory, entry.name), prefix + entry.name + '.')
      : entry.name.endsWith('.class') ? [prefix + entry.name.slice(0, -6)] : []);
}

function graphOf(output) {
  const graph = new Map();
  for (const line of output.split('\n')) {
    const match = line.match(/^\s+([\w.$]+)\s+->\s+([\w.$]+)\s+/);
    if (!match) continue;
    if (!graph.has(match[1])) graph.set(match[1], new Set());
    graph.get(match[1]).add(match[2]);
  }
  return graph;
}

function apiOf(output) {
  const api = new Map();
  let current, method;
  for (const line of output.split('\n')) {
    const header = line.match(/^(?:public |protected |private |abstract |final |static )*(?:class|interface|enum) ([\w.$]+)/);
    if (header) {
      current = { methods: new Set(), parent: line.match(/\bextends ([\w.$]+)/)?.[1] };
      api.set(header[1], current);
      method = undefined;
    } else if (current && line.includes('(') && !line.includes('descriptor:')) {
      method = line.match(/([^\s(]+)\(/)?.[1];
      if (method?.includes('.')) method = '<init>';
    } else if (current && method && line.includes('descriptor:')) {
      current.methods.add(`${method}:${line.trim().slice('descriptor:'.length).trim()}`);
      method = undefined;
    }
  }
  return api;
}

function callsOf(output) {
  const calls = [];
  let caller;
  for (const line of output.split('\n')) {
    const header = line.match(/^(?:public |protected |private |abstract |final |static )*(?:class|interface|enum) ([\w.$]+)/);
    if (header) caller = header[1];
    const call = line.replaceAll('"', '').match(/\/\/ (?:InterfaceMethod|Method) ([\w/$]+)\.([^:]+):(\S+)/);
    if (call && caller) calls.push({ caller, target: call[1].replaceAll('/', '.'), signature: `${call[2]}:${call[3]}` });
  }
  return calls;
}

if (process.argv.includes('--self-test')) {
  const api = apiOf('public class example.Model extends example.Base {\n  public example.Model();\n'
    + '    descriptor: ()V\n  public java.lang.String getName();\n    descriptor: ()Ljava/lang/String;\n}');
  assert.equal(api.get('example.Model').parent, 'example.Base');
  assert(api.get('example.Model').methods.has('getName:()Ljava/lang/String;'));
  assert(api.get('example.Model').methods.has('<init>:()V'));
  assert.deepEqual(callsOf('public class example.View {\n  2: invokevirtual #3 // Method example/Model.getName:()Ljava/lang/String;'),
    [{ caller: 'example.View', target: 'example.Model', signature: 'getName:()Ljava/lang/String;' }]);
  assert(graphOf('   example.View -> example.Model not found').get('example.View').has('example.Model'));
  console.log('PASS: JDK dependency, descriptor and invocation parsing controls');
  process.exit(0);
}

const generatedArgument = process.argv.indexOf('--generated');
if (generatedArgument < 0 || !process.argv[generatedArgument + 1]) {
  throw new Error('Usage: JAVA_HOME=<jdk17> node platform-validation/ui-closure-inventory.mjs --generated <fresh-ui-class-directory>');
}
const baseline = resolve('platform-validation/build/classic-compatibility/WEB-INF/classes');
const generated = resolve(process.argv[generatedArgument + 1]);
assert(existsSync(baseline) && existsSync(generated), 'Prepared baseline and fresh generated UI classes are required');
const run = (tool, args) => {
  const executable = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin', tool) : tool;
  const result = spawnSync(executable, args, { encoding: 'utf8', maxBuffer: 192 * 1024 * 1024 });
  if (result.error || result.status !== 0) throw new Error(`${tool} failed: ${result.error || result.stderr}`);
  return result.stdout;
};
const dump = (names, classpath, code = false) => {
  let output = '';
  for (let offset = 0; offset < names.length; offset += 80) {
    output += run('javap', ['-classpath', classpath, '-p', '-s', ...(code ? ['-c'] : []), ...names.slice(offset, offset + 80)]);
  }
  return output;
};
const available = new Set(classes(baseline));
const libraryDirectory = resolve('platform-validation/build/classic-compatibility/WEB-INF/lib');
const projectLibraries = [];
const libraryOrigins = new Map();
for (const entry of readdirSync(libraryDirectory).filter(name => name.endsWith('.jar')).sort()) {
  const path = join(libraryDirectory, entry);
  const listing = spawnSync('unzip', ['-Z1', path], { encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
  if (listing.error || listing.status !== 0) throw new Error(`Cannot index ${entry}: ${listing.error || listing.stderr}`);
  const projectTypes = listing.stdout.split('\n').filter(name => /^(org\/openbravo\/|com\/etendoerp\/).*\.class$/.test(name))
    .map(name => name.slice(0, -6).replaceAll('/', '.'));
  if (!projectTypes.length) continue;
  projectLibraries.push(path);
  for (const type of projectTypes) {
    if (!available.has(type)) libraryOrigins.set(type, entry);
    available.add(type);
  }
}
const generatedTypes = new Set(classes(generated).filter(name => name.startsWith('org.openbravo.')));
assert(generatedTypes.size > 0, 'The supplied directory contains no generated Openbravo model classes');
const modelTypes = new Set(classes(resolve('platform-validation/build/classes/java/classicGenerated')));
const roots = [...available].filter(name => !modelTypes.has(name)
  && (prefixes.some(prefix => name.startsWith(prefix)) || entryPoints.includes(name)));
const baselineClasspath = baseline + delimiter + join(libraryDirectory, '*');
const graph = graphOf(run('jdeps', ['--multi-release', '17', '--ignore-missing-deps', '-verbose:class', '-filter:none',
  '--class-path', baselineClasspath, baseline, ...projectLibraries]));
const parent = new Map(roots.map(name => [name, null]));
const pending = [...roots];
for (let i = 0; i < pending.length; i++) {
  for (const target of graph.get(pending[i]) || []) {
    if (!available.has(target) || parent.has(target)) continue;
    parent.set(target, pending[i]);
    pending.push(target);
  }
}
const route = type => {
  const path = [type];
  while (parent.get(path[0])) path.unshift(parent.get(path[0]));
  return path;
};
const reachableModelTypes = pending.filter(type => modelTypes.has(type));
const directModelTypes = [...new Set(roots.flatMap(type => [...(graph.get(type) || [])]))]
  .filter(type => modelTypes.has(type)).sort();
const classpath = generated + delimiter + baselineClasspath;
const api = apiOf(dump([...generatedTypes], classpath));
for (;;) {
  const parents = [...new Set([...api.values()].map(item => item.parent).filter(name => name && !api.has(name)))];
  if (!parents.length) break;
  const inherited = apiOf(dump(parents, classpath));
  if (!inherited.size) throw new Error('Could not inspect generated model inheritance');
  for (const [name, value] of inherited) api.set(name, value);
}
function hasMethod(type, signature) {
  const entry = api.get(type);
  return Boolean(entry && (entry.methods.has(signature) || (entry.parent && hasMethod(entry.parent, signature))));
}
const calls = callsOf(dump(roots, baselineClasspath, true));
const missing = new Map();
for (const call of calls) {
  if (!modelTypes.has(call.target) || !generatedTypes.has(call.target) || hasMethod(call.target, call.signature)) continue;
  const key = `${call.target}.${call.signature}`;
  if (!missing.has(key)) missing.set(key, { target: call.target, signature: call.signature, callers: new Set() });
  missing.get(key).callers.add(call.caller);
}
const missingMethods = [...missing.values()].map(item => ({ ...item, callers: [...item.callers].sort() }));
const missingTypes = reachableModelTypes.filter(type => !generatedTypes.has(type));
const unresolvedProjectTypes = [...new Set(pending.flatMap(type => [...(graph.get(type) || [])]))]
  .filter(type => /^(org\.openbravo\.|com\.etendoerp\.)/.test(type) && !available.has(type)).sort();
console.log(JSON.stringify({ scope: 'Static original UI roots and baseline transitive candidates; direct root method calls compared with fresh generated model APIs',
  limitations: ['Not runtime necessity or completion proof', 'Reflection/CDI discovery, SQL names, templates and JavaScript require separate checks',
    'Prepared WEB-INF/classes and JARs containing project namespaces are traversed; unresolved project types remain explicit',
    'Method checks cover direct UI-root calls only; transitive methods, fields and interface defaults are not exhaustively checked'],
  counts: { uiRoots: roots.length, staticClosure: pending.length, generatedModelTypes: generatedTypes.size,
    directModelTypes: directModelTypes.length, reachableModelTypes: reachableModelTypes.length,
    missingModelTypes: missingTypes.length, missingMethods: missingMethods.length,
    unresolvedProjectTypes: unresolvedProjectTypes.length, projectLibraries: projectLibraries.length },
  directForbiddenCallers: roots.flatMap(caller => forbidden.filter(type => graph.get(caller)?.has(type))
    .map(target => ({ caller, target }))),
  forbiddenPaths: forbidden.filter(type => parent.has(type)).map(route), roots,
  libraryProvidedTypes: pending.filter(type => libraryOrigins.has(type)).map(type => ({ type, library: libraryOrigins.get(type) })),
  directMissingTypes: directModelTypes.filter(type => !generatedTypes.has(type)),
  unresolvedProjectTypes, missingTypes, missingMethods }, null, 2));
