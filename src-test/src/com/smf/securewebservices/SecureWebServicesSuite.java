package com.smf.securewebservices;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.smf.securewebservices.cors.AllowCrossDomainsTest;
import com.smf.securewebservices.provider.ContextInfoServletTest;
import com.smf.securewebservices.provider.SWSComponentProviderTest;
import com.smf.securewebservices.rsql.OBRestUtilsMapParametersTest;
import com.smf.securewebservices.rsql.OBRestUtilsTest;
import com.smf.securewebservices.service.BaseActionTest;
import com.smf.securewebservices.service.BaseWebServiceTest;
import com.smf.securewebservices.service.DataSourceServletTest;
import com.smf.securewebservices.service.JsonDalWebServiceParameterTest;
import com.smf.securewebservices.service.JsonDalWebServiceTest;
import com.smf.securewebservices.service.KernelServletTest;
import com.smf.securewebservices.service.SecureLoginServletTest;
import com.smf.securewebservices.service.SecureWebServiceServletTest;
import com.smf.securewebservices.utils.JSONStreamWriterTest;
import com.smf.securewebservices.utils.ResultTest;
import com.smf.securewebservices.utils.SecureWebServicesUtilsAdditionalTests;

/**
 * Test suite for Secure Web Services.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    SWSDataSourceServiceTest.class,
    InitializeSingletonTest.class,
    OBRestUtilsMapParametersTest.class,
    OBRestUtilsTest.class,
    ResultTest.class,
    JSONStreamWriterTest.class,
    AllowCrossDomainsTest.class,
    BaseActionTest.class,
    BaseWebServiceTest.class,
    ContextInfoServletTest.class,
    SWSComponentProviderTest.class,
    SecureWebServiceServletTest.class,
    KernelServletTest.class,
    SecureWebServicesUtilsAdditionalTests.class,
    DataSourceServletTest.class,
    SecureLoginServletTest.class,
    JsonDalWebServiceParameterTest.class,
    JsonDalWebServiceTest.class

})

public class SecureWebServicesSuite {

}
