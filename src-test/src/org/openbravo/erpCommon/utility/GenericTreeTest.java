package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.data.FieldProvider;
/** Tests for {@link GenericTree}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class GenericTreeTest {

  private GenericTree instance;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    instance = new GenericTree() {
      @Override
      protected void setRootTree() {
    // No implementation needed
      }

      @Override
      protected void setSubTree(String nodeId, String level) {
    // No implementation needed
      }
      /** Get html description. */

      @Override
      public String getHTMLDescription(String node) {
        return "";
      }

      @Override
      protected boolean isLastLevelNode(String nodeID) {
        return false;
      }

      @Override
      protected String getNodePosition(String nodeID) {
        return "0";
      }

      @Override
      protected String getParent(String node) {
        return "0";
      }
    };
  }
  /**
   * Set language sets lang field.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetLanguageSetsLangField() throws Exception {
    // Arrange
    String language = "en_US";

    // Act
    instance.setLanguage(language);

    // Assert
    Field langField = GenericTree.class.getDeclaredField("lang");
    langField.setAccessible(true);
    assertEquals(language, langField.get(instance));
  }
  /** Get data returns null by default. */

  @Test
  public void testGetDataReturnsNullByDefault() {
    // Act
    FieldProvider[] result = instance.getData();

    // Assert
    assertNull(result);
  }
  /** To html returns empty when data is null. */

  @Test
  public void testToHtmlReturnsEmptyWhenDataIsNull() {
    // Act
    String result = instance.toHtml();

    // Assert
    assertEquals("", result);
  }
  /**
   * To html returns empty when data is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testToHtmlReturnsEmptyWhenDataIsEmpty() throws Exception {
    // Arrange
    Field dataField = GenericTree.class.getDeclaredField("data");
    dataField.setAccessible(true);
    dataField.set(instance, new FieldProvider[0]);

    // Act
    String result = instance.toHtml();

    // Assert
    assertEquals("", result);
  }
  /**
   * Set notifications sets field.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetNotificationsSetsField() throws Exception {
    // Arrange
    String notifications = "<div>Test notification</div>";

    // Act
    instance.setNotifications(notifications);

    // Assert
    Field field = GenericTree.class.getDeclaredField("HTMLNotifications");
    field.setAccessible(true);
    assertEquals(notifications, field.get(instance));
  }
  /**
   * Show notifications sets field.
   * @throws Exception if an error occurs
   */

  @Test
  public void testShowNotificationsSetsField() throws Exception {
    // Act
    instance.showNotifications(true);

    // Assert
    Field field = GenericTree.class.getDeclaredField("showNotifications");
    field.setAccessible(true);
    assertTrue((boolean) field.get(instance));
  }
  /**
   * Default lang is empty.
   * @throws Exception if an error occurs
   */

  @Test
  public void testDefaultLangIsEmpty() throws Exception {
    // Assert
    Field langField = GenericTree.class.getDeclaredField("lang");
    langField.setAccessible(true);
    assertEquals("", langField.get(instance));
  }
  /**
   * Set is sub tree sets field.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetIsSubTreeSetsField() throws Exception {
    // Arrange
    java.lang.reflect.Method setIsSubTree = GenericTree.class.getDeclaredMethod("setIsSubTree", boolean.class);
    setIsSubTree.setAccessible(true);

    // Act
    setIsSubTree.invoke(instance, true);

    // Assert
    Field field = GenericTree.class.getDeclaredField("isSubTree");
    field.setAccessible(true);
    assertTrue((boolean) field.get(instance));
  }
}
