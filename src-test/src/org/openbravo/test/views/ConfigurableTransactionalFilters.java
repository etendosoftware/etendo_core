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

package org.openbravo.test.views;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.GCSystem;
import org.openbravo.client.application.window.StandardWindowComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Windows;
import org.openbravo.test.base.mock.HttpServletRequestMock;

/**
 * Test case intended to check the grid configuration that allows to enable/disable the
 * transactional filters.
 */
public class ConfigurableTransactionalFilters extends ViewGenerationTest {

  private static final String FILTER_NAME = "This grid is filtered using a transactional filter <i\\>(only draft & modified documents in the last 1 day(s))</i\\>.";

  @Before
  public void shouldExecuteOnlyIfThereIsNoGridConfigAtSystemLevel() {
    assumeThat("Exists grid config at system level", existsSystemGridConfig(),
        equalTo(Boolean.FALSE));
  }

  private boolean existsSystemGridConfig() {
    OBContext.setAdminMode(false);
    try {
      return StandardWindowComponent.getSystemGridConfig().isPresent();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * The transactional filters are enabled by default.
   */
  @Test
  public void transactionalFiltersAreEnabledByDefault() throws Exception {
    try {
      String viewDef = getTransactionalWindowView();
      assertThat("Transactional filters are enabled by default", viewDef,
          containsString(FILTER_NAME));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Having a grid configuration at System level that enables the transactional filters, the
   * transactional filter definition must be found within the view definition of the window.
   */
  @Test
  public void enableTransactionalFilters() throws Exception {
    try {
      setGridConfiguration(true);
      HttpServletRequestMock.setRequestMockInRequestContext();
      String viewDef = getTransactionalWindowView();
      assertThat("Transactional filters are disabled", viewDef, containsString(FILTER_NAME));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * Having a grid configuration at System level that disables the transactional filters, the
   * transactional filter definition must not be found within the view definition of the window.
   */
  @Test
  public void disableTransactionalFilters() throws Exception {
    try {
      setGridConfiguration(false);
      HttpServletRequestMock.setRequestMockInRequestContext();
      String viewDef = getTransactionalWindowView();
      assertThat("Transactional filters are disabled", viewDef, not(containsString(FILTER_NAME)));
    } finally {
      OBDal.getInstance().rollbackAndClose();
    }
  }

  private void setGridConfiguration(boolean allowTransactionalFilters) {
    OBContext.setAdminMode(false);
    try {
      GCSystem gcsystem = OBProvider.getInstance().get(GCSystem.class);
      gcsystem.setClient(OBDal.getInstance().get(Client.class, Clients.SYSTEM));
      gcsystem.setOrganization(OBDal.getInstance().get(Organization.class, Orgs.MAIN));
      gcsystem.setAllowTransactionalFilters(allowTransactionalFilters);
      OBDal.getInstance().save(gcsystem);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getTransactionalWindowView() {
    return generateView(Windows.SALES_ORDER);
  }

}
