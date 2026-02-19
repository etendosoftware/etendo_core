package org.openbravo.service.datasource;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;

/**
 * Tests for {@link ComboTableDatasourceService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComboTableDatasourceServiceTest {

  private ComboTableDatasourceService service;

  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    service = objenesis.newInstance(ComboTableDatasourceService.class);
    obContextStatic = mockStatic(OBContext.class);
  }

  @After
  public void tearDown() {
    if (obContextStatic != null) obContextStatic.close();
  }

  @Test(expected = OBException.class)
  public void testRemoveThrowsOBException() {
    Map<String, String> parameters = new HashMap<>();
    service.remove(parameters);
  }

  @Test(expected = OBException.class)
  public void testAddThrowsOBException() {
    Map<String, String> parameters = new HashMap<>();
    service.add(parameters, "{}");
  }

  @Test(expected = OBException.class)
  public void testUpdateThrowsOBException() {
    Map<String, String> parameters = new HashMap<>();
    service.update(parameters, "{}");
  }

  @Test(expected = OBException.class)
  public void testCheckEditDatasourceAccessThrowsOBException() {
    Map<String, String> parameters = new HashMap<>();
    service.checkEditDatasourceAccess(parameters);
  }
}
