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
 * All portions are Copyright (C) 2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.test.conversionratedoc;

import java.math.BigDecimal;

import javax.persistence.PersistenceException;

import org.jboss.arquillian.junit.InSequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.test.base.OBBaseTest;

/**
 * It centralizes the logic for C_CONVERSION_RATE_DOCUMENT.C_CONVERSIONRATEDOC_UN unique index
 * constraint.
 * 
 * It tries to create two similar conversion rates with the same currencies, and verifies it fails.
 * It also creates two conversion rates with different currencies and verifies it allows it.
 */
public abstract class ConversionRateDocUniqueTest extends OBBaseTest {

  protected abstract String getDocumentEntityName();

  protected abstract String getPropertyName();

  protected abstract String getDocumentId();

  @Before
  public void unpostDocuments() {
    setPosted("N");
    OBDal.getInstance().flush();
  }

  @After
  public void postDocuments() {
    OBDal.getInstance().rollbackAndClose();
  }

  protected void setPosted(final String isPosted) {
    OBDal.getInstance().getProxy(getDocumentEntityName(), getDocumentId()).set("posted", isPosted);
  }

  private void createDummyConversionRate(final String docPropertyName, final String docId,
      final String currencyId, final String currencyToId, final BigDecimal rate) {
    final ConversionRateDoc cr = OBProvider.getInstance().get(ConversionRateDoc.class);
    cr.set(docPropertyName, OBDal.getInstance().getProxy(getDocumentEntityName(), getDocumentId()));
    cr.setCurrency(OBDal.getInstance().getProxy(Currency.class, currencyId));
    cr.setToCurrency(OBDal.getInstance().getProxy(Currency.class, currencyToId));
    cr.setRate(rate);
    OBDal.getInstance().save(cr);
  }

  @Test
  @InSequence(1)
  public void testTwoConversionsForDifferentCurrenciesAndTheSameDocIsAllowed() {
    createDummyConversionRate(getPropertyName(), getDocumentId(), "100", "103",
        new BigDecimal("1.25"));
    createDummyConversionRate(getPropertyName(), getDocumentId(), "100", "104",
        new BigDecimal("1.50"));
    OBDal.getInstance().flush();
  }

  @Test(expected = PersistenceException.class)
  @InSequence(2)
  public void testTwoConversionsForTheSameCurrenciesAndDocIsNotAllowed() {
    createDummyConversionRate(getPropertyName(), getDocumentId(), "100", "102",
        new BigDecimal("1.25"));
    createDummyConversionRate(getPropertyName(), getDocumentId(), "100", "102",
        new BigDecimal("1.50"));
    OBDal.getInstance().flush();
  }
}
