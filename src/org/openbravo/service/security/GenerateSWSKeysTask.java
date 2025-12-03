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
 * All portions are Copyright (C) 2024 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.security;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Task to generate cryptographic keys for Secure Web Service authentication
 */
public class GenerateSWSKeysTask extends Task {
  private static final Logger log = LogManager.getLogger();
  private String propertiesFile;

  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  @Override
  public void execute() throws BuildException {
    log.info("Generating keys for Secure Web Service...");

    ConnectionProvider connProvider = null;
    Connection conn = null;
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      // Get database connection
      connProvider = new ConnectionProviderImpl(propertiesFile);
      conn = connProvider.getConnection();

      // Get encryption algorithm from database
      String algorithm = getEncryptionAlgorithm(conn);
      log.info("Selected algorithm: {}", algorithm);

      // Validate algorithm
      if (!algorithm.equals("ES256") && !algorithm.equals("HS256")) {
        throw new BuildException("Unsupported signing algorithm: " + algorithm);
      }

      // Generate keys based on algorithm
      String keyJson;
      if (algorithm.equals("HS256")) {
        log.info("Generating HS256 secret key...");
        keyJson = generateHS256Key();
      } else {
        log.info("Generating ES256 key pair...");
        keyJson = generateES256Keys();
      }

      // Update database
      updateKeyInDatabase(conn, keyJson);

      log.info("Keys generated and saved successfully for SWS");

    } catch (Exception e) {
      log.error("Error generating keys", e);
      throw new BuildException("Error generating SWS keys: " + e.getMessage(), e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
        if (conn != null) {
          conn.close();
        }
      } catch (Exception e) {
        log.error("Error closing database resources", e);
      }
    }
  }

  private String getEncryptionAlgorithm(Connection conn) {
    String algorithm = "ES256"; // Default value

    String query = "SELECT value FROM ad_preference " + "WHERE property = 'SMFSWS_EncryptionAlgorithm' "
        + "AND isactive = 'Y' " + "LIMIT 1";

    try (PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        String value = rs.getString("value");
        if (value != null && !value.isEmpty()) {
          algorithm = value;
        }
      }
    } catch (Exception e) {
      log.warn("Could not retrieve algorithm from database, using ES256 as default", e);
    }

    return algorithm;
  }

  private String generateES256Keys() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, JsonProcessingException {
    // Generate ECDSA key pair (ES256)
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1"); // P-256 curve
    keyPairGenerator.initialize(ecSpec, new SecureRandom());

    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    // Export private key in PKCS8 format
    byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
    String privateKeyPem = arrayBufferToPem(privateKeyBytes, "PRIVATE KEY");

    // Export public key in X.509 format
    byte[] publicKeyBytes = keyPair.getPublic().getEncoded();
    String publicKeyPem = arrayBufferToPem(publicKeyBytes, "PUBLIC KEY");

    // Create JSON object
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode keyObject = mapper.createObjectNode();
    keyObject.put("private-key", privateKeyPem);
    keyObject.put("public-key", publicKeyPem);

    return mapper.writeValueAsString(keyObject);
  }

  private String generateHS256Key() throws NoSuchAlgorithmException, JsonProcessingException {
    // Generate HMAC secret key (HS256)
    KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
    keyGenerator.init(256, new SecureRandom());

    SecretKey secretKey = keyGenerator.generateKey();
    byte[] secretKeyBytes = secretKey.getEncoded();
    String secretKeyPem = arrayBufferToPem(secretKeyBytes, "SECRET KEY");

    // Create JSON object
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode keyObject = mapper.createObjectNode();
    keyObject.put("private-key", secretKeyPem);
    keyObject.put("public-key", "");

    return mapper.writeValueAsString(keyObject);
  }

  private String arrayBufferToPem(byte[] buffer, String label) {
    String base64 = Base64.getEncoder().encodeToString(buffer);
    return "-----BEGIN " + label + "-----" + base64 + "-----END " + label + "-----";
  }

  private void updateKeyInDatabase(Connection conn, String keyJson) throws SQLException {
    // Check if a record already exists
    String selectQuery = "SELECT smfsws_config_id FROM smfsws_config LIMIT 1";

    try (PreparedStatement ps = conn.prepareStatement(selectQuery); ResultSet rs = ps.executeQuery()) {

      if (rs.next()) {
        // Update existing record
        String configId = rs.getString("smfsws_config_id");
        log.info("Updating existing SWS configuration record...");

        String updateQuery = "UPDATE smfsws_config SET privatekey = ?, updated = NOW(), updatedby = '100', expirationtime = 36000 "
            + "WHERE smfsws_config_id = ?";

        try (PreparedStatement updatePs = conn.prepareStatement(updateQuery)) {
          updatePs.setString(1, keyJson);
          updatePs.setString(2, configId);
          updatePs.executeUpdate();
        }

      } else {
        // Create new record
        log.info("Creating new SWS configuration record...");

        String configId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);

        String insertQuery = "INSERT INTO smfsws_config "
            + "(smfsws_config_id, ad_client_id, ad_org_id, isactive, created, createdby, "
            + "updated, updatedby, expirationtime, privatekey) "
            + "VALUES (?, '0', '0', 'Y', NOW(), '100', NOW(), '100', 36000, ?)";

        try (PreparedStatement insertPs = conn.prepareStatement(insertQuery)) {
          insertPs.setString(1, configId);
          insertPs.setString(2, keyJson);
          insertPs.executeUpdate();
        }
      }
    }
  }
}
