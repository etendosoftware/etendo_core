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
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* global global */

global.OB = {
  I18N: {
    getLabel: jest.fn()
  }
};
require('../web/org.openbravo.client.application/js/utilities/ob-utilities-number');
require('../../org.openbravo.client.kernel/web/org.openbravo.client.kernel/js/BigDecimal-all-1.0.3');

describe('OB.Utilities.Number.*', () => {
  const decimalSeparator = '.';
  const groupSeparator = ',';

  describe('OB.Utilities.Number.ScientificToDecimal', () => {
    it.each`
      number              | expected
      ${'-5E-4'}          | ${'-0.0005'}
      ${'-5E4'}           | ${'-50000'}
      ${'5E7'}            | ${'50000000'}
      ${'-1.056E-5'}      | ${'-0.00001056'}
      ${'-1.056E5'}       | ${'-105600'}
      ${'1.056E-5'}       | ${'0.00001056'}
      ${'1.056E5'}        | ${'105600'}
      ${'-1.2E-5'}        | ${'-0.000012'}
      ${'-1.2E5'}         | ${'-120000'}
      ${'1.2E-5'}         | ${'0.000012'}
      ${'1.2E5'}          | ${'120000'}
      ${'-3.566E-4'}      | ${'-0.0003566'}
      ${'1.020050045E7'}  | ${'10200500.45'}
      ${'-1.020050045E7'} | ${'-10200500.45'}
      ${'1.020050000E7'}  | ${'10200500.00'}
      ${'-1.020050000E7'} | ${'-10200500.00'}
    `('should convert $number to $expected', ({ number, expected }) => {
      expect(
        OB.Utilities.Number.ScientificToDecimal(number, decimalSeparator)
      ).toEqual(expected);
    });
  });

  describe('OB.Utilities.Number.OBMaskedToOBPlain works properly when the exponent of the scientific number ends with 0', () => {
    it.each`
      number          | expected
      ${'1.12E10'}    | ${'11200000000'}
      ${'1.12E-10'}   | ${'0.000000000112'}
      ${'-1.0564E10'} | ${'-10564000000'}
      ${'1.056E-5'}   | ${'0.00001056'}
    `('should convert $number to $expected', ({ number, expected }) => {
      expect(
        OB.Utilities.Number.OBMaskedToOBPlain(
          number,
          decimalSeparator,
          groupSeparator
        )
      ).toEqual(expected);
    });
  });

  describe('OB.Utilities.Number.ScientificToDecimal works properly for the conversion of the numbers from scientific notation to decimal notation in the case of the upper case E, lower case e and if a sign is added to the exponent', () => {
    it.each`
      number          | expected
      ${'1.12E10'}    | ${'11200000000'}
      ${'-5E20'}      | ${'-500000000000000000000'}
      ${'-1.056E10'}  | ${'-10560000000'}
      ${'1.12E+10'}   | ${'11200000000'}
      ${'-5E+20'}     | ${'-500000000000000000000'}
      ${'-1.056E+10'} | ${'-10560000000'}
      ${'1.12e10'}    | ${'11200000000'}
      ${'-5e20'}      | ${'-500000000000000000000'}
      ${'-1.056e10'}  | ${'-10560000000'}
      ${'1.12e+10'}   | ${'11200000000'}
      ${'-5e+20'}     | ${'-500000000000000000000'}
      ${'-1.056e+10'} | ${'-10560000000'}
    `('should convert $number to $expected', ({ number, expected }) => {
      expect(
        OB.Utilities.Number.ScientificToDecimal(number, decimalSeparator)
      ).toEqual(expected);
    });
  });

  describe('OB.Utilities.Number.ScientificToDecimal works properly with numbers with leading zeros', () => {
    it.each`
      numberWithZero | number
      ${'03.4e-2'}   | ${'3.4e-2'}
      ${'03.4e+2'}   | ${'3.4e+2'}
      ${'03.4e2'}    | ${'3.4e2'}
      ${'03.4E-2'}   | ${'3.4E-2'}
      ${'03.4E+2'}   | ${'3.4E+2'}
      ${'03.4E2'}    | ${'3.4E2'}
    `(
      'should return the same value for $numberWithZero and $number',
      ({ numberWithZero, number }) => {
        const decimalFromNumberWithLeadingZero = OB.Utilities.Number.ScientificToDecimal(
          numberWithZero,
          decimalSeparator
        );

        const decimalFromNumberWithNoLeadingZero = OB.Utilities.Number.ScientificToDecimal(
          number,
          decimalSeparator
        );

        expect(decimalFromNumberWithLeadingZero).toEqual(
          decimalFromNumberWithNoLeadingZero
        );
      }
    );
  });

  describe('OB.Utilities.Number.roundJSNumber', () => {
    it.each`
      number                | decimals | expected
      ${0.145}              | ${2}     | ${0.15}
      ${1.145}              | ${2}     | ${1.15}
      ${10.145}             | ${2}     | ${10.15}
      ${14.499999999999998} | ${2}     | ${14.5}
    `(
      'should round $number with $decimals decimals to $expected',
      ({ number, decimals, expected }) => {
        expect(OB.Utilities.Number.roundJSNumber(number, decimals)).toEqual(
          expected
        );
      }
    );
  });

  describe('OB.Utilities.Number.roundJSNumber works properly when the parameter is NaN', () => {
    it.each`
      number     | decimals | expected
      ${'a'}     | ${2}     | ${NaN}
      ${'12a'}   | ${2}     | ${NaN}
      ${'1.1.1'} | ${2}     | ${NaN}
      ${'1..55'} | ${2}     | ${NaN}
      ${'1,,15'} | ${2}     | ${NaN}
    `(
      "should return $expected when rounding '$number'",
      ({ number, decimals, expected }) => {
        expect(OB.Utilities.Number.roundJSNumber(number, decimals)).toEqual(
          expected
        );
      }
    );
  });
});
