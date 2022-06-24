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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.service.datasource.hql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;

/**
 * This class allows to define a qualifier used to register a HQL injection component provider. This
 * qualifier is composed of two attributes:
 * 
 * - tableId: the ID of the HQL table to be used in the HQL insertion
 * 
 * - injectionId: the ID of the injection point in the HQL table
 */
public class HQLInserterQualifier {

  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
  public @interface Qualifier {
    String tableId();

    String injectionId();
  }

  /**
   * A class used to select an injection component provider based on the tableId and the injection
   * id. For instance, the id of the injection point @injection_point_0@ is 0
   */
  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<HQLInserterQualifier.Qualifier>
      implements HQLInserterQualifier.Qualifier {
    private static final long serialVersionUID = 1L;

    final String tableId;
    final String injectionId;

    public Selector(String tableId, String injectionId) {
      this.tableId = tableId;
      this.injectionId = injectionId;
    }

    @Override
    public String tableId() {
      return tableId;
    }

    @Override
    public String injectionId() {
      return injectionId;
    }
  }
}
