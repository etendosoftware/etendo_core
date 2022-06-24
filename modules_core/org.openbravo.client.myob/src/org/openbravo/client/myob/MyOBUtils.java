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
package org.openbravo.client.myob;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Util class for MyOB.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class MyOBUtils {
  private static Logger log = LogManager.getLogger();
  private static String MENU_ITEM_IS_SEPARATOR = "isSeparator";
  private static String MENU_ITEM_TITLE = "title";
  private static String MENU_ITEM_CLICK = "click";
  private ConcurrentHashMap<String, WidgetClassInfo> widgetClasses = new ConcurrentHashMap<>();
  private List<String> anonymousWidgetClasses;

  /**
   * Calls {@link #getWidgetTitle(WidgetClass)} using the
   * 
   * {@link WidgetInstance#getWidgetClass()}
   * 
   * @param widgetInstance
   * @return the (translated) title
   */
  static String getWidgetTitle(WidgetInstance widgetInstance) {
    return getWidgetTitle(widgetInstance.getWidgetClass());
  }

  /**
   * Computes the widget title using the user's language, if no translation is available then the
   * {@link WidgetClass#getWidgetTitle()} is used.
   * 
   * @param widgetClass
   *          the widget class of this instance is used to read the title
   * @return the title of the widget read from the widgetclass
   * @see WidgetInstance#getWidgetClass()
   * @see WidgetClassTrl
   * @see WidgetClass#getWidgetTitle()
   */
  static String getWidgetTitle(WidgetClass widgetClass) {
    final String userLanguageId = OBContext.getOBContext().getLanguage().getId();

    for (WidgetClassTrl widgetClassTrl : widgetClass.getOBKMOWidgetClassTrlList()) {
      final String trlLanguageId = widgetClassTrl.getLanguage().getId();
      if (trlLanguageId.equals(userLanguageId)) {
        return widgetClassTrl.getTitle();
      }
    }
    return widgetClass.getWidgetTitle();
  }

  static JSONArray getWidgetMenuItems(WidgetClass widgetClass) {
    final JSONArray result = new JSONArray();
    List<WidgetClassMenu> menuItems = MyOBUtils.getWidgetClassMenuItemsList(widgetClass);

    for (WidgetClassMenu menuItem : menuItems) {
      final JSONObject item = new JSONObject();
      try {

        if (menuItem.isSeparator()) {
          item.put(MENU_ITEM_IS_SEPARATOR, true);
          result.put(item);
          continue;
        }

        item.putOpt(MENU_ITEM_TITLE, menuItem.getTitle());
        item.putOpt(MENU_ITEM_CLICK, menuItem.getAction());
        result.put(item);

      } catch (JSONException e) {
        log.error(
            "Error trying to build menu items for widget class " + widgetClass.getWidgetTitle(), e);
      }
    }
    return result;
  }

  private static List<WidgetClassMenu> getWidgetClassMenuItemsList(WidgetClass widgetClass) {
    OBCriteria<WidgetClassMenu> obcMenuItems = OBDal.getInstance()
        .createCriteria(WidgetClassMenu.class);
    if (widgetClass.getWidgetSuperclass() != null) {
      obcMenuItems.add(Restrictions
          .or(Restrictions.eq(WidgetClassMenu.PROPERTY_WIDGETCLASS, widgetClass), Restrictions
              .eq(WidgetClassMenu.PROPERTY_WIDGETCLASS, widgetClass.getWidgetSuperclass())));
    } else {
      obcMenuItems.add(Restrictions.eq(WidgetClassMenu.PROPERTY_WIDGETCLASS, widgetClass));
    }
    obcMenuItems.addOrderBy(WidgetClassMenu.PROPERTY_SEQUENCE, true);
    return obcMenuItems.list();
  }

  static List<WidgetInstance> getDefaultWidgetInstances(String availableAtLevel,
      String[] availableAtValues) {
    if ("OB".equals(availableAtLevel)) {
      return getDefaultWidgetInstancesAtOBLevel();
    } else if ("SYSTEM".equals(availableAtLevel)) {
      return getDefaultWidgetInstancesAtSystemLevel();
    } else if ("CLIENT".equals(availableAtLevel)) {
      return getDefaultWidgetInstancesAtClientLevel(availableAtValues[0]);
    } else if ("ORG".equals(availableAtLevel)) {
      return getDefaultWidgetInstancesAtOrgLevel(availableAtValues);
    } else if ("ROLE".equals(availableAtLevel)) {
      return getDefaultWidgetInstancesAtRoleLevel(availableAtValues[0]);
    } else {
      // USER level is not supported
    }
    OBCriteria<WidgetInstance> criteria = OBDal.getInstance().createCriteria(WidgetInstance.class);
    criteria.add(Restrictions.isNull(WidgetInstance.PROPERTY_VISIBLEATUSER));
    return criteria.list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtOBLevel() {
    Client client = OBDal.getInstance().getProxy(Client.class, "0");
    Organization org = OBDal.getInstance().getProxy(Organization.class, "0");
    return getWidgetInstanceCriteria(0L, client, org, null).list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtSystemLevel() {
    Client client = OBDal.getInstance().getProxy(Client.class, "0");
    Organization org = OBDal.getInstance().getProxy(Organization.class, "0");
    return getWidgetInstanceCriteria(1L, client, org, null).list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtClientLevel(String clientId) {
    Client client = OBDal.getInstance().getProxy(Client.class, clientId);
    return getWidgetInstanceCriteria(2L, client, null, null).list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtOrgLevel(Set<String> orgIds) {
    return getWidgetInstanceCriteriaForOrgs(3L, null, orgIds, null).list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtOrgLevel(String[] orgIds) {
    return getWidgetInstanceCriteriaForOrgs(3L, null, orgIds, null).list();
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtRoleLevel(String roleId) {
    final Role role = OBDal.getInstance().getProxy(Role.class, roleId);
    return getDefaultWidgetInstancesAtRoleLevel(role);
  }

  static List<WidgetInstance> getDefaultWidgetInstancesAtRoleLevel(Role role) {
    Organization org = OBDal.getInstance().getProxy(Organization.class, "0");
    return getWidgetInstanceCriteria(4L, null, org, role).list();
  }

  private static OBCriteria<WidgetInstance> getWidgetInstanceCriteria(Long priority, Client client,
      Organization organization, Role role) {
    OBCriteria<WidgetInstance> criteria = OBDal.getInstance().createCriteria(WidgetInstance.class);
    criteria.add(Restrictions.isNull(WidgetInstance.PROPERTY_VISIBLEATUSER));
    criteria.setFilterOnReadableClients(false);
    criteria.add(Restrictions.eq(WidgetInstance.PROPERTY_RELATIVEPRIORITY, priority));
    if (client != null) {
      criteria.add(Restrictions.eq(WidgetInstance.PROPERTY_CLIENT, client));
    }
    if (organization != null) {
      criteria.setFilterOnReadableOrganization(false);
      criteria.add(Restrictions.eq(WidgetInstance.PROPERTY_ORGANIZATION, organization));
    }
    if (role != null) {
      criteria.add(Restrictions.eq(WidgetInstance.PROPERTY_CLIENT, role.getClient()));
      criteria.add(Restrictions.eq(WidgetInstance.PROPERTY_VISIBLEATROLE, role));
    }
    return criteria;
  }

  private static OBCriteria<WidgetInstance> getWidgetInstanceCriteriaForOrgs(Long priority,
      Client client, String[] orgIds, Role role) {
    OBCriteria<WidgetInstance> criteria = getWidgetInstanceCriteria(priority, client, null, role);
    criteria.setFilterOnReadableOrganization(false);
    criteria.add(Restrictions.in(
        WidgetInstance.PROPERTY_ORGANIZATION + "." + Organization.PROPERTY_ID, (Object[]) orgIds));
    return criteria;
  }

  private static OBCriteria<WidgetInstance> getWidgetInstanceCriteriaForOrgs(Long priority,
      Client client, Set<String> orgIds, Role role) {
    OBCriteria<WidgetInstance> criteria = getWidgetInstanceCriteria(priority, client, null, role);
    criteria.setFilterOnReadableOrganization(false);
    criteria.add(Restrictions
        .in(WidgetInstance.PROPERTY_ORGANIZATION + "." + Organization.PROPERTY_ID, orgIds));
    return criteria;
  }

  static List<WidgetInstance> getUserWidgetInstances() {
    return getUserWidgetInstances(true);
  }

  static List<WidgetInstance> getUserWidgetInstances(Boolean isActive) {
    OBCriteria<WidgetInstance> obc = OBDal.getInstance().createCriteria(WidgetInstance.class);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnActive(isActive);
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_CLIENT, OBDal.getInstance()
        .get(Client.class, OBContext.getOBContext().getCurrentClient().getId())));
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_VISIBLEATROLE,
        OBDal.getInstance().get(Role.class, OBContext.getOBContext().getRole().getId())));
    obc.add(Restrictions.eq(WidgetInstance.PROPERTY_VISIBLEATUSER,
        OBDal.getInstance().get(User.class, OBContext.getOBContext().getUser().getId())));
    return obc.list();
  }

  static WidgetClass getWidgetClassFromTitle(String strClassTitle) {
    OBCriteria<WidgetClass> widgetClassCrit = OBDal.getInstance().createCriteria(WidgetClass.class);
    widgetClassCrit.add(Restrictions.eq(WidgetClass.PROPERTY_WIDGETTITLE, strClassTitle));
    if (widgetClassCrit.list().size() == 0) {
      return null;
    }
    return widgetClassCrit.list().get(0);
  }

  @Inject
  private WeldUtils weldUtils;

  /**
   * Creates the widgetProvider from the widgetClass object. Also calls/sets the
   * {@link WidgetProvider#setWidgetClass(WidgetClass)}.
   * 
   * @param widgetClass
   * @return instance of a {@link WidgetProvider}
   */
  WidgetProvider getWidgetProvider(WidgetClass widgetClass) {
    try {
      String strJavaClass = (widgetClass.getWidgetSuperclass() != null)
          ? widgetClass.getWidgetSuperclass().getJavaClass()
          : widgetClass.getJavaClass();
      final Class<?> clz = OBClassLoader.getInstance().loadClass(strJavaClass);
      final WidgetProvider widgetProvider = (WidgetProvider) weldUtils.getInstance(clz);
      widgetProvider.setWidgetClass(widgetClass);
      return widgetProvider;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  WidgetClassInfo getWidgetClassInfo(WidgetClass widgetClass) {
    if (!widgetClasses.containsKey(widgetClass.getId())) {
      widgetClasses.putIfAbsent(widgetClass.getId(), getWidgetClassInfoFromDatabase(widgetClass));
    }
    return widgetClasses.get(widgetClass.getId());
  }

  WidgetClassInfo getWidgetClassInfoFromDatabase(WidgetClass widgetClass) {
    final WidgetProvider widgetProvider = getWidgetProvider(widgetClass);
    if (!widgetProvider.validate()) {
      return null;
    }
    return new WidgetClassInfo(widgetProvider);
  }

  List<String> getAnonymousAccessibleWidgetClasses() {
    if (anonymousWidgetClasses == null) {
      anonymousWidgetClasses = getAnonymousAccessibleWidgetClassesFromDatabase();
    }
    return anonymousWidgetClasses;
  }

  List<String> getAnonymousAccessibleWidgetClassesFromDatabase() {
    //@formatter:off
    String hql = 
            "select widgetClass.id " +
            "  from OBKMO_WidgetClass widgetClass " +
            " where widgetClass.allowAnonymousAccess is true " +
            "   and widgetClass.superclass is false " +
            "   and widgetClass.availableInWorkspace is true";
    //@formatter:on
    Query<String> query = OBDal.getInstance().getSession().createQuery(hql, String.class);
    return query.list();
  }
}
