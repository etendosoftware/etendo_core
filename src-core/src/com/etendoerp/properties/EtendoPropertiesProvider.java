package com.etendoerp.properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Class used by the Dbsourcemanager to obtain the Openbravo.properties file and the 'source.path' dir
 */
public class EtendoPropertiesProvider {

    private final Logger log = LogManager.getLogger();

    private static EtendoPropertiesProvider instance = new EtendoPropertiesProvider();

    public Properties etendoProperties = null;

    public static synchronized EtendoPropertiesProvider getInstance() {
        return instance;
    }

    public static synchronized void setInstance(EtendoPropertiesProvider instance) {
        EtendoPropertiesProvider.instance = instance;
    }

    public Properties getEtendoProperties() {
        if (etendoProperties == null) {
            readPropertiesFromDevelopmentProject();
        }
        return etendoProperties;
    }

    public void setProperties(String fileLocation) {
        log.info("Setting Etendo - Openbravo.properties through file: " + fileLocation);
        etendoProperties = new Properties();
        try {
            final FileInputStream fis = new FileInputStream(fileLocation);
            etendoProperties.load(fis);
            fis.close();
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void readPropertiesFromDevelopmentProject() {
        final File propertiesFile = getFileFromDevelopmentPath("Openbravo.properties");
        if (propertiesFile == null) {
            log.error("Properties file 'Openbravo.properties' not found.");
            throw new IllegalArgumentException("Properties file 'Openbravo.properties' not found.");
        }
        setProperties(propertiesFile.getAbsolutePath());
    }

    public File getFileFromDevelopmentPath(String fileName) {
        // get the location of the current class file
        final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
        File f = new File(url.getPath());
        File propertiesFile = null;
        while (f.getParentFile() != null && f.getParentFile().exists()) {
            f = f.getParentFile();
            final File configDirectory = new File(f, "config");
            if (configDirectory.exists()) {
                propertiesFile = new File(configDirectory, fileName);
                if (propertiesFile.exists()) {
                    // found it and break
                    break;
                }
            }
        }
        if (propertiesFile == null) {
            f = new File(System.getProperty("user.dir") + File.separator + "config", fileName);
            if (f.exists()) {
                propertiesFile = f;
            }
        }
        if (propertiesFile == null) {
            f = new File(System.getProperty("user.dir"));
            propertiesFile = null;
            while (f.getParentFile() != null && f.getParentFile().exists()) {
                f = f.getParentFile();
                final File configDirectory = new File(f, "config");
                if (configDirectory.exists()) {
                    propertiesFile = new File(configDirectory, fileName);
                    if (propertiesFile.exists()) {
                        // found it and break
                        break;
                    }
                }
            }
        }
        if (propertiesFile == null){
            String sourcePath = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path");
            f = new File(sourcePath + File.separator + "config", fileName);
            if (f.exists()) {
                propertiesFile = f;
            }
        }
        return propertiesFile;
    }

}
