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
 * All portions are Copyright (C) 2009-2011 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.BaseKernelServlet.KernelHttpServletResponse;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.portal.PortalAccessible;

/**
 * Is responsible for logging out from the application.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class LogOutActionHandler extends BaseActionHandler implements PortalAccessible {

  private static final Logger log = Logger.getLogger(LogOutActionHandler.class);

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.openbravo.client.kernel.BaseActionHandler#execute(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void execute() {
    final HttpServletResponse response = RequestContext.get().getResponse();
    if (response instanceof KernelHttpServletResponse) {
      final KernelHttpServletResponse kernelResponse = (KernelHttpServletResponse) response;
      // The callback of this handler performs a redirect to the base url, letting to the
      // AuthenticationManager the final logout actions.
      kernelResponse.setDoLogout(true);
    }
    final HttpServletRequest request =  RequestContext.get().getRequest();
    try {
      sendAuth0LogoutRequest(request, response);
    } catch (IOException e) {
      log.error("Error while logging out", e);
      throw new OBException(e);
    }
  }

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    throw new UnsupportedOperationException();
  }

  private void sendAuth0LogoutRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    if (request.getSession() != null) {
      request.getSession().invalidate();
    }
    final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String domain = openbravoProperties.getProperty("sso.domain.url");
    String clientId = openbravoProperties.getProperty("sso.client.id");

    String returnUrl = String.format("%s://%s", request.getScheme(), request.getServerName());
    if ((request.getScheme().equals("http") && request.getServerPort() != 80) ||
        (request.getScheme().equals("https") && request.getServerPort() != 443)) {
      returnUrl += ":" + request.getServerPort();
    }
    returnUrl += "/google16";

    // Build logout URL like:
    // https://{YOUR-DOMAIN}/v2/logout?client_id={YOUR-CLIENT-ID}&returnTo=http://localhost:3000/login
    String logoutUrl = String.format(
        "https://%s/v2/logout?client_id=%s&returnTo=%s",
        domain,
        clientId,
        returnUrl
    );
    response.sendRedirect(logoutUrl);
  }
}
