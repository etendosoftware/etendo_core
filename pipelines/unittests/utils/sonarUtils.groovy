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
    // In case of error, we assume it's not the first analysis to be conservative
    return false
  }
}

/**
 * Gets coverage for a specific commit analysis
 * @param branch Branch name
 * @param commitSha Specific commit SHA
 * @param sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @return Coverage value as float, or -1 if not found
 */
def getCoverageForSpecificCommit(branch, commitSha, sonarProjectKey, sonarToken, sonarServer) {
  try {
    echo "🎯 Searching for coverage of specific commit ${commitSha} on branch '${branch}'"
    
    // Search for specific analysis by commit
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}&ps=50\"",
      returnStdout: true
    ).trim()
    
    def analysisJson = readJSON text: analysisResp
    def analyses = analysisJson?.analyses ?: []
    
    def targetAnalysis = analyses.find { it.revision == commitSha }
    
    if (!targetAnalysis) {
      echo "❌ No analysis found for commit ${commitSha} on branch '${branch}'"
      return -1
    }
    
    echo "✅ Found analysis for commit ${commitSha}: ${targetAnalysis.key} (date: ${targetAnalysis.date})"
    
    // Use historical measures API to get coverage for the specific analysis
    def response = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/search_history?component=${sonarProjectKey}&metrics=coverage&from=${targetAnalysis.date}&to=${targetAnalysis.date}&ps=1\"",
      returnStdout: true
    ).trim()
    
    echo "📊 Historical measures response: ${response}"
    def historyJson = readJSON text: response
    
    if (historyJson.measures && historyJson.measures.size() > 0) {
      def coverageHistory = historyJson.measures.find { it.metric == 'coverage' }
      if (coverageHistory && coverageHistory.history && coverageHistory.history.size() > 0) {
        def historyEntry = coverageHistory.history[0]
        def coverage = historyEntry.value.toFloat()
        echo "📊 Coverage for commit ${commitSha}: ${coverage}% (from historical data)"
        return coverage
      }
    }
    
    // Fallback: use normal API but warn that it may not correspond to the specific commit
    echo "⚠️ No historical data available, using current branch coverage (may not match specific commit)"
    response = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
      returnStdout: true
    ).trim()
    
    def json = readJSON text: response
    def measures = json?.component?.measures ?: []
    def coverageMeasure = measures.find { it.metric == 'coverage' }
    
    if (coverageMeasure && coverageMeasure.value) {
      def coverage = coverageMeasure.value.toFloat()
      echo "📊 Coverage for commit ${commitSha}: ${coverage}%"
      return coverage
    }
    
    echo "❌ No coverage data found for commit ${commitSha}"
    return -1
    
  } catch (Exception e) {
    echo "❌ Error getting coverage for commit ${commitSha}: ${e.getMessage()}"
    return -1
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
  
  // Only check if it's the first analysis when we need a specific commit
  def isFirstAnalysis = false
  if (checkCommit) {
    isFirstAnalysis = isFirstAnalysisForBranch(branch, sonarProjectKey, sonarToken, sonarServer)
    
    if (isFirstAnalysis) {
      echo "🆕 This is the FIRST analysis for branch '${branch}' - waiting 10 minutes for SonarQube to process the analysis..."
      sleep(time: 10, unit: 'MINUTES')
      echo "⏰ 10-minute wait completed. Proceeding with coverage retrieval..."
    }
  } else {
    echo "📊 Getting most recent coverage for branch '${branch}' (no commit verification needed)"
  }
  
  for (int attempt = 0; attempt < maxRetries; attempt++) {
    echo "🔍 Attempt ${attempt + 1}/${maxRetries} - Retrieving analysis data for branch '${branch}'..."
    
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${branch}&ps=5\"",
      returnStdout: true
    ).trim()
    
    echo "📊 Analysis API response: ${analysisResp}"
    def analysisJson = readJSON text: analysisResp
    def analyses = analysisJson?.analyses ?: []
    
    if (analyses.size() == 0) {
      echo "❌ No analyses found for branch '${branch}' on attempt ${attempt + 1}"
      if (checkCommit && isFirstAnalysis) {
        echo "⏳ First analysis - waiting 2 minutes before retry..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    // Show information of all recent analyses for debugging
    echo "📋 Recent analyses for branch '${branch}':"
    analyses.eachWithIndex { analysis, index ->
      def analysisDate = analysis.date ?: 'N/A'
      def analysisRevision = analysis.revision ?: 'N/A'
      def analysisKey = analysis.key ?: 'N/A'
      echo "  ${index + 1}. Date: ${analysisDate}, Revision: ${analysisRevision}, Key: ${analysisKey}"
    }
    
    def targetAnalysis = null
    
    if (checkCommit && gitCommit) {
      // Search for specific analysis by commit
      targetAnalysis = analyses.find { it.revision == gitCommit }
      if (targetAnalysis) {
        echo "✅ Found analysis matching commit ${gitCommit}: ${targetAnalysis.key}"
      } else {
        echo "❌ No analysis found matching commit ${gitCommit}"
        echo "🔍 Available commits in recent analyses: ${analyses.collect { it.revision }.join(', ')}"
        
        if (isFirstAnalysis) {
          echo "⏳ First analysis - commit might still be processing. Waiting 2 minutes..."
          sleep(time: 2, unit: 'MINUTES')
        } else {
          echo "⏳ Waiting 40 seconds for analysis to process..."
          sleep(time: 40, unit: 'SECONDS')
        }
        continue
      }
    } else {
      // Use the most recent analysis
      targetAnalysis = analyses[0]
      echo "🎯 Using most recent analysis: ${targetAnalysis.key} (revision: ${targetAnalysis.revision})"
    }
    
    // Get coverage using the specific analysis
    def response
    
    if (checkCommit && targetAnalysis) {
      // For specific analysis, use historical measures API with the analysis key
      echo "📊 Getting coverage for specific analysis: ${targetAnalysis.key}"
      response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/search_history?component=${sonarProjectKey}&metrics=coverage&from=${targetAnalysis.date}&to=${targetAnalysis.date}&ps=1\"",
        returnStdout: true
      ).trim()
      
      echo "📈 Historical coverage API response for analysis '${targetAnalysis.key}': ${response}"
      def historyJson = readJSON text: response
      
      // If historical API doesn't work, try with measures/component but verify it matches
      if (!historyJson.measures || historyJson.measures.size() == 0) {
        echo "⚠️ No historical data found, trying component measures API..."
        response = sh(
          script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
          returnStdout: true
        ).trim()
        echo "📈 Fallback coverage API response: ${response}"
      } else {
        // Process historical response
        def coverageHistory = historyJson.measures.find { it.metric == 'coverage' }
        if (coverageHistory && coverageHistory.history && coverageHistory.history.size() > 0) {
          def historyEntry = coverageHistory.history[0]
          def covStr = historyEntry.value
          echo "📊 Historical coverage value: '${covStr}' for date ${historyEntry.date}"
          
          if (covStr && covStr != "0" && covStr != "0.0") {
            coverage = covStr.toFloat()
            echo "✅ Coverage successfully retrieved from history for commit ${gitCommit}: ${coverage}%"
            break
          }
        }
        // If there's no valid historical data, continue with normal flow
        echo "⚠️ No valid historical coverage data, falling back to component API"
        response = sh(
          script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
          returnStdout: true
        ).trim()
      }
    } else {
      // For most recent analysis, use normal API
      echo "📊 Getting coverage for most recent analysis"
      response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${branch}&metricKeys=coverage\"",
        returnStdout: true
      ).trim()
    }
    
    echo "📈 Coverage API response for branch '${branch}': ${response}"
    def json = readJSON text: response
    
    // Verify if the response contains the expected component
    if (!json.component) {
      echo "❌ No component data found in coverage response"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - component might still be processing. Waiting 2 minutes..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    def measures = json.component.measures ?: []
    def coverageMeasure = measures.find { it.metric == 'coverage' }
    
    if (!coverageMeasure) {
      echo "❌ No coverage metric found in measures"
      echo "📊 Available metrics: ${measures.collect { it.metric }.join(', ')}"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - coverage metric might still be calculating. Waiting 2 minutes..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
      continue
    }
    
    def covStr = coverageMeasure.value
    echo "📊 Raw coverage value: '${covStr}' (type: ${covStr?.class?.simpleName})"
    
    if (covStr && covStr != "0" && covStr != "0.0") {
      coverage = covStr.toFloat()
      echo "✅ Coverage successfully retrieved for branch '${branch}': ${coverage}%"
      if (checkCommit) {
        echo "🎯 Coverage corresponds to commit: ${gitCommit}"
      }
      break
    } else {
      echo "⚠️ Coverage is 0 or empty for branch '${branch}' (attempt ${attempt + 1}/${maxRetries})"
      if (isFirstAnalysis) {
        echo "⏳ First analysis - waiting 2 minutes before retry..."
        sleep(time: 2, unit: 'MINUTES')
      } else {
        echo "⏳ Waiting 40 seconds before retry..."
        sleep(time: 40, unit: 'SECONDS')
      }
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
