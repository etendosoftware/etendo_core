package com.smf.jobs.defaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.CloneOrderHookCaller;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.db.CallStoredProcedure;

import com.smf.jobs.hooks.CloneRecordHook;

import jakarta.enterprise.context.Dependent;

/**
 * A hook for the Clone Records Job, to handle Orders in a special way.
 * This process creates a new order, and copies the information from the old order to
 * the new one, leaving it in Draft status.
 */
@Dependent
@Qualifier(Order.ENTITY_NAME)
public class CloneOrderHook extends CloneRecordHook {

    @Override
    public boolean shouldCopyChildren(boolean uiCopyChildren) {
        return false;
    }

    @Override
    public BaseOBObject preCopy(BaseOBObject originalRecord) {
        return originalRecord;
    }

    @Override
    public BaseOBObject postCopy(BaseOBObject originalRecord, BaseOBObject newRecord) throws Exception {
        return cloneOrder(OBContext.getOBContext().getUser(), (Order) originalRecord, (Order) newRecord);
    }

    private Order cloneOrder(final User currentUser, final Order objOrder, final Order objCloneOrder) throws Exception {
        objCloneOrder.setDocumentAction("CO");
        objCloneOrder.setDocumentStatus("DR");
        objCloneOrder.setPosted("N");
        objCloneOrder.setProcessed(false);
        objCloneOrder.setDelivered(false);
        objCloneOrder.setSalesTransaction(true);
        objCloneOrder.setDocumentNo(null);
        objCloneOrder.setSalesTransaction(objOrder.isSalesTransaction());
        objCloneOrder.setCreationDate(new Date());
        objCloneOrder.setUpdated(new Date());
        objCloneOrder.setCreatedBy(currentUser);
        objCloneOrder.setUpdatedBy(currentUser);
        objCloneOrder.setReservationStatus(null);
        // Setting order date and scheduled delivery date of header and the order lines to current
        // date to avoid issues with tax rates. Refer issue
        // https://issues.openbravo.com/view.php?id=23671
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        objCloneOrder.setOrderDate(cal.getTime());
        objCloneOrder.setScheduledDeliveryDate(cal.getTime());
        objCloneOrder.setGrandTotalAmount(BigDecimal.ZERO);
        objCloneOrder.setSummedLineAmount(BigDecimal.ZERO);

        // Calling Clone Order Hook
        WeldUtils.getInstanceFromStaticBeanManager(CloneOrderHookCaller.class)
                .executeHook(objCloneOrder);

        // save the cloned order object
        OBDal.getInstance().save(objCloneOrder);

        // Clone the Order Lines related to the original Order
        cloneOrderLines(currentUser, objOrder, objCloneOrder);

        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(objCloneOrder);
        return objCloneOrder;
    }

    private void cloneOrderLines(final User currentUser, final Order objOrder, Order objCloneOrder) {
        Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine = new HashMap<>();
        List<OrderlineServiceRelation> orderLinesServiceRelation = new ArrayList<>();
        List<OrderLine> orderLinesCreatedFromExplodedBOM = new ArrayList<>();

        for (OrderLine ordLine : objOrder.getOrderLineList()) {
            String strPriceVersionId = getPriceListVersion(objOrder.getPriceList().getId(),
                    objOrder.getClient().getId());
            BigDecimal bdPriceList = getPriceList(ordLine.getProduct().getId(), strPriceVersionId);
            OrderLine objCloneOrdLine = (OrderLine) DalUtil.copy(ordLine, false);
            objCloneOrdLine.setReservedQuantity(new BigDecimal("0"));
            objCloneOrdLine.setDeliveredQuantity(new BigDecimal("0"));
            objCloneOrdLine.setInvoicedQuantity(new BigDecimal("0"));
            if (bdPriceList != null && bdPriceList.compareTo(BigDecimal.ZERO) != 0) {
                objCloneOrdLine.setListPrice(bdPriceList);
            }
            objCloneOrdLine.setCreationDate(new Date());
            objCloneOrdLine.setUpdated(new Date());
            objCloneOrdLine.setCreatedBy(currentUser);
            objCloneOrdLine.setUpdatedBy(currentUser);
            objCloneOrdLine.setOrderDate(new Date());
            objCloneOrdLine.setScheduledDeliveryDate(new Date());
            objCloneOrder.getOrderLineList().add(objCloneOrdLine);
            objCloneOrdLine.setSalesOrder(objCloneOrder);
            objCloneOrdLine.setReservationStatus(null);

            mapOriginalOrderLineWithCloneOrderLine.put(ordLine.getId(), objCloneOrdLine);
            List<OrderlineServiceRelation> lineServiceRelation = cloneProductServiceRelation(ordLine,
                    objCloneOrdLine);
            orderLinesServiceRelation.addAll(lineServiceRelation);
            if (ordLine.getBOMParent() != null) {
                orderLinesCreatedFromExplodedBOM.add(ordLine);
            }
        }

        fixRelatedServicesReferences(mapOriginalOrderLineWithCloneOrderLine, orderLinesServiceRelation);

        fixRelatedBOMProductsReferences(mapOriginalOrderLineWithCloneOrderLine,
                orderLinesCreatedFromExplodedBOM);

        mapOriginalOrderLineWithCloneOrderLine.clear();
        orderLinesServiceRelation.clear();
        orderLinesCreatedFromExplodedBOM.clear();
    }

