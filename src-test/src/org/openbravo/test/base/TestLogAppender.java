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
 * All portions are Copyright (C) 2016-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Used in {@link OBBaseTest}, keeps track of all messages written in log in order to make possible
 * to later do assertions on them.
 *
 * @author alostale
 *
 */
@Plugin(name = "TestLogAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class TestLogAppender extends AbstractAppender {
  private static Map<Level, List<String>> messages = new HashMap<>();
  private boolean logStackTraces = false;

  protected TestLogAppender(String name, Filter filter) {
    super(name, filter, null, true, Property.EMPTY_ARRAY);
  }

  @Override
  public synchronized void append(LogEvent event) {
    List<String> levelMsgs = messages.get(event.getLevel());
    if (levelMsgs == null) {
      levelMsgs = new ArrayList<>();
      messages.put(event.getLevel(), levelMsgs);
    }
    levelMsgs.add(event.getMessage().toString());

    if (logStackTraces && event.getThrown() != null) {
      levelMsgs.addAll(
          Arrays.asList(event.getThrownProxy().getExtendedStackTraceAsString().split("\n")));
    }
  }

  @PluginFactory
  public static TestLogAppender createAppender(@PluginAttribute("name") String name,
      @PluginElement("Filter") Filter filter) {
    return new TestLogAppender(name, filter);
  }

  /** Include in messages possible stack traces for logged Throwables */
  void setLogStackTraces(boolean logStackTraces) {
    this.logStackTraces = logStackTraces;
  }

  /** Removes all the messages tracked so far */
  public void reset() {
    messages = new HashMap<Level, List<String>>();
    logStackTraces = false;
  }

  /**
   * Returns a list with all messaged currently tracked. If none is tracked, an empty list is
   * returned.
   */
  public List<String> getAllMessages() {
    List<String> allMessages = new ArrayList<>();
    for (Entry<Level, List<String>> msgLvl : messages.entrySet()) {
      allMessages.addAll(msgLvl.getValue());
    }
    return allMessages;
  }

  /**
   * Returns a list of tracked messages for a given Level
   *
   * @param level
   *          Log level of the tracked messages
   * @return a list of messages for the given Level, or an empty list if none is tracked
   */
  public List<String> getMessages(Level level) {
    return messages.getOrDefault(level, Collections.emptyList());
  }

  /**
   * Returns a list of tracked messages for a given Level
   * 
   * @param level
   *          Log level of the tracked messages
   * @return a list of messages for the given Level, or an empty list if none is tracked
   * @deprecated use {@link #getMessages(Level)}
   */
  @Deprecated
  public List<String> getMessages(org.apache.log4j.Level level) {
    return getMessages(Level.getLevel(level.toString()));
  }

}
