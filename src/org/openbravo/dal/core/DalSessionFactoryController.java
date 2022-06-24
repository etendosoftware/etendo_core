/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.etendoerp.sequences.services.NonTransactionalSequenceService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.service.Service;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.SessionFactoryController;

/**
 * Initializes and provides the session factory for the runtime dal layer. This
 * SessionFactoryController is initialized after the model has been read in-memory. The
 * {@link DalMappingGenerator DalMappingGenerator} is used to generated the Hibernate mapping for
 * the runtime model (see {@link ModelProvider ModelProvider}.
 * 
 * @author mtaal
 */

public class DalSessionFactoryController extends SessionFactoryController {
  private static final Logger log = LogManager.getLogger();

  @Inject
  @Any
  private Instance<SQLFunctionRegister> sqlFunctionRegisters;

  private Map<String, SQLFunction> sqlFunctions;

  @Override
  protected void mapModel(Configuration configuration) {
    DalMappingGenerator mappingGenerator = DalMappingGenerator.getInstance();
    final String mapping = mappingGenerator.generateMapping();
    log.debug("Generated mapping: \n{}", mapping);

    if (mappingGenerator.getHibernateFileLocation() != null) {
      configuration.addFile(mappingGenerator.getHibernateFileLocation());
      return;
    }

    Path tmpFile = null;
    try {
      tmpFile = Files.createTempFile("", ".hbm");
      Files.write(tmpFile, mapping.getBytes());
      configuration.addFile(tmpFile.toString());
    } catch (IOException ioex) {
      throw new OBException("Error writing temporary .hbm file for configuration", ioex);
    } finally {
      try {
        if (tmpFile != null) {
          Files.delete(tmpFile);
        }
      } catch (IOException ioex) {
        log.error("Error deleting temporary .hbm file for configuration", ioex);
      }
    }
  }

  @Override
  protected void setInterceptor(Configuration configuration) {
    configuration.setInterceptor(new OBInterceptor());
  }

  @Override
  protected Map<String, SQLFunction> getSQLFunctions() {
    if (sqlFunctions != null) {
      return sqlFunctions;
    }
    sqlFunctions = new HashMap<>();
    if (sqlFunctionRegisters == null) {
      return sqlFunctions;
    }
    for (SQLFunctionRegister register : sqlFunctionRegisters) {
      Map<String, SQLFunction> registeredSqlFunctions = register.getSQLFunctions();
      if (registeredSqlFunctions == null) {
        continue;
      }
      sqlFunctions.putAll(registeredSqlFunctions);
    }
    return sqlFunctions;
  }

  void setSQLFunctions(Map<String, SQLFunction> sqlFunctions) {
    this.sqlFunctions = sqlFunctions;
  }

  @Override
  protected List<Class<? extends Service>> getServices() {
    return Collections.singletonList(NonTransactionalSequenceService.class);
  }
}
