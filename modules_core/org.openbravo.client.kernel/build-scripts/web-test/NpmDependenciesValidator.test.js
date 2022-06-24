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

const NpmDependenciesValidator = require('../NpmDependenciesValidator');

describe('NpmDependenciesValidator', () => {
  let validator;

  beforeEach(() => {
    validator = new NpmDependenciesValidator();
    validator.getModules = jest.fn(() => {
      return ['module1', 'module2'];
    });

    validator.getPackageJsonPath = jest.fn(module => {
      return `${module}/package.json`;
    });
  });

  it('Does not return warnings or errors if dependency is not included in several modules', async () => {
    validator.readPackageJson = jest.fn(path => {
      if (path === 'module1/package.json') {
        return {
          dependencies: { lodash: '4.17.15' }
        };
      } else if (path === 'module2/package.json') {
        return {
          dependencies: { jest: '26.6.0' }
        };
      } else {
        return {};
      }
    });

    const { warnings, errors } = validator.validate();

    expect(warnings).toHaveLength(0);
    expect(errors).toHaveLength(0);
  });

  it('Returns warning message if dependency is already defined in other module with same version', async () => {
    validator.readPackageJson = jest.fn(() => {
      return {
        dependencies: { lodash: '4.17.15' }
      };
    });

    const { warnings, errors } = validator.validate();

    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toBe(
      'Warning: Package lodash defined in module2/package.json but already defined in module1/package.json with same version 4.17.15'
    );
    expect(errors).toHaveLength(0);
  });

  it('Returns warning message even if one module includes a package as a dependency and another one in as a devDependency', async () => {
    validator.readPackageJson = jest.fn(path => {
      if (path === 'module1/package.json') {
        return {
          dependencies: { lodash: '4.17.15' }
        };
      } else if (path === 'module2/package.json') {
        return {
          devDependencies: { lodash: '4.17.15' }
        };
      } else {
        return {};
      }
    });

    const { warnings, errors } = validator.validate();

    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toBe(
      'Warning: Package lodash defined in module2/package.json but already defined in module1/package.json with same version 4.17.15'
    );
    expect(errors).toHaveLength(0);
  });

  it('Returns error if dependency is already defined in other module with different version', async () => {
    validator.readPackageJson = jest.fn(path => {
      const version = path === 'module1/package.json' ? '4.17.15' : '4.17.16';
      return {
        dependencies: { lodash: version }
      };
    });

    const { warnings, errors } = validator.validate();

    expect(warnings).toHaveLength(0);
    expect(errors).toHaveLength(1);
    expect(errors[0]).toBe(
      'Error: Package lodash defined in module2/package.json with version 4.17.16 but also defined in module1/package.json with version 4.17.15'
    );
  });
});
