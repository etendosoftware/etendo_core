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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.model.ad.ui.Tab;

/**
 * Classes implementing this interface are injected in the {@link FormInitializationComponent}
 * class. Using this interface it is possible to customize the return object of that class or to
 * perform some custom validations.
 */
public interface FICExtension {

  /**
   * This method is executed in the {@link FormInitializationComponent#execute(Map, String) execute}
   * method just before the response JSONObject is built. It receives all the instances of the
   * objects used to build the response.
   * 
   * @param mode
   *          The execution mode.
   * @param tab
   *          The Tab owner of the Form that it is being executed.
   * @param columnValues
   *          Map with the values of forms columns.
   * @param row
   *          The BaseOBObject that it is being edited in the form.
   * @param changeEventCols
   *          The List of dynamic columns that fire the CHANGE event mode.
   * @param calloutMessages
   *          The list of messages returned by the callouts that have been executed.
   * @param attachments
   *          A empty list where can be populated the attachments related to the record that it is
   *          being edited. By default these attachments are loaded when the section is opened and
   *          not by the FIC response.
   * @param jsExcuteCode
   *          The list of JavaScrip code returned by the callouts to be executed in the client.
   * @param hiddenInputs
   *          The Map with all the hidden fields with their values.
   * @param noteCount
   *          count of notes available on the record that it is being edited.
   * @param overwrittenAuxiliaryInputs
   *          The list of the Auxiliary Inputs that have been overriden by callouts.
   */
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs);

}
