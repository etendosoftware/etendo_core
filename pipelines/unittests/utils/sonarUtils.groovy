// pipelines/sonarUtils.groovy

/**
 * Checks if this is the first analysis for a given branch
 * @param branch Branch name to check
 * @param sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @return true if this is the first analysis, false otherwise
 */
def isFirstAnalysisForBranch(branch, sonarProjectKey, sonarToken, sonarServer) {
  try {
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}&ps=1\"",
      returnStdout: true
    ).trim()
    
    echo "Checking if first analysis for branch '${branch}': ${analysisResp}"
    
    def analysisJson = readJSON text: analysisResp
    def analyses = analysisJson?.analyses ?: []
    
    if (analyses.size() == 0) {
      echo "No previous analyses found for branch '${branch}' - this is the first analysis"
      return true
    } else {
      echo "Found ${analyses.size()} previous analysis(es) for branch '${branch}' - not the first analysis"
      return false
    }
  } catch (Exception e) {
    echo "Error checking first analysis status for branch '${branch}': ${e.getMessage()}"
    // En caso de error, asumimos que no es el primer análisis para ser conservadores
    return false
  }
}

/**
 * Gets the total number of analyses for a branch
 * @param branch Branch name to check
 * @param sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @return Number of analyses for the branch
 */
def getAnalysisCountForBranch(branch, sonarProjectKey, sonarToken, sonarServer) {
  try {
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}&ps=500\"",
      returnStdout: true
    ).trim()
    
    def analysisJson = readJSON text: analysisResp
    def totalCount = analysisJson?.paging?.total ?: 0
    
    echo "Branch '${branch}' has ${totalCount} total analyses"
    return totalCount
  } catch (Exception e) {
    echo "Error getting analysis count for branch '${branch}': ${e.getMessage()}"
    return 0
  }
}

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
  
  def isFirstAnalysis = isFirstAnalysisForBranch(branch, sonarProjectKey, sonarToken, sonarServer)
  
  if (isFirstAnalysis) {
    echo "🆕 This is the FIRST analysis for branch '${branch}' - waiting 10 minutes for SonarQube to process the analysis..."
    sleep(time: 10, unit: 'MINUTES')
    echo "⏰ 10-minute wait completed. Proceeding with coverage retrieval..."
  }
  
  for (int attempt = 0; attempt < maxRetries; attempt++) {
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}\"",
      returnStdout: true
    ).trim()
    def analysisJson = readJSON text: analysisResp
    def lastAnalysis = analysisJson?.analyses ? analysisJson.analyses[0] : null
    def lastRevision = lastAnalysis?.revision ?: null
    echo "Last analysis revision for branch '${branch}': ${lastRevision}"
    echo "Current git commit: ${gitCommit}"
    
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
        echo "✅ Coverage found for branch '${branch}': ${coverage}%"
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
        if (isFirstAnalysis) {
          echo "⚠️  First analysis for branch '${branch}' with no coverage data - this is expected for new branches"
        } else {
          echo "Final attempt: No coverage info for branch '${branch}', returning 0."
        }
        return 0.0
      }
      // If coverage is not 0 and measures exist, throw error as before
      error("Could not retrieve coverage for branch '${branch}' after ${maxRetries} attempts.")
    } else {
      if (isFirstAnalysis) {
        error("⚠️  Could not retrieve coverage for FIRST analysis of branch '${branch}' and commit '${gitCommit}' - this might be expected")
      }
      error("Could not retrieve coverage for branch '${branch}' and commit '${gitCommit}' after ${maxRetries} attempts.")
    }
  }
  
  return coverage
}

return this
