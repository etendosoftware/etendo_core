package com.smf.mobile.utils.webservices;

import java.io.Writer;
import org.openbravo.service.json.JsonUtils;
import com.smf.securewebservices.rsql.OBRestUtils;
import com.smf.securewebservices.utils.WSResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.web.WebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Version implements WebService {

    private static final Logger log = LogManager.getLogger(Window.class);

    @Override
    public void doGet(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject jsonResponse = new JSONObject();

        try{
            OBContext.setAdminMode();
            Module core = OBDal.getInstance().get(Module.class, "0");
            jsonResponse.put( "coreVersion", core.getVersion());
            WSResult result = new WSResult();
            result.setData(jsonResponse);
            result.setStatus( WSResult.Status.OK );
            OBRestUtils.writeWSResponse(result, response);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          jsonResponse = new JSONObject(JsonUtils.convertExceptionToJson(e));
        } finally {
          OBContext.restorePreviousMode();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        final Writer w = response.getWriter();
        w.write(jsonResponse.toString());
        w.close();
    }

    @Override
    public void doPost(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

    }

    @Override
    public void doDelete(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

    }

    @Override
    public void doPut(String path, HttpServletRequest request, HttpServletResponse response) throws Exception {

    }
}
