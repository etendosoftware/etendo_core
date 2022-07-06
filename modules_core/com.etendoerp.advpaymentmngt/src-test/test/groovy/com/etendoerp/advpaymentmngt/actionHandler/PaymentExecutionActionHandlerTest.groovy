package com.etendoerp.advpaymentmngt.actionHandler

import com.etendoerp.advpaymentmngt.PaymentExecutionTestUtils
import com.etendoerp.base.EBaseSpecification
import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONObject
import org.codehaus.jettison.json.JSONTokener
import org.openbravo.dal.core.OBContext
import org.openbravo.dal.service.OBDal
import org.openbravo.erpCommon.utility.OBMessageUtils
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount
import org.openbravo.model.financialmgmt.payment.FIN_Payment
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess
import org.openbravo.test.base.Issue
import org.openbravo.test.base.TestConstants
import spock.lang.Title

import java.lang.reflect.Method

@Issue("EPL-199")
@Title("Test cases for the payment execution action handler")
class PaymentExecutionActionHandlerTest extends EBaseSpecification {
    static final String ROOT_PACKAGE_NAME = "com.etendoerp.advpaymentmngt.actionHandler."
    static final String PAYMENT_EXECUTION_ACTION_HANDLER = ROOT_PACKAGE_NAME + "PaymentExecutionActionHandler"
    static final String PAYMENT_EXECUTION_PROCESS_ACTION_HANDLER = ROOT_PACKAGE_NAME + "PaymentExecutionProcessActionHandler"

    def setup() {
        OBContext.setOBContext(
                TestConstants.Users.ADMIN,
                TestConstants.Roles.ESP_ADMIN,
                TestConstants.Clients.FB_GRP,
                TestConstants.Orgs.ESP_NORTE
        )
    }

    def "Refresh grid test"() {
        given: "A user sending a request with the search button value"
        JSONObject jsonContent = new JSONObject()
        jsonContent.put("_buttonValue", "SearchPayment")
        jsonContent.put("_params", new JSONObject())

        when: "The user sends the request to the process handler"
        def (Class clz, Object objInstance) = PaymentExecutionTestUtils.getClass(PAYMENT_EXECUTION_ACTION_HANDLER)
        Method method = clz.getDeclaredMethod("doExecute", Map.class, String.class)
        method.setAccessible(true)
        JSONObject response = (JSONObject) method.invoke(objInstance, new HashMap(), jsonContent.toString())

        then: "The response should contain the retry execution"
        assert response.get("retryExecution") != null

        and: "The response actions should contain the refresh grid values"
        JSONArray responseActions = response.getJSONArray("responseActions")
        JSONObject refreshGridObject = null
        JSONObject refreshGridParameter = null

        for (int i = 0; i < responseActions.length(); i++) {
            def jsonObject = responseActions.opt(i) as JSONObject
            if ((jsonObject.keys().next() as String).equalsIgnoreCase("refreshGrid")) {
                refreshGridObject = jsonObject
            } else {
                refreshGridParameter = jsonObject
            }
        }

        assert refreshGridObject != null
        assert refreshGridObject.getJSONObject("refreshGrid") != null

        assert refreshGridParameter != null
        def refreshGridParameterValue = refreshGridParameter.getJSONObject("refreshGridParameter")
        assert refreshGridParameterValue != null
        assert refreshGridParameterValue.getString("gridName") == PaymentExecutionActionHandler.GRID_PARAMETER
    }

    def "Lines not selected test"() {
        given: "A user sending the a request with the process button value"
        JSONObject jsonContent = new JSONObject()
        jsonContent.put("_buttonValue", PaymentExecutionActionHandler.PROCESS_BUTTON)

        when: "The user sends the request to the process handler without selected lines"
        String jsonPaymentPicks = """
            {
                "payment_pick":{
                    "_selection":[
                    ]
                }
            }
        """
        jsonContent.put("_params", new JSONObject(new JSONTokener(jsonPaymentPicks)))

        def (Class clz, Object objInstance) = PaymentExecutionTestUtils.getClass(PAYMENT_EXECUTION_ACTION_HANDLER)
        Method method = clz.getDeclaredMethod("doExecute", Map.class, String.class)
        method.setAccessible(true)
        JSONObject response = (JSONObject) method.invoke(objInstance, new HashMap(), jsonContent.toString())

        then: "The response should contain a error for the missing lines"
        JSONArray responseActions = response.getJSONArray("responseActions")
        JSONObject showMsgInProcessView = (responseActions.opt(0) as JSONObject)?.getJSONObject("showMsgInProcessView")
        assert showMsgInProcessView != null
        String msgText = showMsgInProcessView.getString("msgText")
        assert msgText == OBMessageUtils.messageBD(PaymentExecutionActionHandler.LINES_NOT_SELECTED_MESSAGE)
    }

