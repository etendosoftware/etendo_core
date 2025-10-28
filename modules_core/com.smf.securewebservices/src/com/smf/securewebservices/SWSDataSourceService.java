package com.smf.securewebservices;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import org.openbravo.service.datasource.BaseDataSourceService;
import org.openbravo.service.datasource.DefaultDataSourceService;

import java.util.Map;

@Dependent
public class SWSDataSourceService extends DefaultDataSourceService {
    @Override
    public String getWhereAndFilterClause(Map<String, String> parameters) {
        return super.getWhereAndFilterClause(parameters);
    }
}
