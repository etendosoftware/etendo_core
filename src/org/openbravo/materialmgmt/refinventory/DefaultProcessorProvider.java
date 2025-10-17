package org.openbravo.materialmgmt.refinventory;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang3.StringUtils;

/**
 * Default implementation of {@link RefInvProcessorProvider} for referenced inventory processing.
 * <p>
 * This provider supports the default referenced inventory type and provides the default processor classes
 * for boxing and unboxing operations.
 */
@ApplicationScoped
public class DefaultProcessorProvider implements RefInvProcessorProvider {

  /**
   * Checks if the given type is supported by this provider.
   * <p>
   * Supports the default referenced inventory type or blank types.
   *
   * @param type
   *     the referenced inventory type to check
   * @return true if the type is supported, false otherwise
   */
  @Override
  public boolean supports(String type) {
    return StringUtils.equals(ReferencedInventoryUtil.DEFAULT_REFERENCED_INVENTORY_TYPE, type) || StringUtils.isBlank(
        type);
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