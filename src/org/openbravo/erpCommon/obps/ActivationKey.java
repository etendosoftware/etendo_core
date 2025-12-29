/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.obps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.obps.DisabledModules.Artifacts;
import org.openbravo.erpCommon.obps.ModuleLicenseRestrictions.ActivationMsg;
import org.openbravo.erpCommon.obps.ModuleLicenseRestrictions.MsgSeverity;
import org.openbravo.erpCommon.security.SessionListener;
import org.openbravo.erpCommon.utility.HttpsUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.StringCollectionUtils;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.Session;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.HeartbeatLog;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlEngine;

public class ActivationKey {
  private final static String OB_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3KuCsRC3ZxmTTryRAX99SJfwtjfTahW+dtXXpI0CQ87A8XxcL4xhhsH4WhyE+sSxji+vSlZLm7kpcJivbrzX2qy1nmM6OpFX4teo65jk3ccxMVx74ZeT/2aHcFNXUVD8jXxSv2U/5PVH//Q3KJyyay73YbkIKIwQWznWrgj2O3Gy2v1VRoUaeaWlEdS8pKEnfW4DkCJtqM3p6ZbRg6pdNUnGDjo1Ck6V9GuNubxkSvAu5vQQbeJurNFBk4Smwm6tJj6XSyefaOrXjcHFqwe4kU3VRu3nnkOl3aR8PUgHS7IS16LtB6C2AR9sIURS7FnoWp5aiCpNPescfFJQn3+VUQIDAQAB";
  private final static String OB_PUBLIC_KEY_OLD = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApaBElaRrI7V0sztIxI44" +
      "Zvf5YJQRNAetqADkO/q819/RgOfpbjlm72jU8GodAEsiFN5l0k37PPgX3IIuOUxm" +
      "Lu5vW59X+HU7quCtOF0A0EclbKaWdch4aSBCFYrXOVAMofOCFkP9Yqb5r9NbRuiG" +
      "ybgE9Hcu8MsZoRWAxlCDp5UGa0ie52CUTH+7D74voz9bTsT590tSb43R9KPqGGgs" +
      "aBgD1FkIQHnZ+PMZlQneDZCP9KMbIkjbGQC5F0fpcNaIwt09YHwHmMEi/7dLGeuA" +
      "uwlB7XhpnJbbDw/L739xFTiGqdRQGAxiYKD9NYKXDaImsgulMqftawAbzd7HPFPY" +
      "gQIDAQAB";


  private static final String HEARTBEAT_URL = "https://activation.futit.cloud:443/license-server/heartbeat";
  private static final String STATELESS_REQUEST = "statelessRequest";
  public static final String WS_PACKS = "wsPacks";
  public static final String WS_UNITS_PER_UNIT = "wsUnitsPerUnit";
  public static final String INSTANCENO = "instanceno";
  public static final String LINCENSETYPE = "lincensetype";

  private boolean isActive = false;
  private boolean hasActivationKey = false;
  private Date lastUpdateTimestamp;
  private String errorMessage = "";
  private String messageType = "Error";
  private Properties instanceProperties;
  private static final Logger log = LogManager.getLogger();
  private String strPublicKey;
  private Long pendingTime;
  private boolean hasExpired = false;
  private boolean subscriptionConvertedProperty = false;
  private boolean subscriptionActuallyConverted = false;
  private LicenseClass licenseClass;
  private LicenseType licenseType;
  private Date lastRefreshTime;
  private boolean trial = false;
  private boolean golden = false;
  private Date startDate;
  private Date endDate;
  private boolean limitedWsAccess = true;
  private Long maxUsers;
  private Long posTerminals;
  private Long posTerminalsWarn;

  private boolean notActiveYet = false;
  private boolean inconsistentInstance = false;

  private long maxWsCalls;
  private AtomicLong wsDayCounter;
  private Date initWsCountTime;
  private List<Date> exceededInLastDays;

  private static final Logger log4j = LogManager.getLogger();

  private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd");

  private Lock deactivateSessionsLock = new ReentrantLock();
  private Lock refreshLicenseLock = new ReentrantLock();
  private Object wsCountLock = new Object();

  /**
   * Number of minutes since last license refresh to wait before doing it again
   */
  private static final int REFRESH_MIN_TIME = 60;

  public enum LicenseRestriction {
    NO_RESTRICTION,
    OPS_INSTANCE_NOT_ACTIVE,
    NUMBER_OF_SOFT_USERS_REACHED,
    NUMBER_OF_CONCURRENT_USERS_REACHED,
    MODULE_EXPIRED,
    NOT_MATCHED_INSTANCE,
    HB_NOT_ACTIVE,
    EXPIRED_GOLDEN,
    POS_TERMINALS_EXCEEDED
  }

  public enum CommercialModuleStatus {
    NO_SUBSCRIBED, ACTIVE, EXPIRED, NO_ACTIVE_YET, CONVERTED_SUBSCRIPTION, DISABLED
  }

  public enum FeatureRestriction {
    NO_RESTRICTION(""),
    DISABLED_MODULE_RESTRICTION("FeatureInDisabledModule"),
    UNKNOWN_RESTRICTION("");

    private String msg;

    private FeatureRestriction(String msg) {
      this.msg = msg;
    }

    @Override
    public String toString() {
      return msg;
    }
  }

  public enum WSRestriction {
    NO_RESTRICTION, EXCEEDED_MAX_WS_CALLS, EXCEEDED_WARN_WS_CALLS, EXPIRED, EXPIRED_MODULES;
  }

  public enum LicenseClass {
    COMMUNITY("C"), BASIC("B"), STD("STD");

    private String code;

