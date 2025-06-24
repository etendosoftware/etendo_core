package org.openbravo.service.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Warehouse;

/**
 * Tests the getters and setters of the ModuleDependency class.
 * Verifies that the properties are correctly set and retrieved.
 */
public class ClientImportProcessorTest {
  public static final String NEW_CLIENT = "NewClient";
  public static final String ORIGINAL_CLIENT = "OriginalClient";

  @InjectMocks
  private ClientImportProcessor clientImportProcessor;

  @Mock
  private BaseOBObject mockBaseOBObject;

  /**
   * Sets up the test environment before each test.
   * Initializes Mockito annotations and configures the ClientImportProcessor instance.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    clientImportProcessor = new ClientImportProcessor();
    clientImportProcessor.setNewName(NEW_CLIENT);
  }

  /**
   * Tests the process method with new objects.
   * Verifies that the client name, description, and search key are updated correctly.
   */
  @Test
  public void testProcessWithNewObjects() {
    List<BaseOBObject> newObjects = new ArrayList<>();
    Client mockClient = mock(Client.class);
    when(mockClient.getName()).thenReturn(ORIGINAL_CLIENT);
    newObjects.add(mockClient);

    List<BaseOBObject> updatedObjects = new ArrayList<>();

    clientImportProcessor.process(newObjects, updatedObjects);

    verify(mockClient, times(1)).setName(NEW_CLIENT);
    verify(mockClient, times(1)).setDescription(NEW_CLIENT);
    verify(mockClient, times(1)).setSearchKey(NEW_CLIENT);
  }

  /**
   * Tests the replaceValue method with an imported value.
   * Verifies that the imported value is returned as the replacement.
   */
  @Test
  public void testReplaceValueWithImportedValue() {
    Property mockProperty = mock(Property.class);
    Object importedValue = "importedValue";

    Object result = clientImportProcessor.replaceValue(mockBaseOBObject, mockProperty, importedValue);

    assertEquals(importedValue, result);
  }

  /**
   * Tests the replaceName method with a Role object.
   * Verifies that the role name is updated with the new client name.
   */
  @Test
  public void testReplaceNameWithRole() {
    Role mockRole = mock(Role.class);
    when(mockRole.getName()).thenReturn("OriginalRole");

    clientImportProcessor.replaceName(ORIGINAL_CLIENT, mockRole);

    verify(mockRole, times(1)).setName("NewClient_OriginalRole");
  }

  /**
   * Tests the replaceName method with a User object.
   * Verifies that the username is updated with the new client name.
   */
  @Test
  public void testReplaceNameWithUser() {
    User mockUser = mock(User.class);
    when(mockUser.getUsername()).thenReturn("OriginalUser");

    clientImportProcessor.replaceName(ORIGINAL_CLIENT, mockUser);

    verify(mockUser, times(1)).setUsername("NewClient_OriginalUser");
  }

  /**
   * Tests the replaceName method with a Warehouse object.
   * Verifies that the warehouse name is updated with the new client name.
   */
  @Test
  public void testReplaceNameWithWarehouse() {
    Warehouse mockWarehouse = mock(Warehouse.class);
    when(mockWarehouse.getName()).thenReturn("OriginalWarehouse");

    clientImportProcessor.replaceName(ORIGINAL_CLIENT, mockWarehouse);

    verify(mockWarehouse, times(1)).setName("NewClient_OriginalWarehouse");
  }

  /**
   * Tests the replace method with valid values.
   * Verifies that the current value is replaced correctly with the new client name.
   */
  @Test
  public void testReplaceWithValidValues() {
    String currentValue = "OriginalClientRole";
    String originalName = ORIGINAL_CLIENT;

    String result = clientImportProcessor.replace(currentValue, originalName);

    assertEquals("NewClientRole", result);
  }

  /**
   * Tests the replace method with a null value.
   * Verifies that the result is null when the current value is null.
   */
  @Test
  public void testReplaceWithNullValue() {
    String currentValue = null;
    String originalName = ORIGINAL_CLIENT;

    String result = clientImportProcessor.replace(currentValue, originalName);

    assertNull(result);
  }
}
