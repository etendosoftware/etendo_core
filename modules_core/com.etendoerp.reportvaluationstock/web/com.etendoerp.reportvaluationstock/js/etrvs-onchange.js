OB.ETRVS = {};
OB.ETRVS.OnChange = {};

OB.ETRVS.OnChange.organizationOnChange = function(item, view, form) {
  var organization = form.getItem('AD_Org_ID')
      ? form.getItem('AD_Org_ID').getValue()
      : '', callback;

  callback = function(response, data, request) {
    form.getItem('C_Currency_ID').setValue(data.currency);
    form.getItem('C_Currency_ID').valueMap[data.currency] = data.currencyIdIdentifier;

    form.redraw();
  };

  if (organization !== '') {
    OB.RemoteCallManager.call(
      'org.openbravo.advpaymentmngt.actionHandler.AddPaymentOrganizationActionHandler',
      {
        organization: organization
      },
      {},
      callback
    );
  }

};
