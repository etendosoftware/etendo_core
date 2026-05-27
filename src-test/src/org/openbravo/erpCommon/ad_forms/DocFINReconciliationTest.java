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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.erpCommon.utility.FieldProviderFactory;

/**
 * Tests for {@link DocFINReconciliation}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class DocFINReconciliationTest {

  private DocFINReconciliation instance;

  @Mock
  private FieldProviderFactory mockFpf1;

  @Mock
  private FieldProviderFactory mockFpf2;

  @Mock
  private FieldProviderFactory mockFpf3;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(DocFINReconciliation.class);

    setFieldValue(instance, "ZERO", BigDecimal.ZERO);
    setFieldValue(instance, "SeqNo", "0");

    String[] amounts = new String[10];
    for (int i = 0; i < amounts.length; i++) {
      amounts[i] = "0";
    }
    setFieldValue(instance, "Amounts", amounts);
    setFieldValue(instance, "p_lines", new DocLine[0]);
  }
  /** Constants. */

  @Test
  public void testConstantTrxTypeBPDeposit() {
    assertEquals("BPD", DocFINReconciliation.TRXTYPE_BPDeposit);
  }

  @Test
  public void testConstantTrxTypeBPWithdrawal() {
    assertEquals("BPW", DocFINReconciliation.TRXTYPE_BPWithdrawal);
  }

  @Test
  public void testConstantTrxTypeBankFee() {
    assertEquals("BF", DocFINReconciliation.TRXTYPE_BankFee);
  }
  /** Default constructor. */

  @Test
  public void testDefaultConstructor() {
    DocFINReconciliation doc = new DocFINReconciliation();
    assertNotNull(doc);
  }
  /** Add both non-null arrays. */

  @Test
  public void testAddBothNonNull() {
    FieldProviderFactory[] first = new FieldProviderFactory[] { mockFpf1 };
    FieldProviderFactory[] second = new FieldProviderFactory[] { mockFpf2, mockFpf3 };

    FieldProviderFactory[] result = instance.add(first, second);

    assertEquals(3, result.length);
    assertSame(mockFpf1, result[0]);
    assertSame(mockFpf2, result[1]);
    assertSame(mockFpf3, result[2]);
  }
  /** Add first null. */

  @Test
  public void testAddFirstNull() {
    FieldProviderFactory[] second = new FieldProviderFactory[] { mockFpf1, mockFpf2 };

    FieldProviderFactory[] result = instance.add(null, second);

    assertSame(second, result);
  }
  /** Add second null. */

  @Test
  public void testAddSecondNull() {
    FieldProviderFactory[] first = new FieldProviderFactory[] { mockFpf1 };

    FieldProviderFactory[] result = instance.add(first, null);

    assertSame(first, result);
  }
  /** Add both null. */

  @Test
  public void testAddBothNull() {
    FieldProviderFactory[] result = instance.add(null, null);

    assertNull(result);
  }
  /** Add with null elements in arrays. */

  @Test
  public void testAddWithNullElements() {
    FieldProviderFactory[] first = new FieldProviderFactory[] { mockFpf1, null };
    FieldProviderFactory[] second = new FieldProviderFactory[] { null, mockFpf2 };

    FieldProviderFactory[] result = instance.add(first, second);

    assertEquals(4, result.length);
    assertSame(mockFpf1, result[0]);
    assertNull(result[1]);
    assertNull(result[2]);
    assertSame(mockFpf2, result[3]);
  }
  /** Add empty arrays. */

  @Test
  public void testAddEmptyArrays() {
    FieldProviderFactory[] first = new FieldProviderFactory[0];
    FieldProviderFactory[] second = new FieldProviderFactory[0];

    FieldProviderFactory[] result = instance.add(first, second);

    assertNotNull(result);
    assertEquals(0, result.length);
  }
  /** SeqNo default value. */

  @Test
  public void testSeqNoDefaultValue() throws Exception {
    Field seqNoField = DocFINReconciliation.class.getDeclaredField("SeqNo");
    seqNoField.setAccessible(true);
    assertEquals("0", seqNoField.get(instance));
  }

  private static void setFieldValue(Object target, String fieldName, Object value)
      throws Exception {
    Field field = findField(target.getClass(), fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
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
