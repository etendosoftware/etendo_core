/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.weld.test.testinfrastructure;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.arquillian.junit.InSequence;
import org.junit.Test;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;

/**
 * Test cases for cdi infrastructure. Checking Arquillian works fine and it is possible to inject
 * beans.
 * 
 * @author alostale
 *
 */
public class CdiInfrastructure extends WeldBaseTest {

  @Inject
  private ApplicationScopedBean applicationBean;

  @Inject
  private SessionScopedBean sessionBean;

  @Inject
  private RequestScopedBean requestBean;

  @Inject
  @Any
  private Instance<ExtensionBean> extensionBeans;

  /** beans are correctly injected */
  @Test
  public void beansAreInjected() {
    assertThat("application bean is injected", applicationBean, notNullValue());
    assertThat("session bean is injected", sessionBean, notNullValue());
    assertThat("request bean is injected", requestBean, notNullValue());
    assertThat("beans are injected with @Any", extensionBeans.isUnsatisfied(), equalTo(false));
  }

  /** starts application and session scopes */
  @Test
  @InSequence(1)
  public void start() {
    applicationBean.setValue("application");
    sessionBean.setValue("session");
    requestBean.setValue("request");

    assertThat(applicationBean.getValue(), equalTo("application"));
    assertThat(sessionBean.getValue(), equalTo("session"));
    assertThat(requestBean.getValue(), equalTo("request"));
  }

  /** application and session scopes are preserved but not request scope */
  @Test
  @InSequence(2)
  public void applicationAndSessionShouldBeKept() {
    assertThat(applicationBean.getValue(), equalTo("application"));
    assertThat(sessionBean.getValue(), equalTo("session"));
    assertThat(requestBean.getValue(), nullValue());
  }

  /** get any instance of a particular bean type */
  @Test
  public void expectedBeanInstancesAreInjected() {
    assertExtensionBeansInjection(extensionBeans.stream());
  }

  /** get any instance of a particular bean type (using WeldUtils) */
  @Test
  public void expectedBeanInstancesAreInjectedWithWeldUtils() {
    assertExtensionBeansInjection(WeldUtils.getInstances(ExtensionBean.class).stream());
  }

  private void assertExtensionBeansInjection(Stream<ExtensionBean> beans) {
    int numberOfExtensionBeans = 2;

    List<String> names = beans.map(ExtensionBean::getName).collect(Collectors.toList());

    assertThat("Retrieved the expected number of beans", names.size(),
        equalTo(numberOfExtensionBeans));

    assertThat("Retrieved the expected beans", names,
        allOf(hasItem("qualifiedBean"), hasItem("unqualifiedBean")));
  }
}
