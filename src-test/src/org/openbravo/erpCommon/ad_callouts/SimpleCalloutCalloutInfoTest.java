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
package org.openbravo.erpCommon.ad_callouts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.service.json.JsonConstants;

/**
 * Unit tests for {@link SimpleCallout.CalloutInfo}.
 * Instantiated via reflection since its constructor is private.
 * Tests addResult, addSelect, showMessage and context accessor methods.
 */
@DisplayName("SimpleCallout.CalloutInfo")
public class SimpleCalloutCalloutInfoTest {

  private static final String INP_QTY = "inpqty";
  private static final String INP_NAME = "inpname";
  private static final String INP_AMOUNT = "inpamount";
  private static final String INP_FIELD = "inpfield";
  private static final String INP_COMBO = "inpcombo";
  private static final String OPTION_A = "Option A";
  private static final String INP_SHARED = "inpshared";

  private SimpleCallout.CalloutInfo info;
  private VariablesSecureApp vars;

  @BeforeEach
  void setUp() throws Exception {
    vars = mock(VariablesSecureApp.class);
    when(vars.getStringParameter("inpLastFieldChanged")).thenReturn("inpfield1");
    when(vars.getStringParameter(eq("inpTabId"), any(RequestFilter.class))).thenReturn("TAB123");
    when(vars.getStringParameter(eq("inpwindowId"), any(RequestFilter.class))).thenReturn("WIN456");
    when(vars.getNumericParameter(INP_QTY, "0")).thenReturn("25.5");

    Constructor<SimpleCallout.CalloutInfo> ctor =
        SimpleCallout.CalloutInfo.class.getDeclaredConstructor(VariablesSecureApp.class);
    ctor.setAccessible(true);
    info = ctor.newInstance(vars);
  }

  // ── Context accessors ────────────────────────────────────────────────

  @Nested
  @DisplayName("Context accessors")
  class ContextAccessors {

    @Test
    void testGetLastFieldChanged() {
      assertEquals("inpfield1", info.getLastFieldChanged());
    }

    @Test
    void testGetTabId() {
      assertEquals("TAB123", info.getTabId());
    }

    @Test
    void testGetWindowId() {
      assertEquals("WIN456", info.getWindowId());
    }
  }

  // ── addResult ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("addResult")
  class AddResultTests {

    @Test
    void testAddStringResult() throws Exception {
      info.addResult(INP_NAME, "John");
      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has(INP_NAME));
      JSONObject field = result.getJSONObject(INP_NAME);
      assertEquals("John", field.get(CalloutConstants.VALUE));
      assertEquals("John", field.get(CalloutConstants.CLASSIC_VALUE));
    }

    @Test
    void testAddObjectResult() throws Exception {
      info.addResult(INP_AMOUNT, (Object) "100.50");
      JSONObject result = info.getJSONObjectResult();
      assertEquals("100.50", result.getJSONObject(INP_AMOUNT).get(CalloutConstants.VALUE));
    }

