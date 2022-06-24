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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.materialmgmt.refinventory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventoryType;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;

/**
 * Utility class for Referenced Inventory feature
 *
 */
public class ReferencedInventoryUtil {
  public static final String REFERENCEDINVENTORYPREFIX = "[";
  public static final String REFERENCEDINVENTORYSUFFIX = "]";

  /**
   * Create and return a new AttributeSetInstance from the given originalAttributeSetInstance and
   * link it to the given referencedInventory
   */
  public static final AttributeSetInstance cloneAttributeSetInstance(
      final AttributeSetInstance _originalAttributeSetInstance,
      final ReferencedInventory referencedInventory) {
    final AttributeSetInstance originalAttributeSetInstance = _originalAttributeSetInstance == null
        ? OBDal.getInstance().get(AttributeSetInstance.class, "0")
        : _originalAttributeSetInstance;

    final AttributeSetInstance newAttributeSetInstance = (AttributeSetInstance) DalUtil
        .copy(originalAttributeSetInstance, false);
    newAttributeSetInstance.setActive(true);
    newAttributeSetInstance.setClient(referencedInventory.getClient());
    newAttributeSetInstance.setOrganization(originalAttributeSetInstance.getOrganization());
    newAttributeSetInstance.setParentAttributeSetInstance(originalAttributeSetInstance);
    newAttributeSetInstance.setReferencedInventory(referencedInventory);
    newAttributeSetInstance.setDescription(getAttributeSetInstanceDescriptionForReferencedInventory(
        newAttributeSetInstance.getDescription(), referencedInventory));
    OBDal.getInstance().save(newAttributeSetInstance);
    return newAttributeSetInstance;
  }

