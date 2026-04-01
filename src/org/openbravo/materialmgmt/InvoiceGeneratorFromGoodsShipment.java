/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2018-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.materialmgmt;

import java.math.BigDecimal;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.Check;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.common.actionhandler.createlinesfromprocess.CreateInvoiceLinesFromProcess;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.businessUtility.BpDocTypeResolver;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.InvoiceCandidateV;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

/**
 * This class generates and processes Invoice from Goods Shipment. Only goods shipment lines not
 * linked to a Sales Order, or from Sales Order with Invoice Term "After Delivery", "After Order
 * Delivery" or "Immediate" are considered.
 *
 */
public class InvoiceGeneratorFromGoodsShipment {
  private static final Logger log = LogManager.getLogger();
  private static final String POS_ORDER = "WR";
  private static final String ERROR_PREFIX = "ERROR:";
  private static final String WHERE_MARKER = "\n  Where:";

  private String shipmentId;
  private Invoice invoice;
  private CreateInvoiceLinesFromProcess createInvoiceLineProcess;
  private Date invoiceDate;
  private String invoiceDocumentNo;
  private String priceListId;
  private final Set<String> ordersWithAfterOrderDeliveryAlreadyInvoiced = new HashSet<>();
  private boolean allowInvoicePOSOrder = false;

  private enum InvoiceTerm {
    IMMEDIATE("I"), AFTER_DELIVERY("D"), CUSTOMERSCHEDULE("S"), AFTER_ORDER_DELIVERY("O");

    private static final List<String> CAN_INVOICE_ORDERLINE_INDIVIDUALLY = Arrays.asList(
        InvoiceTerm.IMMEDIATE.getInvoiceTerm(), InvoiceTerm.AFTER_DELIVERY.getInvoiceTerm(),
        CUSTOMERSCHEDULE.getInvoiceTerm());

    private static final List<String> SHOULD_INVOICE__WEBPOS_ORDERLINE = Arrays
        .asList(InvoiceTerm.AFTER_DELIVERY.getInvoiceTerm(), AFTER_ORDER_DELIVERY.getInvoiceTerm());

    private String invoiceTermId;

    private InvoiceTerm(final String invoiceTermId) {
      this.invoiceTermId = invoiceTermId;
    }

    private static boolean canInvoiceOrderLineIndividually(final String invoiceTerm) {
      return CAN_INVOICE_ORDERLINE_INDIVIDUALLY.contains(invoiceTerm);
    }

    private static boolean shouldInvoicePOSOrderLine(final String invoiceTerm) {
      return SHOULD_INVOICE__WEBPOS_ORDERLINE.contains(invoiceTerm);
    }

    private String getInvoiceTerm() {
      return invoiceTermId;
    }
  }

  public void setAllowInvoicePOSOrder(boolean allowInvoicePOSOrder) {
    this.allowInvoicePOSOrder = allowInvoicePOSOrder;
  }

  /**
   * Creates an {@link InvoiceGeneratorFromGoodsShipment} based only on shipment Id. The invoice
   * date is taken from the shipment movement date, and the invoice price list is taken from
   * shipment business partner
   * 
   * @param shipmentId
   *          The shipment Id
   */
  public InvoiceGeneratorFromGoodsShipment(final String shipmentId) {
    this(shipmentId, null, null);
  }

  /**
   * Creates an {@link InvoiceGeneratorFromGoodsShipment} based on shipment Id
   * 
   * @param shipmentId
   *          The shipment Id
   * @param invoiceDate
   *          The invoice date. If null it takes the from the shipment movement date.
   * @param priceList
   *          The invoice price list. If null it takes the business partner default
   */
  public InvoiceGeneratorFromGoodsShipment(final String shipmentId, final Date invoiceDate,
      final PriceList priceList) {
    this(shipmentId, invoiceDate, priceList, null);
  }

