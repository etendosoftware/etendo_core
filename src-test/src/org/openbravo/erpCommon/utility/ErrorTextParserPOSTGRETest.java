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
package org.openbravo.erpCommon.utility; // NOSONAR java:S120 package must match the package-private class under test

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import javax.servlet.ServletException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;

/**
 * Unit tests for {@link ErrorTextParserPOSTGRE}. The generated {@link ErrorTextParserData} SQL
 * accessors and the {@link Utility} message helpers are mocked statically, so no database is
 * required.
 */
@RunWith(MockitoJUnitRunner.class)
public class ErrorTextParserPOSTGRETest {

  private static final String LANG = "en_US";
  private static final String ERROR_TYPE = "Error";
  private static final String CONSTRAINT = "ad_user_un_username";
  private static final String UNIQUE_MSG = "ERROR: duplicate key value violates unique constraint \""
      + CONSTRAINT + "\"";
  private static final String LOGGER_NAME = ErrorTextParserPOSTGRE.class.getName();

  @Mock
  private ConnectionProvider conn;
  @Mock
  private VariablesSecureApp vars;
  @Mock
  private FieldProvider fieldProvider;

  private MockedStatic<ErrorTextParserData> dataStatic;
  private MockedStatic<Utility> utilityStatic;
  private ErrorTextParserPOSTGRE parser;

  /** Opens the static mocks and builds a parser wired to mocked connection and vars. */
  @Before
  public void setUp() {
    dataStatic = mockStatic(ErrorTextParserData.class);
    utilityStatic = mockStatic(Utility.class);
    parser = new ErrorTextParserPOSTGRE();
    parser.setConnection(conn);
    parser.setVars(vars);
    parser.setLanguage(LANG);
  }

  /** Closes the static mocks. */
  @After
  public void tearDown() {
    dataStatic.close();
    utilityStatic.close();
  }

  private static ErrorTextParserData constraint(String name, String type) {
    ErrorTextParserData d = new ErrorTextParserData();
    d.constraintName = name;
    d.constraintType = type;
    d.tableName = "ad_user";
    d.searchCondition = "";
    return d;
  }

  private static ErrorTextParserData columns(String names) {
    ErrorTextParserData d = new ErrorTextParserData();
    d.columnname = names;
    return d;
  }

