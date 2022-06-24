package com.smf.jobs;

import com.smf.securewebservices.rsql.OBRestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.service.json.AdvancedQueryBuilder;

/**
 * The Filter class represents a {@link com.smf.model.jobs.Job} filter.
 * A Filter will query the database for a list of entities under certain constraints
 */
public class Filter {
    private static int DEFAULT_BATCH_SIZE = 1000;
    private final String entityName;
    private int currentPage = 0;
    private Data currentResult;
    private final String rsql;
    private static final Logger log = LogManager.getLogger();

    /**
     * Creates a filter instance.
     * @param entityName The database entity that will be queried (For example ADUser)
     * @param rsql The RSQL string that represent the restrictions (or a where clause)
     */
    public Filter(String entityName, String rsql) {
        this.entityName = entityName;
        this.rsql = rsql;
        try {
            DEFAULT_BATCH_SIZE = Integer.parseInt(Preferences.getPreferenceValue("Filter_Batch_Size", true, (String) null, null,
                    null, null, null));
        } catch (PropertyException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @return the results of the current page in a {@link Data} instance. If {@link #hasNext()} has not been called yet, it will return null.
     */
    public Data getResults() {
        return currentResult;
    }

    /**
     * Advances a page, allowing to retrieve more results.
     * Page size can be configured using the Filter_Batch_Size preference, and is defaulted to 1000
     */
    public void nextPage() {
        currentPage = currentPage + DEFAULT_BATCH_SIZE + 1;
    }

    /**
     * Checks if the filter has more results available.
     * If it does, it will query the database so that the next {@link #getResults()} call returns new data
     * @return true if there are more results available, false otherwise.
     */
    public boolean hasNext() {
        var results = getQuery().list();

        if (!results.isEmpty()) {
            currentResult = new Data(results);
            nextPage();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the query that this filter represents.
     * In most cases {@link #hasNext()} should be used instead.
     * @return an {@link OBQuery} with the Filter's entity and restrictions applied.
     */
    public OBQuery<BaseOBObject> getQuery() {
        String whereClause;

        // TODO move out from OBRestUtils into a more generic?
        var criteria = OBRestUtils.criteriaFromRSQL(rsql);

        var queryBuilder = new AdvancedQueryBuilder();
        queryBuilder.setCriteria(criteria);
        queryBuilder.setEntity(entityName);

        whereClause = queryBuilder.getWhereClause();

        var query = OBDal.getInstance().createQuery(entityName, whereClause);
        query.setFirstResult(currentPage);
        query.setMaxResult(DEFAULT_BATCH_SIZE);
        query.setFetchSize(DEFAULT_BATCH_SIZE);
        query.setNamedParameters(queryBuilder.getNamedParameters());

        return query;
    }
}
