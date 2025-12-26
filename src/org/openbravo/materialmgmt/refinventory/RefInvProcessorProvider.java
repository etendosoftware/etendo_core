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

/**
 * Provider interface for referenced inventory processors.
 * <p>
 * Implementations of this interface supply processor classes for handling referenced inventory operations,
 * such as boxing and unboxing, and define which inventory types they support.
 */
public interface RefInvProcessorProvider {

  /**
   * Determines if the provider supports the given referenced inventory type.
   *
   * @param inventoryType
   *     the referenced inventory type to check
   * @return true if the type is supported, false otherwise
   */
  boolean supports(String inventoryType);

  /**
   * Returns the priority of this provider. Higher priority providers are preferred when multiple providers support the same type.
   * Default priority is 50.
   *
   * @return the priority value
   */
  default int getPriority() {
    return 50;
  }

  /**
   * Returns the class of the processor used for boxing operations.
   *
   * @return the {@link ReferencedInventoryProcessor} class
   */
  Class<? extends ReferencedInventoryProcessor> getBoxProcessorClass();

  /**
   * Returns the class of the processor used for unboxing operations.
   *
   * @return the {@link ReferencedInventoryProcessor} class
   */
  Class<? extends ReferencedInventoryProcessor> getUnboxProcessorClass();
}
