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

  private DimensionListOriginalColumnFormat instance;
  private Map<String, SequenceDimension> dimensionMap;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DimensionListOriginalColumnFormat.class);

    dimensionMap = new HashMap<>();
    Field mapField = findField(instance.getClass(), "dimensionMap");
    mapField.setAccessible(true);
    mapField.set(instance, dimensionMap);
  }

  @Test
  public void testTransformKeyWithExternalModulePrefix() {
    String result = instance.transformKey("Em_Dbpref_Column_Name_Id");
    assertEquals("column_name_id", result);
  }

  @Test
  public void testTransformKeyWithoutExternalModulePrefix() {
    String result = instance.transformKey("Column_Name_Id");
    assertEquals("column_name_id", result);
  }

  @Test
  public void testTransformKeySimpleColumn() {
    String result = instance.transformKey("AD_Client_ID");
    assertEquals("ad_client_id", result);
  }

  @Test
  public void testSetDimensionTransformingKeyStoresWithTransformedKey() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    instance.setDimensionTransformingKey("Em_Dbpref_Column_Name_Id", dim);

    assertNotNull(dimensionMap.get("column_name_id"));
    assertEquals(dim, dimensionMap.get("column_name_id"));
  }

  @Test
  public void testGetDimensionTransformingKeyRetrievesWithTransformedKey() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    dimensionMap.put("column_name_id", dim);

    SequenceDimension result = instance.getDimensionTransformingKey("Em_Dbpref_Column_Name_Id");
    assertEquals(dim, result);
  }

  @Test
  public void testGetDimensionTransformingKeyReturnsNullWhenNotFound() {
    SequenceDimension result = instance.getDimensionTransformingKey("Nonexistent_Column");
    assertNull(result);
  }

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
