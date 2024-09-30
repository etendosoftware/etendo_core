package com.smf.securewebservices;

import com.smf.securewebservices.data.SMFSWSConfig;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

public class SWSConfig implements OBSingleton {
    private static SWSConfig instance = null;
    private String privateKey = null;
    private String publicKey = null;
    private String algorithm = null;
    private Long expirationTime = null;

    public void refresh(){
        refresh(null);
    }
    public void refresh(SMFSWSConfig _config) {
        OBContext.setAdminMode();
        SMFSWSConfig config = _config;
        try{
            if(config == null){
                OBCriteria<SMFSWSConfig> criteria = OBDal.getInstance().createCriteria(SMFSWSConfig.class);
                criteria.setMaxResults(1);
                config = (SMFSWSConfig) criteria.uniqueResult();
            }

            if (config != null) {
                privateKey = config.getPrivateKey();
                publicKey = config.getPublicKey();
                algorithm = config.getAlgorithm();
                expirationTime = config.getExpirationTime();
            }
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public static SWSConfig getInstance() {
        if (instance == null) {
            instance = OBProvider.getInstance().get(SWSConfig.class);
        }
        return instance;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() { return publicKey; }

    public String getAlgorithm() { return algorithm; }
}
