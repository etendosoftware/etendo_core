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

package org.openbravo.client.application.test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.WindowSettingsActionHandler;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.client.kernel.ComponentGenerator;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.TestConstants.Tabs;
import org.openbravo.test.base.TestConstants.Windows;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/** Additional test cases for {@link ApplicationDictionaryCachedStructures} */
public class ADCSTest extends WeldBaseTest {
  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Inject
  private StandardWindowComponent component;

  @Before
  public void doChecks() {
    assumeTrue("Cache can be used (no modules in development)", adcs.useCache());
    setSystemAdministratorContext();
  }

  @Test
  @Issue("40633")
  public void tabWithProductCharacteristicsIsGeneratedAfterADCSInitialization() {
    // given ADCS initialized with only Discounts and Promotions window
    adcs.init();
    Window w = adcs.getWindow(Windows.DISCOUNTS_AND_PROMOTIONS);

    // when Discounts and Promotions view is requested in a different DAL session
    OBDal.getInstance().commitAndClose();
    component.setWindow(w);
    String generatedView = ComponentGenerator.getInstance().generate(component);

    // then the view gets generated without throwing exceptions
    assertThat(generatedView, not(isEmptyString()));
  }

  @Test
  @Issue("41338")
  public void tabsSharingTableAreCorrectlyInitialized() {
    // given ADCS initialized with only Sales Invoice header tab (uses c_order)
    adcs.init();

    adcs.getTab(Tabs.SALES_INVOICE_HEADER);
    OBDal.getInstance().commitAndClose();

    // when Purchase Invoice header (it also uses c_order) is taken from ADCS
    adcs.getTab(Tabs.PURCHASE_INVOICE_HEADER);
    OBDal.getInstance().commitAndClose();

    // then Purchase Invoice header is fully initialized even if taken in a different session
    Tab t = adcs.getTab(Tabs.PURCHASE_INVOICE_HEADER);
    assertThat(t.getTable().getADColumnList().size(), greaterThan(1));
  }

  @Test
  @Issue("41338")
  public void wsahDoesNotLeaveAdcsInInvalidState() {
    // given a clean ADCS
    adcs.init();

    // when first action using it is to execute WSAH for sales and purchase invoice windows
    WindowSettingsActionHandlerTest wsa = WeldUtils
        .getInstanceFromStaticBeanManager(WindowSettingsActionHandlerTest.class);

    wsa.execute(Windows.SALES_INVOICE);
    wsa.execute(Windows.PURCHASE_INVOICE);

    // then it should be possible to generate Purchase Invoice window in a different session
    Window w = adcs.getWindow(Windows.PURCHASE_INVOICE);
    OBDal.getInstance().commitAndClose();

    component.setWindow(w);
    HttpServletRequestMock.setRequestMockInRequestContext();
    String generatedView = ComponentGenerator.getInstance().generate(component);
    assertThat(generatedView, not(isEmptyString()));
  }

  @Test
  @Issue("41892")
  public void maskedStringsShouldntBreakADCS() {
    // given a clean ADCS initialized with any window
    adcs.init();
    adcs.getWindow(Windows.PURCHASE_INVOICE);
    OBDal.getInstance().commitAndClose();

    // when any column from any tab in that window is obtained by another session
    Reference anyColumnReference = adcs.getWindow(Windows.PURCHASE_INVOICE)
        .getADTabList()
        .get(0)
        .getADFieldList()
        .get(0)
        .getColumn()
        .getReference();

    // then its mask list must be accessible
    assertThat(anyColumnReference.getOBCLKERREFMASKList().size(), greaterThanOrEqualTo(0));
  }

  private static class WindowSettingsActionHandlerTest extends WindowSettingsActionHandler {

    public void execute(String windowId) {
      Map<String, Object> parameters = new HashMap<>();
      parameters.put("windowId", windowId);
      execute(parameters, "");
    }
  }

}
