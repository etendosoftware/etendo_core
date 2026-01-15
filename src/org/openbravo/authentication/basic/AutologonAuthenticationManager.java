/*
 ************************************************************************************
 * Copyright (C) 2001-2018 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.authentication.basic;

import java.io.IOException;

import org.openbravo.authentication.AuthenticationData;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.HttpBaseUtils;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * @author adrianromero
 * @author iperdomo
 */
@Dependent
public class AutologonAuthenticationManager extends AuthenticationManager {

  private String m_sAutologonUsername;
  private String m_sUserId = null;

  public AutologonAuthenticationManager() {
  }

  public AutologonAuthenticationManager(HttpServlet s) throws AuthenticationException {
    super(s);
  }

  @Override
  public void init(HttpServlet s) throws AuthenticationException {

    super.init(s);

    m_sAutologonUsername = ConfigParameters.retrieveFrom(s.getServletConfig().getServletContext())
        .getOBProperty("authentication.autologon.username");

    try {
      m_sUserId = AuthenticationData.getUserId(conn, m_sAutologonUsername);
    } catch (ServletException e) {
      throw new AuthenticationException("Cannot authenticate user: " + m_sAutologonUsername, e);
    }
  }

  @Override
  protected void doLogout(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.sendRedirect(HttpBaseUtils.getLocalAddress(request));
  }

  @Override
  protected String doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {
    if (m_sUserId == null || m_sUserId.equals("") || m_sUserId.equals("-1")) {
      if (m_sAutologonUsername == null || m_sAutologonUsername.equals("")) {
        throw new AuthenticationException("Autologon user emtpy.");
      } else {
        throw new AuthenticationException(
            "Autologon user is not an Etendo ERP user: " + m_sAutologonUsername);
      }
    } else {
      return m_sUserId;
    }
  }

  @Override
  public boolean useExternalLoginPage() {
    return true;
  }
}