    def "Execution process not found test"() {
        given: "A user sending a request with a selected payment"

        String organizationId     = "E443A31992CB4635AFCAEABE7183CE85" // España region norte
        String paymentMethodId    = "A97CFD2AFC234B59BB0A72189BD8FC2A" // Transferencia
        String financialAccountId = "DEDDE613C5314ACD8DCC60C474D1A107" // Cuenta de banco

        def jsonContent = """
            {
            "_buttonValue":"ProcessPayment", 
                "_params":{
                    "ad_org_id":"${organizationId}", 
                    "payment_method":"${paymentMethodId}", 
                    "financial_account":"${financialAccountId}",
                    "payment_pick":{
                        "_selection":[
                            {
                                "_identifier":"1000555 - 31-08-2017 - La Fruta es la Vida, S.L. - 209662.75", 
                                "_entityName":"FIN_Payment", 
                                "id":"00278BDB6B26408D8277806F3516D43B", 
                                "client":"23C59575B9CF467C9620760EB255B389", 
                                "client_identifier":"F&B International Group", 
                                "organization":"B843C30461EA4501935CB1D125C9C25A", 
                                "organization_identifier":"F&B España, S.A"
                            }, 
                            {
                                "_identifier":"1000181 - 31-05-2013 - Bebidas Alegres, S.L. - 251740.5", 
                                "_entityName":"FIN_Payment",
                                "id":"009AE4C3E88B4FCEB0DAE420E408E0F0", 
                                "client":"23C59575B9CF467C9620760EB255B389", 
                                "client_identifier":"F&B International Group", 
                                "organization":"B843C30461EA4501935CB1D125C9C25A", 
                                "organization_identifier":"F&B España, S.A"
                            }
                        ]
                    }
                }
            }
        """

        when: "The user send the request to the process handler"
        def (Class clz, Object objInstance) = PaymentExecutionTestUtils.getClass(PAYMENT_EXECUTION_ACTION_HANDLER)
        Method method = clz.getDeclaredMethod("doExecute", Map.class, String.class)
        method.setAccessible(true)
        JSONObject response = (JSONObject) method.invoke(objInstance, new HashMap(), jsonContent.toString())

        then: "The response should contain a error for the missing 'execution process'"
        JSONArray responseActions = response.getJSONArray("responseActions")
        JSONObject showMsgInProcessView = (responseActions.opt(0) as JSONObject)?.getJSONObject("showMsgInProcessView")
        assert showMsgInProcessView != null
        String msgText = showMsgInProcessView.getString("msgText")
        assert msgText == OBMessageUtils.messageBD(PaymentExecutionActionHandler.NO_EXECUTION_PROCESS_MESSAGE)
    }

    def "Generate popup and execute the process for a custom payment"() {
        given: "The user wanting to run the execution process for a payment with automatic execution type."
        String paymentId          = "E76AA04D0AF940D191FDE2F8237EE95C"
        String organizationId     = "E443A31992CB4635AFCAEABE7183CE85" // España region norte
        String paymentMethodId    = "A97CFD2AFC234B59BB0A72189BD8FC2A" // Transferencia
        String financialAccountId = "DEDDE613C5314ACD8DCC60C474D1A107" // Cuenta de banco

        // Obtain the 'FinAccPaymentMethod' used to set the execution process.
        FIN_PaymentMethod finPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod, paymentMethodId)
        FIN_FinancialAccount finFinancialAccount = OBDal.getInstance().get(FIN_FinancialAccount, financialAccountId)
        FinAccPaymentMethod finAccPaymentMethod = PaymentExecutionUtils.getFinancialAccountPaymentMethod(finFinancialAccount, finPaymentMethod)
        assert finFinancialAccount != null

