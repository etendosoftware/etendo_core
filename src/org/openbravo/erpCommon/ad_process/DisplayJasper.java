package org.openbravo.erpCommon.ad_process;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;

public class DisplayJasper extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    advisePopUp(request, response, "SUCCESS", "Jasper Report", "Displaying Jasper Report");
  }

}
