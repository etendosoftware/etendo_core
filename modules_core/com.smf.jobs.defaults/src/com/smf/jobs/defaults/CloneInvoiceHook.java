package com.smf.jobs.defaults;

import com.smf.jobs.hooks.CloneRecordHook;

import org.apache.commons.lang.time.DateUtils;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.service.db.CallStoredProcedure;

import javax.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A hook for the Clone Records Job, to handle Invoices in a special way.
 * This process creates a new invoice, and copies the information from the old invoice to
 * the new one, leaving it in Draft status.
 */
@ApplicationScoped
@Qualifier(Invoice.ENTITY_NAME)
public class CloneInvoiceHook extends CloneRecordHook {

    @Override
    public boolean shouldCopyChildren(boolean uiCopyChildren) {
        return false;
    }

    @Override
    public BaseOBObject preCopy(BaseOBObject originalRecord) {
        return originalRecord;
    }

    @Override
    public BaseOBObject postCopy(BaseOBObject originalRecord, BaseOBObject newRecord) {
        Invoice objInvoice = (Invoice) originalRecord;
        Invoice objCloneInvoice = (Invoice) newRecord;
        User currentUser = OBContext.getOBContext().getUser();

        objCloneInvoice.setDocumentAction("CO");
        objCloneInvoice.setDocumentStatus("DR");
        objCloneInvoice.setPosted("N");
        objCloneInvoice.setAPRMProcessinvoice("CO");// The button appears with the text 'Complete'
        // instead of 'reactivate'
        objCloneInvoice.setProcessed(false);
        objCloneInvoice.setTotalPaid(BigDecimal.ZERO);
        objCloneInvoice.setOutstandingAmount(BigDecimal.ZERO);
        objCloneInvoice.setPrepaymentamt(BigDecimal.ZERO);
        objCloneInvoice.setPaymentComplete(false);
        objCloneInvoice.setSalesTransaction(objInvoice.isSalesTransaction());
        objCloneInvoice.setSalesOrder(null);
        objCloneInvoice.setDocumentNo(null);

        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        objCloneInvoice.setAccountingDate(today);
        objCloneInvoice.setInvoiceDate(today);

        objCloneInvoice.setCreatedBy(currentUser);
        objCloneInvoice.setUpdatedBy(currentUser);
        objCloneInvoice.setCreationDate(new Date());
        objCloneInvoice.setUpdated(new Date());
        objCloneInvoice.setGrandTotalAmount(BigDecimal.ZERO);
        objCloneInvoice.setSummedLineAmount(BigDecimal.ZERO);

        // save the cloned invoice object
        OBDal.getInstance().save(objCloneInvoice);

        // get the lines associated with the invoice and clone them to the new
        // invoice line.
        for (InvoiceLine invLine : objInvoice.getInvoiceLineList()) {
            InvoiceLine objCloneInvLine = (InvoiceLine) DalUtil.copy(invLine, false);
            if (invLine.getProduct() != null) {
                String strPriceVersionId = getPriceListVersion(objInvoice.getPriceList().getId(),
                        objInvoice.getClient().getId());
                BigDecimal bdPriceList = getPriceList(invLine.getProduct().getId(), strPriceVersionId);
                objCloneInvLine.setListPrice(bdPriceList);
            } else {
                objCloneInvLine.setListPrice(invLine.getListPrice());
            }
            objCloneInvLine.setSalesOrderLine(null);
            objCloneInvLine.setGoodsShipmentLine(null);
            objCloneInvLine.setCreationDate(new Date());
            objCloneInvLine.setUpdated(new Date());
            objCloneInvLine.setCreatedBy(currentUser);
            objCloneInvLine.setUpdatedBy(currentUser);
            objCloneInvoice.getInvoiceLineList().add(objCloneInvLine);
            objCloneInvLine.setInvoice(objCloneInvoice);

            OBDal.getInstance().save(invLine);
        }

        OBDal.getInstance().save(objCloneInvoice);

        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(objCloneInvoice);
        return objCloneInvoice;
    }

    private String getPriceListVersion(String priceList, String clientId) {
            String whereClause = " as plv , PricingPriceList pl where pl.id=plv.priceList.id and plv.active='Y' and "
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
    }

    private BigDecimal getPriceList(String strProductID, String strPriceVersionId) {
        BigDecimal bdPriceList;
        final List<Object> parameters = new ArrayList<>();
        parameters.add(strProductID);
        parameters.add(strPriceVersionId);
        final String procedureName = "M_BOM_PriceList";
        bdPriceList = (BigDecimal) CallStoredProcedure.getInstance().call(procedureName, parameters,
                null);

        return (bdPriceList);
    }

}