        // Update the payment status
        FIN_Payment payment = OBDal.getInstance().get(FIN_Payment, paymentId)
        String currentPaymentStatus = payment.getStatus()
        payment.setStatus("RPAE")

        // Update the 'finAccPaymentMethod' payOutExecutionType to automatic
        String currentPayOutExecutionType = finAccPaymentMethod.getPayoutExecutionType()
        finAccPaymentMethod.setPayoutExecutionType("A")

        // Update the payment execution process to Simple process
        PaymentExecutionProcess currentProcess = finAccPaymentMethod.getPayoutExecutionProcess()
        String simpleProcessId = "301950D5D2F24F49916EDE06A473DF02" // Simple process ID
        PaymentExecutionProcess simpleProcess = OBDal.getInstance().get(PaymentExecutionProcess, simpleProcessId)
        finAccPaymentMethod.setPayoutExecutionProcess(simpleProcess)

        OBDal.getInstance().save(payment)
        OBDal.getInstance().save(finAccPaymentMethod)
        OBDal.getInstance().flush()

        def jsonContent = """
            {
            "_buttonValue":"ProcessPayment", 
                "_params":{
                    "ad_org_id":"${organizationId}", 
                    "payment_method":"${paymentMethodId}", 
                    "financial_account":"${financialAccountId}",
                    "payment_pick":{
                        "_selection":[
                            {
                                "id":"${paymentId}", 
                            }
                        ]
                    }
                }
            }
        """

        when: "The user send the request to the process handler"
        def (Class clz, Object objInstance) = PaymentExecutionTestUtils.getClass(PAYMENT_EXECUTION_ACTION_HANDLER)
        Method method = clz.getDeclaredMethod("doExecute", Map.class, String.class)
        method.setAccessible(true)
        JSONObject response = (JSONObject) method.invoke(objInstance, new HashMap(), jsonContent.toString())

        then: "The response should contain the information of the popup"
        JSONArray responseActions = response.getJSONArray("responseActions")
        JSONObject popUp = (responseActions.opt(0) as JSONObject)?.opt("EAPM_Popup") as JSONObject
        assert popUp != null
        assert popUp.opt("executionProcessId")?.toString() == simpleProcessId
        assert popUp.opt("selectedPaymentsIds")?.toString() == paymentId
        assert popUp.opt("organizationId")?.toString() == organizationId

        when: "The users sends the execution command for the process to be ran."
        def jsonProcessContent = """
            {
                "_params":{
                    "executionProcessId":"${simpleProcessId}", 
                    "executionProcessName":"Simple Execution Process", 
                    "selectedPaymentsIds":"${paymentId}", 
                    "organizationId":"${organizationId}", 
                    "processParameters":[
                    ], 
                    "threadId":"5XibqUKM", 
                    "_processView":{
                    }
                }, 
                "processParameters":[
                ]
            }
        """
        def (Class processClz, Object processObjInstance) = PaymentExecutionTestUtils.getClass(PAYMENT_EXECUTION_PROCESS_ACTION_HANDLER)
        Method processMethod = processClz.getDeclaredMethod("execute", Map.class, String.class)
        processMethod.setAccessible(true)
        JSONObject processResponse = (JSONObject) processMethod.invoke(processObjInstance, new HashMap(), jsonProcessContent.toString())

        then: "The process will execute successfully"
        assert processResponse.opt("success")

        and: "The message will show the documentNo of the processed payment"
        assert processResponse.opt("message")?.toString()?.contains("${payment.documentNo}")

        and: "The payment status will be diferent of 'RPAE'"
        assert payment.status != 'RPAE'

        cleanup:
        payment.setStatus(currentPaymentStatus)
        finAccPaymentMethod.setPayoutExecutionType(currentPayOutExecutionType)
        finAccPaymentMethod.setPayoutExecutionProcess(currentProcess)

        OBDal.getInstance().save(payment)
        OBDal.getInstance().save(finAccPaymentMethod)
        OBDal.getInstance().flush()
        OBDal.getInstance().commitAndClose()
    }

}
