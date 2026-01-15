// pipelines/tomcatUtils.groovy

/**
 * Checks the availability of a Tomcat server by repeatedly issuing HTTP requests
 * to the specified URL until a successful HTTP 200 response is received or the
 * configured timeout is reached.
 *
 * If any parameter is not provided or is `null`, a default value is applied:
 * \- `url`: `http://localhost:8080/etendo/security/Login_FS.html`
 * \- `timeout`: 60 seconds
 * \- `interval`: 5 seconds
 *
 * The method relies on `curl` executed via the Jenkins `sh` step to obtain only
 * the HTTP status code. It polls the target URL at the configured interval and
 * fails the build using the `error` step if Tomcat does not respond with HTTP 200
 * within the timeout window.
 *
 * @param url      Target URL of the Tomcat application to probe. Falls back to the
 *                 local login page if `null` or empty.
 * @param timeout  Maximum time in seconds to wait for Tomcat to become ready. A
 *                 value of `null` is normalized to 60.
 * @param interval Delay in seconds between consecutive health checks. A value of
 *                 `null` is normalized to 5.
 * @return `true` if Tomcat responds with HTTP 200 within the timeout period;
 *         `false` otherwise or if an exception is thrown during the checks.
 */
def checkTomcatStatus(String url, Integer timeout = 60, Integer interval = 5) {
  url      = url      ?: "http://localhost:8080/etendo/security/Login_FS.html"
  timeout  = timeout  ?: 60
  interval = interval ?: 5
  try {
      echo "-------------------- Checking Tomcat Status --------------------"
      def elapsed = 0
      def tomcatReady = false
      def tomcatResponse = "";
      while (elapsed < timeout) {
          try {
              tomcatResponse = sh(
                      script: """curl -sS -o /dev/null -w "%{http_code}" \\
                              "${url}" || echo "000" """,
                      returnStdout: true).trim()
              echo "Tomcat response code: ${tomcatResponse}"
          } catch (e) {
              tomcatResponse = "000"
          }

          if (tomcatResponse == "200") {
              tomcatReady = true
              break
          }

          echo "Tomcat not ready yet. Response code: ${tomcatResponse}. Retrying in ${interval} seconds..."
          sleep interval
          elapsed += interval
      }
      if (!tomcatReady) {
          error("Tomcat did not start within the timeout period of ${timeout} seconds.")
          return false
      }
      echo "Tomcat response code: ${tomcatResponse}"
      if (tomcatResponse != "200") {
          error("Tomcat did not respond with 200. Response code: ${tomcatResponse}")
          return false
      }
      return true
  } catch (Exception e) {
    echo "Error checking tomcat status for '${url}': ${e.getMessage()}"
    return false
  }
}

return this