    private LicenseClass(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  public enum LicenseType {
    CONCURRENT_USERS("USR");

    private String code;

    private LicenseType(String code) {
      this.code = code;
    }

    public String getCode() {
      return code;
    }
  }

  public enum SubscriptionStatus {
    COMMUNITY(
        "COM"),
    ACTIVE("ACT"),
    CANCEL("CAN"),
    EXPIRED("EXP"),
    NO_ACTIVE_YET("NAY"),
    INVALID("INV");

    private String code;

    private SubscriptionStatus(String code) {
      this.code = code;
    }

    /**
     * Returns the name of the current status in the given language.
     */
    public String getStatusName(String language) {
      return Utility.getListValueName("OBPSLicenseStatus", code, language);
    }

    /**
     * Returns the status code associated with this subscription status.
     *
     * @return the status code as a String
     */
    public String getStatusCode() {
      return code;
    }

  }

  private static final int ONE_DAY = 24 * 60;
  private static final int MILLSECS_PER_DAY = ONE_DAY * 60 * 1000;
  private static final int PING_TIMEOUT_SECS = 120;
  private static final Long EXPIRATION_BASIC_DAYS = 30L;
  private static final Long EXPIRATION_PROF_DAYS = 30L;

  private final static int WS_DAYS_EXCEEDING_ALLOWED = 5;
  private final static long WS_DAYS_EXCEEDING_ALLOWED_PERIOD = 30L;
  private final static long WS_MS_EXCEEDING_ALLOWED_PERIOD = MILLSECS_PER_DAY
      * WS_DAYS_EXCEEDING_ALLOWED_PERIOD;

  /**
   * Session types that are not taken into account for counting concurrent users
   */
  private static final List<String> NO_CU_SESSION_TYPES = Arrays.asList(//
      "WS", // Web service
      "WSC", // Connector
      "OBPOS_POS", // WebPOS
      "CUR" // Concurrent users hard limit reached
  );

  private static final List<String> BACKOFFICE_SUCCESS_SESSION_TYPES = Arrays.asList(//
      "S", // Standard success session
      "SUR" // Concurrent users soft limit reached
  );

  public static final Long NO_LIMIT = -1L;

  private static ActivationKey instance = new ActivationKey();

  /**
   * @see ActivationKey#getInstance(boolean)
   */
  public static ActivationKey getInstance() {
    return getInstance(false);
  }

  /**
   * Obtains the ActivationKey instance. Instances should be get in this way, rather than creating a
   * new one.
   * <p>
   * If refreshIfNeeded parameter is true, license is tried to be refreshed if it is needed to.
   *
   * @param refreshIfNeeded
   *     refresh license if needed to
   */
  public static ActivationKey getInstance(boolean refreshIfNeeded) {
    if (refreshIfNeeded) {
      instance.refreshIfNeeded();
    }

    if (instance.startDate != null) {
      // check dates in case there is a license with dates
      instance.checkDates();
    }
    return instance;
  }

  public static synchronized void setInstance(ActivationKey ak) {
    instance = ak;
    ak.resetRefreshTime();
    ak.lastUpdateTimestamp = getSystem().getUpdated();
  }

  private static org.openbravo.model.ad.system.System getSystem() {
    OBContext.setAdminMode(true);
    try {
      return OBDal.getInstance().get(org.openbravo.model.ad.system.System.class, "0");
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void resetRefreshTime() {
    lastRefreshTime = new Date();
  }

  /**
   * Reloads ActivationKey instance from information in DB.
   */
  public static ActivationKey reload() {
    instance.loadFromDB();
    return instance;
  }

  private ActivationKey() {
    loadFromDB();
  }

  private void loadFromDB() {
    if (!refreshLicenseLock.tryLock()) {
      // license being loaded by a different thread
      return;
    }

    try {
      log.info("Loading activation key from DB");
      org.openbravo.model.ad.system.System sys = getSystem();
      strPublicKey = sys.getInstanceKey();
      lastUpdateTimestamp = sys.getUpdated();
      loadInfo(sys.getActivationKey());
      DisabledModules.reload();
    } finally {
      refreshLicenseLock.unlock();
    }
  }

  public ActivationKey(String publicKey, String activationKey) {
    strPublicKey = publicKey;
    loadInfo(activationKey);
    DisabledModules.reload();
  }

  private synchronized void loadInfo(String activationKey) {
    reset();

    if (strPublicKey == null || activationKey == null || strPublicKey.equals("")
        || activationKey.equals("")) {
      hasActivationKey = false;
      return;
    }

    PublicKey pk = getPublicKey(strPublicKey);
    if (pk == null) {
      hasActivationKey = true;
      errorMessage = "@NotAValidKey@";
      return;
    }
    hasActivationKey = true;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      boolean signed = decrypt(activationKey.getBytes(), pk, bos, OB_PUBLIC_KEY_OLD);

      if (signed) {
        byte[] props = bos.toByteArray();
        ByteArrayInputStream isProps = new ByteArrayInputStream(props);
        InputStreamReader reader = new InputStreamReader(isProps, "UTF-8");
        instanceProperties = new Properties();

        instanceProperties.load(reader);
        reader.close();
      } else {
        isActive = false;
        errorMessage = "@NotSigned@";
        return;
      }

      String sysId = getProperty("sysId");
      String dbId = getProperty("dbId");
      String macId = getProperty("macId");

      SystemInfo.loadId(new DalConnectionProvider(false));
      if ((sysId != null && !sysId.isEmpty() && !sysId.equals(SystemInfo.getSystemIdentifier()))
          || (dbId != null && !dbId.isEmpty() && !dbId.equals(SystemInfo.getDBIdentifier()))
          || (macId != null && !macId.isEmpty() && !macId.equals(SystemInfo.getMacAddress()))) {
        isActive = false;
        inconsistentInstance = true;
        errorMessage = "@IncorrectLicenseInstance@";
        return;
      }
    } catch (Exception e) {
      isActive = false;
      errorMessage = "@NotAValidKey@";
      log.error("Could not load activation key " + strPublicKey, e);
      return;
    }

    String pLicenseType = getProperty(LINCENSETYPE);
    if ("USR".equals(pLicenseType)) {
      licenseType = LicenseType.CONCURRENT_USERS;
    } else if ("DMD".equals(pLicenseType)) {
      isActive = false;
      hasActivationKey = false;
      errorMessage = "@OPS_INVALID_ON_DEMAND_LICENSE@";
      return;
    } else {
      log4j.warn("Unknown license type:" + pLicenseType + ". Using Concurrent Users!.");
      licenseType = LicenseType.CONCURRENT_USERS;
    }

    // Get license class, old Activation Keys do not have this info, so treat them as Standard
    // Edition instances
    String pLicenseClass = getProperty("licenseedition");
    if (pLicenseClass == null || pLicenseClass.isEmpty() || pLicenseClass.equals("STD")) {
      licenseClass = LicenseClass.STD;
    } else if (pLicenseClass.equals("B")) {
      licenseClass = LicenseClass.BASIC;
    } else {
      log4j.warn("Unknown license class:" + pLicenseClass + ". Using Basic!.");
      licenseClass = LicenseClass.BASIC;
    }

    if (licenseType == LicenseType.CONCURRENT_USERS) {
      String limitusers = getProperty("limitusers");
      maxUsers = StringUtils.isEmpty(limitusers) ? 0L : Long.valueOf(limitusers);
    }

    // Check for dates to know if the instance is active
    subscriptionConvertedProperty = "true".equals(getProperty("subscriptionConverted"));

    trial = "true".equals(getProperty("trial"));
    golden = "true".equals(getProperty("golden"));

    String strUnlimitedWsAccess = getProperty("unlimitedWsAccess");

    if (StringUtils.isEmpty(strUnlimitedWsAccess)) {
      // old license, setting defaults
      if (trial || golden) {
        limitedWsAccess = true;
        maxWsCalls = 500L;
        instanceProperties.put(WS_PACKS, "1");
        instanceProperties.put(WS_UNITS_PER_UNIT, "500");
        initializeWsCounter();
      } else {
        limitedWsAccess = false;
      }
    } else {
      limitedWsAccess = "false".equals(getProperty("unlimitedWsAccess"));
      if (limitedWsAccess) {
        String packs = getProperty(WS_PACKS);
        String unitsPack = getProperty(WS_UNITS_PER_UNIT);

        if (StringUtils.isEmpty(packs) || StringUtils.isEmpty(unitsPack)) {
          log.warn("Couldn't determine ws call limitation, setting unlimited.");
          limitedWsAccess = false;
        } else {
          try {
            Integer nPacks = Integer.parseInt(packs);
            Integer nUnitsPack = Integer.parseInt(unitsPack);
            maxWsCalls = nPacks * nUnitsPack;
            log.debug("Maximum ws calls: " + maxWsCalls);
            initializeWsCounter();
          } catch (Exception e) {
            log.error("Error setting ws call limitation, setting unlimited.", e);
            limitedWsAccess = false;
          }
        }
      }
    }

    try {
      startDate = sDateFormat.parse(getProperty("startdate"));

      if (getProperty("enddate") != null) {
        endDate = sDateFormat.parse(getProperty("enddate"));
      }
    } catch (Exception e) {
      errorMessage = "@ErrorReadingDates@";
      isActive = false;
      log.error(e.getMessage(), e);
      return;
    }

    if (instanceProperties.containsKey("posTerminals")
        && !StringUtils.isBlank(getProperty("posTerminals"))) {
      try {
        posTerminals = Long.valueOf(getProperty("posTerminals"));
      } catch (Exception e) {
        log.error("Couldn't read number of terminals " + getProperty("posTerminals"), e);
        posTerminals = 0L;
      }
    } else {
      // it can be old license without terminal info, or terminal being empty which stands for no
      // terminal allowed
      posTerminals = 0L;
    }

    if (instanceProperties.containsKey("posTerminalsWarn")
        && !StringUtils.isBlank(getProperty("posTerminalsWarn"))) {
      try {
        posTerminalsWarn = Long.valueOf(getProperty("posTerminalsWarn"));
      } catch (Exception e) {
        log.error("Couldn't read number of terminals warn " + getProperty("posTerminalsWarn"), e);
      }
    } else {
      posTerminalsWarn = null;
    }

    checkDates();

    // Persist activation key information to database
    persistActivationInfoToDB();

    // this occurs on Tomcat start, don't want to try to refresh on next login, let's wait for 24hr
    resetRefreshTime();
  }

  /**
   * Persists the calculated activation key information to the database
   * in the new columns of AD_SYSTEM_INFO table
   */
  private void persistActivationInfoToDB() {
    if (!isActive && !hasActivationKey) {
      // No activation key, don't persist anything
      return;
    }

    OBContext.setAdminMode(true);
    try {
      SystemInformation sysInfo = OBDal.getInstance().get(SystemInformation.class, "0");
      if (sysInfo == null) {
        log.warn("SystemInformation record not found, cannot persist activation info");
        return;
      }

      // Persist customer name
      if (instanceProperties != null && getProperty("customer") != null) {
        sysInfo.setCustomerName(getProperty("customer"));
      }

      // Persist license edition
      if (licenseClass != null) {
        sysInfo.setLicenseEdition(licenseClass.getCode());
      }

      // Persist subscription type (license type)
      if (licenseType != null) {
        sysInfo.setSubscriptionType(licenseType.getCode());
      } else if (instanceProperties != null && getProperty(LINCENSETYPE) != null) {
        sysInfo.setSubscriptionType(getProperty(LINCENSETYPE));
      }

      // Persist subscription start date
      if (startDate != null) {
        sysInfo.setSubscriptionStartDate(startDate);
      }

      // Persist subscription end date
      if (endDate != null) {
        sysInfo.setSubscriptionEndDate(endDate);
      }

      // Persist concurrent users limit
      if (maxUsers != null) {
        sysInfo.setConcurrentGlobalSystemUsers(maxUsers);
      }

      // Persist instance number
      if (instanceProperties != null && getProperty(INSTANCENO) != null) {
        sysInfo.setInstanceNumber(getProperty(INSTANCENO));
      }

      // Persist web service access information
      String wsAccess = null;
      if (limitedWsAccess) {
        String packs = getProperty(WS_PACKS);
        String unitsPack = getProperty(WS_UNITS_PER_UNIT);
        if (packs != null && unitsPack != null) {
          wsAccess = "Limited: " + packs + " packs x " + unitsPack + " calls";
        } else {
          wsAccess = "Limited";
        }
      } else {
        wsAccess = "Unlimited";
      }
      sysInfo.setWEBServiceAccess(wsAccess);

      OBDal.getInstance().save(sysInfo);
      OBDal.getInstance().flush();

      log.info("Activation key information persisted to database successfully");
    } catch (Exception e) {
      log.error("Error persisting activation key information to database", e);
      // Don't throw exception to not break the activation process
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void reset() {
    isActive = false;
    hasActivationKey = false;
    errorMessage = "";
    messageType = "Error";
    instanceProperties = null;
    hasExpired = false;
    subscriptionConvertedProperty = false;
    subscriptionActuallyConverted = false;
    trial = false;
    golden = false;
    licenseClass = LicenseClass.COMMUNITY;
    licenseType = null;
    startDate = null;
    endDate = null;
    pendingTime = null;
    limitedWsAccess = false;
    maxUsers = null;
  }

  private void checkDates() {
    // Check for dates to know if the instance is active
    Date now = new Date();
    if (startDate == null || now.before(startDate)) {
      isActive = false;
      notActiveYet = true;
      String dateFormat = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateFormat.java");
      SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
      errorMessage = "@OPSNotActiveTill@ " + outputFormat.format(startDate);
      messageType = "Warning";
      return;
    }
    if (endDate != null) {
      pendingTime = ((endDate.getTime() - now.getTime()) / MILLSECS_PER_DAY) + 1;
      if (pendingTime <= 0) {
        if (subscriptionConvertedProperty) {
          // A bought out instance is actually converted when the license has expired.
          subscriptionActuallyConverted = true;
        } else {
          isActive = false;
          hasExpired = true;
          String dateFormat = OBPropertiesProvider.getInstance()
              .getOpenbravoProperties()
              .getProperty("dateFormat.java");
          SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
          errorMessage = "@OPSActivationExpired@ " + outputFormat.format(endDate);
          return;
        }
      }
    }
    isActive = true;
  }

  private boolean decrypt(byte[] bytes, PublicKey pk, ByteArrayOutputStream bos,
      String strOBPublicKey) throws Exception {
    PublicKey obPk = getPublicKey(strOBPublicKey); // get OB public key to check signature
    Signature signer = Signature.getInstance("MD5withRSA");
    signer.initVerify(obPk);

    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

    ByteArrayInputStream bis = new ByteArrayInputStream(
        org.apache.commons.codec.binary.Base64.decodeBase64(bytes));

    // Encryptation only accepts 128B size, it must be chuncked
    final byte[] buf = new byte[256];
    final byte[] signature = new byte[256];

    // read the signature
    if (!(bis.read(signature) > 0)) {
      return false;
    }

    // decrypt
    while ((bis.read(buf)) > 0) {
      cipher.init(Cipher.DECRYPT_MODE, pk);
      bos.write(cipher.doFinal(buf));
    }

    // verify signature
    signer.update(bos.toByteArray());
    boolean signed = signer.verify(signature);
    log.debug("signature length:" + buf.length);
    log.debug("singature:" + (new BigInteger(signature).toString(16).toUpperCase()));
    log.debug("signed:" + signed);
    if (!signed) {
      isActive = false;
      errorMessage = "@NotSigned@";
      return false;
    }
    return true;
  }

  public LicenseClass getLicenseClass() {
    return licenseClass == null ? LicenseClass.COMMUNITY : licenseClass;
  }

  /**
   * Returns a CRC hash of the public key
   */
  public String getOpsLogId() {
    CRC32 crc = new CRC32();
    crc.update(getPublicKey().getBytes());
    return Long.toHexString(crc.getValue());
  }

  private PublicKey getPublicKey(String strPublickey) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      byte[] rawPublicKey = org.apache.commons.codec.binary.Base64
          .decodeBase64(strPublickey.getBytes());

      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(rawPublicKey);
      return keyFactory.generatePublic(publicKeySpec);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  public String getPublicKey() {
    return strPublicKey;
  }

  /**
   * Returns true when the instance currently is OBPS active, this is when it has a valid activation
   * key.
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Returns true when the instance currently is OBPS active. It is similar as
   * {@link ActivationKey#isActive}, but it is static and is initialized whenever the ActivationKey
   * class is instantiated.
   */
  public static boolean isActiveInstance() {
    return getInstance().isActive();
  }

  /**
   * Returns true when the instance has a activation key and the activation file has been loaded. It
   * doesn't verify is still valid.
   */
  public boolean isOPSInstance() {
    return instanceProperties != null;
  }

  /**
   * Returns true in case the instance has activation key though it might not be valid or activated
   */
  public boolean hasActivationKey() {
    return hasActivationKey;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getMessageType() {
    return messageType;
  }

  /**
   * activation message to be displayed in Instance Activation window
   */
  public ActivationMsg getActivationMessage() {
    if (StringUtils.isNotEmpty(errorMessage)) {
      // there is a core message (expiration, etc.), return it
      return new ActivationMsg(MsgSeverity.forType(messageType), errorMessage);
    }

    // look for messages defined by modules
    String customMsg = "";
    MsgSeverity severity = MsgSeverity.ERROR;
    for (ModuleLicenseRestrictions moduleRestriction : getModuleLicenseRestrictions()) {
      ActivationMsg moduleMsg = moduleRestriction.getActivationMessage(this,
          OBContext.getOBContext().getLanguage().getLanguage());

      if (moduleMsg != null) {
        customMsg += moduleMsg.getMsgText();
        severity = moduleMsg.getSeverity();
      }
    }

    if (StringUtils.isEmpty(customMsg)) {
      return null;
    }

    return new ActivationMsg(severity, customMsg);
  }

  /**
   * gets HTML to be injected in Instance Activation window with additional actions to be performed
   */
  public String getInstanceActivationExtraActionsHtml(XmlEngine xmlEngine) {
    String html = "";

    for (ModuleLicenseRestrictions moduleRestriction : getModuleLicenseRestrictions()) {
      String moduleHtml = moduleRestriction.getInstanceActivationExtraActionsHtml(xmlEngine);
      if (moduleHtml != null) {
        html += moduleHtml;
      }
    }

    return html;
  }

  /**
   * Deprecated, use instead {@link ActivationKey#checkOPSLimitations(String)}
   */
  @Deprecated
  public LicenseRestriction checkOPSLimitations() {
    return checkOPSLimitations("");
  }

  /**
   * @see ActivationKey#checkOPSLimitations(String, String)
   */
  public LicenseRestriction checkOPSLimitations(String currentSession) {
    return checkOPSLimitations(currentSession, null);
  }

  /**
   * Checks the current activation key
   *
   * @param currentSession
   *     Current session, used for checking the concurrent users limitation.
   * @param sessionType
   *     Successful session type: if the session is finally successful this is the type that
   *     will be marked with in {@code AD_Session}, it is used to determine whether it should
   *     or not count for CU limitation. In case it is {@code null} it will be counted.
   * @return {@link LicenseRestriction} with the status of the restrictions
   */
  public LicenseRestriction checkOPSLimitations(String currentSession, String sessionType) {
    LicenseRestriction result = LicenseRestriction.NO_RESTRICTION;
    if (!isOPSInstance()) {
      return LicenseRestriction.NO_RESTRICTION;
    }

    if (inconsistentInstance) {
      return LicenseRestriction.NOT_MATCHED_INSTANCE;
    }

    if (trial && !isHeartbeatActive()) {
      return LicenseRestriction.HB_NOT_ACTIVE;
    }

    if (!isActive && golden) {
      return LicenseRestriction.EXPIRED_GOLDEN;
    }

    if (!isActive) {
      return LicenseRestriction.OPS_INSTANCE_NOT_ACTIVE;
    }

    Long softUsers = null;
    if (getProperty("limituserswarn") != null) {
      softUsers = Long.valueOf(getProperty("limituserswarn"));
    }

    // maxUsers==0 is unlimited concurrent users
    boolean checkConcurrentUsers = maxUsers != 0 && !STATELESS_REQUEST.equals(currentSession)
        && consumesConcurrentUser(sessionType);
    if (checkConcurrentUsers) {
      OBContext.setAdminMode();
      int activeSessions = 0;
      try {
        activeSessions = getActiveSessions(currentSession);
        log4j.debug("Active sessions: " + activeSessions);
        if (activeSessions >= maxUsers || (softUsers != null && activeSessions >= softUsers)) {
          // Before raising concurrent users error, clean the session with ping timeout and try it
          // again
          if (deactivateTimeOutSessions(currentSession)) {
            activeSessions = getActiveSessions(currentSession);
            log4j.debug("Active sessions after timeout cleanup: " + activeSessions);
          }
        }
      } catch (Exception e) {
        log4j.error("Error checking sessions", e);
      } finally {
        OBContext.restorePreviousMode();
      }
      if (activeSessions >= maxUsers) {
        return LicenseRestriction.NUMBER_OF_CONCURRENT_USERS_REACHED;
      }

      if (softUsers != null && activeSessions >= softUsers) {
        result = LicenseRestriction.NUMBER_OF_SOFT_USERS_REACHED;
      }
    }

    if (getExpiredInstalledModules().size() > 0) {
      result = LicenseRestriction.MODULE_EXPIRED;
    }

    if (result == LicenseRestriction.NO_RESTRICTION) {
      // no restrictions so far, checking now if any of the installed modules adds a new restriction
      for (ModuleLicenseRestrictions moduleRestriction : getModuleLicenseRestrictions()) {
        result = moduleRestriction.checkRestrictions(this, currentSession);
        if (result == null) {
          result = LicenseRestriction.NO_RESTRICTION;
        } else if (result != LicenseRestriction.NO_RESTRICTION) {
          return result;
        }
      }
    }

    return result;
  }

  /**
   * Returns whether a session type is counted for concurrent users
   */
  public static boolean consumesConcurrentUser(String sessionType) {
    return sessionType == null || !NO_CU_SESSION_TYPES.contains(sessionType);
  }

  /**
   * Checks if heartbeat is active and a beat has been sent during last days.
   */
  public boolean isHeartbeatActive() {
    OBContext.setAdminMode();
    try {
      Boolean active = OBDal.getInstance().get(SystemInformation.class, "0").isEnableHeartbeat();
      if (active == null || !active) {
        return false;
      }
      OBCriteria<HeartbeatLog> hbLog = OBDal.getInstance().createCriteria(HeartbeatLog.class);
      Calendar lastDays = Calendar.getInstance();
      lastDays.add(Calendar.DAY_OF_MONTH, -9);
      hbLog.add(Restrictions.ge(HeartbeatLog.PROPERTY_CREATIONDATE,
          new Date(lastDays.getTimeInMillis())));
      return hbLog.count() > 0;

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Looks for all active sessions that have not had activity during last
   * {@link ActivationKey#PING_TIMEOUT_SECS} seconds and deactivates them. Activity is tracked by
   * the requests the browser sends to look for alerts (see
   * {@link org.openbravo.erpCommon.utility.VerticalMenu}).
   * <p/>
   * PING_TIMEOUT_SECS is hardcoded to 120s, pings are hardcoded in front-end to 50s.
   */
  private boolean deactivateTimeOutSessions(String currentSessionId) {
    if (!deactivateSessionsLock.tryLock()) {
      // another thread is already trying to deactivate sessions, don't do anything
      return false;
    }
    try {
      // Last valid ping time is current time substract timeout seconds
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, (-1) * PING_TIMEOUT_SECS);
      Date lastValidPingTime = new Date(cal.getTimeInMillis());

      OBCriteria<Session> obCriteria = OBDal.getInstance().createCriteria(Session.class);

      // sesion_active='Y' and (lastPing is null or lastPing<lastValidPing)
      obCriteria.add(Restrictions.and(Restrictions.eq(Session.PROPERTY_SESSIONACTIVE, true),
          Restrictions.or(Restrictions.isNull(Session.PROPERTY_LASTPING),
              Restrictions.lt(Session.PROPERTY_LASTPING, lastValidPingTime))));
      obCriteria.add(
          Restrictions.not(Restrictions.in(Session.PROPERTY_LOGINSTATUS, NO_CU_SESSION_TYPES)));

      if (currentSessionId != null) {
        obCriteria.add(Restrictions.ne(Session.PROPERTY_ID, currentSessionId));
      }

      List<Session> expiredCandidates = obCriteria.list();
      ArrayList<String> sessionsToDeactivate = new ArrayList<>(expiredCandidates.size());
      for (Session expiredSession : expiredCandidates) {
        if (shouldDeactivateSession(expiredSession, lastValidPingTime)) {
          sessionsToDeactivate.add(expiredSession.getId());
          log4j.info("Deactivating session: " + expiredSession.getId()
              + " beacuse of ping time out. Last ping: " + expiredSession.getLastPing()
              + ". Last valid ping time: " + lastValidPingTime);
        }
      }

      if (!sessionsToDeactivate.isEmpty()) {
        // deactivate sessions is a separate DB trx not to hold locks for a long time
        DalConnectionProvider cp = new DalConnectionProvider(false);
        Connection trxConn = null;
        boolean success = false;
        try {
          trxConn = cp.getTransactionConnection();
          ActivationKeyData.deactivateSessions(trxConn, cp,
              StringCollectionUtils.commaSeparated(sessionsToDeactivate));
          cp.releaseCommitConnection(trxConn);
          success = true;
        } catch (NoConnectionAvailableException | SQLException | ServletException e) {
          log.error("couldn't deactivate timed out sessions: " + sessionsToDeactivate, e);
        } finally {
          if (!success && trxConn != null) {
            try {
              cp.releaseRollbackConnection(trxConn);
            } catch (SQLException e) {
              log.error("couldn't rollback failed trx to deactivate timed out sessions: "
                  + sessionsToDeactivate, e);
            }
          }
        }
      }
      return !sessionsToDeactivate.isEmpty();
    } finally {
      deactivateSessionsLock.unlock();
    }
  }

  /**
   * Do not deactivate those sessions that are not using ping but consume concurrent users (ie.
   * mobile apps) if activity from them has been recently detected.
   */
  private boolean shouldDeactivateSession(Session expiredSession, Date lastValidPingTime) {
    if (BACKOFFICE_SUCCESS_SESSION_TYPES.contains(expiredSession.getLoginStatus())) {
      // backoffice sessions use ping, they can be deactivated even if created in a different node
      return true;
    }

    String sessionId = expiredSession.getId();
    HttpSession session = SessionListener.getActiveSession(sessionId);
    if (session == null) {
      log4j.debug("Session " + sessionId + " not found in context");
      // we cannot deactivate this session because it might have been created in a different node
      // from cluster and we cannot know when was used last time
      return false;
    }
    Date lastRequestTime = new Date(session.getLastAccessedTime());
    log4j.debug("Last request received from session " + sessionId + ": " + lastRequestTime);
    return lastRequestTime.compareTo(lastValidPingTime) < 0;
  }

  public String getPurpose(String lang) {
    return Utility.getListValueName("InstancePurpose", getProperty("purpose"), lang);
  }

  public String getLicenseExplanation(ConnectionProvider conn, String lang) {
    if (licenseType == LicenseType.CONCURRENT_USERS) {
      String userLimit = getProperty("limitusers");
      String userMsg = "0".equals(userLimit) ? Utility.messageBD(conn, "OPSUnlimitedUsers", lang)
          : userLimit;
      return userMsg + " " + Utility.messageBD(conn, "OPSConcurrentUsers", lang);
    } else {
      return Utility.getListValueName("OPSLicenseType", getProperty(LINCENSETYPE), lang);
    }
  }

  /**
   * Returns a message explaining WS call limitations
   */
  public String getWSExplanation(ConnectionProvider conn, String lang) {
    if (!limitedWsAccess) {
      return Utility.messageBD(conn, "OPSWSUnlimited", lang);
    } else {
      String packs = getProperty(WS_PACKS);
      String unitsPack = getProperty(WS_UNITS_PER_UNIT);
      return Utility.messageBD(conn, "OPWSLimited", lang)
          .replace("@packs@", packs)
          .replace("@unitsPerPack@", unitsPack);
    }
  }

  /**
   * Returns a message for POS Terminals limitations
   */
  public String getPOSTerminalsExplanation() {
    if (posTerminals == null || posTerminals.equals(0L)) {
      return OBMessageUtils.messageBD("OPSNone");
    } else if (posTerminals.equals(NO_LIMIT)) {
      return OBMessageUtils.messageBD("OPSWSUnlimited");
    } else {
      return posTerminals.toString();
    }
  }

  public boolean hasExpirationDate() {
    return isOPSInstance() && (getProperty("enddate") != null);
  }

  public String getProperty(String propName) {
    return instanceProperties.getProperty(propName);
  }

  public Long getPendingDays() {
    return pendingTime;
  }

  public boolean isSubscriptionConverted() {
    return subscriptionConvertedProperty;
  }

  public boolean hasExpired() {
    return hasExpired;
  }

  public boolean isNotActiveYet() {
    return notActiveYet;
  }

  public boolean hasUnlimitedWsAccess() {
    return !hasExpired && !limitedWsAccess;
  }

  /**
   * Obtains a List of all the modules that are installed in the instance which license has expired.
   *
   * @return List of the expired modules
   */
  public ArrayList<Module> getExpiredInstalledModules() {
    OBContext.setAdminMode();
    try {
      ArrayList<Module> result = new ArrayList<Module>();
      HashMap<String, CommercialModuleStatus> subscribedModules = getSubscribedModules();
      Iterator<String> iterator = subscribedModules.keySet().iterator();
      while (iterator.hasNext()) {
        String moduleId = iterator.next();
        if (subscribedModules.get(moduleId) == CommercialModuleStatus.EXPIRED) {
          Module module = OBDal.getInstance().get(Module.class, moduleId);
          if (module != null && module.getStatus().equals("A")) {
            result.add(module);
          }
        }
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Obtains a list for modules ID the instance is subscribed to and their statuses
   *
   * @return HashMap&lt;String, CommercialModuleStatus&gt; containing the subscribed modules
   */
  public HashMap<String, CommercialModuleStatus> getSubscribedModules() {
    return getSubscribedModules(true);
  }

  /**
   * get all additional messages to be printed in Instance Activation window.
   */
  public List<ModuleLicenseRestrictions.AdditionalInfo> getAdditionalMessageInfo() {
    List<ModuleLicenseRestrictions.AdditionalInfo> additionalInfo = new ArrayList<ModuleLicenseRestrictions.AdditionalInfo>();
    for (ModuleLicenseRestrictions moduleRestriction : getModuleLicenseRestrictions()) {
      additionalInfo.addAll(moduleRestriction.getAdditionalMessage());
    }
    return additionalInfo;
  }

  /**
   * Returns the number of current active sessions
   */
  public int getActiveSessions(String currentSession) {
    OBCriteria<Session> obCriteria = OBDal.getInstance().createCriteria(Session.class);
    obCriteria.add(Restrictions.eq(Session.PROPERTY_SESSIONACTIVE, true));
    obCriteria
        .add(Restrictions.not(Restrictions.in(Session.PROPERTY_LOGINSTATUS, NO_CU_SESSION_TYPES)));

    if (currentSession != null && !currentSession.equals("")) {
      obCriteria.add(Restrictions.ne(Session.PROPERTY_ID, currentSession));
    }
    return obCriteria.count();
  }

  /**
   * Same as {@link ActivationKey#getSubscribedModules()} with the includeDisabled parameter. When
   * this parameter is true, disabled modules are returned with the DISABLED status, other way they
   * are returned with the status they would have if they were not disabled.
   */
  private HashMap<String, CommercialModuleStatus> getSubscribedModules(boolean includeDisabled) {
    HashMap<String, CommercialModuleStatus> moduleList = new HashMap<String, CommercialModuleStatus>();
    if (instanceProperties == null) {
      return moduleList;
    }

    SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");

    String allModules = getProperty("modules");
    if (allModules == null || allModules.equals("")) {
      return moduleList;
    }
    String modulesInfo[] = allModules.split(",");
    Date now = new Date();
    for (String moduleInfo : modulesInfo) {
      String moduleData[] = moduleInfo.split("\\|");

      Date validFrom = null;
      Date validTo = null;
      try {
        validFrom = sd.parse(moduleData[1]);
        if (moduleData.length > 2) {
          validTo = sd.parse(moduleData[2]);
        }
        if (includeDisabled && !DisabledModules.isEnabled(Artifacts.MODULE, moduleData[0])) {
          moduleList.put(moduleData[0], CommercialModuleStatus.DISABLED);
        } else if (subscriptionActuallyConverted) {
          moduleList.put(moduleData[0], CommercialModuleStatus.CONVERTED_SUBSCRIPTION);
        } else if (validFrom.before(now) && (validTo == null || validTo.after(now))) {
          moduleList.put(moduleData[0], CommercialModuleStatus.ACTIVE);
        } else if (validFrom.after(now)) {
          moduleList.put(moduleData[0], CommercialModuleStatus.NO_ACTIVE_YET);
        } else if (validTo != null && validTo.before(now)) {
          if (subscriptionConvertedProperty) {
            moduleList.put(moduleData[0], CommercialModuleStatus.CONVERTED_SUBSCRIPTION);
          } else {
            moduleList.put(moduleData[0], CommercialModuleStatus.EXPIRED);
          }
        }
      } catch (Exception e) {
        log.error("Error reading module's dates module:" + moduleData[0], e);
      }

    }
    return moduleList;
  }

  /**
   * Checks whether a disabled module can be enabled again. A commercial module cannot be enabled in
   * case its license has expired or the instance is not commercial.
   *
   * @param module
   * @return true in case the module can be enabled
   */
  public boolean isModuleEnableable(Module module) {
    if (!module.isCommercial()) {
      return true;
    }
    if (!isActive()) {
      return false;
    }

    HashMap<String, CommercialModuleStatus> moduleList = getSubscribedModules(false);

    if (!moduleList.containsKey(module.getId())) {
      return false;
    }

    CommercialModuleStatus status = moduleList.get(module.getId());
    return status == CommercialModuleStatus.ACTIVE
        || status == CommercialModuleStatus.CONVERTED_SUBSCRIPTION;
  }

  /**
   * Returns the status for the commercial module passed as parameter. Note that module tier is not
   * checked here, this should be correctly handled in the license itself.
   *
   * @param moduleId
   * @return the status for the commercial module passed as parameter
   */
  public CommercialModuleStatus isModuleSubscribed(String moduleId) {
    return isModuleSubscribed(moduleId, true);
  }

  private CommercialModuleStatus isModuleSubscribed(String moduleId, boolean refreshIfNeeded) {
    HashMap<String, CommercialModuleStatus> moduleList = getSubscribedModules();

    if (!moduleList.containsKey(moduleId)) {
      log4j.debug("Module " + moduleId + " is not in the list of subscribed modules");

      if (!refreshIfNeeded) {
        return CommercialModuleStatus.NO_SUBSCRIBED;
      }

      boolean refreshed = refreshLicense(REFRESH_MIN_TIME);

      if (refreshed) {
        return ActivationKey.instance.isModuleSubscribed(moduleId);
      } else {
        return CommercialModuleStatus.NO_SUBSCRIBED;
      }
    }

    return moduleList.get(moduleId);
  }

  /**
   * Tries to refresh license if it wasn't refreshed for last 24hr or there were updates in
   * ad_system.
   */
  private void refreshIfNeeded() {
    if (hasActivationKey && !subscriptionConvertedProperty && !trial && isTimeToRefresh(ONE_DAY)) {
      refreshLicense(ONE_DAY);
    } else {
      // Reload from DB if it was modified from outside, this can happen if:
      // * License was refreshed in a different node in a cluster
      // * Instance was activated through CLI: ant activate.instance
      if (lastUpdateTimestamp == null || !lastUpdateTimestamp.equals(getSystem().getUpdated())) {
        loadFromDB();
      }
    }
  }

  private boolean isTimeToRefresh(int minutesToRefresh) {
    Date timeToRefresh = null;
    if (instance.lastRefreshTime != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(instance.lastRefreshTime);
      calendar.add(Calendar.MINUTE, minutesToRefresh);
      timeToRefresh = calendar.getTime();
    }

    return timeToRefresh == null || new Date().after(timeToRefresh);
  }

  private boolean refreshLicense(int minutesToRefresh) {
    if (!refreshLicenseLock.tryLock()) {
      // another thread already refreshing license, allow it to complete
      return false;
    }

    try {
      if (!isTimeToRefresh(minutesToRefresh)) {
        return false;
      }

      long t = System.currentTimeMillis();
      log4j.debug("Trying to refresh license, last refresh "
          + (lastRefreshTime == null ? "never" : lastRefreshTime.toString()));

      Map<String, Object> params = new HashMap<String, Object>();
      params.put("publicKey", strPublicKey);
      params.put("activate", true);

      if (instanceProperties != null) {
        // this could happen ie. with old basic licenses signed with a now invalid key
        params.put("purpose", getProperty("purpose"));
        params.put("instanceNo", getProperty(INSTANCENO));
        params.put("updated", getProperty("updated"));
      } else {
        params.put("purpose",
            OBDal.getInstance().get(SystemInformation.class, "0").getInstancePurpose());
      }
      ProcessBundle pb = new ProcessBundle(null, new VariablesSecureApp("0", "0", "0"));
      pb.setParams(params);

      boolean refreshed = false;
      OBContext.setAdminMode();
      try {
        new ActiveInstanceProcess().execute(pb);
        OBError msg = (OBError) pb.getResult();
        refreshed = msg.getType().equals("Success");
        if (refreshed) {
          OBDal.getInstance().flush();
          log4j.debug("Instance refreshed");
        } else {
          log4j.info("Problem refreshing instance " + msg.getMessage());
        }
      } catch (Exception e) {
        log4j.error("Error refreshing instance", e);
        refreshed = false;
      } finally {
        OBContext.restorePreviousMode();
      }

      // Even license couldn't be refreshed, set lastRefreshTime not to try to
      // refresh in the following period of time
      resetRefreshTime();

      log.info("License refreshed in " + (System.currentTimeMillis() - t) + "ms");
      return refreshed;
    } finally {
      refreshLicenseLock.unlock();
    }
  }

  /**
   * Checks whether there is access to an artifact because of license restrictions (checking core
   * advance and premium features).
   *
   * @param type
   *     Type of artifact (Window, Report, Process...)
   * @param id
   *     Id of the Artifact
   * @return true in case it has access, false if not
   */
  public FeatureRestriction hasLicenseAccess(String type, String id) {
    String actualType = type;

    if (actualType == null || actualType.isEmpty() || id == null || id.isEmpty()) {
      return FeatureRestriction.NO_RESTRICTION;
    }
    log4j.debug("Type: {} id: {}", actualType, id);

    if ("W".equals(actualType)) {
      // Access is granted to window, but permissions is checked for tabs
      OBContext.setAdminMode();
      try {
        Tab tab = OBDal.getInstance().get(Tab.class, id);
        if (tab == null) {
          log4j.error("Could't find tab {} to check access. Access not allowed", id);
          return FeatureRestriction.UNKNOWN_RESTRICTION;
        }

        // For windows check whether the window's module is disabled, and later whether the tab is
        // disabled
        if (!DisabledModules.isEnabled(Artifacts.MODULE, tab.getWindow().getModule().getId())) {
          return FeatureRestriction.DISABLED_MODULE_RESTRICTION;
        }
      } finally {
        OBContext.restorePreviousMode();
      }
    } else if ("MW".equals(actualType)) {
      // Menu window, it receives window instead of tab
      actualType = "W";
    } else if ("R".equals(actualType)) {
      actualType = "P";
    }

    // Check disabled modules restrictions
    Artifacts artifactType;
    if ("MW".equals(actualType)) {
      artifactType = Artifacts.WINDOW;
    } else if ("W".equals(actualType)) {
      artifactType = Artifacts.TAB;
    } else if ("X".equals(actualType)) {
      artifactType = Artifacts.FORM;
    } else {
      artifactType = Artifacts.PROCESS;
    }
    // Use id instead of artifactId to keep tabs' ids
    if (!DisabledModules.isEnabled(artifactType, id)) {
      return FeatureRestriction.DISABLED_MODULE_RESTRICTION;
    }

    return FeatureRestriction.NO_RESTRICTION;
  }

  /**
   * Verifies all the commercial installed modules are allowed to the instance.
   *
   * @return List of non allowed modules
   */
  public String verifyInstalledModules() {
    return verifyInstalledModules(true);
  }

  String verifyInstalledModules(boolean refreshIfneeded) {
    String rt = "";

    OBContext.setAdminMode();
    try {
      OBCriteria<Module> mods = OBDal.getInstance().createCriteria(Module.class);
      mods.add(Restrictions.eq(Module.PROPERTY_COMMERCIAL, true));
      mods.add(Restrictions.eq(Module.PROPERTY_ENABLED, true));
      // Allow development of commercial modules which are not in the license.
      mods.add(Restrictions.eq(Module.PROPERTY_INDEVELOPMENT, false));
      mods.addOrder(Order.asc(Module.PROPERTY_NAME));
      for (Module mod : mods.list()) {
        if (isModuleSubscribed(mod.getJavaPackage(),
            refreshIfneeded) == CommercialModuleStatus.NO_SUBSCRIBED) {
          rt += (rt.isEmpty() ? "" : ", ") + mod.getName();
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return rt;
  }

  /**
   * Returns current subscription status
   */
  public SubscriptionStatus getSubscriptionStatus() {
    if (!isOPSInstance() || inconsistentInstance) {
      return SubscriptionStatus.COMMUNITY;
    } else if (isSubscriptionConverted()) {
      return SubscriptionStatus.CANCEL;
    } else if (hasExpired()) {
      return SubscriptionStatus.EXPIRED;
    } else if (isNotActiveYet()) {
      return SubscriptionStatus.NO_ACTIVE_YET;
    } else if (!hasActivationKey) {
      return SubscriptionStatus.INVALID;
    } else {
      return SubscriptionStatus.ACTIVE;
    }
  }

  public boolean isTrial() {
    return trial;
  }

  public boolean isGolden() {
    return golden;
  }

  /**
   * Returns a JSONObject with a message warning about near expiration or already expired instance
   * to be displayed in Login page.
   */
  public JSONObject getExpirationMessage(String lang) {
    JSONObject result = new JSONObject();

    try {
      if (StringUtils.isNotBlank(errorMessage)) {
        result.put("type", "Error");
        result.put("text",
            Utility.parseTranslation(new DalConnectionProvider(false), null, lang, errorMessage));

        return result;
      }

      // Community or professional without expiration
      if (pendingTime == null || subscriptionActuallyConverted) {
        // no restrictions so far, checking now if any of the installed modules adds a new
        // restriction
        for (ModuleLicenseRestrictions moduleRestriction : getModuleLicenseRestrictions()) {
          ActivationMsg msg = moduleRestriction.getActivationMessage(this, lang);

          if (msg != null) {
            result.put("type", "Error"); // always error for login page (warn is shown as an alert)
            result.put("text", msg.getMsgText());
          }
        }
        return result;
      }
      if (!hasExpired) {
        String msg;
        Long daysToExpireMsg = getProperty("daysWarn") == null ? null
            : Long.parseLong(getProperty("daysWarn"));
        if (golden) {
          msg = "OBPS_TO_EXPIRE_GOLDEN";
          if (daysToExpireMsg == null) {
            daysToExpireMsg = 999L; // show always
          }
        } else if (trial) {
          msg = "OBPS_TO_EXPIRE_TRIAL";
          if (daysToExpireMsg == null) {
            daysToExpireMsg = 999L; // show always
          }
        } else if (licenseClass == LicenseClass.BASIC) {
          msg = "OBPS_TO_EXPIRE_BASIC";
          if (daysToExpireMsg == null) {
            daysToExpireMsg = EXPIRATION_BASIC_DAYS;
          }
        } else {
          msg = "OBPS_TO_EXPIRE_PROF";
          if (daysToExpireMsg == null) {
            daysToExpireMsg = EXPIRATION_PROF_DAYS;
          }
        }

        if (pendingTime <= daysToExpireMsg) {
          result.put("type", "Error");
          result.put("text", Utility.messageBD(new DalConnectionProvider(false), msg, lang, false)
              .replace("@days@", pendingTime.toString()));
        }
      } else {
        String msg;
        if (golden) {
          msg = "OBPS_EXPIRED_GOLDEN";
          result.put("disableLogin", true);
        } else if (trial) {
          msg = "OBPS_EXPIRED_TRIAL";
        } else if (licenseClass == LicenseClass.BASIC) {
          msg = "OBPS_EXPIRED_BASIC";
        } else {
          msg = "OBPS_EXPIRED_PROF";
        }

        result.put("type", "Error");
        result.put("text", Utility.messageBD(new DalConnectionProvider(false), msg, lang, false));
      }
    } catch (JSONException e) {
      log4j.error("Error calculating expiration message", e);
    }
    return result;
  }

  /**
   * This method checks web service can be called. If <code>updateCounter</code> parameter is
   * <code>true</code> number of daily calls is increased by one.
   *
   * @param updateCounter
   *     daily calls should be updated
   */
  public WSRestriction checkNewWSCall(boolean updateCounter) {
    if (hasExpired) {
      return WSRestriction.EXPIRED;
    }

    if (getExpiredInstalledModules().size() > 0) {
      return WSRestriction.EXPIRED_MODULES;
    }

    if (!limitedWsAccess) {
      return WSRestriction.NO_RESTRICTION;
    }

    Date today = getDayAt0(new Date());

    if (initWsCountTime == null || today.getTime() != initWsCountTime.getTime()) {
      initializeWsDayCounter();
    }

    long checkCalls = maxWsCalls;
    long currentDayCount;
    if (updateCounter) {
      currentDayCount = wsDayCounter.incrementAndGet();
      // Adding 1 to maxWsCalls because session is already saved in DB
      checkCalls += 1;
    } else {
      currentDayCount = wsDayCounter.get();
    }

    if (currentDayCount > checkCalls) {
      synchronized (wsCountLock) {
        // clean up old days
        while (!exceededInLastDays.isEmpty() && exceededInLastDays.get(0)
            .getTime() < today.getTime() - WS_MS_EXCEEDING_ALLOWED_PERIOD) {
          Date removed = exceededInLastDays.remove(0);
          log.info("Removed date from exceeded days " + removed);
        }

        if (!exceededInLastDays.contains(today)) {
          exceededInLastDays.add(today);

          // Adding a new failing day, send a new beat to butler
          Runnable sendBeatProcess = new Runnable() {
            @Override
            public void run() {
              try {
                String content = "beatType=CWSR";
                content += "&systemIdentifier="
                    + URLEncoder.encode(SystemInfo.getSystemIdentifier(), "utf-8");
                content += "&dbIdentifier="
                    + URLEncoder.encode(SystemInfo.getDBIdentifier(), "utf-8");
                content += "&macId=" + URLEncoder.encode(SystemInfo.getMacAddress(), "utf-8");
                content += "&obpsId=" + URLEncoder.encode(SystemInfo.getOBPSInstance(), "utf-8");
                content += "&instanceNo="
                    + URLEncoder.encode(SystemInfo.getOBPSIntanceNumber(), "utf-8");

                URL url = new URL(HEARTBEAT_URL);
                HttpsUtils.sendSecure(url, content);
                log.info("Sending CWSR beat");
              } catch (Exception e) {
                log.error("Error connecting server", e);
              }

            }
          };
          Thread sendBeat = new Thread(sendBeatProcess);
          sendBeat.start();
        }

        if (exceededInLastDays.size() > WS_DAYS_EXCEEDING_ALLOWED) {
          return WSRestriction.EXCEEDED_MAX_WS_CALLS;
        } else {
          return WSRestriction.EXCEEDED_WARN_WS_CALLS;
        }
      }
    }
    return WSRestriction.NO_RESTRICTION;
  }

  private Date getDayAt0(Date date) {
    try {
      return sDateFormat.parse(sDateFormat.format(date));
    } catch (ParseException e) {
      log.error("Error getting day " + date + " at 0:00", e);
      return new Date();
    }
  }

  private synchronized void initializeWsDayCounter() {
    Date today = getDayAt0(new Date());
    if (!(initWsCountTime == null || today.getTime() != initWsCountTime.getTime())) {
      // already initialized in a different thread
      return;
    }
    initWsCountTime = getDayAt0(new Date());
    OBContext.setAdminMode();
    try {
      wsDayCounter = new AtomicLong(getNumberWSDayCounter());
      log.info("Initialized ws count to " + wsDayCounter.get() + " from " + initWsCountTime);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public int getNumberWSDayCounter() {
    Date date = getDayAt0(new Date());
    OBCriteria<Session> qLogins = OBDal.getInstance().createCriteria(Session.class);
    qLogins.add(Restrictions.eq(Session.PROPERTY_LOGINSTATUS, "WS"));
    qLogins.add(Restrictions.ge(Session.PROPERTY_CREATIONDATE, date));
    return qLogins.count();
  }

  private void initializeWsCounter() {
    //@formatter:off
    String hql = 
            "select min(creationDate) " +
            "  from ADSession " +
            " where loginStatus = 'WS' " +
            "   and creationDate > :firstDay " +
            " group by day(creationDate), month(creationDate), year(creationDate) " +
            "   having count(*) > :maxWsPerDay " +
            " order by 1";
    //@formatter:on
    Query<Date> qExceededDays = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Date.class)
        .setParameter("firstDay",
            new Date(getDayAt0(new Date()).getTime() - WS_MS_EXCEEDING_ALLOWED_PERIOD))
        .setParameter("maxWsPerDay", maxWsCalls);

    exceededInLastDays = new ArrayList<Date>();

    for (Object d : qExceededDays.list()) {
      Date day = getDayAt0((Date) d);
      exceededInLastDays.add(day);
      log.info("Addind exceeded ws calls day " + day);
    }
    initializeWsDayCounter();
  }

  /**
   * Returns the number of days during last 30 days exceeding the maximum allowed number of calls
   */
  public int getWsCallsExceededDays() {
    if (exceededInLastDays == null) {
      return 0;
    }
    return exceededInLastDays.size();
  }

  /**
   * Returns the number of days that can exceed the maximum number of ws calls taking into account
   * the ones that exceeded it during last 30 days.
   */
  public int getExtraWsExceededDaysAllowed() {
    return WS_DAYS_EXCEEDING_ALLOWED - getWsCallsExceededDays();
  }

  /**
   * Returns the number of days pending till the end of ws calls verification period.
   */
  public int getNumberOfDaysLeftInPeriod() {
    if (exceededInLastDays == null || exceededInLastDays.size() == 0) {
      return (int) WS_DAYS_EXCEEDING_ALLOWED_PERIOD;
    }

    Date today = getDayAt0(new Date());
    Date firstDayOfPeriod = exceededInLastDays.get(0);

    long lastDayOfPeriod;
    if (today.getTime() + (getExtraWsExceededDaysAllowed() * MILLSECS_PER_DAY) < firstDayOfPeriod
        .getTime() + WS_MS_EXCEEDING_ALLOWED_PERIOD) {
      lastDayOfPeriod = firstDayOfPeriod.getTime() + WS_MS_EXCEEDING_ALLOWED_PERIOD;
    } else {
      lastDayOfPeriod = today.getTime() + WS_MS_EXCEEDING_ALLOWED_PERIOD;
    }
    new Date(lastDayOfPeriod);
    long pendingMs = lastDayOfPeriod - today.getTime()
        - (exceededInLastDays.size() * MILLSECS_PER_DAY);
    return (int) (pendingMs / MILLSECS_PER_DAY);
  }

  public Long getAllowedPosTerminals() {
    // posTerminals not set if community: do not apply restriction
    return posTerminals == null ? NO_LIMIT : posTerminals;
  }

  public Long getPosTerminalsWarn() {
    return posTerminalsWarn;
  }

  /**
   * Returns whether only System Admin should be allowed, because it is already set in session or
   * there are license restrictions.
   */
  public boolean forceSysAdminLogin(HttpSession session) {
    String dbSessionId = null;
    if (session != null) {
      if ("Y".equals(session.getAttribute("ONLYSYSTEMADMINROLESHOULDBEAVAILABLEINERP"))) {
        return true;
      }
      dbSessionId = (String) session.getAttribute("#AD_SESSION_ID");
    }

    return hasLicenseLimitation(dbSessionId);
  }

  /**
   * Returns whether the stateless request is allowed or not due to any license limitation.
   */
  public boolean isStatelessRequestAllowed() {
    return !hasLicenseLimitation(STATELESS_REQUEST);
  }

  private boolean hasLicenseLimitation(String dbSessionId) {
    LicenseRestriction limitation = checkOPSLimitations(dbSessionId);
    return limitation == LicenseRestriction.OPS_INSTANCE_NOT_ACTIVE
        || limitation == LicenseRestriction.NUMBER_OF_CONCURRENT_USERS_REACHED
        || limitation == LicenseRestriction.MODULE_EXPIRED
        || limitation == LicenseRestriction.NOT_MATCHED_INSTANCE
        || limitation == LicenseRestriction.HB_NOT_ACTIVE
        || limitation == LicenseRestriction.POS_TERMINALS_EXCEEDED;
  }

  private List<ModuleLicenseRestrictions> getModuleLicenseRestrictions() {
    List<ModuleLicenseRestrictions> result = new ArrayList<ModuleLicenseRestrictions>();
    BeanManager bm = WeldUtils.getStaticInstanceBeanManager();
    for (Bean<?> restrictionBean : bm.getBeans(ModuleLicenseRestrictions.class)) {
      result.add((ModuleLicenseRestrictions) bm.getReference(restrictionBean,
          ModuleLicenseRestrictions.class, bm.createCreationalContext(restrictionBean)));
    }
    return result;
  }

  /**
   * @return all license's properties
   */
  public Properties getInstanceProperties() {
    return instanceProperties;
  }

  /**
   * @return starting valid date for license
   */
  public Date getStartDate() {
    return startDate;
  }

  /**
   * @return license's expiration date
   */
  public Date getEndDate() {
    return endDate;
  }

  /**
   * @return maximum allowed concurrent users
   */
  public Long getMaxUsers() {
    return maxUsers;
  }
}
