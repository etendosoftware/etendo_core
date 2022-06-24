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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* global global */

global.OB = {};
require('../web/org.openbravo.client.application/js/utilities/ob-utilities');

describe('OB.Utilities.isValidUrl', () => {
  it('should consider http://localhost as not a valid URL', () => {
    expect(OB.Utilities.isValidURL('http://localhost')).toBeFalsy();
  });

  it('should consider http://localhost.localdomain as a valid URL', () => {
    expect(
      OB.Utilities.isValidURL('http://localhost.localdomain')
    ).toBeTruthy();
  });
});
