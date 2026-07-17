/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;

/**
 * Shared base for the JUnit wrappers around the raw SQL / shell test scripts under
 * {@code modules/com.etendoerp.storedcomputedcolumn/src-test/sql/} (EPL-1807 — stored computed
 * columns).
 *
 * <h2>Why these tests shell out instead of running SQL through JDBC</h2>
 * <p>The scripts are <b>not</b> plain SQL: they are {@code psql} / {@code sqlplus} / {@code bash}
 * programs that rely on client meta-commands ({@code \set ON_ERROR_STOP}, {@code \if},
 * {@code \echo}, {@code :'VAR'} interpolation, session-persistent temp tables, {@code DO $$}
 * anonymous blocks, advisory-lock start barriers, etc.). A JDBC {@link java.sql.Connection} —
 * such as the one an {@code OBBaseTest} would hand out via
 * {@code OBDal.getInstance().getConnection()} / {@link org.openbravo.service.db.DalConnectionProvider}
 * — cannot interpret client meta-commands, so feeding these scripts statement-by-statement through
 * JDBC is impossible. Each script instead <b>self-asserts</b> internally (it aborts on the first
 * failing assertion via {@code RAISE EXCEPTION} / {@code RAISE_APPLICATION_ERROR} under
 * {@code ON_ERROR_STOP} / {@code WHENEVER SQLERROR EXIT ROLLBACK}) and prints a terminal
 * <i>sentinel</i> line only on a fully clean run. The faithful JUnit contract is therefore:
 * launch the script with the native client, and assert (a) a zero exit code and (b) the presence
 * of the sentinel line in its output.</p>
 *
 * <h2>Connection / config mechanism (no hardcoded credentials)</h2>
 * <p>Connection coordinates are resolved from {@link OBPropertiesProvider}, i.e. the running
 * Etendo {@code Openbravo.properties} — the <b>same</b> source of truth that
 * {@link org.openbravo.service.db.DalConnectionProvider#getProperties()} and
 * {@code org.openbravo.base.session.SessionFactoryController} read the {@code bbdd.*} keys from.
 * Nothing is hardcoded: host/port/database/user/password come from {@code bbdd.url},
 * {@code bbdd.sid}, {@code bbdd.user}, {@code bbdd.password}; the dialect from {@code bbdd.rdbms};
 * and the script directory from {@code source.path}. For PostgreSQL the credentials are handed to
 * {@code psql} through the standard {@code PG*} environment variables (never on the command line),
 * which is also exactly how {@code stored_computed_concurrency.sh} expects to be invoked.</p>
 *
 * <h2>Graceful skipping</h2>
 * <p>These are live-DB integration tests. A subclass skips (JUnit {@code Assume}) rather than fails
 * when the required native client is not on the {@code PATH} or the active {@code bbdd.rdbms} does
 * not match the script's dialect (e.g. the Oracle script on a PostgreSQL instance). They still need
 * a running Etendo database with the module deployed ({@code ./gradlew update.database}) to pass.</p>
 */
public abstract class StoredComputedSqlScriptTestBase {

  private static final Logger log = LogManager.getLogger();

  /** Location of the SQL/shell scripts relative to {@code source.path}. */
  private static final String SQL_DIR_REL = "modules/com.etendoerp.storedcomputedcolumn/src-test/sql";

  /** Generous per-script wall-clock cap; scripts self-restore, so a hang must not block forever. */
  private static final long PROCESS_TIMEOUT_MINUTES = 15;

  private static final Pattern PG_URL = Pattern
      .compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?", Pattern.CASE_INSENSITIVE);
  private static final Pattern ORA_URL = Pattern
      .compile("jdbc:oracle:thin:@[/]*([^:/]+):(\\d+)[:/]?(.*)", Pattern.CASE_INSENSITIVE);

  /** Captured outcome of a launched script. */
  protected static final class ScriptResult {
    final int exitCode;
    final String output;

    ScriptResult(int exitCode, String output) {
      this.exitCode = exitCode;
      this.output = output;
    }
  }

  // --- config resolution (single source of truth: OBPropertiesProvider / Openbravo.properties) ---

