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
 * All portions are Copyright (C) 2009-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.modularity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.openbravo.erpCommon.utility.Utility;

/**
 * This test case checks the properties merge functionality used when upgrading core
 * 
 */
public class MergePropertiesTest {
  private static String ORIGINAL_FILE = "test-orig.properties";
  private static String NEW_FILE = "test-new.properties";

  /**
   * crates original properties file to be tested later
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void createOriginalFile1() throws FileNotFoundException, IOException {
    Properties prop = new Properties();
    prop.setProperty("test1", "value1.custom");
    prop.setProperty("test2", "value2.custom");
    prop.store(new FileOutputStream(ORIGINAL_FILE), "Original properties file");
  }

  /**
   * creates new properties file to be tested later
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void createNewFile1() throws FileNotFoundException, IOException {
    Properties prop = new Properties();
    prop.setProperty("test1", "value1.default");
    prop.setProperty("test2", "value2.default");
    prop.setProperty("test3", "value3.default");
    prop.store(new FileOutputStream(NEW_FILE), "New properties file");
  }

  /**
   * merges previously created files and checks expected values
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  @Test
  public void testMerge() throws FileNotFoundException, IOException {
    createOriginalFile1();
    createNewFile1();

    boolean modified = Utility.mergeOpenbravoProperties(ORIGINAL_FILE, NEW_FILE);
    assertTrue("File has not been modified while it should be", modified);

    Properties prop = new Properties();
    prop.load(new FileInputStream(ORIGINAL_FILE));
    assertTrue("Not correctly merged test1 property",
        prop.getProperty("test1").equals("value1.custom"));
    assertTrue("Not correctly merged test2 property",
        prop.getProperty("test2").equals("value2.custom"));
    assertTrue("Not correctly merged test3 property",
        prop.getProperty("test3").equals("value3.default"));

    testDeleteFiles1();
  }

  /**
   * deletes testing files
   */
  private void testDeleteFiles1() {
    assertTrue("couldn't delete " + ORIGINAL_FILE, new File(ORIGINAL_FILE).delete());
    assertTrue("couldn't delete " + ORIGINAL_FILE, new File(NEW_FILE).delete());
  }

  /**
   * Creates another properties file for testing
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void createOriginalFile2() throws FileNotFoundException, IOException {
    Properties prop = new Properties();
    prop.setProperty("test1", "value1.custom");
    prop.setProperty("test2", "value2.custom");
    prop.store(new FileOutputStream(ORIGINAL_FILE), "Original properties file");
  }

  /**
   * creates new properties file with the same properties as the previous one
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void createNewFile2() throws FileNotFoundException, IOException {
    Properties prop = new Properties();
    prop.setProperty("test1", "value1.default");
    prop.setProperty("test2", "value2.default");
    prop.store(new FileOutputStream(NEW_FILE), "New properties file");
  }

  /**
   * merges previously created files, in this case there should not be modifications because they
   * have the same properties
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  @Test
  public void testMerge2() throws FileNotFoundException, IOException {
    createOriginalFile2();
    createNewFile2();

    boolean modified = Utility.mergeOpenbravoProperties(ORIGINAL_FILE, NEW_FILE);
    assertFalse("File has been modified while it shouldn't be", modified);

    Properties prop = new Properties();
    prop.load(new FileInputStream(ORIGINAL_FILE));
    assertTrue("Not correctly merged test1 property",
        prop.getProperty("test1").equals("value1.custom"));
    assertTrue("Not correctly merged test2 property",
        prop.getProperty("test2").equals("value2.custom"));

    deleteFiles2();
  }

  /**
   * deletes testing files
   */
  private void deleteFiles2() {
    assertTrue("couldn't delete " + ORIGINAL_FILE, new File(ORIGINAL_FILE).delete());
    assertTrue("couldn't delete " + ORIGINAL_FILE, new File(NEW_FILE).delete());
  }
}
