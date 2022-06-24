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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.navigationbarcomponents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;

/**
 * This class provides the organizations and warehouses that can be accessed for a particular role.
 * It is is used to populate the 'Profile' widget with the information related to the roles assigned
 * to the current user.
 */
public class RoleInfo {
  private String roleId;
  private String roleName;
  private String clientId;
  private String clientName;
  private Map<String, String> roleOrganizations;
  private Map<String, List<RoleWarehouseInfo>> organizationWarehouses;

  public RoleInfo(Object[] roleInfo) {
    this.roleId = (String) roleInfo[0];
    this.roleName = (String) roleInfo[1];
    this.clientId = (String) roleInfo[2];
    this.clientName = (String) roleInfo[3];
  }

  public RoleInfo(Role role) {
    this.roleId = role.getId();
    this.roleName = role.getIdentifier();
    this.clientId = role.getClient().getId();
    this.clientName = role.getClient().getIdentifier();
  }

  public String getRoleId() {
    return roleId;
  }

  public String getRoleName() {
    return roleName;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientName() {
    return clientName;
  }

  private OrganizationStructureProvider getOrganizationStructureProvider() {
    return OBContext.getOBContext().getOrganizationStructureProvider(clientId);
  }

  public Map<String, String> getOrganizations() {
    if (roleOrganizations != null) {
      return roleOrganizations;
    }
    roleOrganizations = new LinkedHashMap<>();

    //@formatter:off
    String hql = 
            "select ro.organization.id, ro.organization.name " +
            "  from ADRoleOrganization ro " +
            " where ro.active=true" +
            "   and ro.role.id=:roleId" +
            "   and ro.organization.active=true ";
    //@formatter:on
    Query<Object[]> roleOrgs = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("roleId", roleId);
    for (Object[] orgInfo : roleOrgs.list()) {
      roleOrganizations.put((String) orgInfo[0], (String) orgInfo[1]);
    }
    return roleOrganizations;
  }

  public Map<String, List<RoleWarehouseInfo>> getOrganizationWarehouses() {
    if (organizationWarehouses != null) {
      return organizationWarehouses;
    }
    organizationWarehouses = new LinkedHashMap<>();

    if (getOrganizations().isEmpty()) {
      return organizationWarehouses;
    }

    for (String orgId : getOrganizations().keySet()) {
      organizationWarehouses.put(orgId, new ArrayList<RoleWarehouseInfo>());
    }

    //@formatter:off
    String hql = 
            "select w.id, w.name, w.organization.id " +
            "  from Warehouse w " +
            " where w.active=true" +
            "   and w.organization.id in (:orgList)" +
            "   and w.client.id=:clientId" +
            "   and w.organization.active=true ";
    //@formatter:on
    Query<Object[]> orgWarehouses = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameterList("orgList", getOrganizations().keySet())
        .setParameter("clientId", clientId);
    for (Object[] entry : orgWarehouses.list()) {
      RoleWarehouseInfo warehouseInfo = new RoleWarehouseInfo(entry);
      for (Map.Entry<String, List<RoleWarehouseInfo>> ow : organizationWarehouses.entrySet()) {
        Set<String> naturalTree = getOrganizationStructureProvider().getNaturalTree(ow.getKey());
        if (naturalTree.contains(warehouseInfo.getWarehouseOrganizationId())) {
          ow.getValue().add(warehouseInfo);
        }
      }
    }
    return organizationWarehouses;
  }

  public class RoleWarehouseInfo {
    private String warehouseId;
    private String warehouseName;
    private String warehouseOrganizationId;

    public RoleWarehouseInfo(Object[] warehouseInfo) {
      this.warehouseId = (String) warehouseInfo[0];
      this.warehouseName = (String) warehouseInfo[1];
      this.warehouseOrganizationId = (String) warehouseInfo[2];
    }

    public String getWarehouseId() {
      return warehouseId;
    }

    public String getWarehouseName() {
      return warehouseName;
    }

    public String getWarehouseOrganizationId() {
      return warehouseOrganizationId;
    }
  }
}