  /**
   * Creates an {@link InvoiceGeneratorFromGoodsShipment} based on shipment Id
   * 
   * @param shipmentId
   *          The shipment Id
   * @param invoiceDate
   *          The invoice date. If null it takes the from the shipment movement date.
   * @param priceList
   *          The invoice price list. If null it takes the business partner default
   * @param invoiceDocumentNo
   *          The invoice document number.
   */
  public InvoiceGeneratorFromGoodsShipment(final String shipmentId, final Date invoiceDate,
      final PriceList priceList, final String invoiceDocumentNo) {
    Check.isNotNull(shipmentId, "Parameter shipmentId can't be null");
    this.shipmentId = shipmentId;
    setInvoiceDate(invoiceDate);
    setPriceListId(priceList);
    this.invoiceDocumentNo = invoiceDocumentNo;
    this.createInvoiceLineProcess = WeldUtils
        .getInstanceFromStaticBeanManager(CreateInvoiceLinesFromProcess.class);
  }

  private void setInvoiceDate(final Date date) {
    this.invoiceDate = date == null ? getShipment().getMovementDate() : date;
  }

  private void setPriceListId(final PriceList priceList) {
    try {
      this.priceListId = priceList.getId();
    } catch (NullPointerException noPriceListProvided) {
      try {
        this.priceListId = getShipment().getBusinessPartner().getPriceList().getId();
      } catch (NullPointerException bpWithoutPriceList) {
        throw new OBException(OBMessageUtils.messageBD("notnullpricelist"));
      }
    }
  }

  private ShipmentInOut getShipment() {
    return OBDal.getInstance().getProxy(ShipmentInOut.class, shipmentId);
  }

  private Date getInvoiceDate() {
    return this.invoiceDate;
  }

  private PriceList getPriceList() {
    return OBDal.getInstance().getProxy(PriceList.class, priceListId);
  }

