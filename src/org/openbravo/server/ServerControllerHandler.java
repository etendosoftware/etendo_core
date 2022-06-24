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
 * All portions are Copyright (C) 2016 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */

package org.openbravo.server;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

/**
 * Provides methods to determine is the current server has the role of a central or a store server
 * in a multi-tiered architecture
 * 
 * It has a default implementation where it is hardcoded that the current server is a central
 * server, but this functionality can be overwritten by extending the {@link ServerController} class
 */
@ApplicationScoped
public class ServerControllerHandler {

  @Inject
  @Any
  private Instance<ServerController> allServerControllerImplementations;
  // the ServerController that will be used according to its priority
  private ServerController serverControllerImplementation = null;

  /**
   * @return true if the current server has been configured to be a central server, false otherwise
   */
  public boolean isThisACentralServer() {
    if (getServerControllerImplementation() != null) {
      return getServerControllerImplementation().isThisACentralServer();
    } else {
      return true;
    }
  }

  /**
   * @return true if the current server has been configured to be a store server, false otherwise
   */
  public boolean isThisAStoreServer() {
    if (getServerControllerImplementation() != null) {
      return getServerControllerImplementation().isThisAStoreServer();
    } else {
      return false;
    }
  }

  // Of all the server controller implementations, returns the one whose getPriority method returns
  // the lowest value
  private ServerController getServerControllerImplementation() {
    if (serverControllerImplementation == null) {
      for (ServerController nextServerController : allServerControllerImplementations) {
        if (serverControllerImplementation == null) {
          serverControllerImplementation = nextServerController;
        } else if (nextServerController.getPriority() < serverControllerImplementation
            .getPriority()) {
          serverControllerImplementation = nextServerController;
        }
      }
    }
    return serverControllerImplementation;
  }
}
