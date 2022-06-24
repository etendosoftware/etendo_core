/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.modulescript;

import org.openbravo.base.ExecutionLimits;

/**
 * This class is used by ModuleScript objects to store the limits that define if they should be
 * executed or not. This class holds a String with a module version id, together with the first and
 * last module versions that establish under which versions a ModuleScript must be executed.
 * 
 */
public class ModuleScriptExecutionLimits extends ExecutionLimits {

  public ModuleScriptExecutionLimits(String moduleId, OpenbravoVersion firstVersion,
      OpenbravoVersion lastVersion) {
    super(moduleId, firstVersion, lastVersion);
  }

}
