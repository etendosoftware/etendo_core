package com.etendoerp.sequences;

import org.openbravo.client.kernel.RequestContext;
import org.openbravo.model.ad.ui.Field;

public interface UINextSequenceValueInterface {

    String generateNextSequenceValue(Field field, RequestContext requestContext);

}
