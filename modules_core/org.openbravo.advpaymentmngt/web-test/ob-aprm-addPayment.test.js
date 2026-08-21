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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

/* global global */

global.OB = { APRM: {} };
require('../../org.openbravo.client.kernel/web/org.openbravo.client.kernel/js/BigDecimal-all-1.0.3');
require('../web/org.openbravo.advpaymentmngt/js/ob-aprm-addPayment');

const EUR = 'EUR_ID';
const USD = 'USD_ID';

const createItem = value => {
  let current = value;
  return {
    getValue: () => current,
    setValue: newValue => {
      current = newValue;
    },
  };
};

// Minimal stand-in for the Order/Invoice grid of the Add Payment popup, holding a
// single pending document with the given outstanding amount.
const createOrderInvoiceGrid = outstandingAmount => {
  const record = { id: 'invoice1' };
  return {
    selectedIds: [record.id],
    data: { localData: { find: () => record } },
    getRecordIndex: () => 0,
    getFieldByColumnName: columnName => columnName,
    getEditedCell: () => outstandingAmount,
  };
};

// Builds a form equivalent to a saved Payment In whose Add Details popup has just
// auto-selected a pending document.
const createForm = ({ actualPayment, outstandingAmount, currencyToId }) => {
  const items = {
    order_invoice: {
      canvas: { viewGrid: createOrderInvoiceGrid(outstandingAmount) },
    },
    issotrx: createItem(true),
    actual_payment: createItem(actualPayment),
    expected_payment: createItem(0),
    generateCredit: createItem(0),
    amount_gl_items: createItem(0),
    used_credit: createItem(0),
    bslamount: createItem(0),
    conversion_rate: createItem(1.19),
    StdPrecision: createItem(2),
    c_currency_id: createItem(EUR),
    c_currency_to_id: createItem(currencyToId),
  };
  return {
    // the multi-currency block became visible when the popup was opened
    _mcPendingSyncMC: true,
    getItem: name => items[name],
    redraw: () => {},
  };
};

describe('OB.APRM.AddPayment.updateActualExpected', () => {
  beforeEach(() => {
    // these only recalculate dependent fields and pull in the whole popup
    jest
      .spyOn(OB.APRM.AddPayment, 'distributeAmount')
      .mockImplementation(() => {});
    jest
      .spyOn(OB.APRM.AddPayment, 'updateDifference')
      .mockImplementation(() => {});
    jest
      .spyOn(OB.APRM.AddPayment, 'updateConvertedAmount')
      .mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('keeps the amount entered in the document header on multi-currency payments', () => {
    const form = createForm({
      actualPayment: 3332,
      outstandingAmount: '2140.16',
      currencyToId: USD,
    });

    OB.APRM.AddPayment.updateActualExpected(form);

    expect(form.getItem('actual_payment').getValue()).toEqual(3332);
    expect(form.getItem('expected_payment').getValue()).toEqual(2140.16);
  });

  it('inherits the expected payment when no amount was entered at all', () => {
    const form = createForm({
      actualPayment: 0,
      outstandingAmount: '2140.16',
      currencyToId: USD,
    });

    OB.APRM.AddPayment.updateActualExpected(form);

    expect(form.getItem('actual_payment').getValue()).toEqual(2140.16);
  });

  it('does not touch the actual payment when the currencies match', () => {
    const form = createForm({
      actualPayment: 0,
      outstandingAmount: '2140.16',
      currencyToId: EUR,
    });

    OB.APRM.AddPayment.updateActualExpected(form);

    expect(form.getItem('actual_payment').getValue()).toEqual(0);
  });
});
