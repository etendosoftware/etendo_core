/*
 ************************************************************************************
 * Copyright (C) 2013 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.data;

import javax.servlet.ServletException;

/**
 * Interface implemented by java code generated for xsql-files if any SqlMethod defined is using
 * return="scrollable"
 * 
 * Generated classes implementing this allow reading the result row by row without the need to load
 * all the potentially big dataset all at once into memory.
 * 
 * @author huehner
 * 
 */
public interface ScrollableFieldProvider {

  public boolean hasData();

  public boolean next() throws ServletException;

  public FieldProvider get() throws ServletException;

  public void close();

}
