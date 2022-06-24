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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallStoredProcedure;

public class FinancialUtils {
  private static final Logger log4j = LogManager.getLogger();

  public static final String PRECISION_STANDARD = "A";
  public static final String PRECISION_COSTING = "C";
  public static final String PRECISION_PRICE = "P";

  /**
   * @see #getProductStdPrice(Product, Date, boolean, PriceList, Currency, Organization)
   */
  public static BigDecimal getProductStdPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final Currency currency, final Organization organization)
      throws OBException {
    return getProductStdPrice(product, date, useSalesPriceList, null, currency, organization);
  }

  /**
   * Calculates the Standard Price of the given Product. It uses the
   * {@link #getProductPrice(Product, Date, boolean, PriceList) getProductPrice()} method to get the
   * ProductPrice to be used. In case a conversion is needed it uses the
   * {@link #getConvertedAmount(BigDecimal, Currency, Currency, Date, Organization, String)
   * getConvertedAmount()} method.
   * 
   * @param product
   *          Product to get its ProductPrice.
   * @param date
   *          Date when Product Price is needed.
   * @param useSalesPriceList
   *          boolean to set if the price list should be a sales or purchase price list.
   * @param pricelist
   *          PriceList to get its ProductPrice
   * @param currency
   *          Currency to convert to the returned price.
   * @param organization
   *          Organization where price needs to be used to retrieve the proper conversion rate.
   * @return a BigDecimal with the Standard Price of the Product for the given parameters.
   * @throws OBException
   *           when no valid ProductPrice is found.
   */
  public static BigDecimal getProductStdPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final PriceList pricelist, final Currency currency,
      final Organization organization) throws OBException {
    final ProductPrice pp = getProductPrice(product, date, useSalesPriceList, pricelist);
    BigDecimal price = pp.getStandardPrice();
    if (!pp.getPriceListVersion().getPriceList().getCurrency().getId().equals(currency.getId())) {
      // Conversion is needed.
      price = getConvertedAmount(price, pp.getPriceListVersion().getPriceList().getCurrency(),
          currency, date, organization, PRECISION_PRICE);
    }

    return price;
  }

  /**
   * @see #getProductPrice(Product, Date, boolean, PriceList, boolean)
   */
  public static ProductPrice getProductPrice(final Product product, final Date date,
      final boolean useSalesPriceList) throws OBException {
    return getProductPrice(product, date, useSalesPriceList, null, true);
  }

  /**
   * @see #getProductPrice(Product, Date, boolean, PriceList, boolean)
   */
  public static ProductPrice getProductPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final PriceList priceList) throws OBException {
    return getProductPrice(product, date, useSalesPriceList, priceList, true);
  }

  /**
   * @see #getProductPrice(Product, Date, boolean, PriceList, boolean, boolean)
   */
  public static ProductPrice getProductPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final PriceList priceList, final boolean throwException)
      throws OBException {
    return getProductPrice(product, date, useSalesPriceList, priceList, throwException, true);
  }

  /**
   * Method to get a valid ProductPrice for the given Product. It only considers PriceList versions
   * valid on the given date. If a PriceList is given it searches on that one. If PriceList null is
   * passed it search on any Sales or Purchase PriceList based on the useSalesPriceList.
   * 
   * @param product
   *          Product to get its ProductPrice.
   * @param date
   *          Date when Product Price is needed.
   * @param useSalesPriceList
   *          boolean to set if the price list should be a sales or purchase price list.
   * @param priceList
   *          PriceList to get its ProductPrice
   * @param throwException
   *          boolean to determine if an exception has to be thrown when no pricelist is found.
   * @param usePriceIncludeTax
   *          boolean to set if price lists including taxes should be considered or not.
   * @return a valid ProductPrice for the given parameters. Null is no exception is to be thrown.
   * @throws OBException
   *           when no valid ProductPrice is found and throwException is true.
   */
  public static ProductPrice getProductPrice(final Product product, final Date date,
      final boolean useSalesPriceList, final PriceList priceList, final boolean throwException,
      final boolean usePriceIncludeTax) throws OBException {
    //@formatter:off
    String hql =
            "as pp" +
            "  join pp.priceListVersion as plv" +
            "  join plv.priceList as pl" +
            " where pp.product.id = :productId" +
            "   and plv.validFromDate <= :date";
    //@formatter:on
    if (priceList != null) {
      //@formatter:off
      hql +=
            "   and pl.id = :pricelistId";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and pl.salesPriceList = :salespricelist";
      //@formatter:on
    }
    if (!usePriceIncludeTax) {
      //@formatter:off
      hql +=
            "   and pl.priceIncludesTax = false";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " order by pl.default desc" +
            "   , plv.validFromDate desc";
    //@formatter:on

    final OBQuery<ProductPrice> ppQry = OBDal.getInstance()
        .createQuery(ProductPrice.class, hql)
        .setNamedParameter("productId", product.getId())
        .setNamedParameter("date", date);
    if (priceList != null) {
      ppQry.setNamedParameter("pricelistId", priceList.getId());
    } else {
      ppQry.setNamedParameter("salespricelist", useSalesPriceList);
    }

    final List<ProductPrice> ppList = ppQry.list();
    if (ppList.isEmpty()) {
      // No product price found.
      if (throwException) {
        throw new OBException("@PriceListVersionNotFound@. @Product@: " + product.getIdentifier()
            + " @Date@: " + OBDateUtils.formatDate(date));
      } else {
        return null;
      }
    }
    return ppList.get(0);
  }

  /**
   * Method to get the conversion rate defined at system level. If there is not a conversion rate
   * defined on the given Organization it is searched recursively on its parent organization until
   * one is found. If no conversion rate is found null is returned.
   * 
   * @param date
   *          Date conversion is being performed.
   * @param fromCurrency
   *          Currency to convert from.
   * @param toCurrency
   *          Currency to convert to.
   * @param org
   *          Organization of the document that needs to be converted.
   * @return a valid ConversionRate for the given parameters, null if none is found.
   */
  public static ConversionRate getConversionRate(final Date date, final Currency fromCurrency,
      final Currency toCurrency, final Organization org, final Client client) {
    ConversionRate conversionRate;
    // Conversion rate records do not get into account timestamp.
    final Date dateWithoutTimestamp = DateUtils.setHours(
        DateUtils.setMinutes(DateUtils.setSeconds(DateUtils.setMilliseconds(date, 0), 0), 0), 0);
    // Readable Client Org filters to false as organization is filtered explicitly.
    OBContext.setAdminMode(false);
    try {
      final OBCriteria<ConversionRate> obcConvRate = OBDal.getInstance()
          .createCriteria(ConversionRate.class);
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_ORGANIZATION, org));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_CLIENT, client));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_CURRENCY, fromCurrency));
      obcConvRate.add(Restrictions.eq(ConversionRate.PROPERTY_TOCURRENCY, toCurrency));
      obcConvRate.add(Restrictions.le(ConversionRate.PROPERTY_VALIDFROMDATE, dateWithoutTimestamp));
      obcConvRate.add(Restrictions.ge(ConversionRate.PROPERTY_VALIDTODATE, dateWithoutTimestamp));
      obcConvRate.setFilterOnReadableClients(false);
      obcConvRate.setFilterOnReadableOrganization(false);
      conversionRate = (ConversionRate) obcConvRate.uniqueResult();
      if (conversionRate != null) {
        return conversionRate;
      }
      if ("0".equals(org.getId())) {
        return null;
      } else {
        return getConversionRate(date, fromCurrency, toCurrency,
            OBContext.getOBContext()
                .getOrganizationStructureProvider(client.getId())
                .getParentOrg(org),
            client);
      }
    } catch (final Exception e) {
      log4j.error("Exception calculating conversion rate.", e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It calls
   * {@link FinancialUtils#getConvertedAmount(BigDecimal, Currency, Currency, Date, Organization, String, List)}
   * with an empty list of conversion rates at document level
   */
  public static BigDecimal getConvertedAmount(final BigDecimal amount, final Currency curFrom,
      final Currency curTo, final Date date, final Organization org, final String strPrecision)
      throws OBException {
    return getConvertedAmount(amount, curFrom, curTo, date, org, strPrecision,
        Collections.<ConversionRateDoc> emptyList());
  }

  /**
   * Converts an amount.
   * 
   * @param amount
   *          BigDecimal amount to convert.
   * @param curFrom
   *          Currency to convert from.
   * @param curTo
   *          Currency to convert to.
   * @param date
   *          Date conversion is being performed.
   * @param org
   *          Organization of the document that needs to be converted.
   * @param strPrecision
   *          type of precision to be used to round the converted amount.
   * @param rateDocs
   *          list of conversion rates defined on the document of the amount that needs to be
   *          converted.
   * @return a BigDecimal representing the converted amount.
   * @throws OBException
   *           when no Conversion Rate is found for the given parameters.
   */
  public static BigDecimal getConvertedAmount(final BigDecimal amount, final Currency curFrom,
      final Currency curTo, final Date date, Organization org, final String strPrecision,
      final List<ConversionRateDoc> rateDocs) throws OBException {
    Check.isNotNull(rateDocs, OBMessageUtils.messageBD("ParameterMissing") + " (rateDocs)");
    if (curFrom.getId().equals(curTo.getId()) || amount.signum() == 0) {
      return amount;
    }
    BigDecimal rate = null;
    if (!rateDocs.isEmpty()) {
      for (ConversionRateDoc rateDoc : rateDocs) {
        if (curFrom.getId().equals(rateDoc.getCurrency().getId())
            && curTo.getId().equals(rateDoc.getToCurrency().getId())) {
          rate = rateDoc.getRate();
          break;
        }
      }
    }
    if (rate == null) {
      final ConversionRate cr = getConversionRate(date, curFrom, curTo, org, org.getClient());
      if (cr == null) {
        throw new OBException("@NoCurrencyConversion@ " + curFrom.getISOCode() + " @to@ "
            + curTo.getISOCode() + " @ForDate@ " + OBDateUtils.formatDate(date)
            + " @And@ @ACCS_AD_ORG_ID_D@ " + org.getIdentifier());
      }
      rate = cr.getMultipleRateBy();
    }
    Long precision = curTo.getStandardPrecision();
    if (PRECISION_COSTING.equals(strPrecision)) {
      precision = curTo.getCostingPrecision();
    } else if (PRECISION_PRICE.equals(strPrecision)) {
      precision = curTo.getPricePrecision();
    }
    return amount.multiply(rate).setScale(precision.intValue(), RoundingMode.HALF_UP);
  }

  /**
   * Returns the Currency of a Legal Entity. If there is no one defined, returns the currency of the
   * Client.
   */
  public static Currency getLegalEntityCurrency(final Organization organization) {
    final Organization legalEntity = OBContext.getOBContext()
        .getOrganizationStructureProvider(organization.getClient().getId())
        .getLegalEntity(organization);
    return (legalEntity.getCurrency() != null) ? legalEntity.getCurrency()
        : organization.getClient().getCurrency();
  }

  /**
   * Calculates the net unit price using the C_GET_NET_PRICE_FROM_GROSS stored procedure.
   * 
   * @param strTaxId
   *          Tax that applies to the price.
   * @param grossAmount
   *          Gross Amount to calculate the net unit price from.
   * @param pricePrecision
   *          Precision to round the result to.
   * @param alternateAmount
   *          alternate amount in case the tax uses it.
   * @param quantity
   *          number of units to divide the amount to get the price.
   * @return the net unit price
   * 
   * @deprecated Use {@link #calculateNetAmtFromGross} instead
   */
  @Deprecated
  public static BigDecimal calculateNetFromGross(final String strTaxId,
      final BigDecimal grossAmount, final int pricePrecision, final BigDecimal alternateAmount,
      final BigDecimal quantity) {
    if (grossAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    final List<Object> parameters = new ArrayList<>();
    parameters.add(strTaxId);
    parameters.add(grossAmount);
    // TODO: Alternate Base Amount
    parameters.add(alternateAmount);
    parameters.add(pricePrecision);
    parameters.add(quantity);

    final String procedureName = "C_GET_NET_PRICE_FROM_GROSS";
    return (BigDecimal) CallStoredProcedure.getInstance().call(procedureName, parameters, null);
  }

  /**
   * Calculates the net unit price using the C_GET_NET_AMOUNT_FROM_GROSS stored procedure.
   * 
   * @param strTaxId
   *          Tax that applies to the price.
   * @param grossAmount
   *          Gross Amount to calculate the net unit price from.
   * @param stdPrecision
   *          Standard to round the result to.
   * @param alternateAmount
   *          alternate amount in case the tax uses it.
   * @return the net unit price
   */
  public static BigDecimal calculateNetAmtFromGross(final String strTaxId,
      final BigDecimal grossAmount, final int stdPrecision, final BigDecimal alternateAmount) {
    if (grossAmount.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }

    final List<Object> parameters = new ArrayList<>();
    parameters.add(strTaxId);
    parameters.add(grossAmount);
    parameters.add(alternateAmount);
    parameters.add(stdPrecision);

    final String procedureName = "C_GET_NET_AMOUNT_FROM_GROSS";
    return (BigDecimal) CallStoredProcedure.getInstance().call(procedureName, parameters, null);
  }

  /**
   * Get all the payment details with available credit
   */
  public static ScrollableResults getPaymentsWithCredit(final String businessPartnerId,
      final String currencyId) {
    //@formatter:off
    final String hql =
                  "select t1.id" +
                  "  from FIN_Payment as t1" +
                  " where t1.businessPartner.id = :businessPartnerId" +
                  "   and t1.currency.id = :currencyId" +
                  "   and t1.generatedCredit <> 0" +
                  "   and t1.generatedCredit <> t1.usedCredit";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("businessPartnerId", businessPartnerId)
        .setParameter("currencyId", currencyId)
        .scroll(ScrollMode.SCROLL_SENSITIVE);
  }
}
