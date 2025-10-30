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
 * All portions are Copyright (C) 2008-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.xml;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.xml.EntityXMLConverter;
import org.openbravo.model.ad.utility.ReferenceDataStore;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.OBBaseTest;

/**
 * Supports testing of xml imports/export. Provides convenience methods.
 * 
 * @author mtaal
 */

public class XMLBaseTest extends OBBaseTest {

  protected void compare(String result, String file) {
    try {
      final URL url = this.getClass().getResource("testdata/" + file);
      final File f = new File(new URI(url.toString()));
      LineIterator li1 = FileUtils.lineIterator(f);
      final BufferedReader r2 = new BufferedReader(new StringReader(result));
      String line = null;
      int lineNo = 1;
      while (li1.hasNext()) {
        line = li1.nextLine();
        final String otherLine = r2.readLine();
        assertTrue("File: " + file + ": Lines are unequal: \n" + line + "\n" + otherLine
            + "\n Line number is " + lineNo, line.equals(otherLine));
        lineNo++;
      }
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  protected String getFileContent(String file) {
    try {
      final URL url = this.getClass().getClassLoader().getResource("testdata/" + file);
      final File f = new File(new URI(url.toString()));
      return FileUtils.readFileToString(f);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  protected void cleanRefDataLoaded() {
    setSystemAdministratorContext();
    final OBCriteria<ReferenceDataStore> obc = OBDal.getInstance()
        .createCriteria(ReferenceDataStore.class);
    obc.setFilterOnActive(false);
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    for (final ReferenceDataStore rdl : obc.list()) {
      OBDal.getInstance().remove(rdl);
    }
    commitTransaction();
  }

  protected <T extends BaseOBObject> List<T> getList(Class<T> clz, Organization org) {
    final OBCriteria<T> obc = OBDal.getInstance().createCriteria(clz);
    if (org != null) {
      obc.addEqual("organization", org);
    }
    return obc.list();
  }

  protected <T extends BaseOBObject> List<T> getList(Class<T> clz) {
    return getList(clz, null);
  }

  protected <T extends BaseOBObject> String getXML(List<T> objs) {
    final EntityXMLConverter exc = EntityXMLConverter.newInstance();
    exc.setOptionIncludeReferenced(true);
    // exc.setOptionEmbedChildren(true);
    // exc.setOptionIncludeChildren(true);
    return exc.toXML(new ArrayList<BaseOBObject>(objs));
  }

  protected <T extends BaseOBObject> String getXML(Class<T> clz) {
    return getXML(clz, null);
  }

  protected <T extends BaseOBObject> String getXML(Class<T> clz, Organization o) {
    final OBCriteria<T> obc = OBDal.getInstance().createCriteria(clz);
    final EntityXMLConverter exc = EntityXMLConverter.newInstance();
    exc.setOptionIncludeReferenced(true);
    // exc.setOptionEmbedChildren(true);
    // exc.setOptionIncludeChildren(true);
    if (!(o == null)) {
      obc.addEqual("organization", o);
    }
    return exc.toXML(new ArrayList<BaseOBObject>(obc.list()));
  }
}
