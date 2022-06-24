/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at https://www.openbravo.com/legal/license.html
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

/* global global */

require('@testing-library/jest-dom/extend-expect');

// Check if a console.error or console.warn has been thrown during the test and make it fail
// this is necessary, due to jest not failing a test automatically when a console.error/warn appears
let consoleHasErrorOrWarning = false;
const { error, warn } = console;

global.console.error = (...args) => {
  consoleHasErrorOrWarning = true;
  error(...args);
};
global.console.warn = (...args) => {
  consoleHasErrorOrWarning = true;
  warn(...args);
};

beforeEach(() => {
  if (consoleHasErrorOrWarning) {
    consoleHasErrorOrWarning = false;
  }
});

afterEach(() => {
  if (consoleHasErrorOrWarning) {
    throw new Error('console.error and console.warn are not allowed');
  }
});
