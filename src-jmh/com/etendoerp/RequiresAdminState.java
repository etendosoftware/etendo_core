package com.etendoerp;

import org.openjdk.jmh.annotations.Setup;

import static com.etendoerp.utilities.DalUtils.changeContextToAdmin;

/**
 * All states that need the OBContext to be initialized with admin credentials should extend this class.
 * @see com.etendoerp.utilities.DalUtils#changeContextToAdmin()
 */
public abstract class RequiresAdminState {
    @Setup
    public void setupContext() {
        changeContextToAdmin();
    }
}
