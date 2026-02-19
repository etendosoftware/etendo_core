package org.openbravo.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link ConfigureOption}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigureOptionTest {

  private static final String OPTION_B = "Option B";
  private static final String MY_VALUE = "my-value";

  private ArrayList<String> options;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    options = new ArrayList<>();
    options.add("Option A");
    options.add(OPTION_B);
    options.add("Option C");
  }
  /** Constructor sets defaults. */

  @Test
  public void testConstructorSetsDefaults() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick one",
        options);
    assertEquals(0, option.getChosen());
    assertEquals("", option.getChosenString());
    assertEquals(ConfigureOption.TYPE_OPT_CHOOSE, option.getType());
    assertEquals("Pick one", option.getAskInfo());
  }
  /** Get max returns options size. */

  @Test
  public void testGetMaxReturnsOptionsSize() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertEquals(3, option.getMax());
  }
  /** Set chosen valid index. */

  @Test
  public void testSetChosenValidIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(1));
    assertEquals(1, option.getChosen());
  }
  /** Set chosen invalid negative index. */

  @Test
  public void testSetChosenInvalidNegativeIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertFalse(option.setChosen(-1));
    assertEquals(0, option.getChosen());
  }
  /** Set chosen invalid high index. */

  @Test
  public void testSetChosenInvalidHighIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertFalse(option.setChosen(3));
    assertEquals(0, option.getChosen());
  }
  /** Set chosen boundary zero. */

  @Test
  public void testSetChosenBoundaryZero() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(0));
    assertEquals(0, option.getChosen());
  }
  /** Set chosen boundary max minus one. */

  @Test
  public void testSetChosenBoundaryMaxMinusOne() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(2));
    assertEquals(2, option.getChosen());
  }
  /** Get chosen option for choose type. */

  @Test
  public void testGetChosenOptionForChooseType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosen(1);
    assertEquals(OPTION_B, option.getChosenOption());
  }
  /** Get chosen option for string type. */

  @Test
  public void testGetChosenOptionForStringType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter URL",
        options);
    option.setChosenString("http://localhost:8080");
    assertEquals("http://localhost:8080", option.getChosenOption());
  }
  /** Set chosen string updates chosen for choose type. */

  @Test
  public void testSetChosenStringUpdatesChosenForChooseType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosenString(OPTION_B);
    assertEquals(1, option.getChosen());
    assertEquals(OPTION_B, option.getChosenString());
  }
  /** Set chosen string non matching does not update chosen. */

  @Test
  public void testSetChosenStringNonMatchingDoesNotUpdateChosen() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosenString("NonExistent");
    assertEquals(0, option.getChosen());
    assertEquals("NonExistent", option.getChosenString());
  }
  /** Set chosen string for string type. */

  @Test
  public void testSetChosenStringForStringType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter",
        new ArrayList<>());
    option.setChosenString(MY_VALUE);
    assertEquals(MY_VALUE, option.getChosenString());
    assertEquals(MY_VALUE, option.getChosenOption());
  }
  /** Get chosen option default is first option. */

  @Test
  public void testGetChosenOptionDefaultIsFirstOption() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertEquals("Option A", option.getChosenOption());
  }
  /** Get max with empty options. */

  @Test
  public void testGetMaxWithEmptyOptions() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter",
        new ArrayList<>());
    assertEquals(0, option.getMax());
  }
}