  /**
   * An empty message is not parsed at all.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseReturnsNullForEmptyMessage() throws Exception {
    parser.setMessage("");
    assertNull(parser.parse());
  }

  /**
   * A missing connection is not parsed at all.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseReturnsNullWithoutConnection() throws Exception {
    parser.setConnection(null);
    parser.setMessage(UNIQUE_MSG);
    assertNull(parser.parse());
  }

  /**
   * A message with an @-placeholder is translated after stripping the "ERROR: " prefix and the
   * JDBC "Where:" trailer.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseTranslatesPlaceholderMessageAndStripsPrefixAndWhere() throws Exception {
    parser.setMessage("ERROR: @NotEnoughStock@\n  Where: PL/pgSQL function x line 1");
    utilityStatic.when(() -> Utility.parseTranslation(conn, vars, LANG, "@NotEnoughStock@"))
        .thenReturn("Not enough stock");

    OBError error = parser.parse();

    assertEquals(ERROR_TYPE, error.getType());
    assertEquals("Not enough stock", error.getMessage());
    dataStatic.verify(() -> ErrorTextParserData.select(any(), anyString()), never());
  }

  /**
   * A placeholder message without prefix nor Where trailer is translated as is.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseTranslatesPlaceholderMessageWithoutPrefix() throws Exception {
    parser.setMessage("@CODE=0@Some text");
    utilityStatic.when(() -> Utility.parseTranslation(conn, vars, LANG, "@CODE=0@Some text"))
        .thenReturn("translated");

    assertEquals("translated", parser.parse().getMessage());
  }

  /**
   * A short "ERROR: " message (length not greater than 7) keeps its prefix.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseTranslatesShortPlaceholderMessage() throws Exception {
    parser.setMessage("@A@");
    utilityStatic.when(() -> Utility.parseTranslation(conn, vars, LANG, "@A@")).thenReturn("ok");

    assertEquals("ok", parser.parse().getMessage());
  }

  /**
   * Without any quoted name the original message is returned.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseReturnsOriginalMessageWhenNoConstraintNameFound() throws Exception {
    String msg = "ERROR: Unit of Measure mismatch (product/transaction)";
    parser.setMessage(msg);

    OBError error = parser.parse();

    assertEquals(ERROR_TYPE, error.getType());
    assertEquals(msg, error.getMessage());
    dataStatic.verify(() -> ErrorTextParserData.select(any(), anyString()), never());
  }

  /**
   * A single quoted name is used as constraint; a null lookup yields the original message.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseReturnsOriginalMessageWhenConstraintLookupIsNull() throws Exception {
    parser.setMessage(UNIQUE_MSG);
    dataStatic.when(() -> ErrorTextParserData.select(conn, CONSTRAINT)).thenReturn(null);

    OBError error = parser.parse();

    assertEquals(UNIQUE_MSG, error.getMessage());
    dataStatic.verify(() -> ErrorTextParserData.select(conn, CONSTRAINT));
  }

  /**
   * With two quoted names the second one is used; an empty lookup yields the original message.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseUsesSecondQuotedNameAndHandlesEmptyLookup() throws Exception {
    String msg = "ERROR:  insert or update on table \"ad_attachment\" violates foreign key "
        + "constraint \"addatatype_adattachment\"";
    parser.setMessage(msg);
    dataStatic.when(() -> ErrorTextParserData.select(conn, "addatatype_adattachment"))
        .thenReturn(new ErrorTextParserData[0]);

    assertEquals(msg, parser.parse().getMessage());
    dataStatic.verify(() -> ErrorTextParserData.select(conn, "addatatype_adattachment"));
  }

  /**
   * Spanish style guillemet quotes are recognized as well.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseRecognizesGuillemetQuotes() throws Exception {
    String msg = "ERROR:  inserción o actualización en la tabla «c_bpartner» viola la llave "
        + "foránea «c_bpartner_c_bp_group»";
    parser.setMessage(msg);

    assertEquals(msg, parser.parse().getMessage());
    dataStatic.verify(() -> ErrorTextParserData.select(conn, "c_bpartner_c_bp_group"));
  }

  /**
   * A constraint with a dedicated AD message is returned via the shared handler.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseReturnsHandledConstraintError() throws Exception {
    parser.setMessage(UNIQUE_MSG);
    dataStatic.when(() -> ErrorTextParserData.select(conn, CONSTRAINT))
        .thenReturn(new ErrorTextParserData[] { constraint(CONSTRAINT, "U") });
    utilityStatic.when(() -> Utility.locateMessage(conn, CONSTRAINT, LANG))
        .thenReturn(fieldProvider);
    lenient().when(fieldProvider.getField("msgtype")).thenReturn("E");
    lenient().when(fieldProvider.getField("msgtext")).thenReturn("User name already exists");

    OBError error = parser.parse();

    assertEquals(ERROR_TYPE, error.getType());
    assertEquals("User name already exists", error.getMessage());
  }

  /**
   * A constraint that the shared handler cannot handle falls back to the original message.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseFallsBackToOriginalMessageWhenHandlerReturnsNull() throws Exception {
    parser.setMessage(UNIQUE_MSG);
    dataStatic.when(() -> ErrorTextParserData.select(conn, CONSTRAINT))
        .thenReturn(new ErrorTextParserData[] { constraint(CONSTRAINT, "X") });

    OBError error = parser.parse();

    assertEquals(ERROR_TYPE, error.getType());
    assertEquals(UNIQUE_MSG, error.getMessage());
  }

  /**
   * Parsing with debug logging enabled exercises the debug-only branch.
   *
   * @throws Exception if the code under test fails
   */
  @Test
  public void parseWithDebugLoggingEnabled() throws Exception {
    Level previous = LogManager.getLogger(LOGGER_NAME).getLevel();
    Configurator.setLevel(LOGGER_NAME, Level.DEBUG);
    try {
      String msg = "plain message";
      parser.setMessage(msg);
      assertEquals(msg, parser.parse().getMessage());
    } finally {
      Configurator.setLevel(LOGGER_NAME, previous);
    }
  }

  /** Quoted column names are unquoted and sorted. */
  @Test
  public void getColumnNamesForConstraintUnquotesAndSorts() {
    dataStatic.when(
        () -> ErrorTextParserData.selectColumnNamesForConstraint(eq(conn), anyString(), eq("UK")))
        .thenReturn(new ErrorTextParserData[] { columns("\"username\",\"ad_client_id\",x") });

    String[] result = parser.getColumnNamesForConstraint("UK");

    assertArrayEquals(new String[] { "ad_client_id", "username", "x" }, result);
  }

  /** A null lookup result yields a single empty column name. */
  @Test
  public void getColumnNamesForConstraintHandlesNullResult() {
    dataStatic.when(
        () -> ErrorTextParserData.selectColumnNamesForConstraint(eq(conn), anyString(), eq("UK")))
        .thenReturn(null);

    assertArrayEquals(new String[] { "" }, parser.getColumnNamesForConstraint("UK"));
  }

  /** An empty lookup result yields a single empty column name. */
  @Test
  public void getColumnNamesForConstraintHandlesEmptyResult() {
    dataStatic.when(
        () -> ErrorTextParserData.selectColumnNamesForConstraint(eq(conn), anyString(), eq("UK")))
        .thenReturn(new ErrorTextParserData[0]);

    assertArrayEquals(new String[] { "" }, parser.getColumnNamesForConstraint("UK"));
  }

  /** A ServletException during lookup is logged and an empty column name is returned. */
  @Test
  public void getColumnNamesForConstraintHandlesServletException() {
    dataStatic.when(
        () -> ErrorTextParserData.selectColumnNamesForConstraint(eq(conn), anyString(), eq("UK")))
        .thenThrow(new ServletException("db down"));

    String[] result = parser.getColumnNamesForConstraint("UK");

    assertNotNull(result);
    assertArrayEquals(new String[] { "" }, result);
  }
}