    @Test
    void testAddNullResult() throws Exception {
      info.addResult(INP_FIELD, (String) null);
      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has(INP_FIELD));
      assertTrue(result.getJSONObject(INP_FIELD).isNull(CalloutConstants.VALUE));
    }

    @Test
    void testQuotedEmptyStringConvertedToEmpty() throws Exception {
      info.addResult(INP_FIELD, (Object) "\"\"");
      JSONObject field = info.getJSONObjectResult().getJSONObject(INP_FIELD);
      assertEquals("", field.get(CalloutConstants.VALUE));
    }

    @Test
    void testNullStringConvertedToJsonNull() throws Exception {
      info.addResult(INP_FIELD, (Object) "null");
      JSONObject field = info.getJSONObjectResult().getJSONObject(INP_FIELD);
      assertTrue(field.isNull(CalloutConstants.VALUE));
    }

    @Test
    void testOverwritesPreviousValue() throws Exception {
      info.addResult(INP_FIELD, "first");
      info.addResult(INP_FIELD, "second");
      assertEquals("second",
          info.getJSONObjectResult().getJSONObject(INP_FIELD).get(CalloutConstants.VALUE));
    }
  }

  // ── showMessage variants ─────────────────────────────────────────────

  @Nested
  @DisplayName("showMessage methods")
  class ShowMessageTests {

    @Test
    void testShowMessage() throws Exception {
      info.showMessage("Hello");
      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has("MESSAGE"));
      assertEquals("Hello",
          result.getJSONObject("MESSAGE").get(CalloutConstants.VALUE));
    }

    @Test
    void testShowError() throws Exception {
      info.showError("Something failed");
      assertTrue(info.getJSONObjectResult().has("ERROR"));
      assertEquals("Something failed",
          info.getJSONObjectResult().getJSONObject("ERROR").get(CalloutConstants.VALUE));
    }

    @Test
    void testShowWarning() throws Exception {
      info.showWarning("Be careful");
      assertTrue(info.getJSONObjectResult().has("WARNING"));
    }

    @Test
    void testShowInformation() throws Exception {
      info.showInformation("FYI");
      assertTrue(info.getJSONObjectResult().has("INFO"));
    }

    @Test
    void testShowSuccess() throws Exception {
      info.showSuccess("Done!");
      assertTrue(info.getJSONObjectResult().has("SUCCESS"));
    }
  }

  // ── executeCodeInBrowser ─────────────────────────────────────────────

  @Nested
  @DisplayName("executeCodeInBrowser")
  class ExecuteCodeTests {

    @Test
    void testAddsJsExecuteKey() throws Exception {
      info.executeCodeInBrowser("alert('hi');");
      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has("JSEXECUTE"));
      assertEquals("alert('hi');",
          result.getJSONObject("JSEXECUTE").get(CalloutConstants.VALUE));
    }
  }

  // ── addSelect / addSelectResult / endSelect ──────────────────────────

  @Nested
  @DisplayName("Select combo operations")
  class SelectOperations {

    @Test
    void testAddSelectInitializesCombo() {
      info.addSelect(INP_COMBO);
      // Should not throw — combo is initialized
      info.addSelectResult("id1", "Label 1");
    }

    @Test
    void testAddSelectResultUnselected() throws Exception {
      info.addSelect(INP_COMBO);
      info.addSelectResult("id1", OPTION_A);
      info.addSelectResult("id2", "Option B");
      info.endSelect();

      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has(INP_COMBO));
      JSONObject combo = result.getJSONObject(INP_COMBO);
      assertTrue(combo.has(CalloutConstants.ENTRIES));

      JSONArray entries = combo.getJSONArray(CalloutConstants.ENTRIES);
      assertEquals(2, entries.length());
      assertEquals("id1", entries.getJSONObject(0).getString(JsonConstants.ID));
      assertEquals(OPTION_A, entries.getJSONObject(0).getString(JsonConstants.IDENTIFIER));
    }

    @Test
    void testAddSelectResultSelected() throws Exception {
      info.addSelect(INP_COMBO);
      info.addSelectResult("id1", OPTION_A, true);
      info.endSelect();

      JSONObject result = info.getJSONObjectResult();
      JSONObject combo = result.getJSONObject(INP_COMBO);
      assertEquals("id1", combo.get(CalloutConstants.VALUE));
      assertTrue(combo.has(CalloutConstants.ENTRIES));
    }

    @Test
    void testRemoveSelectResult() throws Exception {
      info.addSelect(INP_COMBO);
      info.addSelectResult("id1", OPTION_A);
      info.addSelectResult("id2", "Option B");
      info.removeSelectResult("id1");
      info.endSelect();

      JSONArray entries = info.getJSONObjectResult()
          .getJSONObject(INP_COMBO)
          .getJSONArray(CalloutConstants.ENTRIES);
      assertEquals(1, entries.length());
      assertEquals("id2", entries.getJSONObject(0).getString(JsonConstants.ID));
    }

    @Test
    void testEndSelectEmptyCombo() throws Exception {
      info.addSelect(INP_COMBO);
      info.endSelect();

      JSONArray entries = info.getJSONObjectResult()
          .getJSONObject(INP_COMBO)
          .getJSONArray(CalloutConstants.ENTRIES);
      // Empty combo still has one empty entry
      assertEquals(1, entries.length());
    }
  }

  // ── getStringParameter ───────────────────────────────────────────────

  @Nested
  @DisplayName("getStringParameter")
  class GetStringParameterTests {

    @Test
    void testReturnsValueFromVars() {
      when(vars.getStringParameter(INP_NAME, (RequestFilter) null)).thenReturn("testValue");
      assertEquals("testValue", info.getStringParameter(INP_NAME, null));
    }

    @Test
    void testReturnsModifiedValueFromResult() throws Exception {
      info.addResult(INP_NAME, "modified");
      assertEquals("modified", info.getStringParameter(INP_NAME, null));
    }

    @Test
    void testOverloadWithoutFilter() {
      when(vars.getStringParameter("inpcode", (RequestFilter) null)).thenReturn("ABC");
      assertEquals("ABC", info.getStringParameter("inpcode"));
    }
  }

  // ── getResult / getJSONObjectResult ──────────────────────────────────

  @Nested
  @DisplayName("getResult")
  class GetResultTests {

    @Test
    void testGetResultReturnsJsonString() {
      String result = info.getResult();
      assertNotNull(result);
      assertTrue(result.startsWith("{"));
    }

    @Test
    void testGetJSONObjectResultReturnsObject() {
      assertNotNull(info.getJSONObjectResult());
    }

    @Test
    void testEmptyResultIsEmptyJson() {
      assertEquals("{}", info.getResult());
    }

    @Test
    void testResultReflectsAddedValues() throws Exception {
      info.addResult("inpx", "val");
      String result = info.getResult();
      assertTrue(result.contains("inpx"));
      assertTrue(result.contains("val"));
    }
  }

  // ── getBigDecimalParameter ───────────────────────────────────────────

  @Nested
  @DisplayName("getBigDecimalParameter")
  class GetBigDecimalParameterTests {

    @Test
    void testReturnsParsedValue() throws ServletException {
      when(vars.getNumericParameter(INP_QTY, "0")).thenReturn("25.5");
      BigDecimal result = info.getBigDecimalParameter(INP_QTY);
      assertEquals(new BigDecimal("25.5"), result);
    }

    @Test
    void testReturnsZeroWhenParamMissing() throws ServletException {
      when(vars.getNumericParameter("inpmissing", "0")).thenReturn("0");
      assertEquals(BigDecimal.ZERO, info.getBigDecimalParameter("inpmissing"));
    }

    @Test
    void testLargeNumber() throws ServletException {
      when(vars.getNumericParameter(INP_AMOUNT, "0")).thenReturn("999999.99");
      assertEquals(new BigDecimal("999999.99"), info.getBigDecimalParameter(INP_AMOUNT));
    }

    @Test
    void testNegativeNumber() throws ServletException {
      when(vars.getNumericParameter(INP_AMOUNT, "0")).thenReturn("-50.25");
      assertEquals(new BigDecimal("-50.25"), info.getBigDecimalParameter(INP_AMOUNT));
    }
  }

  // ── executeCallout (callout chaining) ────────────────────────────────

  @Nested
  @DisplayName("executeCallout")
  class ExecuteCalloutTests {

    @Test
    void testChainsCalloutExecution() throws ServletException {
      AtomicBoolean executed = new AtomicBoolean(false);

      SimpleCallout chainedCallout = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo chainedInfo) {
          executed.set(true);
          chainedInfo.addResult("inpchained", "fromChained");
        }
      };

      info.executeCallout(chainedCallout);

      assertTrue(executed.get());
      assertTrue(info.getResult().contains("inpchained"));
      assertTrue(info.getResult().contains("fromChained"));
    }

    @Test
    void testChainedCalloutSharesResult() throws ServletException {
      info.addResult("inporiginal", "originalValue");

      SimpleCallout chainedCallout = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo chainedInfo) {
          // The chained callout can see values from the parent
          chainedInfo.addResult("inpadded", "addedValue");
        }
      };

      info.executeCallout(chainedCallout);

      JSONObject result = info.getJSONObjectResult();
      assertTrue(result.has("inporiginal"));
      assertTrue(result.has("inpadded"));
    }

    @Test
    void testChainedCalloutCanOverwriteValues() throws ServletException {
      info.addResult(INP_SHARED, "before");

      SimpleCallout chainedCallout = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo chainedInfo) {
          chainedInfo.addResult(INP_SHARED, "after");
        }
      };

      info.executeCallout(chainedCallout);

      assertEquals("after",
          info.getJSONObjectResult().optJSONObject(INP_SHARED).optString(CalloutConstants.VALUE));
    }

    @Test
    void testMultipleChainedCallouts() throws ServletException {
      SimpleCallout callout1 = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo ci) {
          ci.addResult("inpfrom1", "val1");
        }
      };
      SimpleCallout callout2 = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo ci) {
          ci.addResult("inpfrom2", "val2");
        }
      };

      info.executeCallout(callout1);
      info.executeCallout(callout2);

      assertTrue(info.getJSONObjectResult().has("inpfrom1"));
      assertTrue(info.getJSONObjectResult().has("inpfrom2"));
    }

    @Test
    void testChainedCalloutExceptionDoesNotCorruptResult() throws ServletException {
      info.addResult("inpsafe", "safeValue");

      SimpleCallout failingCallout = new SimpleCallout() {
        @Override
        protected void execute(CalloutInfo ci) throws ServletException {
          ci.addResult("inpbefore", "ok");
          throw new ServletException("Callout failed");
        }
      };

      try {
        info.executeCallout(failingCallout);
      } catch (ServletException e) {
        // expected
      }

      // Value added before the exception should still be there
      assertTrue(info.getJSONObjectResult().has("inpsafe"));
      assertTrue(info.getJSONObjectResult().has("inpbefore"));
    }
  }
}
