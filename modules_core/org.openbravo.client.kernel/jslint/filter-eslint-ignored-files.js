/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/*
 * This script reads a list of files in the input and filter those that are ignored
 * by the cwd's .eslintignore file if exists. If not, the whole list of files will be passed through.
 *
 * This is required as ESLint decided not to include a flag to hide "ignored file" warnings, although
 * it is possible through their CLIEngine. For more details, see: https://github.com/eslint/eslint/issues/9977
 **/

/*global process*/
var CLIEngine = require('eslint').CLIEngine,
  fs = require('fs'),
  ignoreFile = process.cwd() + '/.eslintignore',
  pathFilterFunction,
  result = [];

if (fs.existsSync(ignoreFile)) {
  var cli = new CLIEngine({ ignorePath: ignoreFile });
  pathFilterFunction = function(file) {
    if (!cli.isPathIgnored(file)) {
      result.push(file);
    }
  };
} else {
  pathFilterFunction = function(file) {
    process.stdout.write(file + ' ');
  };
}

/*
 Apply the pathFilterFunction after removing the two first values of the argv array: "node" and the path to this script.
 */
process.argv.slice(2).forEach(pathFilterFunction);
process.stdout.write(result.join(' '));
