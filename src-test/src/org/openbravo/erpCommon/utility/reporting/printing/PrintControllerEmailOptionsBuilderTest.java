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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility.reporting.printing; //NOSONAR

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.reporting.DocumentType;
import org.openbravo.erpCommon.utility.reporting.Report;
import org.openbravo.erpCommon.utility.reporting.TemplateInfo.EmailDefinition;
import org.openbravo.model.ad.access.User;
import org.openbravo.xmlEngine.XmlDocument;

/**
 * Tests for {@link PrintControllerEmailOptionsBuilder}: {@code Context} value object,
 * and private helper methods exercised through reflection.
 */
@SuppressWarnings({"java:S100", "java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class PrintControllerEmailOptionsBuilderTest {

  private static final String DOC_ID = "DOC-001";
  private static final String FULL_ID = "DOC-001C_INVOICE";
  private static final String CONTACT_NAME = "Alice Smith";
  private static final String CONTACT_EMAIL = "alice@example.com";

  // -------------------------------------------------------------------------
  // instantiation smoke test
  // -------------------------------------------------------------------------

  /** Class can be instantiated via Objenesis without triggering servlet/DAL constructors. */
  @Test
  public void testInstantiation_doesNotThrow() {
    PrintControllerEmailOptionsBuilder builder =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    assertNotNull(builder);
  }

  // -------------------------------------------------------------------------
  // Context inner class
  // -------------------------------------------------------------------------

  /** Context constructor stores documentType correctly. */
  @Test
  public void testContext_documentType_isStored() {
    PrintControllerEmailOptionsBuilder.Context ctx = buildContext();
    assertEquals(DocumentType.SALESINVOICE, ctx.documentType);
  }

  /** Context constructor stores strDocumentId correctly. */
  @Test
  public void testContext_strDocumentId_isStored() {
    PrintControllerEmailOptionsBuilder.Context ctx = buildContext();
    assertEquals(DOC_ID, ctx.strDocumentId);
  }

  /** Context constructor stores the reports map by reference. */
  @Test
  public void testContext_reports_isSameReference() {
    Map<String, org.openbravo.erpCommon.utility.reporting.Report> reports = new HashMap<>();
    PrintControllerEmailOptionsBuilder.Context ctx =
        new PrintControllerEmailOptionsBuilder.Context(
            DocumentType.SALESINVOICE, DOC_ID, reports, new HashMap<>(), FULL_ID);
    assertSame(reports, ctx.reports);
  }

  /** Context constructor stores fullDocumentIdentifier correctly. */
  @Test
  public void testContext_fullDocumentIdentifier_isStored() {
    PrintControllerEmailOptionsBuilder.Context ctx = buildContext();
    assertEquals(FULL_ID, ctx.fullDocumentIdentifier);
  }

  // -------------------------------------------------------------------------
  // resolveToName (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * resolveToName returns empty string when numberOfCustomers is greater than one.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolveToName_multipleCustomers_returnsEmpty() throws Exception {
    String result = invokeResolveToName(null, new PocData[0], 2);
    assertEquals("", result);
  }

  /**
   * resolveToName returns the contact's name when selectedContact is provided.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolveToName_withContact_returnsContactName() throws Exception {
    User contact = mock(User.class);
    when(contact.getName()).thenReturn(CONTACT_NAME);

    String result = invokeResolveToName(contact, new PocData[0], 1);

    assertEquals(CONTACT_NAME, result);
  }

  /**
   * resolveToName falls back to pocData[0].contactName when no contact is selected.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolveToName_noContact_usesPocData() throws Exception {
    PocData poc = new PocData();
    poc.contactName = CONTACT_NAME;

    String result = invokeResolveToName(null, new PocData[]{ poc }, 1);

    assertEquals(CONTACT_NAME, result);
  }

  /**
   * resolveToName returns empty string when no contact and no pocData entries.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolveToName_noContactNoPocData_returnsEmpty() throws Exception {
    String result = invokeResolveToName(null, new PocData[0], 1);
    assertEquals("", result);
  }

  // -------------------------------------------------------------------------
  // getContactField (private static) via reflection
  // -------------------------------------------------------------------------

  /**
   * getContactField returns empty string when the contact is null.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetContactField_nullContact_returnsEmpty() throws Exception {
    String result = invokeGetContactField(null, User::getEmail);
    assertEquals("", result);
  }

  /**
   * getContactField returns the value extracted by the getter.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetContactField_withContact_returnsExtractedValue() throws Exception {
    User contact = mock(User.class);
    when(contact.getEmail()).thenReturn(CONTACT_EMAIL);

    String result = invokeGetContactField(contact, User::getEmail);

    assertEquals(CONTACT_EMAIL, result);
  }

  /**
   * getContactField returns empty string when the getter returns null (via defaultString).
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetContactField_getterReturnsNull_returnsEmpty() throws Exception {
    User contact = mock(User.class);
    when(contact.getEmail()).thenReturn(null);

    String result = invokeGetContactField(contact, User::getEmail);

    assertEquals("", result);
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static PrintControllerEmailOptionsBuilder.Context buildContext() {
    return new PrintControllerEmailOptionsBuilder.Context(
        DocumentType.SALESINVOICE, DOC_ID, new HashMap<>(), new HashMap<>(), FULL_ID);
  }

  private static String invokeResolveToName(User contact, PocData[] pocData,
      int numberOfCustomers) throws Exception {
    PrintControllerEmailOptionsBuilder builder =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "resolveToName", User.class, PocData[].class, int.class);
    m.setAccessible(true);
    return (String) m.invoke(builder, contact, pocData, numberOfCustomers);
  }

  private static String invokeGetContactField(User contact,
      PrintControllerEmailOptionsBuilder.FieldExtractor getter) throws Exception {
    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "getContactField", User.class, PrintControllerEmailOptionsBuilder.FieldExtractor.class);
    m.setAccessible(true);
    return (String) m.invoke(null, contact, getter);
  }

  // -------------------------------------------------------------------------
  // getOptionsList (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * getOptionsList with an empty definition list and not-mandatory adds the blank option placeholder.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetOptionsList_emptyList_notMandatory_hasEmptyOption() throws Exception {
    String result = invokeGetOptionsList(Collections.emptyList(), "", false);
    assertEquals("<option value=\"\"></option>", result);
  }

  /**
   * getOptionsList with an empty definition list and mandatory produces an empty string.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetOptionsList_emptyList_mandatory_returnsEmpty() throws Exception {
    String result = invokeGetOptionsList(Collections.emptyList(), "", true);
    assertEquals("", result);
  }

  /**
   * getOptionsList with one definition not matching the selected value omits the selected attribute.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetOptionsList_oneItem_notSelected_noSelectedAttr() throws Exception {
    EmailDefinition def = mock(EmailDefinition.class);
    when(def.getId()).thenReturn("def-001");
    when(def.getSubject()).thenReturn("Invoice");
    when(def.getLanguage()).thenReturn("en_US");

    String result = invokeGetOptionsList(Collections.singletonList(def), "other-id", false);

    assertTrue(result.contains("value=\"def-001\""));
    assertFalse(result.contains("selected=\"selected\""));
  }

  /**
   * getOptionsList with one definition matching the selected value includes the selected attribute.
   * @throws Exception if reflection fails
   */
  @Test
  public void testGetOptionsList_oneItem_selected_hasSelectedAttr() throws Exception {
    EmailDefinition def = mock(EmailDefinition.class);
    when(def.getId()).thenReturn("def-001");
    when(def.getSubject()).thenReturn("Invoice");
    when(def.getLanguage()).thenReturn("en_US");

    String result = invokeGetOptionsList(Collections.singletonList(def), "def-001", false);

    assertTrue(result.contains("selected=\"selected\""));
    assertTrue(result.contains("value=\"def-001\""));
  }

  // -------------------------------------------------------------------------
  // moreThanOneLanguageDefined (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * moreThanOneLanguageDefined returns false when the reports map is empty.
   * @throws Exception if reflection fails
   */
  @Test
  public void testMoreThanOneLanguageDefined_emptyReports_returnsFalse() throws Exception {
    PrintControllerEmailOptionsBuilder builder = builderWithReports(Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "moreThanOneLanguageDefined");
    m.setAccessible(true);
    boolean result = (Boolean) m.invoke(builder);

    assertFalse(result);
  }

  /**
   * moreThanOneLanguageDefined returns false when each report has only one email definition.
   * @throws Exception if reflection fails or getEmailDefinitions throws
   */
  @Test
  public void testMoreThanOneLanguageDefined_singleDefinition_returnsFalse() throws Exception {
    Report mockReport = mock(Report.class);
    when(mockReport.getEmailDefinitions()).thenReturn(
        Collections.singletonMap("en_US", mock(EmailDefinition.class)));

    PrintControllerEmailOptionsBuilder builder = builderWithReports(
        Collections.singletonMap("DOC001", mockReport));

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "moreThanOneLanguageDefined");
    m.setAccessible(true);
    boolean result = (Boolean) m.invoke(builder);

    assertFalse(result);
  }

  /**
   * moreThanOneLanguageDefined returns true when a report has more than one email definition.
   * @throws Exception if reflection fails or getEmailDefinitions throws
   */
  @Test
  public void testMoreThanOneLanguageDefined_multipleDefinitions_returnsTrue() throws Exception {
    Report mockReport = mock(Report.class);
    Map<String, EmailDefinition> defs = new HashMap<>();
    defs.put("en_US", mock(EmailDefinition.class));
    defs.put("es_ES", mock(EmailDefinition.class));
    when(mockReport.getEmailDefinitions()).thenReturn(defs);

    PrintControllerEmailOptionsBuilder builder = builderWithReports(
        Collections.singletonMap("DOC001", mockReport));

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "moreThanOneLanguageDefined");
    m.setAccessible(true);
    boolean result = (Boolean) m.invoke(builder);

    assertTrue(result);
  }

  // -------------------------------------------------------------------------
  // resolvePreselectedContact (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * resolvePreselectedContact returns null when the ADD command is active (edit mode).
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolvePreselectedContact_addCommand_returnsNull() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD", "DEL")).thenReturn(true);

    PrintControllerEmailOptionsBuilder builder = builderWithVarsAndReports(vars,
        Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "resolvePreselectedContact", PocData[].class);
    m.setAccessible(true);
    User result = (User) m.invoke(builder, new Object[]{ new PocData[]{ new PocData() } });

    assertNull(result);
  }

  /**
   * resolvePreselectedContact returns null when no pocData entries are provided.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolvePreselectedContact_emptyPocData_returnsNull() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD", "DEL")).thenReturn(false);

    PrintControllerEmailOptionsBuilder builder = builderWithVarsAndReports(vars,
        Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "resolvePreselectedContact", PocData[].class);
    m.setAccessible(true);
    User result = (User) m.invoke(builder, new Object[]{ new PocData[0] });

    assertNull(result);
  }

  /**
   * resolvePreselectedContact returns the user from the selector when a contact is found.
   * @throws Exception if reflection or the selector fails
   */
  @Test
  public void testResolvePreselectedContact_withBestContact_returnsContact() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD", "DEL")).thenReturn(false);
    when(vars.getUser()).thenReturn("USER-001");

    User expectedContact = mock(User.class);
    PocData poc = new PocData();
    poc.bpartnerId = "BP-001";

    PrintControllerEmailOptionsBuilder builder = builderWithVarsAndReports(vars,
        Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "resolvePreselectedContact", PocData[].class);
    m.setAccessible(true);

    try (MockedStatic<BPContactEmailSelector> selector = mockStatic(BPContactEmailSelector.class)) {
      selector.when(() -> BPContactEmailSelector.selectBestContact("BP-001", "USER-001"))
          .thenReturn(expectedContact);

      User result = (User) m.invoke(builder, new Object[]{ new PocData[]{ poc } });

      assertSame(expectedContact, result);
    }
  }

  /**
   * resolvePreselectedContact returns null and logs a warning when the selector throws.
   * @throws Exception if reflection fails
   */
  @Test
  public void testResolvePreselectedContact_selectorThrows_returnsNull() throws Exception {
    PrintController ctrl = mock(PrintController.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.commandIn("ADD", "DEL")).thenReturn(false);
    when(vars.getUser()).thenReturn("USER-001");

    PocData poc = new PocData();
    poc.bpartnerId = "BP-001";

    PrintControllerEmailOptionsBuilder builder = builderWithControllerVarsAndReports(ctrl, vars,
        Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "resolvePreselectedContact", PocData[].class);
    m.setAccessible(true);

    try (MockedStatic<BPContactEmailSelector> selector = mockStatic(BPContactEmailSelector.class)) {
      selector.when(() -> BPContactEmailSelector.selectBestContact(any(), any()))
          .thenThrow(new RuntimeException("selector error"));

      User result = (User) m.invoke(builder, new Object[]{ new PocData[]{ poc } });

      assertNull(result);
    }
  }

  // -------------------------------------------------------------------------
  // helpers
  // -------------------------------------------------------------------------

  private static PrintControllerEmailOptionsBuilder.Context buildContext(
      Map<String, Report> reports) {
    return new PrintControllerEmailOptionsBuilder.Context(
        DocumentType.SALESINVOICE, DOC_ID, reports, new HashMap<>(), FULL_ID);
  }

  private static PrintControllerEmailOptionsBuilder builderWithReports(
      Map<String, Report> reports) throws Exception {
    return builderWithControllerVarsAndReports(null, null, reports);
  }

  private static PrintControllerEmailOptionsBuilder builderWithVarsAndReports(
      VariablesSecureApp vars, Map<String, Report> reports) throws Exception {
    return builderWithControllerVarsAndReports(null, vars, reports);
  }

  private static PrintControllerEmailOptionsBuilder builderWithControllerVarsAndReports(
      PrintController ctrl, VariablesSecureApp vars,
      Map<String, Report> reports) throws Exception {
    PrintControllerEmailOptionsBuilder b =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    setBuilderField(b, "controller", ctrl);
    setBuilderField(b, "vars", vars);
    setBuilderField(b, "context", buildContext(reports));
    return b;
  }

  private static void setBuilderField(Object target, String name, Object value) throws Exception {
    Field f = PrintControllerEmailOptionsBuilder.class.getDeclaredField(name);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static String invokeGetOptionsList(List<EmailDefinition> definitions,
      String selectedValue, boolean isMandatory) throws Exception {
    PrintControllerEmailOptionsBuilder b =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "getOptionsList", List.class, String.class, boolean.class);
    m.setAccessible(true);
    return (String) m.invoke(b, definitions, selectedValue, isMandatory);
  }

  // -------------------------------------------------------------------------
  // handleClosedPopup (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * handleClosedPopup returns early when the "closed" parameter is not "yes".
   * @throws Exception if reflection fails
   */
  @Test
  public void testHandleClosedPopup_notClosed_returnsEarly() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter("closed")).thenReturn("");
    HttpServletRequest request = mock(HttpServletRequest.class);
    XmlDocument xmlDoc = mock(XmlDocument.class);

    PrintControllerEmailOptionsBuilder builder =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    setBuilderField(builder, "vars", vars);
    setBuilderField(builder, "request", request);
    setBuilderField(builder, "context", buildContext(Collections.emptyMap()));

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "handleClosedPopup", XmlDocument.class);
    m.setAccessible(true);
    m.invoke(builder, xmlDoc);

    verify(xmlDoc, never()).setParameter(anyString(), anyString());
  }

  /**
   * handleClosedPopup sets the parameter and removes the session attribute when closed=yes.
   * @throws Exception if reflection fails
   */
  @Test
  public void testHandleClosedPopup_closed_setsParameterAndRemovesSession() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter("closed")).thenReturn("yes");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(request.getSession()).thenReturn(session);
    XmlDocument xmlDoc = mock(XmlDocument.class);

    PrintControllerEmailOptionsBuilder builder =
        new ObjenesisStd().newInstance(PrintControllerEmailOptionsBuilder.class);
    setBuilderField(builder, "vars", vars);
    setBuilderField(builder, "request", request);
    setBuilderField(builder, "context", buildContext(Collections.emptyMap()));

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "handleClosedPopup", XmlDocument.class);
    m.setAccessible(true);
    m.invoke(builder, xmlDoc);

    verify(xmlDoc).setParameter("closed", "yes");
    verify(session).removeAttribute(PrintController.SESSION_FILES);
  }

  // -------------------------------------------------------------------------
  // applyEditedEmailParams (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * applyEditedEmailParams calls setParameter exactly 13 times (one per form field).
   * @throws Exception if reflection fails
   */
  @Test
  public void testApplyEditedEmailParams_setsAllParametersFromVars() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter(anyString())).thenReturn("v");
    XmlDocument xmlDoc = mock(XmlDocument.class);

    PrintControllerEmailOptionsBuilder builder = builderWithVarsAndReports(vars,
        Collections.emptyMap());

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "applyEditedEmailParams", XmlDocument.class);
    m.setAccessible(true);
    m.invoke(builder, xmlDoc);

    verify(xmlDoc, times(13)).setParameter(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // applyInitialEmailParams (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * applyInitialEmailParams calls setParameter exactly 13 times (one per form field).
   * @throws Exception if reflection fails
   */
  @Test
  public void testApplyInitialEmailParams_setsAllParametersFromFormData() throws Exception {
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    XmlDocument xmlDoc = mock(XmlDocument.class);

    PrintControllerEmailOptionsBuilder builder = builderWithVarsAndReports(vars,
        Collections.emptyMap());

    Object formData = new ObjenesisStd().newInstance(getEmailFormDataClass());
    for (String fieldName : new String[]{ "fromEmailId", "fromEmail", "toEmail", "toContactId",
        "bccEmail", "bccName", "replyToEmail", "emailSubject", "emailBody" }) {
      setFormDataField(formData, fieldName, "");
    }

    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "applyInitialEmailParams", XmlDocument.class, getEmailFormDataClass());
    m.setAccessible(true);
    m.invoke(builder, xmlDoc, formData);

    verify(xmlDoc, times(13)).setParameter(anyString(), anyString());
  }

  // -------------------------------------------------------------------------
  // fillInitialRecipientData (private) via reflection
  // -------------------------------------------------------------------------

  /**
   * fillInitialRecipientData with a single customer and no contact sets empty toEmail.
   * @throws Exception if reflection fails
   */
  @Test
  public void testFillInitialRecipientData_singleCustomer_noContact_setsEmptyEmail()
      throws Exception {
    PrintControllerEmailOptionsBuilder builder = builderWithReports(Collections.emptyMap());

    Object formData = new ObjenesisStd().newInstance(getEmailFormDataClass());
    setFormDataField(formData, "numberOfCustomers", 1);
    setFormDataField(formData, "selectedContact", null);

    Method m = getFillInitialRecipientDataMethod();
    m.invoke(builder, formData, new HashMap<>(), new PocData[0], null);

    assertEquals("", getFormDataField(formData, "toEmail"));
    assertEquals("", getFormDataField(formData, "toContactId"));
  }

  /**
   * fillInitialRecipientData with an email definition sets subject and body on the form.
   * @throws Exception if reflection fails
   */
  @Test
  public void testFillInitialRecipientData_withEmailDefinition_setsSubjectAndBody()
      throws Exception {
    PrintControllerEmailOptionsBuilder builder = builderWithReports(Collections.emptyMap());

    Object formData = new ObjenesisStd().newInstance(getEmailFormDataClass());
    setFormDataField(formData, "numberOfCustomers", 1);
    setFormDataField(formData, "selectedContact", null);

    EmailDefinition emailDef = mock(EmailDefinition.class);
    when(emailDef.getSubject()).thenReturn("Sub");
    when(emailDef.getBody()).thenReturn("Body");

    Method m = getFillInitialRecipientDataMethod();
    m.invoke(builder, formData, new HashMap<>(), new PocData[0], emailDef);

    assertEquals("Sub", getFormDataField(formData, "emailSubject"));
    assertEquals("Body", getFormDataField(formData, "emailBody"));
  }

  // -------------------------------------------------------------------------
  // EmailFormData helpers
  // -------------------------------------------------------------------------

  private static Class<?> getEmailFormDataClass() {
    for (Class<?> c : PrintControllerEmailOptionsBuilder.class.getDeclaredClasses()) {
      if ("EmailFormData".equals(c.getSimpleName())) {
        return c;
      }
    }
    throw new IllegalStateException("EmailFormData inner class not found");
  }

  private static Method getFillInitialRecipientDataMethod() throws Exception {
    Class<?> efd = getEmailFormDataClass();
    Method m = PrintControllerEmailOptionsBuilder.class.getDeclaredMethod(
        "fillInitialRecipientData", efd, Map.class, PocData[].class, EmailDefinition.class);
    m.setAccessible(true);
    return m;
  }

  private static void setFormDataField(Object formData, String name, Object value)
      throws Exception {
    Field f = getEmailFormDataClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(formData, value);
  }

  private static Object getFormDataField(Object formData, String name) throws Exception {
    Field f = getEmailFormDataClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(formData);
  }
}
