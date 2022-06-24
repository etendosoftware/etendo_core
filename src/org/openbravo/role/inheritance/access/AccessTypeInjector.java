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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */
package org.openbravo.role.inheritance.access;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.role.inheritance.RoleInheritanceManager;

/**
 * An AccessTypeInjector is used by {@link RoleInheritanceManager} to retrieve the access types that
 * should be inherited
 */

@ApplicationScoped
public abstract class AccessTypeInjector implements Comparable<AccessTypeInjector> {

  private static final Logger log = LogManager.getLogger();

  /**
   * Returns the name of the inheritable class.
   * 
   * @return A String with the class name
   */
  public String getClassName() {
    return getClass().getAnnotation(AccessTypeInjector.Qualifier.class).value().getCanonicalName();
  }

  /**
   * Returns the secured object.
   * 
   * @return a String with the name of the method to retrieve the secured element
   */
  protected abstract String getSecuredElementGetter();

  /**
   * Returns the secured element name.
   * 
   * @return a String with the name of the secured element
   */
  protected abstract String getSecuredElementName();

  /**
   * Returns the priority of this injector. It is used to determine the order when adding, updating
   * or removing a particular access, if needed.
   * 
   * @return an integer that represents the priority of this injector
   */
  public int getPriority() {
    return 100;
  }

  /**
   * Allows the comparison between AccessTypeInjector classes. The getPriority() method is used to
   * determine the comparison result.
   * 
   * @return a negative integer, zero, or a positive integer as this object priority is less than,
   *         equal to, or greater than the priority of the specified AccessTypeInjector object.
   */
  @Override
  public int compareTo(AccessTypeInjector accessType) {
    return this.getPriority() - accessType.getPriority();
  }

  /**
   * Determines if a particular access can be inherited according to this injector
   * 
   * @param access
   *          the permission to decide whether is inheritable or not
   * 
   * @return true if the access is inheritable, false otherwise
   */
  public boolean isInheritable(InheritedAccessEnabled access) {
    return true;
  }

  /**
   * Checks if a particular access already exists
   * 
   * @param access
   *          the permission to decide whether exists or not
   * 
   */
  public void checkAccessExistence(InheritedAccessEnabled access) {
  }

  /**
   * Sets the parent for an inheritable access object.
   * 
   * @param newAccess
   *          Access whose parent object will be set
   * @param parentAccess
   *          Access that is used in some cases to find the correct parent
   * @param role
   *          Parent role to set directly when applies
   */
  public void setParent(InheritedAccessEnabled newAccess, InheritedAccessEnabled parentAccess,
      Role role) {
    try {
      Class<?> myClass = Class.forName(getClassName());
      myClass.getMethod("setRole", new Class[] { Role.class })
          .invoke(newAccess, new Object[] { role });
    } catch (Exception ex) {
      log.error("Error setting {} as parent role", role, ex);
      throw new OBException("Error setting parent role");
    }
  }

  /**
   * Returns the role which the access given as parameter is assigned to. In general, the most part
   * of inheritable accesses have Role as their parent entity. If not, this method must be
   * overridden to retrieve the Role property for their particular case.
   * 
   * @param access
   *          An inheritable access
   * 
   * @return the Role owner of the access
   */
  public Role getRole(InheritedAccessEnabled access) {
    try {
      Class<?> myClass = Class.forName(getClassName());
      Role role = (Role) myClass.getMethod("getRole").invoke(access);
      return role;
    } catch (Exception ex) {
      log.error("Error getting role for access with class {}", getClassName(), ex);
      throw new OBException("Error getting role");
    }
  }

  /**
   * Returns the role property related to the entity represented by the injector.
   * 
   * @return the role property that can be retrieved according to the entity of the injector.
   */
  public String getRoleProperty() {
    return "role.id";
  }

  /**
   * Returns the id of the secured object for the given inheritable access.
   * 
   * @param access
   *          An object of an inheritable class,i.e., a class that implements
   *          InheritedAccessEnabled.
   * 
   * @return A String with the id of the secured object
   */
  protected String getSecuredElementIdentifier(InheritedAccessEnabled access) {
    try {
      Class<?> myClass = Class.forName(getClassName());
      BaseOBObject bob = (BaseOBObject) myClass.getMethod(getSecuredElementGetter()).invoke(access);
      String securedElementIndentifier = (String) bob.getId();
      return securedElementIndentifier;
    } catch (Exception ex) {
      log.error("Error getting secured element identifier with method {}",
          getSecuredElementGetter(), ex);
      throw new OBException("Error getting secured element identifier");
    }
  }

