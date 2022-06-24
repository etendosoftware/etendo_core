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
package org.openbravo.service.importprocess;

/**
 * Abstract/default class used to process an import entry when it is created before it is stored in
 * the database.
 * 
 * Is called from the {@link ImportEntryManager#createImportEntry(String, String, String)} method.
 * 
 * Note: modules can implement this interface when they want to keep/maintain custom columns in the
 * {@link ImportEntry} table.
 * 
 * @author mtaal
 */
public abstract class ImportEntryPreProcessor {

  public abstract void beforeCreate(ImportEntry importEntry);

}
