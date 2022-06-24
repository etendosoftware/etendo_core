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
 * All portions are Copyright (C) 2008-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.dal;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.DynamicOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.businesspartner.CategoryAccounts;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test the use of the {@link DynamicOBObject}.
 * 
 * @author mtaal
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DynamicEntityTest extends OBBaseTest {
  private static final Logger log = LogManager.getLogger();

  /**
   * Create a record for the {@link Category} in the database using a {@link DynamicOBObject}.
   */
  @Test
  public void testACreateBPGroup() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    final DynamicOBObject bpGroup = new DynamicOBObject();
    bpGroup.setEntityName(Category.ENTITY_NAME);
    bpGroup.set(Category.PROPERTY_DEFAULT, true);
    bpGroup.set(Category.PROPERTY_DESCRIPTION, "hello world");
    bpGroup.set(Category.PROPERTY_NAME, "hello world");
    bpGroup.set(Category.PROPERTY_SEARCHKEY, "hello world");
    bpGroup.setActive(true);
    OBDal.getInstance().save(bpGroup);
    OBDal.getInstance().commitAndClose();
    printXML(bpGroup);
  }

  /**
   * Queries for the created {@link Category} and then removes.
   */
  @Test
  public void testBRemoveBPGroup() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    addReadWriteAccess(CategoryAccounts.class);
    final OBCriteria<Category> obc = OBDal.getInstance().createCriteria(Category.class);
    obc.add(Restrictions.eq(Category.PROPERTY_NAME, "hello world"));
    final List<Category> bpgs = obc.list();
    assertEquals(1, bpgs.size());
    final Category bog = bpgs.get(0);
    final OBContext obContext = OBContext.getOBContext();
    assertEquals(obContext.getUser().getId(), bog.getCreatedBy().getId());
    assertEquals(obContext.getUser().getId(), bog.getUpdatedBy().getId());

    // first delete the related accounts
    final OBCriteria<CategoryAccounts> obc2 = OBDal.getInstance()
        .createCriteria(CategoryAccounts.class);
    obc2.add(Restrictions.eq(CategoryAccounts.PROPERTY_BUSINESSPARTNERCATEGORY, bpgs.get(0)));
    final List<CategoryAccounts> bogas = obc2.list();
    for (final CategoryAccounts bga : bogas) {
      OBDal.getInstance().refresh(bga);
      OBDal.getInstance().remove(bga);
    }
    OBDal.getInstance().remove(bpgs.get(0));
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Checks if the removal did occur.
   */
  @Test
  public void testCCheckBPGroupRemoved() {
    setTestUserContext();
    addReadWriteAccess(Category.class);
    final OBCriteria<Category> obc = OBDal.getInstance().createCriteria(Category.class);
    obc.add(Restrictions.eq(Category.PROPERTY_NAME, "hello world"));
    final List<Category> bpgs = obc.list();
    assertEquals(0, bpgs.size());
  }

  private void printXML(BaseOBObject bob) {
    // used to print a bit nicer xml
    final String indent = "\t ";

    // get the entity from the runtime model using the entity name of the
    // object
    final String entityName = bob.getEntityName();
    final Entity e = ModelProvider.getInstance().getEntity(entityName);
    // Note: bob.getEntity() also gives the entity of the object

    // print the opening tag
    log.debug("<" + e.getName() + ">");

    // iterate through the properties of the entity
    for (final Property p : e.getProperties()) {

      // and get the value through the dynamic api offered by the
      // BaseOBObject
      final Object value = bob.get(p.getName());

      // handle null, just create an empty tag for that
      if (value == null) {
        log.debug(indent + "<" + p.getName() + "/>");
        continue;
      }

      // make a difference between a primitive and a reference type
      if (p.isPrimitive()) {
        // in reality some form of xml conversion/encoding should take
        // place...
        log.debug(indent + "<" + p.getName() + ">" + value + "</" + p.getName() + ">");
      } else {
        // cast to the parent of all openbravo objects
        final BaseOBObject referencedObject = (BaseOBObject) value;
        // assumes that the id is always a primitive type
        log.debug(
            indent + "<" + p.getName() + ">" + referencedObject.getId() + "</" + p.getName() + ">");
      }
    }

    // and the closing tag
    log.debug("</" + e.getName() + ">");
  }
}
