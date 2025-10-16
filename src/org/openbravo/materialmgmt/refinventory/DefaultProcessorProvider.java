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