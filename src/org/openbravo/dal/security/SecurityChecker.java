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
 * All portions are Copyright (C) 2008-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.security;

import java.util.Arrays;
import java.util.List;

import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * This class combines all security checks which are performed on entity level:
 * <ul>
 * <li>Delete: is the entity deletable (@see {@link Entity#isDeletable()}) and does the user have
 * write access to the entity.</li>
 * <li>Write: is done in case of create and update actions. The following checks are performed: is
 * the organization writable, is the client of the object the same as is the entity writable (@see
 * EntityAccessChecker#isWritable(Entity))
 * </ul>
 *
 * @author mtaal
 *
 * CONTRIBUTORS: androettop
 *
 */

public class SecurityChecker implements OBSingleton {

    private static SecurityChecker instance;

    public static SecurityChecker getInstance() {
        if (instance == null) {
            instance = OBProvider.getInstance().get(SecurityChecker.class);
        }
        return instance;
    }

    public void checkDeleteAllowed(Object o) {
        final OBContext obContext = OBContext.getOBContext();

        if (obContext.getRecordAccessChecker() != null) {
            if(!obContext.getRecordAccessChecker().canDelete(o)){
                throw new OBSecurityException("The current user does not have delete permissions.");
            }
        } else {
            if (!obContext.isInAdministratorMode() && o instanceof BaseOBObject) {
                final BaseOBObject bob = (BaseOBObject) o;
                final Entity entity = ModelProvider.getInstance().getEntity(bob.getEntityName());
                if (!entity.isDeletable()) {
                    throw new OBSecurityException("Entity " + entity.getName() + " is not deletable");
                }
            }
            checkWriteAccess(o);
        }

    }

    /**
     * Performs several write access checks when an object is created or updated:
     * <ul>
     * <li>is the organization writable (@see OBContext#getWritableOrganizations())</li>
     * <li>is the client of the object the same as the client of the user (@see
     * OBContext#getCurrentClient())</li>
     * <li>is the Entity writable for this user (@see EntityAccessChecker#isWritable(Entity))
     * <li>are the client and organization correct from an access level perspective (@see
     * AccessLevelChecker).
     * </ul>
     *
     * @param obj the object to check
     * @return true if writable, false otherwise
     * @see Entity
     */
    public boolean isWritable(Object obj) {
        try {
            checkWriteAccess(obj, false);
        } catch (final OBSecurityException e) {
            return false;
        }
        return true;
    }

    /**
     * Performs the same checks as {@link #isWritable(Object)}. Does not return true/false but throws
     * a OBSecurityException if the object is not writable.
     *
     * @param obj the object to check
     * @throws OBSecurityException
     */
    public void checkWriteAccess(Object obj) {
        checkWriteAccess(obj, true);
    }

    private void checkWriteAccess(Object obj, boolean logError) {
        final OBContext obContext = OBContext.getOBContext();

        if (obContext.getRecordAccessChecker() != null) {
            BaseOBObject bob = (BaseOBObject) obj;
            if(bob.getId() == null){
                if(!obContext.getRecordAccessChecker().canCreate(obj)){
                    throw new OBSecurityException("The current user does not have create permissions.", logError);
                }
            }else{
                if(!obContext.getRecordAccessChecker().canUpdate(obj)){
                    throw new OBSecurityException("The current user does not have update permissions.", logError);
                }
            }
        } else {
            // check that the client id and organization id are resp. in the list of
            // user_client and user_org
            // TODO: throw specific and translated exception, for more info:
            // Utility.translateError(this, vars, vars.getLanguage(),
            // Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()))

            String clientId = "";
            if (obj instanceof ClientEnabled && ((ClientEnabled) obj).getClient() != null) {
                clientId = ((ClientEnabled) obj).getClient().getId();
            } else if (obj instanceof Client) {
                clientId = ((Client) obj).getId();
            }

            String orgId = "";
            boolean isOrganization = false;
            if (obj instanceof OrganizationEnabled
                    && ((OrganizationEnabled) obj).getOrganization() != null) {
                orgId = ((OrganizationEnabled) obj).getOrganization().getId();
            } else if (obj instanceof Organization) {
                orgId = ((Organization) obj).getId();
                isOrganization = true;
            }

            final Entity entity = ((BaseOBObject) obj).getEntity();
            if ((!obContext.isInAdministratorMode() || obContext.doOrgClientAccessCheck())
                    && clientId.length() > 0) {
                if (obj instanceof ClientEnabled || obj instanceof Client) {
                    if (!obContext.getCurrentClient().getId().equals(clientId)) {
                        // TODO: maybe move rollback to exception throwing
                        SessionHandler.getInstance().setDoRollback(true);
                        throw new OBSecurityException("Client (" + clientId + ") of object (" + obj
                                + ") is not present in ClientList " + obContext.getCurrentClient().getId(), logError);
                    }
                }

                if (!obContext.getEntityAccessChecker().isWritable(entity)) {
                    throw new OBSecurityException("Entity " + entity + " is not writable by this user",
                            logError);
                }

                if (orgId != null && orgId.length() > 0) {
                    // Due to issue 23419: Impossible to add an organization to one role, it has been necessary
                    // to add the below check. The system is going to check if it can avoid the permission
                    // during the record insertion. The application is allowed to avoid the permission when the
                    // user is inserting one record in table "AD_ORG_ACCESS" and the role of the user is the
                    // client administrator and also is inserting the record in the same client
                    boolean checkOrgAccess = !(entity.getTableName().equals("AD_Role_OrgAccess")
                            && OBContext.getOBContext().getRole().isClientAdmin()
                            && OBContext.getOBContext().getRole().getClient().getId().equals(clientId));
                    boolean notWritableOrganization = !obContext.getWritableOrganizations().contains(orgId);
                    boolean isDisabledOrganization = isOrganization
                            && obContext.getDeactivatedOrganizations().contains(orgId);

                    if (checkOrgAccess && notWritableOrganization && !isDisabledOrganization) {
                        // TODO: maybe move rollback to exception throwing
                        SessionHandler.getInstance().setDoRollback(true);
                        throw new OBSecurityException(
                                "Organization " + orgId + " of object (" + obj
                                        + ") is not present in OrganizationList " + obContext.getWritableOrganizations(),
                                logError);
                    }
                }
            }

            // accesslevel check must also be done for administrators
            entity.checkAccessLevel(clientId, orgId);
        }
    }

    /**
     * Checks if there is access to the entity and if the organization is readable. If not, it throws
     * an OBSecurityException.
     *
     * @param organizationEnabledObject a {@link BaseOBObject} that implements the {@link OrganizationEnabled} interface. This
     *                                  method will check if the user has read access to the provided object
     */
    public void checkReadableAccess(OrganizationEnabled organizationEnabledObject) {
        OBContext obContext = OBContext.getOBContext();
        if (obContext.getRecordAccessChecker() != null) {
            if(!obContext.getRecordAccessChecker().canRead(organizationEnabledObject)){
                throw new OBSecurityException("The current user does not have read permissions.");
            }
        } else {
            final Entity entity = ((BaseOBObject) organizationEnabledObject).getEntity();

            obContext.getEntityAccessChecker().checkReadable(entity);
            String orgId = organizationEnabledObject.getOrganization().getId();
            String readableOrganizations[] = obContext.getReadableOrganizations();
            List<String> organizations = Arrays.asList(readableOrganizations);
            if (!organizations.contains(orgId)) {
                throw new OBSecurityException(
                        "Organization " + orgId + " of object (" + organizationEnabledObject
                                + ") is not present in OrganizationList " + organizations.toString());
            }
        }

    }
}
