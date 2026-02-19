package org.openbravo.erpCommon.ad_actionButton;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

@RunWith(MockitoJUnitRunner.class)
public class ExportReferenceDataTest {

  private ExportReferenceData instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ExportReferenceData.class);
  }

  @Test
  public void testGetServletInfo() {
    assertEquals("Servlet Copy from settlement", instance.getServletInfo());
  }
}
