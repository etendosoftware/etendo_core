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
 * All portions are Copyright (C) 2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_process;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductBOM;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class VerifyBOM extends DalBaseProcess {

  static Logger log4j = LogManager.getLogger();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    try {
      final String strProduct = (String) bundle.getParams().get("M_Product_ID");
      Product product = OBDal.getInstance().get(Product.class, strProduct);

      List<Product> productList = new ArrayList<Product>();
      productList.add(product);
      List<ProductBOM> productBOMList = product.getProductBOMList();

      if (productBOMList.isEmpty()) {
        throw new Exception(Utility.messageBD(new DalConnectionProvider(), "BOM_Without_Lines",
            OBContext.getOBContext().getLanguage().getLanguage()));
      }

      for (ProductBOM productBom : productBOMList) {
        if (productBom.getBOMQuantity().floatValue() < 0) {
          throw new Exception(Utility.messageBD(new DalConnectionProvider(), "BOM_NegativeQty",
              OBContext.getOBContext().getLanguage().getLanguage()));
        }
      }

      boolean cycle = checkForcycles(productList, productBOMList);

      if (cycle) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage("@LOOP_IN_BOM@");
      } else {
        product.setBOMVerified(true);
        OBDal.getInstance().save(product);
      }

    } catch (final Exception e) {
      log4j.error("Exception found in VerifyBOM: ", e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(e.getMessage());

    } finally {
      bundle.setResult(msg);
    }
  }

  /**
   * This method checks recursively if there is a cycle in the lists
   */
  private boolean checkForcycles(List<Product> productList, List<ProductBOM> productBOMList) {
    // Checks if some element of the first list appears in the second
    for (Product product : productList) {
      for (ProductBOM productBOM : productBOMList) {
        if (product.equals(productBOM.getBOMProduct())) {
          return true;
        }
      }
    }
    // If the elements are different, it checks recursively through the children of the second list
    // for cycles
    for (ProductBOM productBOM : productBOMList) {
      if (productBOM.getBOMProduct().getProductBOMList().size() > 0) {
        List<Product> auxProductList = new ArrayList<Product>();
        auxProductList.addAll(productList);
        auxProductList.add(productBOM.getBOMProduct());
        if (checkForcycles(auxProductList, productBOM.getBOMProduct().getProductBOMList())) {
          return true;
        }
      }
    }
    return false;
  }

}
