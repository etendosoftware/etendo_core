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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;

/**
 * Generates the client side javascript holding the labels defined in modules and the labels defined
 * in core which are configured to be forcibly available in the client.
 * 
 * @author mtaal
 */
public class I18NComponent extends SessionDynamicTemplateComponent {
  private static Map<String, String> cachedLabels = new ConcurrentHashMap<>();

  @Override
  public String getId() {
    return KernelConstants.LABELS_COMPONENT_ID;
  }

  @Override
  protected String getTemplateId() {
    return KernelConstants.I18N_TEMPLATE_ID;
  }

  @Override
  public String generate() {
    String languageId = OBContext.getOBContext().getLanguage().getId();
    if (cachedLabels.containsKey(languageId)) {
      return cachedLabels.get(languageId);
    }
    String labels = super.generate();
    if (!isInDevelopment()) {
      cachedLabels.put(languageId, labels);
    }
    return labels;
  }

  /**
   * Read the labels of all modules except core, first read the default labels from the AD_Message
   * table and then reads the language specific labels.
   * 
   * @return a collection of labels.
   */
  public Collection<Label> getLabels() {
    final Map<String, Label> labels = new HashMap<>();
    OBContext.setAdminMode();
    try {
      // first read the labels from the base table
      final OBQuery<Message> messages = OBDal.getInstance()
          .createQuery(Message.class, "module.id!='0' or includeInI18N='Y'");
      for (Message message : messages.list()) {
        final Label label = new Label();
        label.setKey(message.getSearchKey());
        label.setValue(message.getMessageText());
        labels.put(message.getSearchKey(), label);
      }

      final OBQuery<MessageTrl> messagesTrl = OBDal.getInstance()
          .createQuery(MessageTrl.class,
              "(message.module.id!='0' or message.includeInI18N='Y') and message.active = true and language.id=:languageId");
      messagesTrl.setNamedParameter("languageId", OBContext.getOBContext().getLanguage().getId());
      for (MessageTrl message : messagesTrl.list()) {
        final Label label = new Label();
        label.setKey(message.getMessage().getSearchKey());
        label.setValue(message.getMessageText());
        labels.put(message.getMessage().getSearchKey(), label);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return labels.values();
  }

  public static class Label {
    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
