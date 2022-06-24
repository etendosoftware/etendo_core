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
require('../web/org.openbravo.client.application/js/utilities/ob-utilities-date');

describe('OB.Utilities.Date.*', () => {
  describe('OB.Utilities.Date.normalizeDisplayFormat', () => {
    it.each`
      format                   | expected
      ${'DD-MM-YYYY'}          | ${'%d-%m-%Y'}
      ${'DD-MM-YY'}            | ${'%d-%m-%y'}
      ${'dd-mm-yyyy'}          | ${'%d-%m-%Y'}
      ${'dd-mm-yy'}            | ${'%d-%m-%y'}
      ${'%D-%M-%Y'}            | ${'%d-%m-%Y'}
      ${'%D-%M-%y'}            | ${'%d-%m-%y'}
      ${'%d-%m-%Y'}            | ${'%d-%m-%Y'}
      ${'%d-%m-%y'}            | ${'%d-%m-%y'}
      ${'%d-%m-%Y hh:mi:ss'}   | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y HH:MI:SS'}   | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y HH24:mi:ss'} | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y HH24:MI:SS'} | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y HH:MM:SS'}   | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y HH24:MM:SS'} | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y %H:%M:%S'}   | ${'%d-%m-%Y %H:%M:%S'}
      ${'%d-%m-%Y %H.%M.%S'}   | ${'%d-%m-%Y %H.%M.%S'}
      ${'%d-%m-%Y %H:%M:%S a'} | ${'%d-%m-%Y %H:%M:%S A'}
      ${'%d-%m-%Y %H.%M.%S a'} | ${'%d-%m-%Y %H.%M.%S A'}
    `(
      "Display format '$format' is normalized as '$expected'",
      ({ format, expected }) => {
        expect(OB.Utilities.Date.normalizeDisplayFormat(format)).toEqual(
          expected
        );
      }
    );
  });

  describe('OB.Utilities.Date.OBToJS', () => {
    it.each`
      date                        | format                   | expected
      ${'31/12/2010'}             | ${'%d/%m/%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'31:12:2010'}             | ${'%d:%m:%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'31.12.2010'}             | ${'%d.%m.%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'31-12-2010'}             | ${'%d-%m-%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'12-31-2010'}             | ${'%m-%d-%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'2010-31-12'}             | ${'%Y-%d-%m'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'2010-12-31'}             | ${'%Y-%m-%d'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'01-01-0001'}             | ${'%d-%m-%Y'}            | ${createDateObjectOfFirstDayOfYear1()}
      ${'31-12-10'}               | ${'%d-%m-%y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'12-31-10'}               | ${'%m-%d-%y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'10-31-12'}               | ${'%y-%d-%m'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'10-12-31'}               | ${'%y-%m-%d'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'01-01-00'}               | ${'%d-%m-%y'}            | ${new Date(2000, 0, 1, 0, 0, 0, 0)}
      ${'01-01-01'}               | ${'%d-%m-%y'}            | ${new Date(2001, 0, 1, 0, 0, 0, 0)}
      ${'01-01-49'}               | ${'%d-%m-%y'}            | ${new Date(2049, 0, 1, 0, 0, 0, 0)}
      ${'01-01-50'}               | ${'%d-%m-%y'}            | ${new Date(1950, 0, 1, 0, 0, 0, 0)}
      ${'01-01-99'}               | ${'%d-%m-%y'}            | ${new Date(1999, 0, 1, 0, 0, 0, 0)}
      ${'31-12-2010'}             | ${'%d-%m-%Y'}            | ${new Date(2010, 11, 31, 0, 0, 0, 0)}
      ${'31-12-2010 23:59'}       | ${'%d-%m-%Y %H:%M'}      | ${new Date(2010, 11, 31, 23, 59, 0, 0)}
      ${'31-12-2010 23:59:58'}    | ${'%d-%m-%Y %H:%M:%S'}   | ${new Date(2010, 11, 31, 23, 59, 58, 0)}
      ${'31-12-2010 12:59:58'}    | ${'%d-%m-%Y %H:%M:%S'}   | ${new Date(2010, 11, 31, 12, 59, 58, 0)}
      ${'31-12-2010 00:59:58'}    | ${'%d-%m-%Y %H:%M:%S'}   | ${new Date(2010, 11, 31, 0, 59, 58, 0)}
      ${'31-12-2010 12:59:58 PM'} | ${'%d-%m-%Y %H:%M:%S a'} | ${new Date(2010, 11, 31, 12, 59, 58, 0)}
      ${'31-12-2010 12:59:58 AM'} | ${'%d-%m-%Y %H:%M:%S a'} | ${new Date(2010, 11, 31, 0, 59, 58, 0)}
    `(
      "Date '$date' is converted to Date object with value '$expected' using format '$format'",
      ({ date, format, expected }) => {
        expect(OB.Utilities.Date.OBToJS(date, format)).toEqual(expected);
      }
    );
  });

  describe('OB.Utilities.Date.JSToOB', () => {
    it.each`
      date                                     | format                   | expected
      ${'BadDefinedJSDate'}                    | ${'%d-%m-%Y'}            | ${null}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d/%m/%Y'}            | ${'31/12/2010'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d:%m:%Y'}            | ${'31:12:2010'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d.%m.%Y'}            | ${'31.12.2010'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d-%m-%Y'}            | ${'31-12-2010'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%m-%d-%Y'}            | ${'12-31-2010'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%Y-%d-%m'}            | ${'2010-31-12'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%Y-%m-%d'}            | ${'2010-12-31'}
      ${createDateObjectOfFirstDayOfYear1()}   | ${'%d-%m-%Y'}            | ${'01-01-0001'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d-%m-%y'}            | ${'31-12-10'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%m-%d-%y'}            | ${'12-31-10'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%y-%d-%m'}            | ${'10-31-12'}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%y-%m-%d'}            | ${'10-12-31'}
      ${new Date(2000, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${'01-01-00'}
      ${new Date(2001, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${'01-01-01'}
      ${new Date(2049, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${'01-01-49'}
      ${new Date(1950, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${'01-01-50'}
      ${new Date(1999, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${'01-01-99'}
      ${new Date(1949, 0, 1, 0, 0, 0, 0)}      | ${'%d-%m-%y'}            | ${null}
      ${new Date(2010, 11, 31, 0, 0, 0, 0)}    | ${'%d-%m-%Y'}            | ${'31-12-2010'}
      ${new Date(2010, 11, 31, 23, 59, 0, 0)}  | ${'%d-%m-%Y %H:%M'}      | ${'31-12-2010 23:59'}
      ${new Date(2010, 11, 31, 23, 59, 58, 0)} | ${'%d-%m-%Y %H:%M:%S'}   | ${'31-12-2010 23:59:58'}
      ${new Date(2010, 11, 31, 12, 59, 58, 0)} | ${'%d-%m-%Y %H:%M:%S'}   | ${'31-12-2010 12:59:58'}
      ${new Date(2010, 11, 31, 0, 59, 58, 0)}  | ${'%d-%m-%Y %H:%M:%S'}   | ${'31-12-2010 00:59:58'}
      ${new Date(2010, 11, 31, 12, 59, 58, 0)} | ${'%d-%m-%Y %H:%M:%S a'} | ${'31-12-2010 12:59:58 PM'}
      ${new Date(2010, 11, 31, 0, 59, 58, 0)}  | ${'%d-%m-%Y %H:%M:%S a'} | ${'31-12-2010 12:59:58 AM'}
    `(
      "Date with value '$date' is converted to '$expected' using format '$format'",
      ({ date, format, expected }) => {
        expect(OB.Utilities.Date.JSToOB(date, format)).toEqual(expected);
      }
    );
  });

  function createDateObjectOfFirstDayOfYear1() {
    let date = new Date(1, 0, 1, 0, 0, 0, 0);
    date.setFullYear('1');
    return date;
  }
});
