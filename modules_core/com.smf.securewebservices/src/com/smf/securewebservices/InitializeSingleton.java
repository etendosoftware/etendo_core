package com.smf.securewebservices;

import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.client.kernel.ApplicationInitializer;
import org.openbravo.dal.core.OBInterceptor;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class InitializeSingleton implements ApplicationInitializer{

    @Inject
    private SWSInterceptor swsOBInterceptor;

    @Override
    public void initialize() {
        SWSConfig.getInstance().refresh();
        setInterceptor();
    }
    public synchronized void setInterceptor() {
        final OBInterceptor interceptor = (OBInterceptor) SessionFactoryController.getInstance()
                .getConfiguration()
                .getInterceptor();
        interceptor.setInterceptorListener(swsOBInterceptor);
    }
}
