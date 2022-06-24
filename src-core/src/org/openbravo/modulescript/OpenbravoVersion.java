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
 * All portions are Copyright (C) 2010 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.modulescript;

/**
 * This class represents an Openbravo ERP version number. It is designed to compare Openbravo ERP
 * version numbers
 * 
 * @author adrian
 */
public class OpenbravoVersion implements Comparable<OpenbravoVersion> {

  private int major1;
  private int major2;
  private int minor;
  private int patch;

  /**
   * Creates a new Openbravo version object based on its three version numbers.
   * 
   * @param major1
   *          the major1 version number
   * @param major2
   *          the major2 version number
   * @param minor
   *          the minor version number
   */
  public OpenbravoVersion(int major1, int major2, int minor) {
    this.major1 = major1;
    this.major2 = major2;
    this.minor = minor;
    this.patch = 0;
  }

  /**
   * Creates a new Openbravo version object based on its three version numbers.
   *
   * @param major1
   *          the major1 version number
   * @param major2
   *          the major2 version number
   * @param minor
   *          the minor version number
   * @param patch
   *          the patch version number
   */
  public OpenbravoVersion(int major1, int major2, int minor, int patch) {
    this.major1 = major1;
    this.major2 = major2;
    this.minor = minor;
    this.patch = patch;
  }

  /**
   * Creates a new Openbravo version object based on its <code>String</code> representation.
   * 
   * @param version
   *          the version representation
   */
  public OpenbravoVersion(String version) {

    String[] numbers = version.split("\\.");

    if (numbers.length > 4) {
      throw new IllegalArgumentException("Version must consist in four numbers separated by .");
    }
    this.major1 = Integer.valueOf(numbers[0]);
    this.major2 = Integer.valueOf(numbers[1]);
    this.minor = Integer.valueOf(numbers[2]);
    // make sure new version system is compatible with old OB ones (ex: 3.0.20221)
    if (numbers.length > 3) {
      this.patch = Integer.valueOf(numbers[3]);
    } else {
      this.patch = 0;
    }
  }

  /**
   * Gets the Major1 version number
   * 
   * @return The Major1 version number
   */
  public int getMajor1() {
    return major1;
  }

  /**
   * Returns the Major1 version number
   * 
   * @return The Major2 version number
   */
  public int getMajor2() {
    return major2;
  }

  /**
   * Returns the Minor version number
   * 
   * @return The Minor version number
   */
  public int getMinor() {
    return minor;
  }

  /**
   * Returns the Patch version number
   *
   * @return The Patch version number
   */
  public int getPatch() {
    return patch;
  }

  @Override
  public int compareTo(OpenbravoVersion o) {
    if (major1 == o.major1) {
      if (major2 == o.major2) {
        if (minor == o.minor) {
          return compareVersions(patch, o.patch);
        } else {
          return compareVersions(minor, o.minor);
        }
      } else {
        return compareVersions(major2, o.major2);
      }
    } else {
      return compareVersions(major1, o.major1);
    }
  }

  private int compareVersions(int v1, int v2) {
    if (v1 < v2) {
      return -1;
    } else if (v1 == v2) {
      return 0;
    } else {
      return 1;
    }
  }

  @Override
  public String toString() {
    return Integer.toString(major1) + "." + Integer.toString(major2) + "."
        + Integer.toString(minor) + "." + Integer.toString(patch);
  }
}
