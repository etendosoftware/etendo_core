/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base;

import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This class is used by ExecutionLimitBaseProcess subclasses to store the limits that define if
 * they should be executed or not. This class holds a String with a module version id, together with
 * the first and last module versions that establish under which versions they must be executed.
 */
public class ExecutionLimits {
  private String moduleId;
  private OpenbravoVersion firstVersion;
  private OpenbravoVersion lastVersion;
  private boolean correct;

  /**
   * Basic ExecutionLimits constructor. Used to define a dependency between
   * ExecutionLimitBaseProcess subclasses and a module. When upgrading the module whose id is
   * moduleId, they will be executed only if its version before upgrading is higher than
   * firstVersion and lower than lastVersion. Otherwise they will not be executed.
   * 
   * @param moduleId
   *          A String with the module id
   * @param firstVersion
   *          An OpenbravoVersion which defines the first version of the module with id = moduleId
   *          since an ExecutionLimitBaseProcess subclass can be executed.
   * @param lastVersion
   *          An OpenbravoVersion with the last version of the module with id = moduleId which
   *          allows the execution of an ExecutionLimitBaseProcess subclass.
   */
  public ExecutionLimits(String moduleId, OpenbravoVersion firstVersion,
      OpenbravoVersion lastVersion) {
    this.moduleId = moduleId;
    this.firstVersion = firstVersion;
    this.lastVersion = lastVersion;
    if (firstVersion != null && lastVersion != null && firstVersion.compareTo(lastVersion) > 0) {
      correct = false;
    } else {
      correct = true;
    }
  }

  /**
   * Returns the dependent module id
   * 
   * @return a String with the module id
   */
  public String getModuleId() {
    return moduleId;
  }

  /**
   * Returns the first version of the dependent module for which an ExecutionLimitBaseProcess
   * subclass should be executed.
   * 
   * @return a OpenbravoVersion with the first execution version of the dependent module
   */
  public OpenbravoVersion getFirstVersion() {
    return firstVersion;
  }

  /**
   * Returns the last version of the dependent module for which an ExecutionLimitBaseProcess
   * subclass should be executed.
   * 
   * @return a OpenbravoVersion with the last execution version of the dependent module
   */
  public OpenbravoVersion getLastVersion() {
    return lastVersion;
  }

  /**
   * This method returns true when the last version and first version values are correctly defined.
   * 
   * @return false in case first version is higher than last version. Otherwise, it returns true.
   */
  public boolean areCorrect() {
    return correct;
  }
}
