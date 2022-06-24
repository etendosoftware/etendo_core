package org.openbravo.erpCommon.utility;

import javax.servlet.ServletException;

import org.openbravo.data.ScrollableFieldProvider;

public class LimitRowsScrollableFieldProviderFilter extends AbstractScrollableFieldProviderFilter {
  private final int rowLimit;
  private int rowCount = 0;

  public LimitRowsScrollableFieldProviderFilter(ScrollableFieldProvider input, int rowLimit) {
    super(input);
    this.rowLimit = rowLimit;
  }

  @Override
  public boolean next() throws ServletException {
    if (input.next()) {
      rowCount++;
      if (rowCount > rowLimit) {
        throw new ServletException(OBMessageUtils.messageBD("numberOfRowsExceeded"));
      }
      return true;
    }
    return false;
  }

}
