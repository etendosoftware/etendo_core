import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';

// Evaluate generated descriptors only; this is not a browser or widget-backend test.
for (const role of ['R1', 'R_READ', 'R_EXCLUDE']) {
  const context = { OB: { Application: {} } };
  for (const component of ['NavigationBarComponent', 'MainLayoutComponent']) {
    const file = new URL(`./build/ui-${component}-${role}.js`, import.meta.url);
    runInNewContext(readFileSync(file, 'utf8'), context, { timeout: 1000, filename: file.pathname });
  }
  const application = context.OB.Application;
  const staticComponents = application.navigationBarComponents;
  const denied = role === 'R_EXCLUDE';
  assert.equal(staticComponents.length, denied ? 6 : 7, `Unexpected navbar size for ${role}`);
  for (const name of ['OBAlertIcon', 'OBHelpAbout', 'OBUserProfile', 'OBLogout']) {
    assert.equal(staticComponents.filter(component => component.className === name).length, 1);
  }
  assert.equal(staticComponents.filter(component => component.className === 'OBQuickLaunch').length, 2);
  assert.equal(staticComponents.filter(component => component.className === '_OBNavBarDynamicComponent').length, denied ? 0 : 1);
  const dynamic = runInNewContext('OB.Application.dynamicNavigationBarComponents()', context, { timeout: 1000 });
  assert.equal(dynamic.length, denied ? 0 : 1);
  if (!denied) assert.equal(dynamic[0].className, 'OBApplicationMenuButton');
}
console.log('PASS: Original static descriptors and dynamic menu assembly preserve three-role access');
