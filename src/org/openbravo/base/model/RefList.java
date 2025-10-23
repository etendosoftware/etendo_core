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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;

/**
 * A limited mapping of the ref_list to support validation of string types.
 * 
 * @author mtaal
 */
public class RefList extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  private Reference reference;

  private String value;

  /**
   * Intenta registrar el valor enumerado en el DomainType sólo cuando
   * referencia y value están ya inicializados y el domain type es enumerado de String.
   * Evita añadir valores null (que provocaban el [null] observado en la validación).
   */
  private void maybeRegisterEnumeratedValue() {
    if (reference == null || value == null) {
      return; // aún no están ambos disponibles
    }
    final DomainType domainType = reference.getDomainType();
    if (domainType == null) {
      if (log.isDebugEnabled()) {
        log.debug("Reference {} aún no tiene domainType al intentar registrar valor {}", reference.getId(), value);
      }
      return;
    }
    if (domainType instanceof StringEnumerateDomainType) {
      ((StringEnumerateDomainType) domainType).addEnumerateValue(value);
      if (log.isTraceEnabled()) {
        log.trace("Registrado valor enumerado '{}' para reference {}", value, reference.getId());
      }
    } else if (log.isDebugEnabled()) {
      log.debug("Domain type de reference {} no es StringEnumerateDomainType: {}", reference.getId(), domainType.getClass().getName());
    }
  }

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    if (reference == null) {
      this.reference = null;
      log.debug("Hibernate called setReference(null) for RefList during initialization");
      return;
    }
    this.reference = reference;
    // Registro diferido: sólo cuando también exista value (ver maybeRegisterEnumeratedValue)
    maybeRegisterEnumeratedValue();
  }


  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
    // Si la referencia ya fue establecida, intentamos registrar ahora.
    maybeRegisterEnumeratedValue();
  }

}
