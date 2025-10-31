package com.smf.securewebservices;

import java.util.Map;

import org.openbravo.service.datasource.DefaultDataSourceService;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SWSDataSourceService extends DefaultDataSourceService {
    @Override
    public String getWhereAndFilterClause(Map<String, String> parameters) {
        return super.getWhereAndFilterClause(parameters);
    }
}
