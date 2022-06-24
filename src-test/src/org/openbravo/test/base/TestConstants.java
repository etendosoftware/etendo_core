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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.base;

/** Some constants to be used in tests */
public class TestConstants {

  public static class Orgs {
    public static final String MAIN = "0";
    public static final String FB_GROUP = "19404EAD144C49A0AF37D54377CF452D";

    public static final String US = "2E60544D37534C0B89E765FE29BC0B43";
    public static final String US_EST = "7BABA5FF80494CAFA54DEBD22EC46F01";
    public static final String US_WEST = "BAE22373FEBE4CCCA24517E23F0C8A48";

    public static final String ESP = "B843C30461EA4501935CB1D125C9C25A";
    public static final String ESP_SUR = "DC206C91AA6A4897B44DA897936E0EC3";
    public static final String ESP_NORTE = "E443A31992CB4635AFCAEABE7183CE85";
  }

  public static class Clients {
    public static final String SYSTEM = "0";
    public static final String FB_GRP = "23C59575B9CF467C9620760EB255B389";
  }

  public static class Roles {
    public static final String FB_GRP_ADMIN = "42D0EEB1C66F497A90DD526DC597E6F0";
    public static final String ESP_ADMIN = "F3196A30B53A42778727B2852FF90C24";
    public static final String QA_ADMIN_ROLE = "4028E6C72959682B01295A071429011E";
    public static final String SYS_ADMIN = "0";
  }

  public static class Tables {
    public static final String C_ORDER = "259";
  }

  public static class Windows {
    public static final String SALES_ORDER = "143";
    public static final String DISCOUNTS_AND_PROMOTIONS = "800028";
    public static final String SALES_INVOICE = "167";
    public static final String PURCHASE_INVOICE = "183";
  }

  public static class Tabs {
    public static final String SALES_INVOICE_HEADER = "263";
    public static final String PURCHASE_INVOICE_HEADER = "290";
  }

  public static class Entities {
    public static final String COUNTRY = "Country";
  }

  public static class Users {
    public static final String ADMIN = "100";
    public static final String SYSTEM = "0";
  }

  public static class Languages {
    public static final String ES_ES_LANG_ID = "140";
    public static final String ES_ES_ISOCODE = "es_ES";
    public static final String SQ_AL_LANG_ID = "181";
    public static final String SQ_AL_ISOCODE = "sq_AL";
  }

  public static class Modules {
    public static final String ID_CORE = "0";
  }

  private TestConstants() {
  }
}
