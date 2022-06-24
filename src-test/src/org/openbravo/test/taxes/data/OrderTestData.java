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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes.data;

import java.util.ArrayList;
import java.util.List;

public class OrderTestData {

  private final String customer;
  private final String location;
  private final String priceList;
  private final String expectedNet;
  private final String expectedGross;
  private final String expectedNet2;
  private final String expectedGross2;
  private final List<OrderLineTestData> lines;
  private final List<OrderLineRelTestData> relations;

  public OrderTestData(final String customer, final String location, final String priceList,
      final String expectedNet, final String expectedGross, final String expectedNet2,
      final String expectedGross2, final List<OrderLineTestData> lines,
      final List<OrderLineRelTestData> relations) {
    this.customer = customer;
    this.location = location;
    this.priceList = priceList;
    this.expectedNet = expectedNet;
    this.expectedGross = expectedGross;
    this.expectedNet2 = expectedNet2;
    this.expectedGross2 = expectedGross2;
    this.lines = lines;
    this.relations = relations;
  }

  public OrderTestData(final String customer, final String location, final String priceList,
      final String expectedNet, final String expectedGross, final String expectedNet2,
      final String expectedGross2, final List<OrderLineTestData> lines) {
    this(customer, location, priceList, expectedNet, expectedGross, expectedNet2, expectedGross2,
        lines, new ArrayList<OrderLineRelTestData>());
  }

  public String getLocation() {
    return location;
  }

  public String getCustomer() {
    return customer;
  }

  public String getPriceList() {
    return priceList;
  }

  public String getExpectedNet() {
    return expectedNet;
  }

  public String getExpectedGross() {
    return expectedGross;
  }

  public String getExpectedNet2() {
    return expectedNet2;
  }

  public String getExpectedGross2() {
    return expectedGross2;
  }

  public List<OrderLineTestData> getLines() {
    return lines;
  }

  public List<OrderLineRelTestData> getRelations() {
    return relations;
  }
}
