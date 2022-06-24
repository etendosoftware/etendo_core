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
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Calendar;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

public class AccDefUtility {
  static Logger log4j = LogManager.getLogger();

  public static Calendar getCalendar(Organization organization) {
    if ("0".equals(organization.getId())) {
      log4j.error("SL_Invoice_Product - No calendar defined for organization");
      return null;
    }
    if (organization.getCalendar() != null) {
      return organization.getCalendar();
    } else {
      return getCalendar(new OrganizationStructureProvider().getParentOrg(organization));
    }
  }

  public static Period getCurrentPeriod(Date date, Calendar fiscalCalendar) {
    OBCriteria<Period> obc = OBDal.getInstance().createCriteria(Period.class);
    obc.createAlias(Period.PROPERTY_YEAR, "y");
    obc.add(Restrictions.eq("y." + Year.PROPERTY_CALENDAR, fiscalCalendar));
    obc.add(Restrictions.ge(Period.PROPERTY_ENDINGDATE, date));
    obc.add(Restrictions.ne(Period.PROPERTY_PERIODTYPE, "A"));
    obc.add(Restrictions.le(Period.PROPERTY_STARTINGDATE, date));
    obc.addOrderBy(Period.PROPERTY_PERIODNO, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnReadableClients(false);
    List<Period> periods = obc.list();
    if (periods.size() == 0) {
      log4j.error("AccDefUtility - No period defined for invoice date");
      return null;
    } else {
      return periods.get(0);
    }
  }

  public static Period getNextPeriod(Period period) {
    OBCriteria<Period> obc = OBDal.getInstance().createCriteria(Period.class);
    obc.add(Restrictions.eq(Period.PROPERTY_YEAR, period.getYear()));
    obc.add(Restrictions.ne(Period.PROPERTY_PERIODTYPE, "A"));
    obc.addOrderBy(Period.PROPERTY_PERIODNO, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnReadableClients(false);
    Period targetPeriod = null;
    if (period.equals(obc.list().get(0))) {
      targetPeriod = getFirstPeriodOfNextYear(period.getYear());
    } else {
      for (Period p : obc.list()) {
        if (p == period) {
          return targetPeriod;
        }
        targetPeriod = p;
      }
    }
    return targetPeriod;
  }

  public static Period getFirstPeriodOfNextYear(Year year) {
    OBCriteria<Period> obc = OBDal.getInstance().createCriteria(Period.class);
    obc.add(Restrictions.eq(Period.PROPERTY_YEAR, getNextYear(year)));
    obc.add(Restrictions.ne(Period.PROPERTY_PERIODTYPE, "A"));
    obc.addOrderBy(Period.PROPERTY_PERIODNO, true);
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnReadableClients(false);
    List<Period> periods = obc.list();
    if (periods.size() == 0) {
      throw new OBException("AccDefUtility - Error getting next year period");
    }
    return periods.get(0);
  }

  public static Year getNextYear(Year year) {
    OBCriteria<Year> obc = OBDal.getInstance().createCriteria(Year.class);
    obc.add(Restrictions.eq(Year.PROPERTY_CALENDAR, year.getCalendar()));
    obc.addOrderBy(Year.PROPERTY_FISCALYEAR, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setFilterOnReadableClients(false);
    Year targetYear = null;
    if (year.equals(obc.list().get(0))) {
      throw new OBException("AccDefUtility - Error getting next year period");
    }
    for (Year y : obc.list()) {
      if (y == year) {
        return targetYear;
      }
      targetYear = y;
    }
    return targetYear;
  }

  public static HashMap<String, String> getDeferredPlanForInvoiceProduct(String invoiceId,
      String productId) {
    // Calculate Acc and Def Plan from Product
    HashMap<String, String> result = new HashMap<String, String>();
    String planType = "";
    String periodNumber = "";
    String startingPeriodId = "";
    boolean isSOTRX = false;
    if (!"".equals(invoiceId) && !"".equals(productId)) {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);
      Product product = OBDal.getInstance().get(Product.class, productId);
      final String CURRENT_MONTH = "C";
      final String NEXT_MONTH = "N";
      if (invoice != null) {
        isSOTRX = invoice.isSalesTransaction();
      }
      if (isSOTRX && product.isDeferredRevenue()) {
        if (CURRENT_MONTH.equals(product.getDefaultPeriod())) {
          startingPeriodId = AccDefUtility
              .getCurrentPeriod(invoice.getAccountingDate(),
                  AccDefUtility.getCalendar(invoice.getOrganization()))
              .getId();
        } else if (NEXT_MONTH.equals(product.getDefaultPeriod())) {
          startingPeriodId = AccDefUtility
              .getNextPeriod(AccDefUtility.getCurrentPeriod(invoice.getAccountingDate(),
                  AccDefUtility.getCalendar(invoice.getOrganization())))
              .getId();
        }
        if (startingPeriodId != null && !"".equals(startingPeriodId)) {
          planType = product.getRevenuePlanType();
          periodNumber = product.getPeriodNumber().toString();
        }
      } else if (!isSOTRX && product.isDeferredexpense()) {
        if (CURRENT_MONTH.equals(product.getDefaultPeriodExpense())) {
          startingPeriodId = AccDefUtility
              .getCurrentPeriod(invoice.getAccountingDate(),
                  AccDefUtility.getCalendar(invoice.getOrganization()))
              .getId();
        } else if (NEXT_MONTH.equals(product.getDefaultPeriodExpense())) {
          startingPeriodId = AccDefUtility
              .getNextPeriod(AccDefUtility.getCurrentPeriod(invoice.getAccountingDate(),
                  AccDefUtility.getCalendar(invoice.getOrganization())))
              .getId();
        }
        if (startingPeriodId != null && !"".equals(startingPeriodId)) {
          planType = product.getExpplantype();
          periodNumber = product.getPeriodnumberExp().toString();
        }
      }
    }
    result.put("planType", planType);
    result.put("periodNumber", periodNumber);
    result.put("startingPeriodId", startingPeriodId);
    return result;
  }
}
