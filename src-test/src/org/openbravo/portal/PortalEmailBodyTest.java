package org.openbravo.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the {@link PortalEmailBody} class.
 * Verifies the behavior of methods that retrieve client name, URL, and contact email
 * based on preferences and mocked data.
 */
@ExtendWith(MockitoExtension.class)
public class PortalEmailBodyTest {

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<Preferences> mockedPreferences;
  private PortalEmailBody portalEmailBody;

  private Client mockClient;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static methods and dependencies.
   */
  @BeforeEach
  public void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedPreferences = mockStatic(Preferences.class);

    mockClient = mock(Client.class);
    Organization mockOrganization = mock(Organization.class);
    OBContext mockOBContext = mock(OBContext.class);

    when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    portalEmailBody = new PortalEmailBody() {
    };
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedPreferences != null) {
      mockedPreferences.close();
    }
  }

  /**
   * Tests the `getClientName` method.
   * Verifies that the client name is retrieved correctly from the mocked client.
   */
  @Test
  public void testGetClientName() {
    when(mockClient.getName()).thenReturn("OpenbravoTestClient");

    String clientName = portalEmailBody.getClientName();

    assertEquals("OpenbravoTestClient", clientName);
  }

  /**
   * Tests the `getUrl` method when the "PortalURL" preference exists.
   * Verifies that the URL is retrieved correctly from the mocked preference.
   */
  @Test
  public void testGetUrlWhenPreferenceExists() {
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq("PortalURL"), eq(true), any(Client.class), any(), isNull(), isNull(),
            isNull())).thenReturn("http://test.url");

    String url = portalEmailBody.getUrl();

    assertEquals("http://test.url", url);
  }

  /**
   * Tests the `getUrl` method when the "PortalURL" preference is not set.
   * Verifies that an empty string is returned when the preference is missing.
   */
  @Test
  public void testGetUrlWhenPreferenceNotSet() {
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq("PortalURL"), eq(true), any(Client.class), any(), isNull(), isNull(),
            isNull())).thenThrow(new PropertyException("Preference not set"));

    String url = portalEmailBody.getUrl();

    assertEquals("", url);
  }

  /**
   * Tests the `getContactEmail` method when the "PortalContactEmail" preference exists.
   * Verifies that the contact email is retrieved correctly from the mocked preference.
   */
  @Test
  public void testGetContactEmailWhenPreferenceExists() {
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq("PortalContactEmail"), eq(true), any(Client.class), any(), isNull(),
            isNull(), isNull())).thenReturn("contact@test.email");

    String email = portalEmailBody.getContactEmail();

    assertEquals("contact@test.email", email);
  }

  /**
   * Tests the `getContactEmail` method when the "PortalContactEmail" preference is not set.
   * Verifies that an empty string is returned when the preference is missing.
   */
  @Test
  public void testGetContactEmailWhenPreferenceNotSet() {
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq("PortalContactEmail"), eq(true), any(Client.class), any(), isNull(),
            isNull(), isNull())).thenThrow(new PropertyException("Preference not set"));

    String email = portalEmailBody.getContactEmail();

    assertEquals("", email);
  }
}
