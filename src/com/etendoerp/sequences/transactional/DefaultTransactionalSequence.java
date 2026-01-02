package com.etendoerp.sequences.transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Property;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.utility.Sequence;

import com.etendoerp.sequences.DefaultSequenceGenerator;
import com.etendoerp.sequences.SequenceDatabaseUtils;
import com.etendoerp.sequences.dimensions.DimensionListOriginalColumnFormat;
import com.etendoerp.sequences.parameters.SequenceParameterList;
import com.etendoerp.sequences.parameters.SequenceParametersUtils;

import jakarta.enterprise.context.Dependent;

@Dependent
public class DefaultTransactionalSequence extends DefaultSequenceGenerator {

  protected static final Logger log = LogManager.getLogger();

  public DefaultTransactionalSequence(String propertyValue) {
    super(propertyValue);
  }

  @Override
  public String generateValue(Session session, Object owner) {

    var baseOBObject = (BaseOBObject) owner;
    var entity = baseOBObject.getEntity();

    Property property = entity.getProperty(propertyValue);
    String sequenceValue = (String) baseOBObject.getValue(property.getName());
    String columnId = property.getColumnId();

    // The sequenceValue is inserted by the user.
    if (sequenceValue != null && !sequenceValue.isBlank() && !sequenceValue.startsWith(SequenceDatabaseUtils.PREFIX)) {
      return sequenceValue;
    }

    Column column = OBDal.getInstance().get(Column.class, columnId);

    // Dimensions set by a user in the Dimensions List table, could be empty.
    var sequenceDimensionList = new DimensionListOriginalColumnFormat(column);

    // List of parameters values used to search a Sequence.
    SequenceParameterList defaultSequenceParameterList = SequenceParametersUtils.generateDefaultParameterList(
        baseOBObject);
    defaultSequenceParameterList.setParameter(SequenceDatabaseUtils.PROPERTY_COLUMN,
        SequenceParametersUtils.generateTableDirParameter(columnId));

    SequenceParameterList sequenceParameterList = SequenceParametersUtils.getParametersFromBaseOBObject(baseOBObject,
        sequenceDimensionList);
    sequenceParameterList.putAll(defaultSequenceParameterList);

    // Validation

    sequenceDimensionList.validateParametersList(sequenceParameterList);

    Sequence sequence = null;
    String nextSequenceValue = null;
    try {
      sequence = TransactionalSequenceUtils.getSequenceFromParameters(sequenceParameterList);
      nextSequenceValue = TransactionalSequenceUtils.getNextValueFromSequence(sequence, true);
    } catch (RequiredDimensionException e) {

      throw new OBException(OBMessageUtils.getI18NMessage(SequenceDatabaseUtils.DIMENSION_ERROR_CODE,
          new String[]{ e.getRequiredDimension() }));
    } catch (NotFoundSequenceException e) {
      log.error("Error generating the next sequence value for the field {} " +
              ". No Sequence found using the parameters {}", column.getDBColumnName(),
          sequenceParameterList.getParameterValues());
      String message = OBMessageUtils.getI18NMessage(
          SequenceDatabaseUtils.SEQUENCE_NOT_FOUND) + " " + property.getNameOfColumn();
      throw new OBException(message);
    } catch (MaskValueGenerationException e) {
      String message = OBMessageUtils.getI18NMessage(TransactionalSequenceUtils.SEQUENCE_ERROR_PARCE_CODE);
      String exceptionMessage = e.getMessage();
      if (!StringUtils.isBlank(exceptionMessage)) {
        message = message + " " + exceptionMessage;
      }
      log.error(message);
      throw new OBException(message, e);
    }

    return nextSequenceValue;
  }

}
