package com.smf.securewebservices.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.weld.WeldUtils;
import org.openbravo.service.web.WebService;

public class KernelServlet implements WebService {

  /**
   * Calls have to be like this in order to OB Kernel class to not fail because of the uri not having its servlet path:
   * http://localhost:8080/openbravo/sws/com.smf.securewebservices.kernel/org.openbravo.client.kernel?processId=ABCDE&_action=com.example.someClass
   */
  @Override
  public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    // TODO: deprecate this call and make special services to call processes, etc.

    WeldUtils.getInstanceFromStaticBeanManager(org.openbravo.client.kernel.KernelServlet.class).doGet(request, response);

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
