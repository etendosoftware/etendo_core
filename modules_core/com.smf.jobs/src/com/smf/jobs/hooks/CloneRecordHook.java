package com.smf.jobs.hooks;

import com.smf.jobs.defaults.CloneOrderHook;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.erpCommon.businessUtility.CloneOrderHookCaller;

/**
 * Abstract class representing a hook for the Clone Records job.
 * Allows for a more manual handle of the copy process.
 * These annotations must be added for the class to be used with dependency injection:
 *
 * <code>&#64;ApplicationScoped</code>
 * <p>
 * <code>&#64;Qualifier("entityName"), entityName being the name of the DAL entity associated with the tree.
 * </code>
 *
 */
public abstract class CloneRecordHook {

    /**
     * Main copy action. Will use {@link DalUtil#copy(BaseOBObject, boolean, boolean)}.
     * copyChildren and resetId are defined by the instance's {@link #shouldCopyChildren(boolean)} and {@link #shouldResetId()} respectively.
     * Override this method only when the {@link DalUtil#copy(BaseOBObject, boolean, boolean)} method needs to be changed
     * @param bob the record to copy
     * @param copyChildren if the record's children should be also copied
     * @return the copied record
     * @throws Exception when the copy action (or any of its pre and post actions) fail
     */
    public BaseOBObject copy(BaseOBObject bob, boolean copyChildren) throws Exception {
        var preCloneBOB = preCopy(bob);
        var clonedBOB = DalUtil.copy(preCloneBOB, shouldCopyChildren(copyChildren), shouldResetId());
        return postCopy(preCloneBOB, clonedBOB);
    }

    /**
     * Implement here any actions or modifications to the object before it's copied.
     * If no pre action is to be done, return the parameter.
     * @param originalRecord the {@link BaseOBObject} that will be copied.
     * @return the same {@link BaseOBObject} that will be copied, with modifications where applicable.
     * @throws Exception in case the hook implementations fails.
     */
    public abstract BaseOBObject preCopy(BaseOBObject originalRecord) throws Exception;

    /**
     * Implement here any actions or modifications to the object after it was copied.
     * If no post action is to be done, return the parameter.
     * @param originalRecord the original {@link BaseOBObject}
     * @param newRecord the copy {@link BaseOBObject}.
     * @return the same {@link BaseOBObject} that was copied, with modifications where applicable.
     * @throws Exception in case the hook implementations fails.
     */
    public abstract BaseOBObject postCopy(BaseOBObject originalRecord, BaseOBObject newRecord) throws Exception;

    /**
     * Override this when the copy operations should not copy the Record's children.
     * The choice made from the UI is passed as a parameter. This can be ignored
     * Should this return false, {@link #postCopy(BaseOBObject, BaseOBObject)} will be in charge of copying children if necessary.
     * @param uiCopyChildren the user choice from the UI
     * @return true if children should be copied, false otherwise.
     */
    public boolean shouldCopyChildren(boolean uiCopyChildren) {
        return uiCopyChildren;
    }

    /**
     * Override this when the copy operations should not reset the resulting Record's ID.
     * @return true if the ID should be reset, false otherwise.
     */
    public boolean shouldResetId() {
        return true;
    }

    /**
     * Override this to change the Hook Priority.
     * When several hooks of the same entity are present, the one with the lowest priority will be executed.
     * NOTE: The {@link CloneOrderHook} has support for the {@link CloneOrderHookCaller}
     */
    public int getPriority() {
        return 100;
    }
}
