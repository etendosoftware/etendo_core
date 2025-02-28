package com.etendoerp.sequences.parameters;

import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.SequenceUtils;
import com.etendoerp.sequences.dimensions.ASequenceDimensionList;
import com.etendoerp.sequences.dimensions.SequenceDimension;
import com.etendoerp.sequences.transactional.RequiredDimensionException;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.TableDirDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;

import java.util.Map;

public class SequenceParametersUtils {

    private SequenceParametersUtils() {}

    /**
     * Creates a Sequence Parameter using the TableDir domain type, a subtype of the ForeignKeyDomainType.
     * This is used to indicate that the parameter is a Foreign key to other table.
     * @param id Foreign key
     * @return SequenceParameter Object
     */
    public static SequenceParameter generateTableDirParameter(String id) {
        return new SequenceParameter(id, new TableDirDomainType());
    }

    /**
     * Verifies that the required dimensions are present in the 'parameters' Map.
     * This map is used later to search a Sequence using HQL.
     *
     * @param parameters The parameters to verify.
     */
    public static void validateRequiredDimensions(Map<String, SequenceParameter> parameters) throws RequiredDimensionException {
        for (String reqDimension : SequenceDatabaseUtils.REQUIRED_DIMENSIONS) {
            SequenceParameter parameter = parameters.getOrDefault(reqDimension, null);
            if (parameter == null || parameter.getDataValue() == null) {
                throw new RequiredDimensionException(reqDimension);
            }
        }
    }

    /**
     * Obtains a List of the default parameters from a BaseOBObject.
     *
     * Only the 'Foreign Key' values are taking into account.
     *
     * @param baseOBObject The object from where obtain the values
     * @return SequenceParameterList
     */
    public static SequenceParameterList generateDefaultParameterList(BaseOBObject baseOBObject) {
        SequenceParameterList sequenceParameterList = new SequenceParameterList();

        Entity entity = baseOBObject.getEntity();
        for (String prop : SequenceDatabaseUtils.DEFAULT_DIMENSIONS) {
            if (entity.hasProperty(prop)) {
                Object obj = baseOBObject.getValue(prop);
                if (obj instanceof BaseOBObject) {
                    SequenceParameter sequenceParameter = generateTableDirParameter(((BaseOBObject) obj).getId().toString());
                    if (prop.equals(SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE_TARGET)) {
                        prop = SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE;
                    }
                    sequenceParameterList.setParameter(prop, sequenceParameter);
                }
            }
        }
        return sequenceParameterList;
    }

    /**
     * Gets the parameters from a BaseOBObject, used to search a Sequence.
     * The parameters are indicated by the SequenceDimensionList.
     *
     * @param baseOBObject The Entity who is being created, from where the parameter values are obtained.
     * @param sequenceDimensionList The dimensions set by a used, this list contains the parameters to search
     *                              in the baseOBObject.
     * @return A SequenceParameterList with the values of the parameters used to perform a search.
     */
    public static SequenceParameterList getParametersFromBaseOBObject(BaseOBObject baseOBObject, ASequenceDimensionList<String> sequenceDimensionList) {
        SequenceParameterList sequenceParameterList = new SequenceParameterList();
        Entity entity = baseOBObject.getEntity();

        if (!sequenceDimensionList.isEmpty()) {
            // Loops over all the properties because the ColumnName (defaultColumn variable) could be from a external module
            // and the method 'entity.getPropertyByColumnName()' will not work.
            for (Property entityProp : entity.getProperties()) {
                String defaultColumn = entityProp.getColumnName();
                if (StringUtils.isBlank(defaultColumn)) {
                    continue;
                }
                SequenceDimension sequenceDimension = sequenceDimensionList.getDimensionTransformingKey(defaultColumn);

                if (sequenceDimension != null && sequenceDimension.getProperty() != null) {

                    Object data = baseOBObject.getValue(entityProp.getName());

                    if (data instanceof BaseOBObject) {
                        data = ((BaseOBObject) data).getId();
                    }

                    SequenceParameter sequenceParameter = new SequenceParameter(data, sequenceDimension.getOriginalDimensionList().getColumn());
                    sequenceParameterList.setParameter(sequenceDimension.getPropertyName(), sequenceParameter);
                }
            }
        }
        return sequenceParameterList;
    }

    /**
     * Gets the parameters from the RequestContext, used later to search a Sequence.
     * The parameters are indicated by the SequenceDimensionList.
     *
     * @param requestContext The object from where the values of the parameters are obtained.
     * @param sequenceDimensionList The dimensions set by a used, this list contains the parameters to search
*      *                              in the RequestContext.
     * @return A SequenceParameterList with the values of the parameters used to perform a search.
     */
    public static SequenceParameterList getParametersFromRequest(RequestContext requestContext, ASequenceDimensionList<String> sequenceDimensionList) {
        SequenceParameterList sequenceParameterList = new SequenceParameterList();
        if (!sequenceDimensionList.isEmpty()) {

            var parameters = requestContext.getRequest().getParameterNames();
            // Loops over all the RequestContext parameters because they could belong to an external module.
            // The method requestContext.getRequestParameter() will not work.
            while (parameters.hasMoreElements()) {
                String requestParameter = parameters.nextElement();
                String parameterValue   = requestContext.getRequestParameter(requestParameter);
                if (requestParameter.startsWith(SequenceUtils.INP) && parameterValue != null && !parameterValue.isBlank()) {

                    SequenceDimension sequenceDimension = sequenceDimensionList.getDimensionTransformingKey(requestParameter);

                    if (sequenceDimension != null && sequenceDimension.getProperty() != null) {
                        SequenceParameter sequenceParameter = new SequenceParameter(parameterValue, sequenceDimension.getOriginalDimensionList().getColumn());
                        sequenceParameterList.setParameter(sequenceDimension.getPropertyName(), sequenceParameter);
                    }
                }
            }
        }
        return sequenceParameterList;
    }

}
