/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.materialmgmt.refinventory;

import javax.enterprise.context.ApplicationScoped;

/**
 * Default implementation of {@link RefInvProcessorProvider} for referenced inventory processing.
 * <p>
 * This provider supports the default referenced inventory type and provides the default processor classes
 * for boxing and unboxing operations.
 */
@ApplicationScoped
public class DefaultProcessorProvider implements RefInvProcessorProvider {

  /**
   * Checks if the given referenced inventory type is supported by this provider.
   * <p>
   * This generic implementation always returns false, indicating that it does not explicitly support any type.
   * The dispatcher {@link org.openbravo.common.actionhandler.ReferencedInventoryBoxHandler} will select this provider only when no specific
   * implementation exists for the referenced inventory type, ensuring default behavior is applied.
   *
   * @param type
   *     the referenced inventory type to check
   * @return false, as this provider is only used as a fallback when no specific provider matches
   */
  @Override
  public boolean supports(String type) {
    return false;
  }

  /**
   * Returns the class of the processor used for boxing operations.
   *
   * @return the {@link BoxProcessor} class
   */
  @Override
  public Class<? extends BoxProcessor> getBoxProcessorClass() {
    return BoxProcessor.class;
  }

  /**
   * Returns the class of the processor used for unboxing operations.
   *
   * @return the {@link UnboxProcessor} class
   */
  @Override
  public Class<? extends ReferencedInventoryProcessor> getUnboxProcessorClass() {
    return UnboxProcessor.class;
  }
}