    private String getPriceListVersion(final String priceList, final String clientId) {
        try {
            String whereClause = " as plv left outer join plv.priceList pl where plv.active='Y' and plv.active='Y' and "
                    + " pl.id = :priceList and plv.client.id = :clientId order by plv.validFromDate desc";

            OBQuery<PriceListVersion> ppriceListVersion = OBDal.getInstance()
                    .createQuery(PriceListVersion.class, whereClause);
            ppriceListVersion.setNamedParameter("priceList", priceList);
            ppriceListVersion.setNamedParameter("clientId", clientId);

            if (!ppriceListVersion.list().isEmpty()) {
                return ppriceListVersion.list().get(0).getId();
            } else {
                return "0";
            }
        } catch (Exception e) {
            throw new OBException(e);
        }
    }

    private BigDecimal getPriceList(final String strProductID, final String strPriceVersionId) {
        BigDecimal bdPriceList;
        try {
            final List<Object> parameters = new ArrayList<>();
            parameters.add(strProductID);
            parameters.add(strPriceVersionId);
            final String procedureName = "M_BOM_PriceList";
            bdPriceList = (BigDecimal) CallStoredProcedure.getInstance()
                    .call(procedureName, parameters, null);
        } catch (Exception e) {
            throw new OBException(e);
        }

        return (bdPriceList);
    }

    private List<OrderlineServiceRelation> cloneProductServiceRelation(final OrderLine ordLine,
                                                                       OrderLine objCloneOrdLine) {
        List<OrderlineServiceRelation> cloneServiceRelation = new ArrayList<>(
                ordLine.getOrderlineServiceRelationList().size());
        for (OrderlineServiceRelation orderLineServiceRelation : ordLine
                .getOrderlineServiceRelationList()) {
            OrderlineServiceRelation lineServiceRelation = (OrderlineServiceRelation) DalUtil
                    .copy(orderLineServiceRelation, false);
            lineServiceRelation.setOrderlineRelated(orderLineServiceRelation.getOrderlineRelated());
            lineServiceRelation.setSalesOrderLine(objCloneOrdLine);
            cloneServiceRelation.add(lineServiceRelation);
        }
        objCloneOrdLine.setOrderlineServiceRelationList(cloneServiceRelation);

        return cloneServiceRelation;
    }

    private void fixRelatedServicesReferences(
            final Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine,
            final List<OrderlineServiceRelation> orderLinesServiceRelation) {
        for (OrderlineServiceRelation lineServiceRelation : orderLinesServiceRelation) {
            OrderLine clonedOrderLine = mapOriginalOrderLineWithCloneOrderLine
                    .get(lineServiceRelation.getOrderlineRelated().getId());
            lineServiceRelation.setOrderlineRelated(clonedOrderLine);
            OBDal.getInstance().save(lineServiceRelation);
        }
    }

    private void fixRelatedBOMProductsReferences(
            final Map<String, OrderLine> mapOriginalOrderLineWithCloneOrderLine,
            final List<OrderLine> orderLinesCreatedFromExplodedBOM) {
        for (OrderLine orderLine : orderLinesCreatedFromExplodedBOM) {
            OrderLine clonedOrderLine = mapOriginalOrderLineWithCloneOrderLine.get(orderLine.getId());
            String bomParentId = orderLine.getBOMParent().getId();
            OrderLine clonedBomParent = mapOriginalOrderLineWithCloneOrderLine.get(bomParentId);
            clonedOrderLine.setBOMParent(clonedBomParent);
        }
    }

    public static BigDecimal getLineNetAmt(final String strOrderId) {
        BigDecimal bdLineNetAmt = new BigDecimal("0");
        final String readLineNetAmtHql = " select (coalesce(ol.lineNetAmount,0) + coalesce(ol.freightAmount,0) + coalesce(ol.chargeAmount,0)) as LineNetAmt from OrderLine ol where ol.salesOrder.id=:orderId";
        final Query<BigDecimal> readLineNetAmtQry = OBDal.getInstance()
                .getSession()
                .createQuery(readLineNetAmtHql, BigDecimal.class);
        readLineNetAmtQry.setParameter("orderId", strOrderId);

        for (BigDecimal amount : readLineNetAmtQry.list()) {
            bdLineNetAmt = bdLineNetAmt.add(amount);
        }

        return bdLineNetAmt;
    }
}
