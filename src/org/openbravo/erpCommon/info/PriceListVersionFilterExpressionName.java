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

package org.openbravo.erpCommon.info;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;

public class PriceListVersionFilterExpressionName implements FilterExpression {
  private Logger log = LogManager.getLogger();
  private Map<String, String> requestMap;
  private HttpSession httpSession;
  private String windowId;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    httpSession = RequestContext.get().getSession();
    windowId = requestMap.get(OBBindingsConstants.WINDOW_ID_PARAM);
    final PriceList priceList = getPriceList();
    if (priceList == null) {
      log.warn("No PriceList found");
      return "";
    }
    Date date = getDate();
    PriceListVersion priceListVersion = getPriceListVersion(priceList, date);
    if (priceListVersion != null) {
      return priceListVersion.getName();
    }
    return "";
  }

  private PriceList getPriceList() {
    PriceList priceList = null;
    if (requestMap.containsKey("inpmPricelistId")) {
      priceList = OBDal.getInstance().get(PriceList.class, requestMap.get("inpmPricelistId"));
    }
    if (priceList != null) {
      return priceList;
    }
    String mPriceListId = (String) httpSession.getAttribute(windowId + "|" + "M_PRICELIST_ID");
    if (StringUtils.isNotEmpty(mPriceListId)) {
      priceList = OBDal.getInstance().get(PriceList.class, mPriceListId);
      if (priceList != null) {
        log.debug("Return priceList obtained from window's session: " + priceList.getIdentifier());
        return priceList;
      }
    }
    priceList = getDefaultPriceList(isSalesTransaction());
    return priceList;
  }

  private PriceList getDefaultPriceList(boolean salesTransaction) {
    final OBCriteria<PriceList> priceListCrit = OBDal.getInstance().createCriteria(PriceList.class);
    priceListCrit.add(Restrictions.eq(PriceList.PROPERTY_SALESPRICELIST, salesTransaction));
    priceListCrit.add(Restrictions.eq(PriceList.PROPERTY_DEFAULT, true));
    String orgs = getOrgs();
    if (StringUtils.isNotEmpty(orgs)) {
      priceListCrit.add(Restrictions.in(PriceList.PROPERTY_ORGANIZATION,
          OBDao.getOBObjectListFromString(Organization.class, orgs)));
      priceListCrit.setFilterOnReadableOrganization(false);
    }
    if (priceListCrit.count() > 0) {
      log.debug(
          "Return client's default PriceList: " + priceListCrit.list().get(0).getIdentifier());
      return priceListCrit.list().get(0);
    }
    return null;
  }

  private Date getDate() {
    Date date = parseDate(requestMap.get("inpDate"));
    if (date != null) {
      log.debug("Return date ordered from request." + date.toString());
      return date;
    }
    date = parseDate((String) httpSession.getAttribute(windowId + "|" + "DATEORDERED"));
    if (date != null) {
      log.debug("Return date ordered from window's session: " + date.toString());
      return date;
    }
    date = parseDate((String) httpSession.getAttribute(windowId + "|" + "DATEINVOICED"));
    if (date != null) {
      log.debug("Return date invoiced from window's session: " + date.toString());
      return date;
    }
    return DateUtils.truncate(new Date(), Calendar.DATE);
  }

  private PriceListVersion getPriceListVersion(PriceList priceList, Date date) {
    OBCriteria<PriceListVersion> plVersionCrit = OBDal.getInstance()
        .createCriteria(PriceListVersion.class);
    plVersionCrit.add(Restrictions.eq(PriceListVersion.PROPERTY_PRICELIST, priceList));
    plVersionCrit.add(Restrictions.le(PriceListVersion.PROPERTY_VALIDFROMDATE, date));
    plVersionCrit.addOrderBy(PriceListVersion.PROPERTY_VALIDFROMDATE, false);
    plVersionCrit.setMaxResults(1);
    return (PriceListVersion) plVersionCrit.uniqueResult();
  }

  private boolean isSalesTransaction() {
    if (requestMap.get(OBBindingsConstants.SO_TRX_PARAM) == null) {
      return true;
    }
    return "Y".equalsIgnoreCase(requestMap.get(OBBindingsConstants.SO_TRX_PARAM));
  }

  private Date parseDate(String date) {
    if (StringUtils.isEmpty(date) || date.equals("null")) {
      return null;
    }
    final SimpleDateFormat dateFormat = new SimpleDateFormat(
        (String) httpSession.getAttribute("#AD_JAVADATEFORMAT"));
    try {
      Date result = dateFormat.parse(date);
      return result;
    } catch (Exception e) {
      log.error("Error parsing string date " + date + " with format: " + dateFormat, e);
    }
    return null;
  }

  private String getOrgs() {
    StringBuffer orgPart = new StringBuffer();
    if (requestMap.containsKey("inpadOrgId")) {
      String orgId = requestMap.get("inpadOrgId");

      if (StringUtils.isNotEmpty(orgId)) {
        final Set<String> orgSet = OBContext.getOBContext()
            .getOrganizationStructureProvider()
            .getNaturalTree(orgId);
        if (orgSet.size() > 0) {
          boolean addComma = false;
          for (String org : orgSet) {
            if (addComma) {
              orgPart.append(",");
            }
            orgPart.append("'" + org + "'");
            addComma = true;
          }
        }
      }
    }
    return orgPart.toString();
  }

}
