package org.openbravo.common.hooks;

import java.util.Map;

public interface DatasourceFilterHook {

  default void preProcess(Map<String, String> parameters, Map<String, Object> filtersCriteria){
    // Default implementation does nothing
  }

  default void posProcess(Map<String, String> parameters, Map<String, Object> filtersCriteria){
    // Default implementation does nothing
  }

}
