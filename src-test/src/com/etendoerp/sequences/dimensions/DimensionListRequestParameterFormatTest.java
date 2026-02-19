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
 * Tests for {@link DimensionListRequestParameterFormat}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DimensionListRequestParameterFormatTest {

  private DimensionListRequestParameterFormat instance;
  private Map<String, SequenceDimension> dimensionMap;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DimensionListRequestParameterFormat.class);

    dimensionMap = new HashMap<>();
    Field mapField = findField(instance.getClass(), "dimensionMap");
    mapField.setAccessible(true);
    mapField.set(instance, dimensionMap);
  }

  @Test
  public void testTransformKeySimpleColumn() {
    // Column_Name_Id -> inpcolumnNameId (via Sqlc.TransformaNombreColumna + truncate)
    String result = instance.transformKey("AD_Client_ID");
    assertNotNull(result);
    // The result should start with "inp"
    assertEquals(true, result.startsWith("inp"));
  }

  @Test
  public void testTransformKeyWithExternalModulePrefix() {
    // Em_Dbpref_Column_Name_Id should have the EM prefix truncated
    String result = instance.transformKey("Em_Dbpref_Column_Name_Id");
    assertNotNull(result);
    assertEquals(true, result.startsWith("inp"));
    // Should NOT contain "em" prefix in the result
    assertEquals(false, result.startsWith("inpem"));
  }

  @Test
  public void testSetDimensionTransformingKeyStoresWithTransformedKey() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    instance.setDimensionTransformingKey("AD_Client_ID", dim);

    // The map should have an entry with the transformed key
    assertEquals(1, dimensionMap.size());
    // Verify we can retrieve via the transformKey
    String expectedKey = instance.transformKey("AD_Client_ID");
    assertEquals(dim, dimensionMap.get(expectedKey));
  }

  @Test
  public void testGetDimensionTransformingKeyWithParameterFormat() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    // Store via set (which transforms column name to parameter format)
    String transformedKey = instance.transformKey("AD_Client_ID");
    dimensionMap.put(transformedKey, dim);

    // getDimensionTransformingKey expects a parameter-format key and truncates the EM prefix
    SequenceDimension result = instance.getDimensionTransformingKey(transformedKey);
    assertEquals(dim, result);
  }

  @Test
  public void testGetDimensionTransformingKeyReturnsNullWhenNotFound() {
    SequenceDimension result = instance.getDimensionTransformingKey("inpnonExistentColumn");
    assertNull(result);
  }

  @Test
  public void testGetDimensionTransformingOriginalColumn() {
    SequenceDimension dim = new ObjenesisStd().newInstance(SequenceDimension.class);
    String transformedKey = instance.transformKey("AD_Client_ID");
    dimensionMap.put(transformedKey, dim);

    // getDimensionTransformingOriginalColumn transforms the original column format before lookup
    SequenceDimension result = instance.getDimensionTransformingOriginalColumn("AD_Client_ID");
    assertEquals(dim, result);
  }

  @Test
  public void testGetDimensionTransformingOriginalColumnReturnsNullWhenNotFound() {
    SequenceDimension result = instance.getDimensionTransformingOriginalColumn("Nonexistent_Col");
    assertNull(result);
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
