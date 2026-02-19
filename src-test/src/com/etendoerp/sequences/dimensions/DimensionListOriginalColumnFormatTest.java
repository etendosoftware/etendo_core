/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package com.etendoerp.sequences.dimensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link DimensionListOriginalColumnFormat}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DimensionListOriginalColumnFormatTest {

  private static final String EM_DBPREF_COLUMN_NAME_ID = "Em_Dbpref_Column_Name_Id";
  private static final String COLUMN_NAME_ID = "column_name_id";

  private DimensionListOriginalColumnFormat instance;
  private Map<String, SequenceDimension> dimensionMap;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DimensionListOriginalColumnFormat.class);

    dimensionMap = new HashMap<>();
    Field mapField = findField(instance.getClass(), "dimensionMap");
    mapField.setAccessible(true);
    mapField.set(instance, dimensionMap);
  }
  /** Transform key with external module prefix. */

  @Test
  public void testTransformKeyWithExternalModulePrefix() {
    String result = instance.transformKey(EM_DBPREF_COLUMN_NAME_ID);
    assertEquals(COLUMN_NAME_ID, result);
  }
  /** Transform key without external module prefix. */

  @Test
  public void testTransformKeyWithoutExternalModulePrefix() {
    String result = instance.transformKey("Column_Name_Id");
    assertEquals(COLUMN_NAME_ID, result);
  }
  /** Transform key simple column. */

  @Test
  public void testTransformKeySimpleColumn() {
    String result = instance.transformKey("AD_Client_ID");
    assertEquals("ad_client_id", result);
  }
  /** Set dimension transforming key stores with transformed key. */

  @Test
  public void testSetDimensionTransformingKeyStoresWithTransformedKey() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    instance.setDimensionTransformingKey(EM_DBPREF_COLUMN_NAME_ID, dim);

    assertNotNull(dimensionMap.get(COLUMN_NAME_ID));
    assertEquals(dim, dimensionMap.get(COLUMN_NAME_ID));
  }
  /** Get dimension transforming key retrieves with transformed key. */

  @Test
  public void testGetDimensionTransformingKeyRetrievesWithTransformedKey() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    dimensionMap.put(COLUMN_NAME_ID, dim);

    SequenceDimension result = instance.getDimensionTransformingKey(EM_DBPREF_COLUMN_NAME_ID);
    assertEquals(dim, result);
  }
  /** Get dimension transforming key returns null when not found. */

  @Test
  public void testGetDimensionTransformingKeyReturnsNullWhenNotFound() {
    SequenceDimension result = instance.getDimensionTransformingKey("Nonexistent_Column");
    assertNull(result);
  }
  /** Set and get dimension round trip. */

  @Test
  public void testSetAndGetDimensionRoundTrip() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    instance.setDimensionTransformingKey("AD_Org_ID", dim);

    SequenceDimension result = instance.getDimensionTransformingKey("AD_Org_ID");
    assertEquals(dim, result);
  }

  private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
