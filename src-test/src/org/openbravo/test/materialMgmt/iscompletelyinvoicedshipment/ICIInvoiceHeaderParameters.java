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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import java.util.Date;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;

public class ICIInvoiceHeaderParameters {

  private Client client;
  private Organization organization;
  private BusinessPartner businessPartner;
  private Location businessPartnerLocation;
  private boolean isReceipt;
  private DocumentType documentType;
  private Currency currency;
  private PriceList priceList;
  private FIN_PaymentMethod paymentMethod;
  private PaymentTerm paymentTerms;
  private Date accountingDate;
  private Date invoiceDate;
  private Date taxDate;

  public ICIInvoiceHeaderParameters() {
    this.client = OBContext.getOBContext().getCurrentClient();
    this.organization = OBContext.getOBContext().getCurrentOrganization();
    this.businessPartner = OBDal.getInstance()
        .get(BusinessPartner.class, ICIConstants.BUSINESS_PARTNER_ID);
    this.businessPartnerLocation = this.businessPartner.getBusinessPartnerLocationList().get(0);
    this.isReceipt = true;
    this.documentType = OBDal.getInstance()
        .get(DocumentType.class, ICIConstants.INVOICE_DOCTYPE_ID);
    this.currency = OBDal.getInstance().get(Currency.class, ICIConstants.EURO_ID);
    this.priceList = OBDal.getInstance().get(PriceList.class, ICIConstants.PRICELIST_ID);
    this.paymentMethod = this.businessPartner.getPaymentMethod();
    this.paymentTerms = OBDal.getInstance().get(PaymentTerm.class, ICIConstants.PAYMENTTERM_ID);
    this.accountingDate = new Date();
    this.invoiceDate = new Date();
    this.taxDate = new Date();
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public BusinessPartner getBusinessPartner() {
    return businessPartner;
  }

  public void setBusinessPartner(BusinessPartner businessPartner) {
    this.businessPartner = businessPartner;
  }

  public Location getBusinessPartnerLocation() {
    return businessPartnerLocation;
  }

  public void setBusinessPartnerLocation(Location businessPartnerLocation) {
    this.businessPartnerLocation = businessPartnerLocation;
  }

  public boolean isReceipt() {
    return isReceipt;
  }

  public void setReceipt(boolean isReceipt) {
    this.isReceipt = isReceipt;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }

  public void setDocumentType(DocumentType documentType) {
    this.documentType = documentType;
  }

  public Currency getCurrency() {
    return currency;
  }

  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  public PriceList getPriceList() {
    return priceList;
  }

  public void setPriceList(PriceList priceList) {
    this.priceList = priceList;
  }

  public FIN_PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(FIN_PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public PaymentTerm getPaymentTerms() {
    return paymentTerms;
  }

  public void setPaymentTerms(PaymentTerm paymentTerms) {
    this.paymentTerms = paymentTerms;
  }

  public Date getAccountingDate() {
    return accountingDate;
  }

  public void setAccountingDate(Date accountingDate) {
    this.accountingDate = accountingDate;
  }

  public Date getInvoiceDate() {
    return invoiceDate;
  }

  public void setInvoiceDate(Date invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public Date getTaxDate() {
    return taxDate;
  }

  public void setTaxDate(Date taxDate) {
    this.taxDate = taxDate;
  }

}
