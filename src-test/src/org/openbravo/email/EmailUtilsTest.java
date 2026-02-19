package org.openbravo.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Tests for {@link EmailUtils}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailUtilsTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBCriteria<EmailServerConfiguration> mockCriteria;

  @Mock
  private Organization mockOrg;

  @Mock
  private org.openbravo.model.ad.system.Client mockClient;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);

    lenient().when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
    lenient().when(mockOBDal.createCriteria(EmailServerConfiguration.class)).thenReturn(mockCriteria);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
  }

  @Test
  public void testGetEmailConfigurationReturnsNullForNullOrg() {
    EmailServerConfiguration result = EmailUtils.getEmailConfiguration(null);
    assertNull(result);
  }

  @Test
  public void testGetEmailConfigurationReturnsConfigForOrgZero() {
    EmailServerConfiguration mockConfig = mock(EmailServerConfiguration.class);
    List<EmailServerConfiguration> configList = new ArrayList<>();
    configList.add(mockConfig);

    when(mockOrg.getId()).thenReturn("0");
    when(mockCriteria.list()).thenReturn(configList);

    EmailServerConfiguration result = EmailUtils.getEmailConfiguration(mockOrg);
    assertEquals(mockConfig, result);
  }

  @Test
  public void testGetEmailConfigurationReturnsNullWhenNoConfigForOrgZero() {
    when(mockOrg.getId()).thenReturn("0");
    when(mockCriteria.list()).thenReturn(Collections.emptyList());

    EmailServerConfiguration result = EmailUtils.getEmailConfiguration(mockOrg);
    assertNull(result);
  }
}
