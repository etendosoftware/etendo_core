package com.smf.test;

import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite for SMF packages.
 */
@Suite
@SelectPackages("com.smf")
@ExcludeClassNamePatterns({
    // Coverage unit tests (mockito-based) excluded to avoid interference with integration tests
    "com.smf.jobs.ActionResultTest",
    "com.smf.jobs.ActionTest",
    "com.smf.jobs.FilterTest",
    "com.smf.jobs.background.BackgroundRunnerTest",
})
public class CoreTestSuite {
}
