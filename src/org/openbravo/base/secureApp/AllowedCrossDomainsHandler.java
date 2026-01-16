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
 * All portions are Copyright (C) 2016-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.secureApp;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.weld.WeldUtils;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides/handles the domain checkers which determine if a specific cross domain request is
 * allowed on the OB server.
 * 
 * https://en.wikipedia.org/wiki/Cross-origin_resource_sharing
 * 
 * @author Martin Taal
 */
@Dependent
public class AllowedCrossDomainsHandler {

  private static final Logger log = LogManager.getLogger();

  private static AllowedCrossDomainsHandler instance = new AllowedCrossDomainsHandler();

  public static AllowedCrossDomainsHandler getInstance() {
    return instance;
  }

  public static void setInstance(AllowedCrossDomainsHandler instance) {
    AllowedCrossDomainsHandler.instance = instance;
  }

  private Collection<AllowedCrossDomainsChecker> checkers = null;

  /**
   * Returns true if the origin of the request is allowed, in that case the cors headers can be set
   * ( {@link #setCORSHeaders(HttpServletRequest, HttpServletResponse)}.
   * 
   * @param request
   * @return true if the origin if the request is in the list of allowed domains
   */
  private boolean fromAllowedOrigin(HttpServletRequest request) {
    final String origin = request.getHeader("Origin");

    if (isNullOrEmpty(origin)) {
      return false;
    }

    for (AllowedCrossDomainsChecker checker : getCheckers()) {
      if (checker.isAllowedOrigin(request, origin)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if an origin is set on the header, if not then false is returned. If there are no
   * checkers installed then also false is returned. If there are checkers installed then the origin
   * is checked and the result is returned.
   * 
   * Note: will return true if there is indeed an invalid confirmed origin.
   */
  public boolean isCheckedInvalidOrigin(HttpServletRequest request) {
    final String origin = request.getHeader("Origin");

    if (isNullOrEmpty(origin)) {
      return false;
    }

    if (getCheckers().isEmpty()) {
      return false;
    }

    for (AllowedCrossDomainsChecker checker : getCheckers()) {
      if (checker.isAllowedOrigin(request, origin)) {
        return false;
      }
    }
    return true;
  }

  private Collection<AllowedCrossDomainsChecker> getCheckers() {
    if (checkers == null) {
      setCheckers();
    }
    return checkers;
  }

  private synchronized void setCheckers() {
    if (checkers != null) {
      return;
    }
    final Collection<AllowedCrossDomainsChecker> localCheckers = new ArrayList<AllowedCrossDomainsChecker>();
    for (AllowedCrossDomainsChecker checker : WeldUtils
        .getInstances(AllowedCrossDomainsChecker.class)) {
      localCheckers.add(checker);
    }
    checkers = localCheckers;
  }

  /**
   * Utility method to set CORS headers on a request.
   */
  public void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {

    final String origin = request.getHeader("Origin");

    // don't do anything if no checkers or no origin
    if (getCheckers().isEmpty() || isNullOrEmpty(origin)) {
      return;
    }

    try {
      if (request.getRequestURL().indexOf(origin) == 0) {
        // if the request url starts with the origin then no need to set
        // headers either
        return;
      }

      if (!fromAllowedOrigin(request)) {
        return;
      }

      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Credentials", "true");
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers",
              "Authorization, Content-Type, Origin, Accept, X-Requested-With, Access-Control-Allow-Credentials");

      response.setHeader("Access-Control-Max-Age", "10000");
    } catch (Exception logIt) {
      // on purpose not stopping on this to retain some robustness
      log.error("Error when setting cors headers " + logIt.getMessage() + " "
          + request.getRequestURL() + " " + request.getQueryString(), logIt);
    }
  }

  private boolean isNullOrEmpty(final String origin) {
    return origin == null || origin.equals("") || origin.equals("null");
  }

  /**
   * Implementation provided by modules which determine if a request is coming from an allowed
   * origin.
   * 
   * @author mtaal
   */
  @Dependent
  public static abstract class AllowedCrossDomainsChecker {

    public abstract boolean isAllowedOrigin(HttpServletRequest request, String origin);

  }

}
