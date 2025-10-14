package com.smf.securewebservices.event;

import com.smf.securewebservices.EntityPreFlush;
import com.smf.securewebservices.SWSConfig;
import com.smf.securewebservices.data.SMFSWSConfig;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.datamodel.Table;

import jakarta.enterprise.event.Observes;
import java.util.Arrays;
import java.util.Iterator;

public class SingletonEventHandler extends EntityPersistenceEventObserver {
    private static Entity[] entities = { ModelProvider.getInstance().getEntity(SMFSWSConfig.ENTITY_NAME) };

    public void onPostFlush(@Observes EntityPreFlush event){
        Iterator entities = event.getBobs();
        while (entities.hasNext()) {
            BaseOBObject bob = (BaseOBObject) entities.next();
            Entity bobEntity = bob.getEntity();
            if(Arrays.asList(getObservedEntities()).contains(bobEntity)){
                SWSConfig.getInstance().refresh((SMFSWSConfig) bob);
                //Todo: ver si hay que dejar el break o no.
                break;
            }
        }
    }

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }
}
