package com.smf.securewebservices;

import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class EntityPreFlush {
    private Iterator bobs;

    public void setBobs(Iterator bobs) {
        this.bobs = bobs;
    }

    public Iterator getBobs() {
        return bobs;
    }
}
