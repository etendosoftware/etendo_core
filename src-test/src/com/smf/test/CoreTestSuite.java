package com.smf.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

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

/**
 * Test suite to run all unit tests for the SMF Jobs defaults module.
 * <p>
 * This suite includes tests for core functionalities such as order processing,
 * invoice creation, shipment processing, and offer handling.
 * </p>
 *
 * <p>JUnit {@link Suite} is used to group related test classes and execute them together.</p>
 *
 * <h2>Test Classes Included</h2>
 * <ul>
 *   <li>{@link CreateFromOrdersHQLTransformerTest}</li>
 *   <li>{@link CreateFromOrderTest}</li>
 *   <li>{@link OfferAddOrgTest}</li>
 *   <li>{@link OfferAddProductCategoryTest}</li>
 *   <li>{@link OfferAddProductTest}</li>
 *   <li>{@link JobsComponentProviderTest}</li>
 *   <li>{@link CloneOrderHookTest}</li>
 *   <li>{@link ProcessInvoicesTest}</li>
 *   <li>{@link ProcessOrdersDefaultsTest}</li>
 *   <li>{@link ProcessOrdersTest}</li>
 *   <li>{@link ProcessShipmentTest}</li>
 * </ul>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({

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
    ProcessShipmentTest.class

})

public class CoreTestSuite {
  /**
   * The CoreTestSuite class is used as an entry point to run all
   * the unit tests in the SMF Jobs defaults module.
   * <p>
   * No additional logic is required in this class, as it serves as a container
   * for the list of test classes defined in the {@code @SuiteClasses} annotation.
   * </p>
   */
}
