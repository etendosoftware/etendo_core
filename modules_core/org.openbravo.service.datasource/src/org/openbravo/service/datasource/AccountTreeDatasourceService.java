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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.datasource;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.financialmgmt.accounting.coa.Element;

/**
 * Tree Datasource for Account Tree
 * 
 */
public class AccountTreeDatasourceService extends ADTreeDatasourceService {
  private static final Logger logger = LogManager.getLogger();
  private static final String DATASOURCE_ID = "D2F94DC86DEC48D69E4BFCE59DC670CF";
  private static final String C_ELEMENTVALUE_TABLE_ID = "188";
  private static final String FINANCIALMGMTELEMENT_ID = "@FinancialMgmtElement.id@";
  private static final String CUSTOM_STRUCTURE = "Custom";

  @Override
  protected Map<String, Object> getDatasourceSpecificParams(Map<String, String> parameters) {
    Map<String, Object> datasourceParams = new HashMap<String, Object>();
    String accountTreeId = parameters.get(FINANCIALMGMTELEMENT_ID);
    if (accountTreeId == null || "null".equals(accountTreeId)) {
      return datasourceParams;
    }
    Element element = OBDal.getInstance().get(Element.class, accountTreeId);
    Tree tree = element.getTree();
    if (tree.getTable() == null) {
      // In case the table is not defined, the C_ElementValue table is assigned to the account tree
      // This prevents a NullPointerException when fetching the account tree nodes
      Table cElementValueTable = OBDal.getInstance().get(Table.class, C_ELEMENTVALUE_TABLE_ID);
      tree.setTable(cElementValueTable);
    }
    datasourceParams.put("tree", tree);
    logger.debug("Retrieved tree for Account Element with id = {}", accountTreeId);
    return datasourceParams;
  }

  @Override
  protected TableTree getTableTree(Table table) {
    TableTree tableTree = null;
    DataSource accountTreeDatasource = OBDal.getInstance().get(DataSource.class, DATASOURCE_ID);
    OBCriteria<TableTree> criteria = OBDal.getInstance().createCriteria(TableTree.class);
    criteria.add(Restrictions.eq(TableTree.PROPERTY_TABLE, table));
    criteria.add(Restrictions.eq(TableTree.PROPERTY_TREESTRUCTURE, CUSTOM_STRUCTURE));
    criteria.add(Restrictions.eq(TableTree.PROPERTY_DATASOURCE, accountTreeDatasource));
    criteria.setMaxResults(1);
    tableTree = (TableTree) criteria.uniqueResult();
    return tableTree;
  }
}
