package com.smf.jobs.defaults.invoices;

import jakarta.enterprise.context.Dependent;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

import java.util.Map;

@Dependent
@ComponentProvider.Qualifier("903BE70A11F849F19BAD389E9A84BDF9")
public class CreateFromOrdersHQLTransformer extends HqlQueryTransformer {

    @Override
    public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters, Map<String, Object> queryNamedParameters) {
        boolean linesIncludeTaxes = Boolean.parseBoolean(requestParameters.getOrDefault("linesIncludeTaxes", "false"));
        String linesIncludeTaxesYesNo = linesIncludeTaxes ? "'Y'" : "'N'";

        return hqlQuery.replaceAll("@linesIncludeTaxes@", linesIncludeTaxesYesNo);
    }
}
