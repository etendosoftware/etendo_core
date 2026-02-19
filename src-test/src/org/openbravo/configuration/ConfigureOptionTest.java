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

  private ArrayList<String> options;

  @Before
  public void setUp() {
    options = new ArrayList<>();
    options.add("Option A");
    options.add("Option B");
    options.add("Option C");
  }

  @Test
  public void testConstructorSetsDefaults() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick one",
        options);
    assertEquals(0, option.getChosen());
    assertEquals("", option.getChosenString());
    assertEquals(ConfigureOption.TYPE_OPT_CHOOSE, option.getType());
    assertEquals("Pick one", option.getAskInfo());
  }

  @Test
  public void testGetMaxReturnsOptionsSize() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertEquals(3, option.getMax());
  }

  @Test
  public void testSetChosenValidIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(1));
    assertEquals(1, option.getChosen());
  }

  @Test
  public void testSetChosenInvalidNegativeIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertFalse(option.setChosen(-1));
    assertEquals(0, option.getChosen());
  }

  @Test
  public void testSetChosenInvalidHighIndex() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertFalse(option.setChosen(3));
    assertEquals(0, option.getChosen());
  }

  @Test
  public void testSetChosenBoundaryZero() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(0));
    assertEquals(0, option.getChosen());
  }

  @Test
  public void testSetChosenBoundaryMaxMinusOne() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertTrue(option.setChosen(2));
    assertEquals(2, option.getChosen());
  }

  @Test
  public void testGetChosenOptionForChooseType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosen(1);
    assertEquals("Option B", option.getChosenOption());
  }

  @Test
  public void testGetChosenOptionForStringType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter URL",
        options);
    option.setChosenString("http://localhost:8080");
    assertEquals("http://localhost:8080", option.getChosenOption());
  }

  @Test
  public void testSetChosenStringUpdatesChosenForChooseType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosenString("Option B");
    assertEquals(1, option.getChosen());
    assertEquals("Option B", option.getChosenString());
  }

  @Test
  public void testSetChosenStringNonMatchingDoesNotUpdateChosen() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    option.setChosenString("NonExistent");
    assertEquals(0, option.getChosen());
    assertEquals("NonExistent", option.getChosenString());
  }

  @Test
  public void testSetChosenStringForStringType() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter",
        new ArrayList<>());
    option.setChosenString("my-value");
    assertEquals("my-value", option.getChosenString());
    assertEquals("my-value", option.getChosenOption());
  }

  @Test
  public void testGetChosenOptionDefaultIsFirstOption() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_CHOOSE, "Pick", options);
    assertEquals("Option A", option.getChosenOption());
  }

  @Test
  public void testGetMaxWithEmptyOptions() {
    ConfigureOption option = new ConfigureOption(ConfigureOption.TYPE_OPT_STRING, "Enter",
        new ArrayList<>());
    assertEquals(0, option.getMax());
  }
}
