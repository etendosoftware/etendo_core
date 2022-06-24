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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.freemarker.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.I18NComponent;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.SessionDynamicTemplateComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;

/**
 * Test the {@link I18NComponent}, test that a translated label actually is used inside of the label
 * template.
 * 
 * @author mtaal
 */

public class LabelTest extends WeldBaseTest {

  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<SessionDynamicTemplateComponent> components;

  @Before
  public void setUpLt() throws Exception {
    // super.setUp();
    setSystemAdministratorContext();
  }

  @Test
  public void testLabel() throws Exception {
    SessionDynamicTemplateComponent i18nComponent = getI18NComponent();
    final Module module = OBDal.getInstance().get(Module.class, i18nComponent.getModule().getId());
    module.setInDevelopment(true);
    OBDal.getInstance().flush();

    final String msgKeyWithTranslation = "OBCLKER_MSG_WITH_TRANSLATION";
    final String msgTextWithTranslation = "WITH TRANSLATION";
    final String msgTextTranslated = "TRANSLATED";
    final String msgKeyWithOutTranslation = "OBCLKER_MSG_WITHOUT_TRANSLATION";
    final String msgTextWithOutTranslation = "WITHOUT TRANSLATION";

    // create a message
    final Message messageWithTrl = OBProvider.getInstance().get(Message.class);
    messageWithTrl.setModule(module);
    messageWithTrl.setMessageText(msgTextWithTranslation);
    messageWithTrl.setSearchKey(msgKeyWithTranslation);

    final MessageTrl messageTrl = OBProvider.getInstance().get(MessageTrl.class);
    messageTrl.setMessage(messageWithTrl);
    messageTrl.setMessageText(msgTextTranslated);
    messageTrl.setTranslation(true);
    messageTrl.setLanguage(
        OBDal.getInstance().get(Language.class, OBContext.getOBContext().getLanguage().getId()));
    messageWithTrl.getADMessageTrlList().add(messageTrl);
    OBDal.getInstance().save(messageWithTrl);

    final Message messageWithOutTrl = OBProvider.getInstance().get(Message.class);
    messageWithOutTrl.setModule(module);
    messageWithOutTrl.setMessageText(msgTextWithOutTranslation);
    messageWithOutTrl.setSearchKey(msgKeyWithOutTranslation);
    OBDal.getInstance().save(messageWithOutTrl);

    OBDal.getInstance().flush();

    // generate the javascript and check if the above strings are present
    final String output = i18nComponent.generate();
    log.debug(output);

    // do some checks
    assertTrue(output.contains(msgKeyWithOutTranslation));
    assertTrue(output.contains(msgTextWithOutTranslation));
    assertTrue(output.contains(msgKeyWithTranslation));
    assertTrue(output.contains(msgTextTranslated));
    assertFalse(output.contains(msgTextWithTranslation));

    // and prevent the database from being updated
    OBDal.getInstance().rollbackAndClose();
  }

  private SessionDynamicTemplateComponent getI18NComponent() {
    for (SessionDynamicTemplateComponent component : components) {
      if (component.getId().equals(KernelConstants.LABELS_COMPONENT_ID)) {
        return component;
      }
    }
    return null;
  }
}
