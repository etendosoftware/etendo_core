package com.etendoerp.sequences.transactional;

import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import com.etendoerp.sequences.annotations.SequenceFilter;
import com.etendoerp.sequences.dimensions.DimensionListRequestParameterFormat;
import com.etendoerp.sequences.parameters.SequenceParameterList;
import com.etendoerp.sequences.parameters.SequenceParametersUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.utility.Sequence;

@SequenceFilter(TransactionalSequenceUtils.TRANSACTIONAL_SEQUENCE_ID)
public class UITransactionalSequence implements UINextSequenceValueInterface {

    protected static final Logger log = LogManager.getLogger();

    public static final String INP_DOCUMENTTARGET = "inpcDoctypetargetId";
    public static final String INP_DOCUMENTTYPE   = "inpcDoctypeId";
    public static final String INP_CLIENT         = "inpadClientId";
    public static final String INP_ORGANIZATION   = "inpadOrgId";

    @Override
    public String generateNextSequenceValue(Field field, RequestContext rq) {
        SequenceParameterList defaultSequenceParameterList = new SequenceParameterList();

        defaultSequenceParameterList.setParameter(
                SequenceDatabaseUtils.PROPERTY_COLUMN,
                SequenceParametersUtils.generateTableDirParameter(field.getColumn().getId())
        );

        // Get the values from the request
        String documentType = rq.getRequestParameter(INP_DOCUMENTTARGET);
        if (documentType == null || documentType.isBlank()) {
            documentType = rq.getRequestParameter(INP_DOCUMENTTYPE);
        }

        if (documentType != null && !documentType.isBlank()) {
            defaultSequenceParameterList.setParameter(
                    SequenceDatabaseUtils.PROPERTY_DOCUMENTTYPE,
                    SequenceParametersUtils.generateTableDirParameter(documentType)
            );
        }

        String clientId = rq.getRequestParameter(INP_CLIENT);
        String organizationId = rq.getRequestParameter(INP_ORGANIZATION);

        defaultSequenceParameterList.setParameter(
                SequenceDatabaseUtils.PROPERTY_CLIENT,
                SequenceParametersUtils.generateTableDirParameter(clientId)
        );
        defaultSequenceParameterList.setParameter(
                SequenceDatabaseUtils.PROPERTY_ORGANIZATION,
                SequenceParametersUtils.generateTableDirParameter(organizationId)
        );

        // Get the dimension list set by a user
        var sequenceDimensionList = new DimensionListRequestParameterFormat(field.getColumn());

        // The list of parameters used to search a Sequence.
        var sequenceParameterList = SequenceParametersUtils.getParametersFromRequest(rq, sequenceDimensionList);
        sequenceParameterList.putAll(defaultSequenceParameterList);

        // Validations
        sequenceDimensionList.validateParametersList(sequenceParameterList);

        Sequence sequence;
        String nextSequenceValue = null;
        try {
            sequence = TransactionalSequenceUtils.getSequenceFromParameters(sequenceParameterList);
            nextSequenceValue = TransactionalSequenceUtils.getNextValueFromSequence(sequence, false);
        } catch (RequiredDimensionException e) {
            throw new OBException(OBMessageUtils.getI18NMessage(SequenceDatabaseUtils.DIMENSION_ERROR_CODE, new String[] { e.getRequiredDimension() }));
        } catch (NotFoundSequenceException e) {
            log.error("Error generating the next sequence value for the field {} " +
                    ". No Sequence found using the parameters {}", field.getColumn().getDBColumnName(), sequenceParameterList.getParameterValues());
            String message = OBMessageUtils.getI18NMessage(SequenceDatabaseUtils.SEQUENCE_NOT_FOUND) + " " + field.getName();
            throw new OBException(message);
        } catch (MaskValueGenerationException e) {
            String message = OBMessageUtils.getI18NMessage(TransactionalSequenceUtils.SEQUENCE_ERROR_PARCE_CODE);
            String exceptionMessage = e.getMessage();
            if (!StringUtils.isBlank(exceptionMessage)) {
                message = message + " " + exceptionMessage;
            }
            log.error(message);
            throw new OBException(message, e);
        } catch (Exception e) {
            String message = e.getMessage();
            log.error(message);
            throw new OBException(message, e);
        }

        return nextSequenceValue;

    }

}