  protected static Properties props() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties();
  }

  protected static String rdbms() {
    return props().getProperty("bbdd.rdbms", "").trim().toUpperCase();
  }

  protected static boolean isPostgres() {
    return rdbms().startsWith("POSTGRE");
  }

  protected static boolean isOracle() {
    return rdbms().startsWith("ORACLE");
  }

  /** Absolute path to the script directory, resolved from {@code source.path}. */
  protected static File sqlDir() {
    String sourcePath = props().getProperty("source.path");
    assertTrue("source.path is not set in Openbravo.properties",
        sourcePath != null && !sourcePath.isEmpty());
    File dir = new File(sourcePath, SQL_DIR_REL);
    assertTrue("Script directory not found: " + dir.getAbsolutePath(), dir.isDirectory());
    return dir;
  }

  protected static File script(String name) {
    File f = new File(sqlDir(), name);
    assertTrue("Script not found: " + f.getAbsolutePath(), f.isFile());
    return f;
  }

  // --- native client discovery / skipping ---

  /** Skip the test (do not fail) when the required native client binary is not on the PATH. */
  protected static void assumeBinaryOnPath(String binary) {
    assumeTrue(binary + " is not available on PATH — skipping live-DB script test",
        isOnPath(binary));
  }

  private static boolean isOnPath(String binary) {
    String path = System.getenv("PATH");
    if (path == null) {
      return false;
    }
    for (String dir : path.split(File.pathSeparator)) {
      File candidate = new File(dir, binary);
      if (candidate.isFile() && candidate.canExecute()) {
        return true;
      }
    }
    return false;
  }

  // --- process execution ---

  /**
   * Populates the child environment with the standard PostgreSQL {@code PG*} variables parsed from
   * {@code bbdd.url}/{@code bbdd.sid}/{@code bbdd.user}/{@code bbdd.password}. The password is
   * passed only via {@code PGPASSWORD}, never on the command line.
   */
  protected static void applyPostgresEnv(Map<String, String> env) {
    Matcher m = PG_URL.matcher(props().getProperty("bbdd.url", ""));
    assertTrue("bbdd.url is not a recognised PostgreSQL JDBC URL: " + props().getProperty("bbdd.url"),
        m.find());
    env.put("PGHOST", m.group(1));
    env.put("PGPORT", m.group(2) != null ? m.group(2) : "5432");
    env.put("PGDATABASE", props().getProperty("bbdd.sid", ""));
    env.put("PGUSER", props().getProperty("bbdd.user", ""));
    env.put("PGPASSWORD", props().getProperty("bbdd.password", ""));
  }

  /** Best-effort {@code user/password@//host:port/service} connect string for {@code sqlplus}. */
  protected static String oracleConnectString() {
    String user = props().getProperty("bbdd.user", "");
    String pass = props().getProperty("bbdd.password", "");
    Matcher m = ORA_URL.matcher(props().getProperty("bbdd.url", ""));
    if (m.find()) {
      String host = m.group(1);
      String port = m.group(2);
      String svc = props().getProperty("bbdd.sid", m.group(3));
      return user + "/" + pass + "@//" + host + ":" + port + "/" + svc;
    }
    // Fall back to a plain TNS alias (bbdd.sid) when the URL is not the thin //host:port form.
    return user + "/" + pass + "@" + props().getProperty("bbdd.sid", "");
  }

  /**
   * Runs a command, merging stderr into stdout, and returns the captured result. The command runs
   * in {@link #sqlDir()} so relative script references resolve, with {@code extraEnv} overlaid on
   * the inherited environment.
   */
  protected static ScriptResult run(List<String> command, Map<String, String> extraEnv)
      throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(sqlDir());
    pb.redirectErrorStream(true);
    if (extraEnv != null) {
      pb.environment().putAll(extraEnv);
    }
    log.info("Running stored-computed script: {}", String.join(" ", command));
    Process process = pb.start();
    byte[] out = process.getInputStream().readAllBytes();
    String output = new String(out, StandardCharsets.UTF_8);
    boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    if (!finished) {
      process.destroyForcibly();
      throw new AssertionError("Script timed out after " + PROCESS_TIMEOUT_MINUTES + " minutes:\n"
          + tail(output));
    }
    return new ScriptResult(process.exitValue(), output);
  }

  /** Asserts a clean exit (0) and the presence of the script's terminal success sentinel. */
  protected static void assertScriptPassed(ScriptResult result, String sentinel) {
    assertEquals("Script exited non-zero. Output:\n" + tail(result.output), 0, result.exitCode);
    assertTrue("Success sentinel '" + sentinel + "' not found. Output:\n" + tail(result.output),
        result.output.contains(sentinel));
  }

  /** Last ~4000 chars of output, so a failure message stays readable but complete enough to debug. */
  private static String tail(String s) {
    if (s == null) {
      return "";
    }
    int max = 4000;
    return s.length() <= max ? s : "…(truncated)…\n" + s.substring(s.length() - max);
  }

  // --- convenience runners per dialect ---

  /** Runs a psql script with the PG* environment from Openbravo.properties. */
  protected static ScriptResult runPsql(String scriptName, String... extraArgs)
      throws IOException, InterruptedException {
    String psql = System.getProperty("scd.psql.bin", "psql");
    List<String> cmd = new ArrayList<>();
    cmd.add(psql);
    cmd.add("-v");
    cmd.add("ON_ERROR_STOP=1");
    cmd.add("-X");
    cmd.add("-q");
    for (String a : extraArgs) {
      cmd.add(a);
    }
    cmd.add("-f");
    cmd.add(script(scriptName).getAbsolutePath());
    Map<String, String> env = new java.util.HashMap<>();
    applyPostgresEnv(env);
    return run(cmd, env);
  }
}
