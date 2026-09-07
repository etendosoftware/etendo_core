/*
 ************************************************************************************
 * Copyright (C) 2001-2021 Openbravo S.L.U.
 * Copyright (C) 2026 Etendo Software
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import java.util.Map;
import java.util.Set;
import jakarta.servlet.ServletException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.Restrictions;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.DimensionDisplayUtility;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Original warehouse and accounting session initialization extracted from LoginUtils.
 *
 * <p>MODULE-BOUNDARY LOGIN-ERP: destination platform-compat-etendo. Keep this class
 * out of ERP-free deployments. It stays in the legacy package while its SQLC
 * collaborators are package-private; the public LoginUtils facade remains stable.
 */
public final class ErpLoginSessionSupport extends LoginSessionSupport {
  @Override
  public String getContextWarehouseId() {
    return OBContext.getOBContext().getWarehouse() == null ? null
        : OBContext.getOBContext().getWarehouse().getId();
  }

  @Override
  public String getUserDefaultWarehouse(ConnectionProvider connection, String user,
      String client, String organization, String role) throws ServletException {
    String warehouse = DefaultOptionsData.defaultWarehouse(connection, user);
    return warehouse == null ? getDefaultWarehouse(connection, client, organization, role) : warehouse;
  }

  @Override
  public String getDefaultWarehouse(ConnectionProvider connectionProvider, String strClient,
      String strOrg, String strRole) throws ServletException {
    long t = System.currentTimeMillis();
    String strWarehouse;
    if (!strRole.equals("0")) {
      strWarehouse = DefaultOptionsData.getDefaultWarehouse(connectionProvider, strClient,
          "'" + strOrg + "'");
      if (strWarehouse == null || strWarehouse.isEmpty()) {
        OrganizationStructureProvider osp = OBContext.getOBContext()
            .getOrganizationStructureProvider(strClient);
        Set<String> orgNaturalTree = osp.getNaturalTree(strOrg);
        strWarehouse = DefaultOptionsData.getDefaultWarehouse(connectionProvider, strClient,
            StringCollectionUtils.commaSeparated(orgNaturalTree));
      }
    } else {
      strWarehouse = "";
    }
    LoginUtils.log4j.debug("getDefaultWarehouse " + (System.currentTimeMillis() - t));
    return strWarehouse;
  }

  @Override
  public boolean isAccountingDimensionConfigCentrally(Client client) {
    return client.isAcctdimCentrallyMaintained();
  }

  @Override
  public void initializeApproval(ConnectionProvider connection, VariablesSecureApp vars,
      String role, String user) throws ServletException {
    SeguridadData[] data = SeguridadData.select(connection, role, user);
    if (data == null || data.length == 0) {
      throw new ServletException("ERP login role disappeared during session initialization");
    }
    vars.setSessionValue("#Approval_C_Currency_ID", data[0].cCurrencyId);
    vars.setSessionValue("#Approval_Amt", data[0].amtapproval);
  }

  @Override
  public void initializeAccounting(ConnectionProvider conn, VariablesSecureApp vars, Client client,
      boolean isAccountingDimensionConfigCentrally, String strOrg, String strCliente)
      throws ServletException {
    AttributeData[] attr = null;
    String acctSchemaId = OBLedgerUtils.getOrgLedger(strOrg);
    if (StringUtils.isNotEmpty(acctSchemaId)) {
      attr = AttributeData.selectAcctSchema(conn, acctSchemaId,
          Utility.getContext(conn, vars, "#User_Client", "LoginHandler"));
    }

    if (ArrayUtils.isEmpty(attr) && existsAnyOrgWithLedgerConfigured()) {
      String[] orgList = Utility.getContext(conn, vars, "#User_Org", "LoginHandler")
          .replace("'", "").split(",");
      for (String orgId : orgList) {
        if (!StringUtils.equals(orgId, strOrg)) {
          acctSchemaId = OBLedgerUtils.getOrgLedger(orgId);
          if (StringUtils.isNotEmpty(acctSchemaId)) {
            attr = AttributeData.selectAcctSchema(conn, acctSchemaId,
                Utility.getContext(conn, vars, "#User_Client", "LoginHandler"));
            if (ArrayUtils.isNotEmpty(attr)) {
              break;
            }
          }
        }
      }
    }

    if (attr != null && attr.length > 0) {
      vars.setSessionValue("$C_AcctSchema_ID", attr[0].value);
      AttributeData[] orgCurrency = AttributeData.selectOrgCurrency(conn, strOrg, strCliente);
      if (orgCurrency.length > 0) {
        vars.setSessionValue("$C_Currency_ID", orgCurrency[0].cCurrencyId);
      } else {
        vars.setSessionValue("$C_Currency_ID", attr[0].attribute);
      }
      vars.setSessionValue("#StdPrecision",
          AttributeData.selectStdPrecision(conn, attr[0].attribute,
              Utility.getContext(conn, vars, "#User_Client", "LoginHandler"),
              Utility.getContext(conn, vars, "#User_Org", "LoginHandler")));
      vars.setSessionValue("$HasAlias", attr[0].hasalias);
      for (int i = 0; i < attr.length; i++) {
        vars.setSessionValue("$Element_" + attr[i].elementtype, "Y");
      }
    }

    vars.setSessionValue(DimensionDisplayUtility.IsAcctDimCentrally,
        isAccountingDimensionConfigCentrally ? "Y" : "N");
    if (isAccountingDimensionConfigCentrally) {
      Map<String, String> acctDimMap = DimensionDisplayUtility
          .getAccountingDimensionConfiguration(client);
      for (Map.Entry<String, String> entry : acctDimMap.entrySet()) {
        vars.setSessionValue(entry.getKey(), entry.getValue());
      }
    }
    Map<String, String> readOnlySessionVariableMap = DimensionDisplayUtility
        .getReadOnlyLogicSessionVariables();
    for (Map.Entry<String, String> entry : readOnlySessionVariableMap.entrySet()) {
      vars.setSessionValue(entry.getKey(), entry.getValue());
    }
  }

  private static boolean existsAnyOrgWithLedgerConfigured() {
    OBCriteria<Organization> orgCriteria = OBDal.getInstance().createCriteria(Organization.class);
    orgCriteria.add(Restrictions.isNotNull(Organization.PROPERTY_GENERALLEDGER));
    orgCriteria.setMaxResults(1);
    return orgCriteria.uniqueResult() != null;
  }
}