  /**
   * Returns an AttributeSetInstance previously created from the given _originalAttributeSetInstance
   * and referenced inventory. If not found returns null.
   */
  public static final AttributeSetInstance getAlreadyClonedAttributeSetInstance(
      final AttributeSetInstance _originalAttributeSetInstance,
      final ReferencedInventory referencedInventory) {
    try {
      OBContext.setAdminMode(true);
      final AttributeSetInstance originalAttributeSetInstance = _originalAttributeSetInstance == null
          ? OBDal.getInstance().getProxy(AttributeSetInstance.class, "0")
          : _originalAttributeSetInstance;

      final OBCriteria<AttributeSetInstance> criteria = OBDao.getFilteredCriteria(
          AttributeSetInstance.class,
          Restrictions.eq(AttributeSetInstance.PROPERTY_PARENTATTRIBUTESETINSTANCE + ".id",
              originalAttributeSetInstance.getId()),
          Restrictions.eq(AttributeSetInstance.PROPERTY_REFERENCEDINVENTORY + ".id",
              referencedInventory.getId()));
      criteria.setMaxResults(1);
      return criteria.list().get(0);
    } catch (final Exception notFound) {
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Generates a description with the originalDesc +
   * {@value org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil#REFERENCEDINVENTORYPREFIX}
   * + referenced Inventory search key +
   * {@value org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil#REFERENCEDINVENTORYSUFFIX}
   */
  public static final String getAttributeSetInstanceDescriptionForReferencedInventory(
      final String originalDesc, final ReferencedInventory referencedInventory) {
    return StringUtils
        .left((StringUtils.isBlank(originalDesc) ? "" : originalDesc) + REFERENCEDINVENTORYPREFIX
            + referencedInventory.getSearchKey() + REFERENCEDINVENTORYSUFFIX, 255);
  }

  /**
   * Returns the parent attribute set instance for the given storage detail. If not found it returns
   * null
   */
  public static final AttributeSetInstance getParentAttributeSetInstance(
      final StorageDetail storageDetail) {
    try {
      return storageDetail.getAttributeSetValue().getParentAttributeSetInstance();
    } catch (NullPointerException noParentFound) {
      return null;
    }
  }

  /**
   * If the given referenced inventory type id is associated to a sequence, it then return the next
   * value in that sequence. Otherwise returns null.
   * 
   * @param referencedInventoryTypeId
   *          Referenced Inventory Type Id used to get its sequence
   * @param updateNext
   *          if true updates the sequence's next value in database
   */
  public static String getProposedValueFromSequenceOrNull(final String referencedInventoryTypeId,
      final boolean updateNext) {
    if (StringUtils.isBlank(referencedInventoryTypeId)) {
      return null;
    } else {
      return FIN_Utility.getDocumentNo(updateNext, getSequence(referencedInventoryTypeId));
    }
  }

  /**
   * Returns the sequence associated to the given referenced inventory type id or null if not found
   */
  private static Sequence getSequence(final String referencedInventoryTypeId) {
    return OBDal.getInstance()
        .get(ReferencedInventoryType.class, referencedInventoryTypeId)
        .getSequence();
  }

  /**
   * Throw an exception if the given attribute set instance is linked to a referenced inventory
   */
  public static void avoidUpdatingIfLinkedToReferencedInventory(
      final String attributeSetInstanceId) {
    try {
      OBContext.setAdminMode(true);
      final AttributeSetInstance attributeSetInstance = OBDal.getInstance()
          .getProxy(AttributeSetInstance.class, attributeSetInstanceId);
      if (attributeSetInstance.getParentAttributeSetInstance() != null
          || !attributeSetInstance.getAttributeSetInstanceParentAttributeSetInstanceIDList()
              .isEmpty()) {
        throw new OBException("@RefInventoryAvoidUpdatingAttribute@");
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  static InternalMovement createAndSaveGoodsMovementHeader(final Organization organization,
      final String name) {
    final InternalMovement header = OBProvider.getInstance().get(InternalMovement.class);
    header.setClient(OBContext.getOBContext().getCurrentClient());
    header.setOrganization(organization);
    header.setName(name);
    header.setMovementDate(DateUtils.truncate(new Date(), Calendar.DATE));
    OBDal.getInstance().save(header);
    return header;
  }

  static InternalMovementLine createAndSaveMovementLine(final InternalMovement internalMovement,
      final BigDecimal movementQty, final Locator newStorageBin,
      final AttributeSetInstance newAttributeSetInstance, final long lineNo,
      final StorageDetail storageDetail, final Reservation reservation) {
    final InternalMovementLine line = OBProvider.getInstance().get(InternalMovementLine.class);
    line.setClient(internalMovement.getClient());
    line.setOrganization(storageDetail.getOrganization());
    line.setLineNo(lineNo);
    line.setProduct(storageDetail.getProduct());
    line.setMovementQuantity(movementQty);
    line.setUOM(storageDetail.getProduct().getUOM());
    line.setAttributeSetValue(storageDetail.getAttributeSetValue());
    line.setStorageBin(storageDetail.getStorageBin());
    line.setNewStorageBin(newStorageBin);
    line.setMovement(internalMovement);
    line.setAttributeSetInstanceTo(newAttributeSetInstance);
    line.setStockReservation(reservation);
    internalMovement.getMaterialMgmtInternalMovementLineList().add(line);
    OBDal.getInstance().save(line);
    return line;
  }

  static boolean isGreaterThanZero(final BigDecimal qty) {
    return qty.compareTo(BigDecimal.ZERO) > 0;
  }

  /**
   * Returns a ScrollableResults with the available stock reservations for the given storage detail
   * that can be boxed to the given newStorageBin. They are ordered by first non-allocated, without
   * a defined attribute set instance at reservation header first and with the lower reserved
   * quantity
   */
  public static ScrollableResults getAvailableStockReservations(final StorageDetail storageDetail,
      final Locator newStorageBin) {
    Check.isNotNull(storageDetail, "storageDetail parameter can't be null");
    final String olHql = "select sr, sr.quantity - sr.released " + //
        "from MaterialMgmtReservationStock sr " + //
        "join sr.reservation res " + //
        "where coalesce(sr.storageBin.id, res.storageBin.id) = :sdBinId " + //
        // Skip reservations forced to a bin different from the destination bin
        "and (res.storageBin.id is null or res.storageBin.id = :toBindId) " + //
        "and coalesce(sr.attributeSetValue.id, res.attributeSetValue.id) = :sdAttributeSetId " + //
        "and sr.quantity - sr.released > 0 " + //
        "and res.product.id = :productId " + //
        "and res.uOM.id = :uomId " + //
        "and res.rESStatus = 'CO' " + //
        "order by case when sr.allocated = 'Y' then 1 else 0 end, " + //
        "      case when res.attributeSetValue.id is not null then 1 else 0 end, " + //
        "      sr.quantity - sr.released asc  ";
    final Session session = OBDal.getInstance().getSession();
    final Query<Object[]> sdQuery = session.createQuery(olHql, Object[].class);
    sdQuery.setParameter("sdBinId", storageDetail.getStorageBin().getId());
    sdQuery.setParameter("toBindId",
        newStorageBin != null ? newStorageBin.getId() : "noStorageBinToIDShouldMatch");
    sdQuery.setParameter("sdAttributeSetId", storageDetail.getAttributeSetValue().getId());
    sdQuery.setParameter("productId", storageDetail.getProduct().getId());
    sdQuery.setParameter("uomId", storageDetail.getUOM().getId());
    sdQuery.setFetchSize(1000);
    return sdQuery.scroll(ScrollMode.FORWARD_ONLY);
  }
}
