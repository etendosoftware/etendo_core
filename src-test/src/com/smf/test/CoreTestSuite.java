package com.smf.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.smf.jobs.TestActionHookCall;
import com.smf.jobs.defaults.CloneOrderHookTest;
import com.smf.jobs.defaults.ProcessInvoicesTest;
import com.smf.jobs.defaults.ProcessOrdersDefaultsTest;
import com.smf.jobs.defaults.ProcessOrdersTest;
import com.smf.jobs.defaults.ProcessShipmentTest;
import com.smf.jobs.defaults.invoices.CreateFromOrderTest;
import com.smf.jobs.defaults.invoices.CreateFromOrdersHQLTransformerTest;
import com.smf.jobs.defaults.offerPick.OfferAddOrgTest;
import com.smf.jobs.defaults.offerPick.OfferAddProductCategoryTest;
import com.smf.jobs.defaults.offerPick.OfferAddProductTest;
import com.smf.jobs.defaults.provider.JobsComponentProviderTest;
import com.smf.mobile.utils.webservices.FormValuesServiceTest;
import com.smf.mobile.utils.webservices.VersionTest;
import com.smf.mobile.utils.webservices.WindowTest;
import com.smf.mobile.utils.webservices.WindowUtilsTest;
import com.smf.securewebservices.SecureWebServicesSuite;
/**
 * Test suite for SMF packages.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    // SMF Jobs
    TestActionHookCall.class,

    // SMF Jobs defaults
    CreateFromOrdersHQLTransformerTest.class,
    CreateFromOrderTest.class,
    OfferAddOrgTest.class,
    OfferAddProductCategoryTest.class,
    OfferAddProductTest.class,
    JobsComponentProviderTest.class,
    CloneOrderHookTest.class,
    ProcessInvoicesTest.class,
    ProcessOrdersDefaultsTest.class,
    ProcessOrdersTest.class,
    ProcessShipmentTest.class,
    FormValuesServiceTest.class,
    VersionTest.class,
    WindowTest.class,
    WindowUtilsTest.class,

    // Secure webservices
    SecureWebServicesSuite.class

})

public class CoreTestSuite {

}
