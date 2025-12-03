/*
 *************************************************************************
 * The contents of this file are subject to the Etendo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Etendo ERP.
 * The Initial Developer of the Original Code is Etendo SLU
 * All portions are Copyright (C) 2026 Etendo SLU
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
 * Ant task that generates cryptographic keys for Secure Web Service (SWS) authentication.
 * <p>
 * This task supports two signing algorithms:
 * <ul>
 *   <li>ES256 (ECDSA with P-256 curve and SHA-256)</li>
 *   <li>HS256 (HMAC with SHA-256)</li>
 * </ul>
 * The algorithm selection is retrieved from the database preference 'SMFSWS_EncryptionAlgorithm'.
 * Generated keys are stored in the smfsws_config database table in JSON format.
 */
public class GenerateSWSKeysTask extends Task {
  private static final Logger log = LogManager.getLogger();
  private String propertiesFile;

  /**
   * Executes the key generation task.
   * <p>
   * This method performs the following steps:
   * <ol>
   *   <li>Establishes a database connection using the configured properties file</li>
   *   <li>Retrieves the encryption algorithm preference from the database</li>
   *   <li>Validates that the algorithm is either ES256 or HS256</li>
   *   <li>Generates the appropriate cryptographic keys based on the algorithm</li>
   *   <li>Stores the generated keys in the database</li>
   * </ol>
   *
   * @throws BuildException
   *     if the algorithm is unsupported, if key generation fails,
   *     or if database operations encounter errors
   */
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

  /**
   * Retrieves the encryption algorithm preference from the database.
   * <p>
   * Queries the ad_preference table for the 'SMFSWS_EncryptionAlgorithm' property.
   * If no preference is found or an error occurs, returns "ES256" as the default value.
   *
   * @param conn
   *     the database connection to use for the query
   * @return the encryption algorithm identifier (ES256 or HS256)
   */
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

  /**
   * Generates an ES256 (ECDSA) key pair for asymmetric encryption.
   * <p>
   * Creates a public/private key pair using the secp256r1 (P-256) elliptic curve.
   * Keys are encoded in standard formats:
   * <ul>
   *   <li>Private key: PKCS8 format</li>
   *   <li>Public key: X.509 format</li>
   * </ul>
   * Both keys are converted to PEM format and returned as a JSON string.
   *
   * @return a JSON string containing "private-key" and "public-key" fields in PEM format
   * @throws NoSuchAlgorithmException
   *     if the EC algorithm is not available
   * @throws InvalidAlgorithmParameterException
   *     if the secp256r1 curve specification is invalid
   * @throws JsonProcessingException
   *     if JSON serialization fails
   */
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

  /**
   * Generates an HS256 (HMAC-SHA256) secret key for symmetric encryption.
   * <p>
   * Creates a 256-bit secret key suitable for HMAC-SHA256 operations.
   * The key is converted to PEM format and returned as a JSON string.
   * The "public-key" field is included but left empty since HS256 uses symmetric encryption.
   *
   * @return a JSON string containing "private-key" (the secret) and an empty "public-key" field
   * @throws NoSuchAlgorithmException
   *     if the HmacSHA256 algorithm is not available
   * @throws JsonProcessingException
   *     if JSON serialization fails
   */
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

  /**
   * Converts a byte array to PEM (Privacy Enhanced Mail) format.
   * <p>
   * The byte array is Base64-encoded and wrapped with standard PEM header and footer markers.
   *
   * @param buffer
   *     the byte array to convert (typically a key in binary format)
   * @param label
   *     the key type label (e.g., "PRIVATE KEY", "PUBLIC KEY", "SECRET KEY")
   * @return a PEM-formatted string with appropriate BEGIN/END markers
   */
  private String arrayBufferToPem(byte[] buffer, String label) {
    String base64 = Base64.getEncoder().encodeToString(buffer);
    return "-----BEGIN " + label + "-----" + base64 + "-----END " + label + "-----";
  }

  /**
   * Updates or inserts the generated cryptographic keys in the database.
   * <p>
   * This method checks if a configuration record exists in the smfsws_config table:
   * <ul>
   *   <li>If a record exists: updates the privatekey field and sets the updated timestamp</li>
   *   <li>If no record exists: creates a new record with a generated UUID as the primary key</li>
   * </ul>
   * The expiration time is set to 36000 seconds (10 hours) by default.
   *
   * @param conn
   *     the database connection to use for the operation
   * @param keyJson
   *     the JSON string containing the generated keys
   * @throws SQLException
   *     if database update or insert operations fail
   */
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

        String configId = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();

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