  /**
   * Creates a Sales Invoice from Goods Shipment, considering the invoice terms of available orders
   * linked to the shipment lines.
   *
   * @param doProcessInvoice
   *          if true the invoice will be automatically processed, otherwise it will remain in draft
   *          status
   *
   * @return The invoice created
   */
  public Invoice createInvoiceConsideringInvoiceTerms(boolean doProcessInvoice) {
    try {
      createInvoiceIfPossible();
      if (doProcessInvoice && invoice != null) {
        processInvoice();
        OBDal.getInstance().refresh(invoice);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      String errorMessage = extractDatabaseErrorMessage(e);
      throw new OBException(OBMessageUtils.translateError(errorMessage).getMessage());
    }

    return invoice;
  }

  /**
   * Extracts the most relevant error message from a database exception, handling trigger errors
   * and batch update exceptions properly.
   * @param e The exception to extract the message from
   * @return The extracted error message
   */
  protected String extractDatabaseErrorMessage(Exception e) {
    Throwable current = e;
    String triggerErrorMessage = null;
    while (current != null) {
      if (current instanceof BatchUpdateException) {
        BatchUpdateException batchEx = (BatchUpdateException) current;
        SQLException nextEx = batchEx.getNextException();
        if (nextEx != null && nextEx.getMessage() != null) {
          triggerErrorMessage = nextEx.getMessage();
          if (triggerErrorMessage.contains(ERROR_PREFIX)) {
            int errorStart = triggerErrorMessage.indexOf(ERROR_PREFIX);
            int whereStart = triggerErrorMessage.indexOf(WHERE_MARKER);
            int extractStart = errorStart + ERROR_PREFIX.length();
            if (whereStart > errorStart) {
              triggerErrorMessage = triggerErrorMessage.substring(extractStart, whereStart).trim();
            } else {
              triggerErrorMessage = triggerErrorMessage.substring(extractStart).trim();
            }
          }
          break;
        }
      }
      current = current.getCause();
    }
    if (StringUtils.isNotBlank(triggerErrorMessage)) {
      return triggerErrorMessage;
    }
    Throwable ex = DbUtility.getUnderlyingSQLException(e);
    String message = ex != null ? ex.getMessage() : null;
    return StringUtils.isNotBlank(message) ? message : "Unknown database error";
  }

  private Invoice createInvoiceIfPossible() {
    try (ScrollableResults scrollShipmentLines = getShipmentLines()) {
      while (scrollShipmentLines.next()) {
        final ShipmentInOutLine shipmentLine = (ShipmentInOutLine) scrollShipmentLines.get(0);
        final OrderLine orderLine = (OrderLine) scrollShipmentLines.get(1);
        final Order order = orderLine == null ? null : orderLine.getSalesOrder();
        if (orderLine == null) {
          invoiceShipmentLineWithoutRelatedOrderLine(shipmentLine);
        } else if (isOrderCandidateToBeInvoiced(order)) {
          invoiceShipmentLineWithRelatedOrderLine(shipmentLine, orderLine, order);
        }
        evictObjects(shipmentLine, orderLine, order);
      }
      OBDal.getInstance().flush();
    }
    return invoice;
  }

  private ScrollableResults getShipmentLines() {
    final String shipmentLinesHQLQuery = "select iol, ol " //
        + "from " + ShipmentInOutLine.ENTITY_NAME + " iol " //
        + "left join iol." + ShipmentInOutLine.PROPERTY_SALESORDERLINE + " ol " //
        + "where iol." + ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT + ".id = :shipmentId ";
    final Session session = OBDal.getInstance().getSession();
    final Query<Object[]> query = session.createQuery(shipmentLinesHQLQuery, Object[].class);
    query.setParameter("shipmentId", shipmentId);
    return query.scroll(ScrollMode.FORWARD_ONLY);
  }

  private void invoiceShipmentLineWithoutRelatedOrderLine(final ShipmentInOutLine shipmentLine) {
    final BigDecimal qtyToInvoice = shipmentLine.getMovementQuantity()
        .subtract(getTotalInvoicedForShipmentLine(shipmentLine));
    invoicePendingQtyForShipmentLine(shipmentLine, qtyToInvoice);
  }

  private BigDecimal getTotalInvoicedForShipmentLine(final ShipmentInOutLine iol) {
    final String invoiceLinesHqlQuery = "select coalesce(sum(il. "
        + InvoiceLine.PROPERTY_INVOICEDQUANTITY + "), 0) " //
        + "from " + InvoiceLine.ENTITY_NAME + " il " //
        + "where il." + InvoiceLine.PROPERTY_GOODSSHIPMENTLINE + ".id = :shipmentLineId " //
        + "and il.invoice." + Invoice.PROPERTY_DOCUMENTSTATUS + "= 'CO' ";

    final Session sessionInvoiceLines = OBDal.getInstance().getSession();
    final Query<BigDecimal> queryInvoiceLines = sessionInvoiceLines
        .createQuery(invoiceLinesHqlQuery, BigDecimal.class);
    queryInvoiceLines.setParameter("shipmentLineId", iol.getId());

    return queryInvoiceLines.uniqueResult();
  }

  protected void invoicePendingQtyForShipmentLine(final ShipmentInOutLine shipmentLine,
      final BigDecimal qtyToInvoice) {
    if (BigDecimal.ZERO.compareTo(qtyToInvoice) != 0) {
      createInvoiceLine(shipmentLine, qtyToInvoice);
    }
  }

  private boolean isOrderCandidateToBeInvoiced(final Order order) {
    if (this.allowInvoicePOSOrder && POS_ORDER.equals(order.getDocumentType().getSOSubType())) {
      return InvoiceTerm.shouldInvoicePOSOrderLine(order.getInvoiceTerms());
    }
    return !OBDao
        .getFilteredCriteria(InvoiceCandidateV.class,
            Restrictions.eq(InvoiceCandidateV.PROPERTY_ID, order.getId()))
        .setMaxResults(1)
        .list()
        .isEmpty();
  }

  private void invoiceShipmentLineWithRelatedOrderLine(final ShipmentInOutLine shipmentLine,
      final OrderLine orderLine, final Order order) {
    final String invoiceTerm = order.getInvoiceTerms();
    if (InvoiceTerm.canInvoiceOrderLineIndividually(invoiceTerm)) {
      final BigDecimal qtyToInvoice = orderLine.getOrderedQuantity()
          .subtract(orderLine.getInvoicedQuantity())
          .min(shipmentLine.getMovementQuantity());
      invoicePendingQtyForShipmentLine(shipmentLine, qtyToInvoice);
    } else {
      if (InvoiceTerm.AFTER_ORDER_DELIVERY.getInvoiceTerm().equals(invoiceTerm)) {
        if (order.getDeliveryStatus() == 100
            && !ordersWithAfterOrderDeliveryAlreadyInvoiced.contains(order.getId())) {
          invoiceAllOrderLines(order);
          ordersWithAfterOrderDeliveryAlreadyInvoiced.add(order.getId());
        }
      } else {
        throw new OBException("Not supported Invoice Term: " + invoiceTerm);
      }
    }
  }

  private void invoiceAllOrderLines(final Order order) {
    try (ScrollableResults scrollOrderShipmentLines = getAllShipmentLinesLinkedToOrder(order)) {
      while (scrollOrderShipmentLines.next()) {
        final ShipmentInOutLine iol = (ShipmentInOutLine) scrollOrderShipmentLines.get()[0];
        final BigDecimal invoicedQuantity = getTotalInvoicedForShipmentLine(iol);
        if (invoicedQuantity.compareTo(iol.getMovementQuantity()) != 0) {
          createInvoiceLine(iol, iol.getMovementQuantity().subtract(invoicedQuantity));
        }
        OBDal.getInstance().getSession().evict(iol);
      }
    }
  }

  private ScrollableResults getAllShipmentLinesLinkedToOrder(final Order order) {
    final String orderLinesHqlQuery = "select iol " //
        + "from " + ShipmentInOutLine.ENTITY_NAME + " iol " //
        + "join iol." + ShipmentInOutLine.PROPERTY_SALESORDERLINE + " ol " //
        + "where ol." + OrderLine.PROPERTY_SALESORDER + ".id = :orderId ";

    final Session sessionOrderLines = OBDal.getInstance().getSession();
    final Query<ShipmentInOutLine> queryOrderLines = sessionOrderLines
        .createQuery(orderLinesHqlQuery, ShipmentInOutLine.class);
    queryOrderLines.setParameter("orderId", order.getId());

    return queryOrderLines.scroll(ScrollMode.FORWARD_ONLY);
  }

  private void createInvoiceLine(final ShipmentInOutLine shipmentLine,
      final BigDecimal invoicedQuantity) {
    createInvoiceLineProcess.createInvoiceLinesFromDocumentLines(
        formatShipmentLineToBeInvoiced(shipmentLine, invoicedQuantity), getInvoiceHeader(),
        ShipmentInOutLine.class);
  }

  private JSONArray formatShipmentLineToBeInvoiced(final ShipmentInOutLine shipmentInOutLine,
      final BigDecimal invoicedQuantity) {
    final JSONArray lines = new JSONArray();
    try {
      final JSONObject line = new JSONObject();
      line.put("uOM", shipmentInOutLine.getUOM().getId());
      line.put("uOM$_identifier", shipmentInOutLine.getUOM().getIdentifier());
      line.put("product", shipmentInOutLine.getProduct().getId());
      line.put("product$_identifier", shipmentInOutLine.getProduct().getIdentifier());
      line.put("lineNo", shipmentInOutLine.getLineNo());
      line.put("movementQuantity", invoicedQuantity.toString());
      line.put("operativeQuantity",
          shipmentInOutLine.getOperativeQuantity() == null
              ? shipmentInOutLine.getMovementQuantity().toString()
              : shipmentInOutLine.getOperativeQuantity().toString());
      line.put("id", shipmentInOutLine.getId());
      line.put("operativeUOM",
          shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getId()
              : shipmentInOutLine.getOperativeUOM().getId());
      line.put("operativeUOM$_identifier",
          shipmentInOutLine.getOperativeUOM() == null ? shipmentInOutLine.getUOM().getIdentifier()
              : shipmentInOutLine.getOperativeUOM().getIdentifier());
      line.put("orderQuantity", "");
      lines.put(line);
    } catch (JSONException e) {
      log.error(e.getMessage());
    }
    return lines;
  }

  private void evictObjects(final Object... objects) {
    for (final Object object : objects) {
      if (object != null) {
        OBDal.getInstance().getSession().evict(object);
      }
    }
  }

  private Invoice getInvoiceHeader() {
    if (invoice == null) {
      invoice = createInvoiceHeader();
    }
    return invoice;
  }

  private Invoice createInvoiceHeader() {

    final Invoice newInvoice = OBProvider.getInstance().get(Invoice.class);
    final ShipmentInOut shipment = getShipment();

    newInvoice.setClient(shipment.getClient());
    newInvoice.setOrganization(shipment.getOrganization());
    final DocumentType invoiceDocumentType = getDocumentTypeForARI(getShipment().getOrganization(), shipment.getBusinessPartner());
    newInvoice.setDocumentType(invoiceDocumentType);
    newInvoice.setTransactionDocument(invoiceDocumentType);
    newInvoice.setDocumentNo(
        invoiceDocumentNo != null ? invoiceDocumentNo : generateInvoiceDocumentNo(newInvoice));
    newInvoice.setDocumentAction("CO");
    newInvoice.setDocumentStatus("DR");
    newInvoice.setAccountingDate(getInvoiceDate());
    newInvoice.setInvoiceDate(getInvoiceDate());
    newInvoice.setTaxDate(getInvoiceDate());
    newInvoice.setSalesTransaction(true);
    newInvoice.setBusinessPartner(shipment.getBusinessPartner());
    newInvoice.setUserContact(shipment.getUserContact());

    if (shipment.getSalesOrder() != null) {
      // Get Invoice Address from Order
      Order order = shipment.getSalesOrder();
      if (order.getInvoiceAddress() != null && order.getInvoiceAddress().isInvoiceToAddress()) {
        newInvoice.setPartnerAddress(order.getInvoiceAddress());
      } else if (order.getPartnerAddress() != null && order.getPartnerAddress().isInvoiceToAddress()) {
        newInvoice.setPartnerAddress(order.getPartnerAddress());
      } else {
        // Use either the shipment's or business partner's address
        createInvoiceAddressFromShipmentOrBPartner(shipment, newInvoice);
      }
    } else {
      // Use either the shipment's or business partner's address
      createInvoiceAddressFromShipmentOrBPartner(shipment, newInvoice);
    }

    newInvoice.setPriceList(getPriceList());
    newInvoice.setCurrency(getCurrency());
    newInvoice.setSummedLineAmount(BigDecimal.ZERO);
    newInvoice.setGrandTotalAmount(BigDecimal.ZERO);
    newInvoice.setWithholdingamount(BigDecimal.ZERO);
    newInvoice.setPaymentMethod(shipment.getBusinessPartner().getPaymentMethod());
    newInvoice.setPaymentTerms(shipment.getBusinessPartner().getPaymentTerms());

    checkInvoiceHasAllMandatoryFields(newInvoice);

    OBDal.getInstance().save(newInvoice);
    OBDal.getInstance().flush();
    return newInvoice;
  }

  private void createInvoiceAddressFromShipmentOrBPartner (ShipmentInOut shipment, Invoice newInvoice) {
    if (shipment.getDeliveryLocation() != null && shipment.getDeliveryLocation().isInvoiceToAddress()) {
      // Invoice Address from SHIPMENT
      newInvoice.setPartnerAddress(shipment.getDeliveryLocation());
    } else if (shipment.getPartnerAddress() != null && shipment.getPartnerAddress().isInvoiceToAddress()) {
      newInvoice.setPartnerAddress(shipment.getPartnerAddress());
    } else {
      // Invoice Address from BUSINESS PARTNER
      String businessPartnerAddressId = getOneInvoiceBPartnerAddress(shipment.getBusinessPartner());
      if (businessPartnerAddressId != null) {
        newInvoice.setPartnerAddress(OBDal.getInstance().get(Location.class, businessPartnerAddressId));
      } else {
        // There is no Invoice Address
        throw new OBException(OBMessageUtils.messageBD("NoInvoicingAddress"));
      }
    }
  }

  private String getOneInvoiceBPartnerAddress(BusinessPartner businessPartner) {
    final String hql = "select max(bpl.id) from BusinessPartnerLocation bpl where bpl.invoiceToAddress = true and bpl.businessPartner = :bp";

    final Query<String> query = OBDal.getInstance().getSession().createQuery(hql, String.class);
    query.setParameter("bp", businessPartner);
    query.setMaxResults(1);

    return query.uniqueResult();
  }

  private DocumentType getDocumentTypeForARI(final Organization org,  final BusinessPartner bp) {
    try {
      return resolveDocTypeFor(org, bp, "ARI", true);
    } catch (Exception e) {
      throw new OBException("There is no Document type for Sales Invoice defined");
    }
  }

  private String generateInvoiceDocumentNo(final Invoice newInvoice) {
    final Entity invoiceEntity = ModelProvider.getInstance().getEntity(Invoice.class);
    return Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
        new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), "",
        invoiceEntity.getTableName(),
        newInvoice.getTransactionDocument() == null ? ""
            : newInvoice.getTransactionDocument().getId(),
        newInvoice.getDocumentType() == null ? "" : newInvoice.getDocumentType().getId(), false,
        true);
  }

  private Currency getCurrency() {
    return (getPriceList() == null) ? null : getPriceList().getCurrency();
  }

  private void checkInvoiceHasAllMandatoryFields(final Invoice newInvoice) {
    Check.isNotNull(newInvoice.getInvoiceDate(), OBMessageUtils.messageBD("ParameterMissing") + " "
        + OBMessageUtils.messageBD(Invoice.PROPERTY_INVOICEDATE));
    Check.isNotNull(newInvoice.getPriceList(), OBMessageUtils.messageBD("notnullpricelist"));
    Check.isNotNull(newInvoice.getCurrency(), OBMessageUtils.messageBD("ParameterMissing") + " "
        + OBMessageUtils.messageBD(Invoice.PROPERTY_CURRENCY));
    Check.isNotNull(newInvoice.getPaymentMethod(), newInvoice.getBusinessPartner().getIdentifier()
        + " " + OBMessageUtils.messageBD("PayementMethodNotdefined"));
    Check.isNotNull(newInvoice.getPaymentTerms(), OBMessageUtils.messageBD("notnullpaymentterm"));
  }

  private void processInvoice() {
    if (invoice != null) {
      final List<Object> parameters = new ArrayList<>(2);
      parameters.add(null); // Process Instance parameter
      parameters.add(invoice.getId());
      CallStoredProcedure.getInstance().call("C_INVOICE_POST", parameters, null, false, false);
    }
  }

  /**
   * Resolves the most suitable {@link DocumentType} for a given organization, business partner,
   * and {@code DocBaseType}. The resolution follows a two-step strategy
   * @param org the organization context used to resolve the document type; must not be {@code null}
   * @param bp the business partner whose mapping may override the org default; may be {@code null}
   * @param docBaseType the concrete DocBaseType to resolve (e.g., {@code "ARI"}, {@code "API"}, {@code "SOO"})
   * @param isAutomation whether the caller is an automatic process; when {@code true}, only BP mappings
   *   with {@code isforceautomation = 'Y'} are eligible
   * @return the resolved and loaded {@link DocumentType} entity
   * @throws OBException if no document type can be determined either via BP mapping or {@code AD_GET_DOCTYPE}
   */
  protected DocumentType resolveDocTypeFor(final Organization org, final BusinessPartner bp, final String docBaseType, final boolean isAutomation) {
    String docTypeId = null;
    try {
      docTypeId = new BpDocTypeResolver().resolveId(org.getId(), bp != null ? bp.getId() : null, docBaseType, isAutomation);
    } catch (Exception e) {
      log.warn("DocType resolution failed; falling back to AD_GET_DOCTYPE. org={}, bp={}, dbt={}",
        org.getId(), bp != null ? bp.getId() : "null", docBaseType, e);
    } finally {
      BpDocTypeResolver.clearCache();
    }
    if (StringUtils.isBlank(docTypeId)) {
      final List<Object> parameters = new ArrayList<>(3);
      parameters.add(org.getClient().getId());
      parameters.add(org.getId());
      parameters.add(docBaseType);
      docTypeId = (String) CallStoredProcedure.getInstance().call("AD_GET_DOCTYPE", parameters, null, false);
    }
    if (StringUtils.isBlank(docTypeId)) {
      throw new OBException("There is no Document type defined for DocBaseType " + docBaseType);
    }
    return OBDal.getInstance().get(DocumentType.class, docTypeId);
  }

}