  /**
   * Returns the list of accesses of a particular type for the Role given as parameter.
   * 
   * @param role
   *          The role whose list of accesses of a particular type will be retrieved
   * 
   * @return a list of accesses
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseOBObject> List<? extends InheritedAccessEnabled> getAccessList(Role role) {
    String className = getClassName();
    try {
      String roleProperty = getRoleProperty();
      Class<T> clazz = (Class<T>) Class.forName(className);
      //@formatter:off
      String whereClause = 
              " as p " +
              " where p." + roleProperty + " = :roleId";
      //@formatter:on
      whereClause = addEntityWhereClause(whereClause);
      final OBQuery<T> query = OBDal.getInstance().createQuery(clazz, whereClause)
              .setNamedParameter("roleId", role.getId());
      doEntityParameterReplacement(query);
      query.setFilterOnActive(false);
      return (List<? extends InheritedAccessEnabled>) query.list();
    } catch (Exception ex) {
      log.error("Error getting access list of class {}", className, ex);
      throw new OBException("Error getting access list of class " + className);
    }
  }

  /**
   * @param access
   *          The access with the secured element to be found
   * @param roleId
   *          Id of the role owner of the access to be found
   * @return The searched access or null if not found
   * 
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseOBObject> InheritedAccessEnabled findAccess(InheritedAccessEnabled access,
      String roleId) throws ClassNotFoundException {
    String roleProperty = getRoleProperty();
    Class<T> clazz = (Class<T>) Class.forName(getClassName());
    //@formatter:off
    String whereClause = 
            " as p " +
            " where p." + roleProperty + " = :roleId" +
            "   and p." + getSecuredElementName() + ".id = :elementId";
    //@formatter:on
    whereClause = addEntityWhereClause(whereClause);
    final OBQuery<T> query = OBDal.getInstance().createQuery(clazz, whereClause)
            .setNamedParameter("roleId", roleId)
            .setNamedParameter("elementId", getSecuredElementIdentifier(access))
            .setFilterOnActive(false)
            .setMaxResult(1);
    return (InheritedAccessEnabled) query.uniqueResult();
  }

  /**
   * Includes in the where clause some filtering needed for same cases.
   * 
   * @param whereClause
   *          The initial where clause
   * @return Entity where clause with the filtering included
   */
  public String addEntityWhereClause(String whereClause) {
    return whereClause;
  }

  /**
   * Performs the needed parameter substitution according to the entity represented by the injector.
   * 
   * @param query
   *          The query where to perform the parameter substitution
   */
  public <T extends BaseOBObject> void doEntityParameterReplacement(OBQuery<T> query) {
  }

  /**
   * Retrieves the properties of the entity related to the injector that will not be copied when
   * updating an access by propagation.
   * 
   * @return the list of the properties that will not be copied
   */
  public List<String> getSkippedProperties() {
    List<String> skippedProperties = new ArrayList<String>(
        Arrays.asList("creationDate", "createdBy"));
    return skippedProperties;
  }

  /**
   * Sets to null the Inherited From field to child elements when applies (for example, this is used
   * for TabAccess and FieldAccess). This allows the cascade deletion of these elements when
   * removing an inherited Window Access or Tab Access.
   * 
   * @param access
   *          The access to be removed from the parent list
   * @param clearAll
   *          Flag to indicate if the Inherited From field should be nullified in every child
   *          inherited access or not
   */
  public void clearInheritFromFieldInChilds(InheritedAccessEnabled access, boolean clearAll) {
  }

  /**
   * Removes references to child elements from the parent list. Using this method prevents the
   * "deleted object would be re-saved by cascade" error. This can happen, for example, after
   * deleting an inherited TabAccess or FieldAccess.
   * 
   * @param access
   *          The access to be removed from the parent list
   */
  public void removeReferenceInParentList(InheritedAccessEnabled access) {
  }

  /**
   * Sets to null the Inherited From field of an inherited access.
   * 
   * @param access
   *          The access with the Inherit From field to be nullified
   */
  protected void clearInheritedFromField(InheritedAccessEnabled access) {
    String inheritedFromId = access.getInheritedFrom() != null ? access.getInheritedFrom().getId()
        : "";
    if (!StringUtils.isEmpty(inheritedFromId)) {
      access.setInheritedFrom(null);
    }
  }

  /**
   * Sets to null the Inherited From field of an access whenever the value of the field is equal to
   * the entered role id.
   * 
   * @param access
   *          The access with the Inherit From field to be nullified
   * @param roleId
   *          The id of the role used to decide whether the field should be nullified or not
   */
  protected void clearInheritedFromField(InheritedAccessEnabled access, String roleId) {
    String inheritedFromId = access.getInheritedFrom() != null ? access.getInheritedFrom().getId()
        : "";
    if (!StringUtils.isEmpty(inheritedFromId) && roleId.equals(inheritedFromId)) {
      access.setInheritedFrom(null);
    }
  }

  /**
   * Defines the qualifier used to register an access type.
   */
  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface Qualifier {
    /**
     * Retrieves the class of the access type
     */
    Class<? extends InheritedAccessEnabled> value();
  }

  /**
   * A class used to select the correct access type injector.
   */
  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<AccessTypeInjector.Qualifier>
      implements AccessTypeInjector.Qualifier {

    Class<? extends InheritedAccessEnabled> clazz;

    /**
     * Basic constructor
     * 
     * @param className
     *          The name of the class handled by the injector
     * 
     * @throws Exception
     *           In case the class is not found or is not an instance of InheritedAccessEnabled an
     *           exception is thrown
     */
    @SuppressWarnings("unchecked")
    public <T extends InheritedAccessEnabled> Selector(String className) throws Exception {
      this.clazz = (Class<? extends InheritedAccessEnabled>) Class.forName(className);
    }

    @Override
    public Class<? extends InheritedAccessEnabled> value() {
      return this.clazz;
    }
  }
}
