/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.client.application.navigationbarcomponents;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.MenuManager;
import org.openbravo.client.application.MenuManager.MenuOption;

/**
 * Tests for {@link ApplicationMenuComponent}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApplicationMenuComponentTest {

  private ApplicationMenuComponent component;

  @Mock
  private MenuManager menuManager;

  @Mock
  private MenuOption rootMenuOption;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    component = objenesis.newInstance(ApplicationMenuComponent.class);

    Field menuManagerField = ApplicationMenuComponent.class.getDeclaredField("menuManager");
    menuManagerField.setAccessible(true);
    menuManagerField.set(component, menuManager);
  }
  /** Get id returns application menu id. */

  @Test
  public void testGetIdReturnsApplicationMenuId() {
    String id = component.getId();
    assertEquals(ApplicationConstants.APPLICATION_MENU_ID, id);
  }
  /** Get root menu options returns children from menu manager. */

  @Test
  public void testGetRootMenuOptionsReturnsChildrenFromMenuManager() {
    List<MenuOption> expectedChildren = new ArrayList<>();
    expectedChildren.add(rootMenuOption);

    when(menuManager.getMenu()).thenReturn(rootMenuOption);
    when(rootMenuOption.getChildren()).thenReturn(expectedChildren);

    List<MenuOption> result = component.getRootMenuOptions();
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(rootMenuOption, result.get(0));
  }
  /** Get root menu options returns empty list when no children. */

  @Test
  public void testGetRootMenuOptionsReturnsEmptyListWhenNoChildren() {
    List<MenuOption> emptyChildren = new ArrayList<>();

    when(menuManager.getMenu()).thenReturn(rootMenuOption);
    when(rootMenuOption.getChildren()).thenReturn(emptyChildren);

    List<MenuOption> result = component.getRootMenuOptions();
    assertNotNull(result);
    assertEquals(0, result.size());
  }
}
