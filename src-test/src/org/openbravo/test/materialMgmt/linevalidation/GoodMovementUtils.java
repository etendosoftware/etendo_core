package org.openbravo.test.materialMgmt.linevalidation;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.service.db.CallProcess;

/**
 * Utility class for handling Goods Movements.
 */
public class GoodMovementUtils {

  private static final String M_MOVEMENT_POST_ID = "122";

  /**
   * Private constructor to prevent instantiation of this utility class.
   * Throws an exception if instantiation is attempted.
   */
  private GoodMovementUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * Creates a new Internal Movement document with a default test name and a movement date 15 days
   * in the future.
   *
   * @param client
   *     the client to associate the goods movement with.
   * @param org
   *     the organization to associate the goods movement with.
   * @return the created InternalMovement object.
   */
  public static InternalMovement createGoodsMovement(Client client, Organization org) {
    InternalMovement movement = OBProvider.getInstance().get(InternalMovement.class);
    movement.setClient(client);
    movement.setOrganization(org);
    movement.setName("Test");
    movement.setMovementDate(DateUtils.addDays(new Date(), 15));

    OBDal.getInstance().save(movement);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(movement);
    return movement;
  }

  /**
   * Processes a Goods Movement document by calling the "M_Movement_Post" process.
   *
   * @param goodsMovementId
   *     the ID of the goods movement document to process.
   * @return the result of the process execution as an {@link OBError} object.
   */
  public static OBError processGoodsMovement(final String goodsMovementId) {
    final org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(Process.class, M_MOVEMENT_POST_ID);
    final ProcessInstance pinstance = CallProcess.getInstance().call(process, goodsMovementId, null);
    return OBMessageUtils.getProcessInstanceMessage(pinstance);
  }
}
