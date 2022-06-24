package com.smf.securewebservices.service;

import com.smf.securewebservices.utils.WSResult;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.Map;

public abstract class BaseAction extends BaseWebService{
    public abstract JSONObject execute(JSONObject params) throws Exception;

    public WSResult get(String path, Map<String, String> parameters) throws Exception{
        return post(path,parameters,new JSONObject());
    }

    public WSResult put(String path, Map<String, String> parameters, JSONObject body) throws Exception{
        return post(path,parameters,body);
    }

    public WSResult delete(String path, Map<String, String> parameters, JSONObject body) throws Exception{
        return post(path,parameters,body);
    }

    public WSResult post(String path, Map<String, String> parameters, JSONObject body) throws Exception{
        WSResult wsResult = new WSResult();
        wsResult.setResultType(WSResult.ResultType.SINGLE);
        try{
            wsResult.setAction(execute(body));
            wsResult.setStatus(WSResult.Status.OK);
        }catch(Exception e){
            wsResult.setStatus(WSResult.Status.INTERNAL_SERVER_ERROR);
        }
        return wsResult;
    }
}
