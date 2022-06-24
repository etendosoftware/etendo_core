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
 * All portions are Copyright (C) 2012-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

class ElementValueEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ElementValue.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ElementValue account = (ElementValue) event.getTargetInstance();
    // If value is not a number account will be folded in the root directory of the tree. So do
    // nothing, DB trigger will manage
    try {
      new BigInteger(account.getSearchKey());
    } catch (NumberFormatException e) {
      return;
    }
    // Skip for initial client setup and initial org setup;
    // - Initial organization setup: Organization is not yet Ready
    // - Initial Client Setup: Readable client list just contains system client ('0')
    // Skip is required as accounts come with a tree definition
    OBContext.setAdminMode();
    try {
      if (!account.getOrganization().isReady() || !ArrayUtils
          .contains(OBContext.getOBContext().getReadableClients(), account.getClient().getId())) {
        return;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    doIt(account);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    if (event.getPreviousState(getValueProperty())
        .equals(event.getCurrentState(getValueProperty()))) {
      return;
    }
    final ElementValue account = (ElementValue) event.getTargetInstance();
    doIt(account);
  }

  private void doIt(ElementValue account) {
    boolean isNumber = true;
    try {
      new BigInteger(account.getSearchKey());
    } catch (NumberFormatException e) {
      isNumber = false;
    }
    String rootNode = "0";
    OBCriteria<TreeNode> obc = OBDal.getInstance().createCriteria(TreeNode.class);
    obc.add(Restrictions.eq(TreeNode.PROPERTY_NODE, account.getId()));
    obc.setMaxResults(1);
    List<TreeNode> nodes = obc.list();
    HashMap<String, String> result = getParentAndSeqNo(account);
    String parentId = result.get("ParentID");
    String seqNo = result.get("SeqNo");
    if (!nodes.isEmpty()) {
      TreeNode node = nodes.get(0);
      node.setReportSet(!isNumber ? rootNode : parentId);
      node.setSequenceNumber(Long.valueOf(seqNo));
      OBDal.getInstance().save(node);
    } else {
      TreeNode treeElement = OBProvider.getInstance().get(TreeNode.class);
      treeElement.setOrganization(account.getOrganization());
      treeElement.setNode(account.getId());
      treeElement.setTree(account.getAccountingElement().getTree());
      treeElement.setReportSet(!isNumber ? rootNode : parentId);
      treeElement.setSequenceNumber(Long.valueOf(seqNo));
      OBDal.getInstance().save(treeElement);
    }

  }

  HashMap<String, String> getParentAndSeqNo(ElementValue account) {
    HashMap<String, String> result = new HashMap<>();
    // Default values for result
    result.put("ParentID", "0");
    result.put("SeqNo",
        String.valueOf(getNextSeqNo(account.getAccountingElement().getTree(), "0")));
    List<ElementValue> accounts = getAccountList(account);
    ElementValue previousElement = null;
    for (ElementValue elementValue : accounts) {
      previousElement = elementValue;
    }
    if (previousElement != null && previousElement.isSummaryLevel() && !account.isSummaryLevel()) {
      result.put("ParentID", previousElement.getId());
      result.put("SeqNo", "0");
    } else if (previousElement == null) {
      return result;
    } else {
      OBCriteria<TreeNode> obc = OBDal.getInstance().createCriteria(TreeNode.class);
      obc.add(Restrictions.eq(TreeNode.PROPERTY_NODE, previousElement.getId()));
      obc.setMaxResults(1);
      List<TreeNode> nodes = obc.list();
      result.put("ParentID", nodes.get(0).getReportSet());
      result.put("SeqNo", String.valueOf(nodes.get(0).getSequenceNumber() + 10));
    }
    updateSeqNo(result.get("ParentID"), account.getAccountingElement().getTree(),
        result.get("SeqNo"));
    return result;

  }

  List<ElementValue> getAccountList(ElementValue account) {
    OBCriteria<ElementValue> obc = OBDal.getInstance().createCriteria(ElementValue.class);
    obc.add(
        Restrictions.eq(ElementValue.PROPERTY_ACCOUNTINGELEMENT, account.getAccountingElement()));
    obc.add(Restrictions.eq(ElementValue.PROPERTY_ACTIVE, true));
    obc.add(Restrictions.le(ElementValue.PROPERTY_SEARCHKEY, account.getSearchKey()));
    obc.add(Restrictions.ne(ElementValue.PROPERTY_ID, account.getId()));
    obc.addOrder(Order.desc(ElementValue.PROPERTY_SEARCHKEY));
    obc.setMaxResults(1);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    return obc.list();
  }

  void updateSeqNo(String parentID, Tree tree, String seqNo) {
    OBCriteria<TreeNode> obc = OBDal.getInstance().createCriteria(TreeNode.class);
    obc.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
    obc.add(Restrictions.eq(TreeNode.PROPERTY_REPORTSET, parentID));
    obc.add(Restrictions.ge(TreeNode.PROPERTY_SEQUENCENUMBER, Long.valueOf(seqNo)));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    for (TreeNode node : obc.list()) {
      node.setSequenceNumber(node.getSequenceNumber() + 10l);
      OBDal.getInstance().save(node);
    }
  }

  long getNextSeqNo(Tree tree, String parentId) {
    OBCriteria<TreeNode> obc = OBDal.getInstance().createCriteria(TreeNode.class);
    obc.add(Restrictions.eq(TreeNode.PROPERTY_REPORTSET, parentId));
    obc.add(Restrictions.eq(TreeNode.PROPERTY_TREE, tree));
    obc.addOrder(Order.desc(TreeNode.PROPERTY_SEQUENCENUMBER));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    List<TreeNode> nodes = obc.list();
    if (!nodes.isEmpty() && obc.list().get(0).getSequenceNumber() != null) {
      return obc.list().get(0).getSequenceNumber() + 10l;
    } else {
      return 10l;
    }
  }

  private Property getValueProperty() {
    return entities[0].getProperty(ElementValue.PROPERTY_SEARCHKEY);
  }
}
