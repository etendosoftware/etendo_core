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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_callouts;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.geography.Location;

public class SL_BPartnerLocation extends SimpleCallout {
  private static Logger log = LogManager.getLogger();

  private static final RequestFilter filterYesNo = new ValueListFilter("Y", "N");

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    checkTaxLocation(info);
  }

  /**
   * Shows a warning if any of these conditions is fulfilled for the business partner:
   * <ul>
   * <li>More than 1 country set as Tax Location</li>
   * <li>Other country is declared as Tax Location and the user is trying to set a new country as
   * Tax Location</li>
   * <li>The business partner has several countries and neither of them is declared as Tax
   * Location</li>
   * </ul>
   * 
   */
  private void checkTaxLocation(final CalloutInfo info) {
    final String locationId = info.getStringParameter("inpcLocationId", IsIDFilter.instance);

    try {
      OBContext.setAdminMode(true);
      final Location location = OBDal.getInstance().get(Location.class, locationId);
      if (location != null) {
        final String isTaxLocation = info.getStringParameter("inpistaxlocation", filterYesNo);
        final String bPartnerId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);

        String hql = "" + //
            "select count(distinct c.id) " + //
            "from BusinessPartnerLocation bpl " + //
            "inner join bpl.locationAddress as ad " + //
            "inner join ad.country as c " + //
            "where bpl.businessPartner.id = :bPartnerId " + //
            "and bpl.taxLocation = true " + //
            "and c.id <> :countryId "; //
        Query<Long> query = OBDal.getInstance()
            .getSession()
            .createQuery(hql, Long.class);
        query.setParameter("bPartnerId", bPartnerId);
        query.setParameter("countryId", location.getCountry().getId());
        int otherCountriesTaxLocationYes = query.list().get(0).intValue();

        if (otherCountriesTaxLocationYes > 1) {
          // Detected several countries defined as Tax Location
          info.addResult("WARNING", OBMessageUtils
              .messageBD("BusinessPartnerTaxLocation_SeveralCountriesWithTaxLocation"));
        } else if (StringUtils.equals("Y", isTaxLocation) && otherCountriesTaxLocationYes == 1) {
          // Detected other previous country defined as Tax Location
          info.addResult("WARNING",
              OBMessageUtils.messageBD("BusinessPartnerTaxLocation_OtherCountryWithTaxLocation"));
        } else if (StringUtils.equals("N", isTaxLocation) && otherCountriesTaxLocationYes == 0) {
          hql = "" + //
              "select count(distinct c.id) " + //
              "from BusinessPartnerLocation bpl " + //
              "inner join bpl.locationAddress as ad " + //
              "inner join ad.country as c " + //
              "where bpl.businessPartner.id = :bPartnerId " + //
              "and c.id <> :countryId "; //
          query = OBDal.getInstance().getSession().createQuery(hql, Long.class);
          query.setParameter("bPartnerId", bPartnerId);
          query.setParameter("countryId", location.getCountry().getId());
          int otherCountries = query.list().get(0).intValue();

          if (otherCountries > 0) {
            hql = "" + //
                "select count(*) " + //
                "from BusinessPartnerLocation bpl " + //
                "inner join bpl.locationAddress as ad " + //
                "inner join ad.country as c " + //
                "where bpl.businessPartner.id = :bPartnerId " + //
                "and c.id = :countryId " + //
                "and bpl.taxLocation = true ";
            query = OBDal.getInstance().getSession().createQuery(hql, Long.class);
            query.setParameter("bPartnerId", bPartnerId);
            query.setParameter("countryId", location.getCountry().getId());
            int thisCountryTaxLocationYes = query.list().get(0).intValue();

            if (thisCountryTaxLocationYes == 0) {
              // Detected several countries, neither of them defined as Tax Location
              info.addResult("WARNING", OBMessageUtils
                  .messageBD("BusinessPartnerTaxLocation_NoTaxLocation_SeveralCountries"));
            }
          }
        }
      }
    } catch (Exception e) {
      // Log and don't do anything
      log.error("Exception in SL_BPartnerLocation.checkTaxLocation()", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
