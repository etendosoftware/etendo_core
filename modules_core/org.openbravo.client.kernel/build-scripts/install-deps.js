/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
/* eslint-disable no-console */
/* global process */

/**
 * node script that installs the npm dependencies defined in the package.json of the Openbravo root and
 * on openbravo modules that include a package.json file.
 *
 * it runs npm ci to ensure that a clean installation is done, enforcing the use of the dependency versions
 * included in the package-lock files
 *
 * the dependencies defined in each module will be installed in a node_modules folder in the module folder where
 * the package.json file is included
 *
 * symlinks are created to ensure that a given module has access to the dependencies defined in modules that it
 * depends on
 */

const execSync = require('child_process').execSync;
const path = require('path');
const fs = require('fs');
const NpmDependenciesValidator = require('./NpmDependenciesValidator');

const WEB_JSPACK = 'web-jspack';
const PACKAGE_JSON = 'package.json';
const GLOBAL_MODULES = 'node_modules_global';

const modulesDir = path.resolve('modules');
const globalModulesPath = path.resolve(GLOBAL_MODULES);
const npmDependenciesValidator = new NpmDependenciesValidator();

const production = process.env.NODE_ENV === 'production';

validateDependencies();
// prepares folder where links to openbravo node modules will be linked
fs.rmdirSync(globalModulesPath, {
  recursive: true
});
fs.mkdirSync(globalModulesPath, {
  recursive: true
});

// install modules in openbravo root rolder
// ignore scripts to avoid a infinite loop caused by this script already being executed as part of a npm script
const environment = production ? '--production' : '';
execSync(`npm ci --ignore-scripts ${environment}`, { stdio: 'inherit' });

getModules()
  .filter(module => moduleContainsPackageJson(module))
  .forEach(module => {
    const packageJsonPaths = [
      path.resolve(modulesDir, module),
      path.resolve(modulesDir, module, WEB_JSPACK, module)
    ];
    packageJsonPaths
      .filter(packageJsonPath =>
        fs.existsSync(path.resolve(packageJsonPath, PACKAGE_JSON))
      )
      .forEach(packageJsonPath => {
        console.log(`Installing node modules in ${packageJsonPath}`);
        console.log(`npm ci...`);
        execSync(`npm ci ${environment}`, {
          stdio: 'inherit',
          cwd: packageJsonPath
        });
        if (!production) {
          linkDependenciesInBaseNodeModules(module, packageJsonPath);
        }
      });
  });

function validateDependencies() {
  const { warnings, errors } = npmDependenciesValidator.validate();

  warnings.forEach(warning => console.log(warning));
  if (errors.length > 0) {
    console.log(
      'Errors found in the npm dependency validation, stopping process'
    );
    errors.forEach(error => console.log(error));
    process.exit(1);
  }
}

function getModules() {
  return fs.readdirSync(modulesDir);
}

function moduleContainsPackageJson(m) {
  const modulesDir = path.resolve('modules');
  const paths = [
    path.resolve(modulesDir, m, PACKAGE_JSON),
    path.resolve(modulesDir, m, WEB_JSPACK, m, PACKAGE_JSON)
  ];
  return paths.some(path => fs.existsSync(path));
}

function getScopeAndName(npmDependency) {
  let scope;
  let packageName;
  if (npmDependency.startsWith('@')) {
    const slashIndex = npmDependency.indexOf('/');
    scope = npmDependency.substring(0, slashIndex);
    packageName = npmDependency.substring(slashIndex + 1);
  } else {
    scope = '';
    packageName = npmDependency;
  }
  return { scope, packageName };
}

/**
 * Create symbolic links of first-level dependencies to base node_modules folder. This will make those
 * dependencies available to all modules, enabling the execution of jest tests from the root and the
 * resolution of modules in IDEs
 */
function linkDependenciesInBaseNodeModules(module, packageJsonPath) {
  console.log(
    `Running npm link to make ${packageJsonPath} available to other modules`
  );
  execSync(`npm_config_prefix=${globalModulesPath} npm link`, {
    stdio: 'inherit',
    cwd: packageJsonPath
  });
  console.log(
    `Creating links for ${module} dependencies in ${packageJsonPath}`
  );
  const nodeModulesRootPath = path.resolve('node_modules');
  let linkTarget = `../${GLOBAL_MODULES}/lib/node_modules/${module}`;
  let linkPath = `${nodeModulesRootPath}/${module}`;
  fs.symlinkSync(linkTarget, linkPath);

  const jsonContent = JSON.parse(
    fs.readFileSync(path.resolve(packageJsonPath, PACKAGE_JSON))
  );
  const dependencies = {
    ...jsonContent.dependencies,
    ...jsonContent.devDependencies
  };

  Object.keys(dependencies).forEach(depFullName => {
    const { scope, packageName } = getScopeAndName(depFullName);
    if (scope.length > 0) {
      fs.mkdirSync(`${nodeModulesRootPath}/${scope}`, {
        recursive: true
      });
      linkTarget = `../../${GLOBAL_MODULES}/lib/node_modules/${module}/node_modules/${scope}/${packageName}`;
      linkPath = `${nodeModulesRootPath}/${scope}/${packageName}`;
    } else {
      linkTarget = `../${GLOBAL_MODULES}/lib/node_modules/${module}/node_modules/${packageName}`;
      linkPath = `${nodeModulesRootPath}/${packageName}`;
    }
    if (!fs.existsSync(linkPath)) {
      fs.symlinkSync(linkTarget, linkPath);
    }
  });
}
