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
 * All portions are Copyright (C) 2017-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.erpCommon.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.xml.XMLUtil;
import org.openbravo.erpCommon.ad_callouts.SL_Currency_StdPrecision;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Returns the standard currency precission for a specific currency defined in ISO 4217. If not
 * found in ISO, returns a standard precision of 2.
 *
 * @see SL_Currency_StdPrecision
 *
 * @author markmm82
 * @author jorge-garcia
 */
public class ISOCurrencyPrecision {
  private static Logger log4j = LogManager.getLogger();
  private static final int DEFAULT_CURRENCY_STANDARD_PRECISION = 2;
  private static final String CURRENCY_TAG_NAME = "CcyNtry";
  private static final String CURRENCY_ISO_CODE_TAG_NAME = "Ccy";
  private static final String CURRENCY_PRECISION_TAG_NAME = "CcyMnrUnts";
  private static final String CURRENCY_PRECISION_NOT_ASSIGNED = "N.A.";

  /**
   * Find defined currency precision for specific ISO Code in ISO 4217 XML specification. If there
   * is no currency defined for ISO Code parameter, then it returns 2 as the default standard
   * precision value.
   *
   * @param paramISOCode
   *          Currency ISO Code identifier to be found
   * @return Currency Precision in ISO Specifications or default standard precision used in system
   *         for currency standard precision
   */
  public static int getCurrencyPrecisionInISO4217Spec(String paramISOCode) {
    log4j.debug("Starting getCurrencyPrecisionInISO4217Spec at: {}", new Date());

    try {
      Document doc = getISOCurrencyDocument();
      if (doc != null) {
        // Get all currency nodes
        NodeList ccyList = doc.getElementsByTagName(CURRENCY_TAG_NAME);
        return getPrecisionForCurrencyFromList(paramISOCode, ccyList);
      } else {
        log4j.error("No ISO0417 XML file found.");
        return DEFAULT_CURRENCY_STANDARD_PRECISION;
      }
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    } finally {
      log4j.debug("Ending getCurrencyPrecisionInISO4217Spec at: {}", new Date());
    }
  }

  private static Document getISOCurrencyDocument()
      throws IOException, ParserConfigurationException, SAXException {
    long t1 = System.currentTimeMillis();
    try (InputStream isoXMLDoc = DalContextListener.getServletContext()
        .getResourceAsStream(
            ReportingUtils.getBaseDesign() + "/org/openbravo/erpCommon/ad_callouts/ISO_4217.xml")) {
      if (isoXMLDoc == null) {
        return null;
      }

      DocumentBuilder dBuilder = XMLUtil.getInstance().newDocumentBuilder();
      Document doc = dBuilder.parse(isoXMLDoc);
      doc.getDocumentElement().normalize();
      long t2 = System.currentTimeMillis();
      log4j.debug("createDocumentFromFile took: {} ms", (t2 - t1));
      return doc;
    }
  }

  private static int getPrecisionForCurrencyFromList(String paramISOCode, NodeList ccyList) {
    // Iterate them to find currency specification for currency iso code param
    for (int currentIndex = 0; currentIndex < ccyList.getLength(); currentIndex++) {
      Node ccyNode = ccyList.item(currentIndex);

      // Checks the current node is an ELEMENT NODE
      if (ccyNode.getNodeType() == Node.ELEMENT_NODE) {

        // Gets the ISO Code and precision of current node
        String ccyIsoCode = getFirstValueWithTagName(ccyNode, CURRENCY_ISO_CODE_TAG_NAME);
        String ccyPrecision = getFirstValueWithTagName(ccyNode, CURRENCY_PRECISION_TAG_NAME);

        if (isSameCurrencyAsParameterAndIsPrecisionDefined(paramISOCode, ccyIsoCode,
            ccyPrecision)) {
          return Integer.parseInt(ccyPrecision);
        }
      }
    }
    // If no match has been found or no precision defined, return the default value
    return DEFAULT_CURRENCY_STANDARD_PRECISION;
  }

  private static String getFirstValueWithTagName(Node ccyNode, String tagName) {
    Element element = (Element) ccyNode;
    NodeList tags = element.getElementsByTagName(tagName);
    return tags.getLength() > 0 ? tags.item(0).getTextContent() : null;
  }

  private static boolean isSameCurrencyAsParameterAndIsPrecisionDefined(String paramISOCode,
      String ccyIsoCode, String ccyPrecision) {
    return StringUtils.isNotEmpty(ccyIsoCode) && StringUtils.equals(paramISOCode, ccyIsoCode)
        && StringUtils.isNotEmpty(ccyPrecision)
        && !StringUtils.equals(ccyPrecision, CURRENCY_PRECISION_NOT_ASSIGNED);
  }

}
