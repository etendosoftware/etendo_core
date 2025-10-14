package com.smf.mobile.utils.webservices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.WSResult;

/**
 * Test class for VersionTest.
 */
@RunWith(MockitoJUnitRunner.class)
public class VersionTest {

  private Version version;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private Module module;

  @Mock
  private OBDal obDal;

  /**
   * Sets up the test environment before each test.
   * Initializes the VersionTest instance.
   */
  @Before
  public void setUp() {
    version = new Version();
  }

  /**
   * Test that the doGet method correctly returns the version of the module.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoGetSuccess() throws Exception {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);

    when(module.getVersion()).thenReturn("1.2.3");

    try (MockedStatic<OBContext> obContextMock = mockStatic(
        OBContext.class); MockedStatic<OBDal> obDalMock = mockStatic(
        OBDal.class); MockedStatic<OBRestUtils> obRestUtilsMock = mockStatic(OBRestUtils.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(Module.class, "0")).thenReturn(module);

      version.doGet(null, request, response);

      printWriter.flush();
      String result = stringWriter.toString();
      JSONObject json = new JSONObject(result);
      assert (json.getString("coreVersion").equals("1.2.3"));

      obContextMock.verify(OBContext::setAdminMode);
      obContextMock.verify(OBContext::restorePreviousMode);

      obRestUtilsMock.verify(() -> OBRestUtils.writeWSResponse(any(WSResult.class), eq(response)));
    }
  }
}
