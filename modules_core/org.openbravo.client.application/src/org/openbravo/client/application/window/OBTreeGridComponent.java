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
 * All portions are Copyright (C) 2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import org.openbravo.client.kernel.BaseTemplateComponent;
import org.openbravo.client.kernel.Template;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TableTree;

/**
 * The backing bean for generating the OBTreeGridPopup client-side representation.
 * 
 * @author AugustoMauch
 */
public class OBTreeGridComponent extends BaseTemplateComponent {

  private static final String DEFAULT_TEMPLATE_ID = "74451C30650946FC855FCFDB4577070C";

  private static final String TREENODE_DATASOURCE = "90034CAE96E847D78FBEF6D38CB1930D";
  private static final String LINKTOPARENT_DATASOURCE = "610BEAE5E223447DBE6FF672B703F72F";

  private static final String TREENODE_STRUCTURE = "ADTree";
  private static final String LINKTOPARENT_STRUCTURE = "LinkToParent";

  private Tab tab;
  private OBViewTab viewTab;

  @Override
  protected Template getComponentTemplate() {
    return OBDal.getInstance().get(Template.class, DEFAULT_TEMPLATE_ID);
  }

  public Tab getTab() {
    return tab;
  }

  public void setTab(Tab tab) {
    this.tab = tab;
  }

  public OBViewTab getViewTab() {
    return viewTab;
  }

  public void setViewTab(OBViewTab viewTab) {
    this.viewTab = viewTab;
  }

  public String getReferencedTableId() {
    return tab.getTable().getId();
  }

  public boolean isOrderedTree() {
    TableTree tableTree = tab.getTableTree();
    if (tableTree != null) {
      return tableTree.isOrdered();
    } else {
      return false;
    }
  }

  public boolean isCanReorderRecords() {
    String uiPattern = tab.getUIPattern();
    boolean isReadOnlyTree = tab.isReadOnlyTree();
    if (uiPattern.equals("RO") || isReadOnlyTree) {
      return false;
    } else {
      return true;
    }
  }

  public boolean isShowNodeIcons() {
    String uiPattern = tab.getUIPattern();
    boolean isShowTreeNodeIcons = tab.isShowTreeNodeIcons();
    boolean isReadOnlyTree = tab.isReadOnlyTree();
    if ((uiPattern.equals("RO") || isReadOnlyTree) && !isShowTreeNodeIcons) {
      return false;
    } else {
      return true;
    }
  }

  public boolean isApplyWhereClauseToChildren() {
    TableTree tableTree = tab.getTableTree();
    return tableTree.isApplyWhereClauseToChildNodes();
  }

  public String getDataSourceId() {
    String dataSourceId = null;
    TableTree tableTree = tab.getTableTree();
    if (tableTree != null) {
      if (TREENODE_STRUCTURE.equals(tableTree.getTreeStructure())) {
        dataSourceId = TREENODE_DATASOURCE;
      } else if (LINKTOPARENT_STRUCTURE.equals(tableTree.getTreeStructure())) {
        dataSourceId = LINKTOPARENT_DATASOURCE;
      } else {
        return tableTree.getDatasource().getId();
      }
      return dataSourceId;
    } else {
      return null;
    }
  }

  public String getTreeStructure() {
    TableTree tableTree = tab.getTableTree();
    if (tableTree != null) {
      return tableTree.getTreeStructure();
    } else {
      return null;
    }
  }
}
