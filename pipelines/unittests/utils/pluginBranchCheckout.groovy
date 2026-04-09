// pipelines/unittests/utils/pluginBranchCheckout.groovy

/**
 * Checks out the appropriate branch on the Etendo plugin clone,
 * based on whether the current branch is a backport/release branch or a regular one.
 *
 * @param isFeatureBranch  true if the current branch starts with "feature/"
 * @param epicKey          epic key resolved from Jira (may be empty)
 */
def run(boolean isFeatureBranch, String epicKey) {
    def branch = env.GIT_BRANCH
    def isBackportOrRelease = branch.contains("-Y") || branch.startsWith("release")
    def isRegular = !branch.contains("-Y") && !branch.startsWith("release") &&
                    !branch.startsWith("main") && !branch.startsWith("master") &&
                    !branch.startsWith("hotfix")

    if (isRegular) {
        sh isFeatureBranch && epicKey
            ? "cd ${env.PLUGIN_NAME} && git checkout ${env.EPIC_BRANCH} || git checkout ${env.DEVELOP_BRANCH}"
            : "cd ${env.PLUGIN_NAME} && git checkout ${env.DEVELOP_BRANCH}"
    } else if (isBackportOrRelease) {
        sh isFeatureBranch && epicKey
            ? "cd ${env.PLUGIN_NAME} && git checkout ${env.EPIC_BRANCH} || git checkout ${env.BACKPORT_BRANCH}"
            : "cd ${env.PLUGIN_NAME} && git checkout ${env.BACKPORT_BRANCH}"
    }
}

return this
