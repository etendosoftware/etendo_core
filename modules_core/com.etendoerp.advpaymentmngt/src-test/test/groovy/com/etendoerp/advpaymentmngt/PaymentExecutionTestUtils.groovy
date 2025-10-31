package com.etendoerp.advpaymentmngt

import org.openbravo.dal.service.OBCriteria
import org.openbravo.dal.service.OBDal
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod

class PaymentExecutionTestUtils {

    static def getClass(String className) {
        def clz = Class.forName(className)
        def clzInstance = clz.getDeclaredConstructor().newInstance()
        return [clz, clzInstance]
    }

    static FinAccPaymentMethod getFinancialAccountPaymentMethod(FIN_FinancialAccount account, FIN_PaymentMethod paymentMethod) {
        final OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
                .createCriteria(FinAccPaymentMethod.class);
        obc.addEqual(FinAccPaymentMethod.PROPERTY_ACCOUNT, account);
        obc.addEqual(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod);
        obc.addEqual(FinAccPaymentMethod.PROPERTY_ACTIVE, true);
        obc.setFilterOnReadableClients(false);
        obc.setFilterOnReadableOrganization(false);
        try {
            return obc.list().get(0);
        } catch (Exception e) {
            return null
        }
    }

}
