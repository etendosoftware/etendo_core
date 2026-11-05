/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.dal.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.payment.DebtPayment;

/**
 * Unit tests for {@link PrimitiveReferenceHandler}. Covers singleton handling, discovery of
 * primitive reference properties per entity, and resolution of the referenced entity for each
 * supported entity type, including the error paths.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrimitiveReferenceHandlerTest {

  private static final String UNKNOWN_ENTITY = "UnknownEntity";
  private static final String OTHER_PROPERTY = "otherProperty";
  private static final String TABLE_NAME = "C_Order";
  private static final String TABLE_ID = "259";
  private static final String TREE_TYPE = "OO";

  @Mock
  private ModelProvider modelProvider;
  @Mock
  private Entity targetEntity;

  private MockedStatic<ModelProvider> modelProviderStatic;
  private PrimitiveReferenceHandler handler;

  /**
   * Creates a fresh handler and mocks the static ModelProvider accessor.
   */
  @Before
  public void setUp() {
    handler = new PrimitiveReferenceHandler();
    modelProviderStatic = mockStatic(ModelProvider.class);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(modelProvider);
  }

  /**
   * Closes static mocks and resets the singleton instance.
   */
  @After
  public void tearDown() {
    modelProviderStatic.close();
    PrimitiveReferenceHandler.setInstance(null);
  }

  private static Entity entityNamed(String name) {
    Entity entity = mock(Entity.class);
    lenient().when(entity.getName()).thenReturn(name);
    return entity;
  }

  private static Property propertyOf(String entityName, String propertyName) {
    Entity entity = entityNamed(entityName);
    Property property = mock(Property.class);
    lenient().when(property.getEntity()).thenReturn(entity);
    lenient().when(property.getName()).thenReturn(propertyName);
    return property;
  }

  private static Table tableNamed(String name) {
    Table table = mock(Table.class);
    lenient().when(table.getName()).thenReturn(name);
    return table;
  }

  /**
   * Verifies getInstance obtains the instance from OBProvider once and caches it.
   */
  @Test
  public void getInstanceUsesOBProviderAndCaches() {
    PrimitiveReferenceHandler.setInstance(null);
    OBProvider provider = mock(OBProvider.class);
    PrimitiveReferenceHandler provided = new PrimitiveReferenceHandler();
    when(provider.get(PrimitiveReferenceHandler.class)).thenReturn(provided);
    try (MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {
      obProviderStatic.when(OBProvider::getInstance).thenReturn(provider);
      assertSame(provided, PrimitiveReferenceHandler.getInstance());
      assertSame(provided, PrimitiveReferenceHandler.getInstance());
      obProviderStatic.verify(OBProvider::getInstance);
    }
  }

  /**
   * Verifies setInstance replaces the singleton returned by getInstance.
   */
  @Test
  public void setInstanceOverridesSingleton() {
    PrimitiveReferenceHandler.setInstance(handler);
    assertSame(handler, PrimitiveReferenceHandler.getInstance());
  }

  /**
   * Verifies TreeNode primitive references are built once and then served from cache.
   */
  @Test
  public void getPrimitiveReferencesForTreeNode() {
    Entity entity = entityNamed(TreeNode.ENTITY_NAME);
    Property reportSet = mock(Property.class);
    Property node = mock(Property.class);
    when(entity.getProperty(TreeNode.PROPERTY_REPORTSET)).thenReturn(reportSet);
    when(entity.getProperty(TreeNode.PROPERTY_NODE)).thenReturn(node);

    List<Property> result = handler.getPrimitiveReferences(entity);
    assertEquals(2, result.size());
    assertSame(reportSet, result.get(0));
    assertSame(node, result.get(1));
    assertSame(result, handler.getPrimitiveReferences(entity));
  }

  /**
   * Verifies Alert primitive references are built once and then served from cache.
   */
  @Test
  public void getPrimitiveReferencesForAlert() {
    Entity entity = entityNamed(Alert.ENTITY_NAME);
    Property key = mock(Property.class);
    when(entity.getProperty(Alert.PROPERTY_REFERENCESEARCHKEY)).thenReturn(key);

    List<Property> result = handler.getPrimitiveReferences(entity);
    assertEquals(1, result.size());
    assertSame(key, result.get(0));
    assertSame(result, handler.getPrimitiveReferences(entity));
  }

  /**
   * Verifies Attachment primitive references are built once and then served from cache.
   */
  @Test
  public void getPrimitiveReferencesForAttachment() {
    Entity entity = entityNamed(Attachment.ENTITY_NAME);
    Property recordProperty = mock(Property.class);
    when(entity.getProperty(Attachment.PROPERTY_RECORD)).thenReturn(recordProperty);

    List<Property> result = handler.getPrimitiveReferences(entity);
    assertEquals(1, result.size());
    assertSame(recordProperty, result.get(0));
    assertSame(result, handler.getPrimitiveReferences(entity));
  }

  /**
   * Verifies AccountingFact primitive references are built once and then served from cache.
   */
  @Test
  public void getPrimitiveReferencesForAccountingFact() {
    Entity entity = entityNamed(AccountingFact.ENTITY_NAME);
    Property recordId = mock(Property.class);
    Property recordId2 = mock(Property.class);
    when(entity.getProperty(AccountingFact.PROPERTY_RECORDID)).thenReturn(recordId);
    when(entity.getProperty(AccountingFact.PROPERTY_RECORDID2)).thenReturn(recordId2);

    List<Property> result = handler.getPrimitiveReferences(entity);
    assertEquals(2, result.size());
    assertSame(recordId, result.get(0));
    assertSame(recordId2, result.get(1));
    assertSame(result, handler.getPrimitiveReferences(entity));
  }

  /**
   * Verifies an entity without primitive references raises an OBException.
   */
  @Test
  public void getPrimitiveReferencesForUnknownEntityThrows() {
    Entity entity = entityNamed(UNKNOWN_ENTITY);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferences(entity));
  }

  /**
   * Verifies isPrimitiveReference recognises the TreeNode reference properties only.
   */
  @Test
  public void isPrimitiveReferenceForTreeNode() {
    assertTrue(handler
        .isPrimitiveReference(propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_REPORTSET)));
    assertTrue(
        handler.isPrimitiveReference(propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_NODE)));
    assertFalse(handler.isPrimitiveReference(propertyOf(TreeNode.ENTITY_NAME, OTHER_PROPERTY)));
  }

  /**
   * Verifies isPrimitiveReference recognises the Alert reference property only.
   */
  @Test
  public void isPrimitiveReferenceForAlert() {
    assertTrue(handler
        .isPrimitiveReference(propertyOf(Alert.ENTITY_NAME, Alert.PROPERTY_REFERENCESEARCHKEY)));
    assertFalse(handler.isPrimitiveReference(propertyOf(Alert.ENTITY_NAME, OTHER_PROPERTY)));
  }

  /**
   * Verifies isPrimitiveReference recognises the Attachment reference property only.
   */
  @Test
  public void isPrimitiveReferenceForAttachment() {
    assertTrue(handler
        .isPrimitiveReference(propertyOf(Attachment.ENTITY_NAME, Attachment.PROPERTY_RECORD)));
    assertFalse(handler.isPrimitiveReference(propertyOf(Attachment.ENTITY_NAME, OTHER_PROPERTY)));
  }

  /**
   * Verifies isPrimitiveReference recognises the AccountingFact reference properties only.
   */
  @Test
  public void isPrimitiveReferenceForAccountingFact() {
    assertTrue(handler.isPrimitiveReference(
        propertyOf(AccountingFact.ENTITY_NAME, AccountingFact.PROPERTY_RECORDID)));
    assertTrue(handler.isPrimitiveReference(
        propertyOf(AccountingFact.ENTITY_NAME, AccountingFact.PROPERTY_RECORDID2)));
    assertFalse(
        handler.isPrimitiveReference(propertyOf(AccountingFact.ENTITY_NAME, OTHER_PROPERTY)));
  }

  /**
   * Verifies isPrimitiveReference returns false for properties of unrelated entities.
   */
  @Test
  public void isPrimitiveReferenceForUnknownEntity() {
    assertFalse(handler.isPrimitiveReference(propertyOf(UNKNOWN_ENTITY, TreeNode.PROPERTY_NODE)));
  }

  /**
   * Verifies hasObjectPrimitiveReference is true for every supported entity and false otherwise.
   */
  @Test
  public void hasObjectPrimitiveReference() {
    String[] supported = { TreeNode.ENTITY_NAME, Alert.ENTITY_NAME, Attachment.ENTITY_NAME,
        AccountingFact.ENTITY_NAME };
    for (String name : supported) {
      BaseOBObject obj = mock(BaseOBObject.class);
      Entity entity = entityNamed(name);
      when(obj.getEntity()).thenReturn(entity);
      assertTrue(name, handler.hasObjectPrimitiveReference(obj));
    }
    BaseOBObject other = mock(BaseOBObject.class);
    Entity otherEntity = entityNamed(UNKNOWN_ENTITY);
    when(other.getEntity()).thenReturn(otherEntity);
    assertFalse(handler.hasObjectPrimitiveReference(other));
  }

  private TreeNode treeNodeWith(Table table) {
    TreeNode treeNode = mock(TreeNode.class);
    Tree tree = mock(Tree.class);
    when(treeNode.getTree()).thenReturn(tree);
    lenient().when(tree.getTypeArea()).thenReturn(TREE_TYPE);
    lenient().when(tree.getTable()).thenReturn(table);
    return treeNode;
  }

  /**
   * Verifies a TreeNode reference is resolved through its tree type.
   */
  @Test
  public void referencedEntityForTreeNodeByTreeType() {
    TreeNode treeNode = treeNodeWith(null);
    when(modelProvider.getEntityFromTreeType(TREE_TYPE)).thenReturn(targetEntity);
    Property property = propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_NODE);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(treeNode, property));
  }

  /**
   * Verifies a TreeNode reference falls back to the tree table when the tree type is unknown.
   */
  @Test
  public void referencedEntityForTreeNodeByTable() {
    Table table = mock(Table.class);
    when(table.getId()).thenReturn(TABLE_ID);
    TreeNode treeNode = treeNodeWith(table);
    when(modelProvider.getEntityFromTreeType(TREE_TYPE)).thenReturn(null);
    when(modelProvider.getEntityByTableId(TABLE_ID)).thenReturn(targetEntity);
    Property property = propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_NODE);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(treeNode, property));
  }

  /**
   * Verifies an OBException when neither tree type nor tree table resolves an entity.
   */
  @Test
  public void referencedEntityForTreeNodeTableNotFoundThrows() {
    Table table = mock(Table.class);
    when(table.getId()).thenReturn(TABLE_ID);
    TreeNode treeNode = treeNodeWith(table);
    when(modelProvider.getEntityFromTreeType(TREE_TYPE)).thenReturn(null);
    when(modelProvider.getEntityByTableId(TABLE_ID)).thenReturn(null);
    Property property = propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_NODE);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferencedEntity(treeNode, property));
  }

  /**
   * Verifies an OBException when the tree type is unknown and the tree has no table.
   */
  @Test
  public void referencedEntityForTreeNodeWithoutTableThrows() {
    TreeNode treeNode = treeNodeWith(null);
    when(modelProvider.getEntityFromTreeType(TREE_TYPE)).thenReturn(null);
    Property property = propertyOf(TreeNode.ENTITY_NAME, TreeNode.PROPERTY_NODE);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferencedEntity(treeNode, property));
  }

  private Alert alertWith(Table table) {
    Alert alert = mock(Alert.class);
    AlertRule rule = mock(AlertRule.class);
    Tab tab = mock(Tab.class);
    when(alert.getAlertRule()).thenReturn(rule);
    when(rule.getTab()).thenReturn(tab);
    when(tab.getTable()).thenReturn(table);
    return alert;
  }

  /**
   * Verifies an Alert reference is resolved through the alert rule tab table.
   */
  @Test
  public void referencedEntityForAlert() {
    Alert alert = alertWith(tableNamed(TABLE_NAME));
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(targetEntity);
    Property property = propertyOf(Alert.ENTITY_NAME, Alert.PROPERTY_REFERENCESEARCHKEY);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(alert, property));
  }

  /**
   * Verifies an OBException when the Alert table does not map to an entity.
   */
  @Test
  public void referencedEntityForAlertNotFoundThrows() {
    Alert alert = alertWith(tableNamed(TABLE_NAME));
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(null);
    Property property = propertyOf(Alert.ENTITY_NAME, Alert.PROPERTY_REFERENCESEARCHKEY);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferencedEntity(alert, property));
  }

  /**
   * Verifies an Attachment reference is resolved through the attachment table.
   */
  @Test
  public void referencedEntityForAttachment() {
    Attachment attachment = mock(Attachment.class);
    Table table = tableNamed(TABLE_NAME);
    when(attachment.getTable()).thenReturn(table);
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(targetEntity);
    Property property = propertyOf(Attachment.ENTITY_NAME, Attachment.PROPERTY_RECORD);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(attachment, property));
  }

  /**
   * Verifies an OBException when the Attachment table does not map to an entity.
   */
  @Test
  public void referencedEntityForAttachmentNotFoundThrows() {
    Attachment attachment = mock(Attachment.class);
    Table table = tableNamed(TABLE_NAME);
    when(attachment.getTable()).thenReturn(table);
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(null);
    Property property = propertyOf(Attachment.ENTITY_NAME, Attachment.PROPERTY_RECORD);
    assertThrows(OBException.class,
        () -> handler.getPrimitiveReferencedEntity(attachment, property));
  }

  /**
   * Verifies the AccountingFact recordID2 reference always resolves to DebtPayment.
   */
  @Test
  public void referencedEntityForAccountingFactRecordId2() {
    AccountingFact fact = mock(AccountingFact.class);
    when(modelProvider.getEntity(DebtPayment.ENTITY_NAME)).thenReturn(targetEntity);
    Property property = propertyOf(AccountingFact.ENTITY_NAME, AccountingFact.PROPERTY_RECORDID2);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(fact, property));
  }

  /**
   * Verifies the AccountingFact recordID reference is resolved through the fact table.
   */
  @Test
  public void referencedEntityForAccountingFactRecordId() {
    AccountingFact fact = mock(AccountingFact.class);
    Table table = tableNamed(TABLE_NAME);
    when(fact.getTable()).thenReturn(table);
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(targetEntity);
    Property property = propertyOf(AccountingFact.ENTITY_NAME, AccountingFact.PROPERTY_RECORDID);
    assertSame(targetEntity, handler.getPrimitiveReferencedEntity(fact, property));
  }

  /**
   * Verifies an OBException when the AccountingFact table does not map to an entity.
   */
  @Test
  public void referencedEntityForAccountingFactNotFoundThrows() {
    AccountingFact fact = mock(AccountingFact.class);
    Table table = tableNamed(TABLE_NAME);
    when(fact.getTable()).thenReturn(table);
    when(modelProvider.getEntity(TABLE_NAME)).thenReturn(null);
    Property property = propertyOf(AccountingFact.ENTITY_NAME, AccountingFact.PROPERTY_RECORDID);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferencedEntity(fact, property));
  }

  /**
   * Verifies an OBException when the property does not belong to a supported entity.
   */
  @Test
  public void referencedEntityForUnknownEntityThrows() {
    BaseOBObject obj = mock(BaseOBObject.class);
    Property property = propertyOf(UNKNOWN_ENTITY, OTHER_PROPERTY);
    assertThrows(OBException.class, () -> handler.getPrimitiveReferencedEntity(obj, property));
  }
}
