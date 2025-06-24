package org.openbravo.client.application.process.dto;
/**
 * Data Transfer Object (DTO) used to represent a button in the warehouse management process.
 * <p>
 * This class encapsulates the information needed to render a button in the UI,
 * including its unique identifier (search key) and its translated display name.
 * It is typically used to pass button data to FreeMarker templates for UI rendering.
 * </p>
 *
 * Example usage:
 * <pre>
 *   new ButtonDTO("btn_confirm", "Confirm");
 * </pre>
 */
public class TrlButtonDTO {

  /**
   * The unique identifier for the button (used internally).
   */
  private final String searchKey;

  /**
   * The translated display name of the button (used in the UI).
   */
  private final String name;

  /**
   * Constructs a new ButtonDTO.
   *
   * @param searchKey The internal key of the button.
   * @param name      The translated name to be displayed in the UI.
   */
  public TrlButtonDTO(String searchKey, String name) {
    this.searchKey = searchKey;
    this.name = name;
  }

  /**
   * Gets the search key of the button.
   *
   * @return The search key.
   */
  public String getSearchKey() {
    return searchKey;
  }

  /**
   * Gets the translated name of the button.
   *
   * @return The translated button name.
   */
  public String getName() {
    return name;
  }
}