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
 * All portions are Copyright (C) 2009-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.json.JsonConstants;

/**
 * This call out computes the columnid of a Selector Field.
 * 
 * @author mtaal
 */
public class SelectorFieldPropertyCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String selectorID = info.getStringParameter("inpobuiselSelectorId", IsIDFilter.instance)
        .trim();
    final String property = info.getStringParameter("inpproperty").trim();
    final Selector selector = OBDal.getInstance().get(Selector.class, selectorID);
    final Table table;
    if (selector.getTable() != null) {
      table = selector.getTable();
    } else if (selector.getObserdsDatasource() != null
        && selector.getObserdsDatasource().getTable() != null) {
      table = selector.getObserdsDatasource().getTable();
    } else {
      // no table don't do anything
      return;
    }
    // some cases:
    // name --> name column of current table
    // bankAccount.bank.name --> bank column of bankAccount
    final Entity entity = ModelProvider.getInstance().getEntity(table.getName());

    Property foundProperty = null;
    if (StringUtils.equals(property, JsonConstants.IDENTIFIER)) {
      if (entity.getIdentifierProperties().isEmpty()) {
        // no properties don't do anything
        return;
      }
      foundProperty = entity.getIdentifierProperties().get(0);
    } else {
      final String[] parts = property.split("\\.");
      Entity currentEntity = entity;
      Property currentProperty = null;
      for (String part : parts) {
        if (StringUtils.isEmpty(part)) {
          return;
        }
        if (StringUtils.equals(part, JsonConstants.IDENTIFIER)
            || StringUtils.equals(part, JsonConstants.ID)) {
          if (foundProperty == null) {
            return;
          }
          break;
        }
        currentProperty = currentEntity.getProperty(part);
        foundProperty = currentProperty;
        if (currentProperty.isPrimitive()) {
          break;
        }

        if (StringUtils.equals(currentProperty.getName(), Entity.COMPUTED_COLUMNS_PROXY_PROPERTY)) {
          currentEntity = ModelProvider.getInstance()
              .getEntity(currentEntity.getName() + Entity.COMPUTED_COLUMNS_CLASS_APPENDIX);
        } else {
          currentEntity = foundProperty.getTargetEntity();
        }
      }
    }

    // retrieve the column id
    OBContext.setAdminMode();
    try {
      // get the table
      final Entity propertyEntity = foundProperty.getEntity();

      String tableId = propertyEntity.getTableId();

      if (propertyEntity.isVirtualEntity()) {
        // If it is a virtual entity, that means that the variable tableId will be
        // the id of the table concatenated to the "_CC" substring, this is why the
        // tableId variable is get by removing the last three characters.
        tableId = tableId.substring(0, tableId.length() - 3);
      }

      final Table propertyTable = OBDal.getInstance().getProxy(Table.class, tableId);

      final OBCriteria<Column> columnCriteria = OBDal.getInstance().createCriteria(Column.class);
      columnCriteria.add(Restrictions.and(Restrictions.eq(Column.PROPERTY_TABLE, propertyTable),
          Restrictions.eq(Column.PROPERTY_DBCOLUMNNAME, foundProperty.getColumnName())));
      final List<Column> columnList = columnCriteria.list();
      if (columnList.isEmpty()) {
        // No columns, don't do anything
        return;
      }

      // Update the column ID
      info.addResult("inpadColumnId", columnList.get(0).getId());

    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
