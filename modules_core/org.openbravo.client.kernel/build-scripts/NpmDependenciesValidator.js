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
 ************************************************************************
 */

/* eslint-disable no-console */

const path = require('path');
const fs = require('fs');

const WEB_JSPACK = 'web-jspack';
const PACKAGE_JSON = 'package.json';

/**
 * Class whose validate function will check if there are conflicts in the npm dependencies defined in
 * the Openbravo modules. It returns an object with two properties: warnings and errors.
 *
 * If two modules include the same npm dependency, same version, a warning message will be added
 * to the reponse.
 *
 * If two modules include the same npm dependency, but with different version, an error message will
 * be added to the reponse.
 *
 */
class NpmDependenciesValidator {
  constructor() {
    this.allDependencies = {};
    this.warnings = [];
    this.errors = [];
  }

  validate() {
    console.log('Validating npm dependencies included in Openbravo modules...');
    const modulesDir = path.resolve('modules');
    this.getModules(modulesDir)
      .map(module => this.getPackageJsonPath(module))
      .filter(path => Boolean(path))
      .forEach(path => {
        this.validateDependencies(path);
      });
    return { warnings: this.warnings, errors: this.errors };
  }

  validateDependencies(path) {
    const packageJsonContent = this.readPackageJson(path);
    const relativePath = this.toRelativePath(path);
    const dependencies = {
      ...packageJsonContent.dependencies,
      ...packageJsonContent.devDependencies
    };
    Object.entries(dependencies).forEach(([depName, version]) => {
      if (this.allDependencies[depName]) {
        Object.keys(this.allDependencies[depName]).forEach(otherPath => {
          const otherVersion = this.allDependencies[depName][otherPath];
          if (version === otherVersion) {
            // eslint-disable-next-line no-console
            this.warnings.push(
              `Warning: Package ${depName} defined in ${relativePath} but already defined in ${otherPath} with same version ${version}`
            );
          } else {
            this.errors.push(
              `Error: Package ${depName} defined in ${relativePath} with version ${version} but also defined in ${otherPath} with version ${otherVersion}`
            );
          }
        });
      }
      this.allDependencies[depName] = {
        ...this.allDependencies[depName],
        [relativePath]: version
      };
    });
  }

  readPackageJson(path) {
    return JSON.parse(fs.readFileSync(path));
  }

  getModules(modulesDir) {
    return fs.readdirSync(modulesDir);
  }

  getPackageJsonPath(module) {
    const modulesDir = path.resolve('modules');
    const supportedPaths = [
      path.resolve(modulesDir, module, PACKAGE_JSON),
      path.resolve(modulesDir, module, WEB_JSPACK, module, PACKAGE_JSON)
    ];
    return supportedPaths.find(path => fs.existsSync(path));
  }

  toRelativePath(toPath) {
    return path.relative('', toPath);
  }
}
// eslint-disable-next-line no-undef
module.exports = NpmDependenciesValidator;
