package com.smf.securewebservices;

import org.openbravo.service.datasource.BaseDataSourceService;
import org.openbravo.service.datasource.DefaultDataSourceService;

import java.util.Map;

public class SWSDataSourceService extends DefaultDataSourceService {
    @Override
    public String getWhereAndFilterClause(Map<String, String> parameters) {
        return super.getWhereAndFilterClause(parameters);
    }
}
