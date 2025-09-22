// pipelines/sonarUtils.groovy

/**
 * Utility function to retrieve SonarQube coverage for a branch, with retry logic and commit verification.
 * @param branch Branch name to query coverage for
 * @param checkCommit Whether to verify the latest analysis matches the current commit
 * @param sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @param gitCommit Current commit SHA
 * @return Coverage value as float
 * @throws error if coverage cannot be retrieved after max retries
 */
def getCoverageWithRetry(branch, checkCommit, sonarProjectKey, sonarToken, sonarServer, gitCommit) {
  int maxRetries = 4
  float coverage = -1
  for (int attempt = 0; attempt < maxRetries; attempt++) {
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}\"",
      returnStdout: true
    ).trim()
    def analysisJson = readJSON text: analysisResp
    def lastAnalysis = analysisJson?.analyses ? analysisJson.analyses[0] : null
    def lastRevision = lastAnalysis?.revision ?: null
    if (!checkCommit || (lastRevision && lastRevision == gitCommit)) {
      def response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
        returnStdout: true
      ).trim()
      echo "SonarQube coverage API response for branch '${branch}': ${response}"
      def json = readJSON text: response
      def measures = json?.component?.measures ?: []
      def covStr = measures.find { it.metric == 'coverage' }?.value
      if (covStr && covStr != "0" && covStr != "0.0") {
        coverage = covStr.toFloat()
        break
      } else {
        echo "Coverage is 0 or not available for branch '${branch}' (attempt ${attempt + 1}/${maxRetries}), waiting 40 seconds before retrying..."
        sleep(time: 40, unit: 'SECONDS')
      }
    } else if (checkCommit) {
      echo "Latest analysis for branch '${branch}' does not match current commit (expected: ${gitCommit}, got: ${lastRevision}). Waiting 40 seconds before retrying..."
      sleep(time: 40, unit: 'SECONDS')
    }
  }
  if (coverage == -1) {
    if (!checkCommit) {
      // If it is the main branch and after the attempts the coverage is 0, return 0
      def response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
        returnStdout: true
      ).trim()
      def json = readJSON text: response
      def measures = json?.component?.measures ?: []
      def covStr = measures.find { it.metric == 'coverage' }?.value
      if (covStr == "0" || covStr == "0.0" || measures.size() == 0) {
        echo "Final attempt: No coverage info for branch '${branch}', returning 0."
        return 0.0
      }
      // If coverage is not 0 and measures exist, throw error as before
      error("Could not retrieve coverage for branch '${branch}' after ${maxRetries} attempts.")
    } else {
      error("Could not retrieve coverage for branch '${branch}' and commit '${gitCommit}' after ${maxRetries} attempts.")
    }
  }
  return coverage
}

return this
