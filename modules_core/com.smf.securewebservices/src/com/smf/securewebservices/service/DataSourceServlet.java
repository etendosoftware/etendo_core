package com.smf.securewebservices.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.secureApp.LoginUtils.RoleDefaults;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

public class DataSourceServlet implements WebService {

  /**
   * Calls have to be like this in order to OB DataSourceServlet class to not fail because of the uri not having its servlet path:
   * http://localhost:8080/openbravo/sws/com.smf.securewebservices.datasource/org.openbravo.service.datasource/DATASOURCENAME
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    
    try {
      OBContext.setAdminMode();
      DalConnectionProvider conn = new DalConnectionProvider(); 
      RoleDefaults defaults = LoginUtils.getLoginDefaults(OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          conn);
      
      LoginUtils.fillSessionArguments(conn, new VariablesSecureApp(request), OBContext.getOBContext().getUser().getId(), 
          OBContext.getOBContext().getLanguage().getLanguage(), OBContext.getOBContext().isRTL() ? "Y" : "N", 
              defaults.role, defaults.client, 
              OBContext.getOBContext().getCurrentOrganization().getId(), 
              defaults.warehouse);
    } finally {
      OBContext.restorePreviousMode();
    }

    // TODO: deprecate this call and make special services with needed calls and responses

    WeldUtils.getInstanceFromStaticBeanManager(org.openbravo.service.datasource.DataSourceServlet.class).doGet(request, response);

  }

  @Override
  public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    doGet(path, request, response);
  }

  @Override
  public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

  @Override
  public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
  }

}
