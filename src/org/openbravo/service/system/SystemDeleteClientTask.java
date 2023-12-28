package org.openbravo.service.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.DalInitializingTask;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;

/**
 * Performs delete client process.
 *
 * @author szapata
 */
public class SystemDeleteClientTask extends DalInitializingTask {
  private static final Logger log = LogManager.getLogger("SystemDeleteClientTask");
  protected String clientId;

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @Override
  protected void doExecute() {
    // Validate module
    Client client = OBDal.getInstance().get(Client.class, clientId);
    if (client == null) {
      throw new OBException("Client not found");
    }
    log.info("Deleting Client: {}", client.getName());

    SystemService.getInstance().deleteClient(client);
  }
}
