/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.modulescript;

import org.openbravo.base.ExecutionLimitBaseProcess;
import org.openbravo.base.ExecutionLimits;

/**
 * A script executed AFTER {@code update.database} (or {@code update.database.java} /
 * {@code import.sample.data}) has fully completed, as opposed to a plain {@link ModuleScript},
 * which dbsm runs at an intermediate stage of the update — after the new DB model (functions,
 * DDL) has been applied but BEFORE the Application Dictionary sourcedata has been imported, and
 * with all triggers disabled.
 *
 * <p>Use this type instead of {@link ModuleScript} when the script needs to observe the fully
 * updated database: new DB functions AND new AD configuration rows, with triggers re-enabled
 * (EPL-1810). Typical case: (re)generating database objects derived from both halves, such as the
 * stored-computed-column triggers built from {@code ad_scd_*} functions plus
 * {@code AD_COLUMN_COMP_DEPENDENCY} configuration.</p>
 *
 * <p>Lifecycle and conventions are identical to {@link ModuleScript}: sources live in
 * {@code src-util/modulescript/src} (or a module's {@code src-util/modulescript/src}), compiled
 * classes are discovered by classpath scan — no registration needed — and execution can be
 * limited by module version through {@link #getPostUpdateModuleScriptExecutionLimits()} and
 * {@link #executeOnInstall()}. The only difference is the execution moment: these scripts are
 * driven by {@link PostUpdateModuleScriptHandler} from the {@code postupdate.modulescripts} Ant
 * target, which runs after the update flow has otherwise finished and before the successful
 * update timestamp is stamped (so a failure here leaves the update marked as failed).</p>
 *
 * <p><b>Important:</b> this class deliberately does NOT extend {@link ModuleScript}.
 * {@code ModuleScriptHandler} executes anything assignable to {@code ModuleScript}, so extending
 * it would also run these scripts mid-update — exactly what this type exists to avoid.</p>
 */
public abstract class PostUpdateModuleScript extends ExecutionLimitBaseProcess {

  private static final String POST_UPDATE_MODULE_SCRIPT = "PostUpdateModuleScript";

  /**
   * This method must be implemented by the PostUpdateModuleScripts, and is used to define the
   * actions performed by the script. It is executed once the database update has fully completed.
   */
  public abstract void execute();

  /**
   * This method executes the script through the execute() method
   */
  @Override
  protected void doExecute() {
    log4j.info("Executing postUpdateModuleScript: " + this.getClass().getName());
    execute();
  }

  @Override
  protected String getTypeName() {
    return POST_UPDATE_MODULE_SCRIPT;
  }

  /**
   * This method can be overridden by the PostUpdateModuleScripts, to specify the module and the
   * limit versions to define whether the script should be executed or not. The
   * {@link ModuleScriptExecutionLimits} type is reused: the semantics are identical to
   * {@link ModuleScript#getModuleScriptExecutionLimits()}.
   *
   * @return a ModuleScriptExecutionLimits object which contains the dependent module id and the
   *         first and last versions of the module that define the execution logic.
   */
  protected ModuleScriptExecutionLimits getPostUpdateModuleScriptExecutionLimits() {
    return null;
  }

  @Override
  protected ExecutionLimits getExecutionLimits() {
    return getPostUpdateModuleScriptExecutionLimits();
  }

  /**
   * This method handles the errors thrown while executing the script. Note it does not throw
   * {@code org.apache.tools.ant.BuildException} directly: {@link PostUpdateModuleScriptHandler}
   * wraps any exception escaping a script into one, failing the build.
   *
   * @param t
   *          the caught error or exception
   */
  protected void handleError(Throwable t) {
    log4j.error("Error executing postUpdateModuleScript " + this.getClass().getName() + ": "
        + t.getMessage(), t);
    throw new IllegalStateException(
        "Execution of postUpdateModuleScript " + this.getClass().getName() + " failed.", t);
  }
}
