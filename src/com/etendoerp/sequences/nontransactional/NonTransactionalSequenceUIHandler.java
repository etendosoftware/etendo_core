package com.etendoerp.sequences.nontransactional;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.model.ad.ui.Field;

import com.etendoerp.sequences.SequenceUtils;
import com.etendoerp.sequences.UINextSequenceValueInterface;
import com.etendoerp.sequences.annotations.SequenceFilter;
import com.etendoerp.sequences.services.NonTransactionalSequenceServiceImpl;

import jakarta.enterprise.context.Dependent;

@Dependent
@SequenceFilter(SequenceUtils.NON_TRANSACTIONAL_SEQUENCE_ID)
public class NonTransactionalSequenceUIHandler implements UINextSequenceValueInterface {
  @Override
  public String generateNextSequenceValue(Field field, RequestContext requestContext) {
    Entity entity = ModelProvider.getInstance().getEntityByTableId(field.getColumn().getTable().getId());
    String sequenceName = entity.getPropertyByColumnName(field.getColumn().getDBColumnName()).getDBSequenceName();
    return NonTransactionalSequenceServiceImpl.INSTANCE.nextValue(sequenceName);
  }
}
