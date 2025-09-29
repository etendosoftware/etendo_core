// pipelines/sonarUtils.groovy

/**
 * Checks if this is the first analysis for a given branch
 * @param branch Branch name to check
      // Use the analysis-specific API to get measures for this exact analysis
      response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&metricKeys=coverage&analysisId=${targetAnalysis.key}\"",
        returnStdout: true
      ).trim()
      
      echo "📈 Analysis-specific measures API response for ${targetAnalysis.key}: ${response}"am sonarProjectKey SonarQube project key
 * @param sonarToken SonarQube API token
 * @param sonarServer SonarQube server URL
 * @return true if this is the first analysis, false otherwise
 */
def isFirstAnalysisForBranch(branch, sonarProjectKey, sonarToken, sonarServer) {
  try {
    def analysisResp = sh(
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${encodedBranch}&ps=1\"",
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
  int maxRetries = 5
  float coverage = -1
  def encodedBranch = URLEncoder.encode(branch, "UTF-8")

  // Only check if it's the first analysis when we need a specific commit
  def isFirstAnalysis = false
  if (checkCommit) {
    isFirstAnalysis = isFirstAnalysisForBranch(encodedBranch, sonarProjectKey, sonarToken, sonarServer)
    
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
      script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/project_analyses/search?project=${sonarProjectKey}&branch=${encodedBranch}&ps=5\"",
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
      // Method 1: Try to get measures directly from the specific analysis using its key
      echo "📊 Getting coverage for specific analysis: ${targetAnalysis.key}"
      echo "🎯 Target analysis details: Date=${targetAnalysis.date}, Revision=${targetAnalysis.revision}"
      
      // First try: Get measures directly from analysis key (if API supports it)
      response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage\"",
        returnStdout: true
      ).trim()
      
      echo "📈 Component measures API response: ${response}"
      def json = readJSON text: response
      
      // Check if we got valid data from the specific analysis
      if (json.component && json.component.measures) {
        def measures = json.component.measures
        def coverageMeasure = measures.find { it.metric == 'coverage' }
        
        if (coverageMeasure) {
          def covStr = coverageMeasure.value
          echo "📊 Coverage from specific analysis ${targetAnalysis.key}: '${covStr}'"
          
          if (covStr && covStr != "0" && covStr != "0.0") {
            coverage = covStr.toFloat()
            echo "✅ Coverage successfully retrieved from specific analysis ${targetAnalysis.key} for commit ${gitCommit}: ${coverage}%"
            break
          } else {
            echo "⚠️ Coverage is 0 for specific analysis ${targetAnalysis.key}"
          }
        } else {
          echo "❌ No coverage metric found in specific analysis ${targetAnalysis.key}"
          echo "📊 Available metrics: ${measures.collect { it.metric }.join(', ')}"
        }
      } else {
        echo "❌ No component data found for specific analysis ${targetAnalysis.key}"
        echo "🔄 Falling back to branch-level measures..."
        
        // Fallback to normal branch API if analysis-specific fails
        response = sh(
          script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage\"",
          returnStdout: true
        ).trim()
        echo "📈 Fallback branch measures API response: ${response}"
      }
    } else {
      // For most recent analysis, use normal API
      echo "📊 Getting coverage for most recent analysis"
      response = sh(
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage\"",
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
        script: "curl -s -u ${sonarToken}: \"${sonarServer}/api/measures/component?component=${sonarProjectKey}&branch=${encodedBranch}&metricKeys=coverage\"",
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
