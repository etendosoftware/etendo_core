package com.smf.mobile.utils.webservices;

import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.service.BaseWebService;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.order.Order;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.Map;

public class FormValuesService extends BaseWebService {
    private static final Logger log = LogManager.getLogger();

    @Override
    public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        RequestContext.get().setRequest(request);
        fillSessionVariables(request);
        WSResult result = get(path, OBRestUtils.requestParamsToMap(request));
        OBRestUtils.writeWSResponse(result, response);
    }
    @Override
    public WSResult get(String path, Map<String, String> parameters) {
        WSResult wsResult = new WSResult();

        try {
            OBContext.setAdminMode(true);
            String tabId = parameters.get("tabId");
            String parentId = parameters.get("parentId");
            String parentEntityName = parameters.get("parentEntity");
            String action = parameters.get("action");
            String rowId = parameters.get("rowId");
            Entity parentEntity = null;
            if (parentId != null) {
                parentEntity = ModelProvider.getInstance().getEntity(parentEntityName);
                if (parentEntity == null) {
                    throw new OBException(OBMessageUtils.getI18NMessage("SMFMU_EntityNotFound", new String[] {parentEntityName}));
                }
            }
            if(action != null && action.equals("eventhandler")) {
                try {
                    var obObject = OBDal.getInstance().get(Order.class, rowId);
                    obObject.setValue("updated", new Date());
                    OBDal.getInstance().save(obObject);
                    OBDal.getInstance().flush();
                } catch (Exception e) {
                    log.debug(e.getMessage(), e);
                    wsResult.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
                    wsResult.setMessage(e.getMessage());
                }
            } else {
                Tab tab = OBDal.getInstance().get(Tab.class,tabId);
                if (tab != null) {
                    wsResult.setData(WindowUtils.computeColumnValues(tab,parentId,parentEntity, null));
                }
            }

            wsResult.setStatus(WSResult.Status.OK);

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            wsResult.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
            wsResult.setMessage(e.getMessage());
        }
        finally {
            OBContext.restorePreviousMode();
        }

        wsResult.setResultType(WSResult.ResultType.SINGLE);
        return wsResult;
    }

    @Override
    public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        fillSessionVariables(request);
        super.doPost(path, request, response);
    }

    @Override
    public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception {
        WSResult wsResult = new WSResult();

        try {
            OBContext.setAdminMode(true);
            String tabId = parameters.get("tabId");
            String parentId = parameters.get("parentId");
            String parentEntityName = parameters.get("parentEntity");
            JSONObject context = body.getJSONObject("context");
            Entity parentEntity = null;
            if (parentId != null) {
                parentEntity = ModelProvider.getInstance().getEntity(parentEntityName);
                if (parentEntity == null) {
                    throw new OBException(OBMessageUtils.getI18NMessage("SMFMU_EntityNotFound", new String[] {parentEntityName}));
                }
            }

            Tab tab = OBDal.getInstance().get(Tab.class,tabId);
            if (tab != null) {
                wsResult.setData(WindowUtils.computeColumnValues(tab,parentId,parentEntity, context));
            }
            wsResult.setStatus(WSResult.Status.OK);

        } catch (Exception e) {
            log.debug(e.getMessage(), e);
            wsResult.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
            wsResult.setMessage(e.getMessage());
        }
        finally {
            OBContext.restorePreviousMode();
        }

        wsResult.setResultType(WSResult.ResultType.SINGLE);
        return wsResult;
    }

    @Override
    public WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception {
        return null;
    }

    @Override
    public WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception {
        return null;
    }

    private void fillSessionVariables(HttpServletRequest request) throws ServletException {
        try {
            OBContext.setAdminMode();
            SecureWebServicesUtils.fillSessionVariables(request);
        } finally {
            OBContext.restorePreviousMode();
        }
    }
}
