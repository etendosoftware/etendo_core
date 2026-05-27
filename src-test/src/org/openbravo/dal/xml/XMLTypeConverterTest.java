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
package org.openbravo.dal.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openbravo.base.exception.OBException;

/**
 * Unit tests for {@link XMLTypeConverter}.
 */
@DisplayName("XMLTypeConverter")
public class XMLTypeConverterTest {

  private XMLTypeConverter converter;
  private final SimpleDateFormat xmlDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd'T'HH:mm:ss.S'Z'");

  @BeforeEach
  void setUp() {
    converter = XMLTypeConverter.getInstance();
  }

  // ── getInstance ────────────────────────────────────────────────────

  @Test
  @DisplayName("getInstance returns non-null singleton")
  void getInstanceReturnsNonNull() {
    assertNotNull(XMLTypeConverter.getInstance());
  }

  // ── toXML ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("toXML")
  class ToXML {

    @Test
    @DisplayName("Date formats correctly")
    void dateFormatsCorrectly() throws Exception {
      Date date = xmlDateFormat.parse("2025-01-15T10:30:00.0Z");
      String result = converter.toXML(date);
      assertTrue(result.contains("2025"));
      assertTrue(result.contains("01"));
      assertTrue(result.contains("15"));
    }

    @Test
    @DisplayName("Number returns toString")
    void numberReturnsToString() {
      assertEquals("42", converter.toXML(Integer.valueOf(42)));
      assertEquals("3.14", converter.toXML(Double.valueOf(3.14)));
      assertEquals("100", converter.toXML(Long.valueOf(100L)));
    }

    @Test
    @DisplayName("String returns same string")
    void stringReturnsSame() {
      assertEquals("hello", converter.toXML("hello"));
      assertEquals("", converter.toXML(""));
    }

    @Test
    @DisplayName("Boolean returns true/false")
    void booleanReturnsString() {
      assertEquals("true", converter.toXML(Boolean.TRUE));
      assertEquals("false", converter.toXML(Boolean.FALSE));
    }

    @Test
    @DisplayName("null Object returns empty string")
    void nullReturnsEmpty() {
      assertEquals("", converter.toXML((Object) null));
    }

    @Test
    @DisplayName("Object dispatches to correct toXML")
    void objectDispatchesCorrectly() {
      assertEquals("42", converter.toXML((Object) Integer.valueOf(42)));
      assertEquals("test", converter.toXML((Object) "test"));
      assertEquals("true", converter.toXML((Object) Boolean.TRUE));
    }

    @Test
    @DisplayName("byte array encodes to Base64")
    void byteArrayEncodesToBase64() {
      byte[] data = "Hello".getBytes();
      String result = converter.toXML((Object) data);
      assertNotNull(result);
      assertTrue(result.length() > 0);
    }

    @Test
    @DisplayName("unknown Object falls back to toString")
    void unknownObjectUsesToString() {
      StringBuilder sb = new StringBuilder("test");
      assertEquals("test", converter.toXML((Object) sb));
    }
  }

  // ── fromXML ────────────────────────────────────────────────────────

  @Nested
  @DisplayName("fromXML")
  class FromXML {

    @Test
    @DisplayName("empty string returns null")
    void emptyReturnsNull() {
      assertNull(converter.fromXML(String.class, ""));
    }

    @Test
    @DisplayName("String class returns same string")
    void stringReturnsSame() {
      assertEquals("hello", converter.fromXML(String.class, "hello"));
    }

    @Test
    @DisplayName("BigDecimal parses correctly")
    void bigDecimalParses() {
      BigDecimal result = converter.fromXML(BigDecimal.class, "123.456");
      assertEquals(new BigDecimal("123.456"), result);
    }

    @Test
    @DisplayName("Long parses correctly")
    void longParses() {
      Long result = converter.fromXML(Long.class, "42");
      assertEquals(Long.valueOf(42L), result);
    }

    @Test
    @DisplayName("Boolean parses true")
    void booleanParsesTrue() {
      assertEquals(Boolean.TRUE, converter.fromXML(Boolean.class, "true"));
    }

    @Test
    @DisplayName("Boolean parses false")
    void booleanParsesFalse() {
      assertEquals(Boolean.FALSE, converter.fromXML(Boolean.class, "false"));
    }

    @Test
    @DisplayName("boolean primitive parses")
    void booleanPrimitiveParses() {
      assertEquals(Boolean.TRUE, converter.fromXML(boolean.class, "true"));
    }

    @Test
    @DisplayName("Float parses correctly")
    void floatParses() {
      Float result = converter.fromXML(Float.class, "3.14");
      assertEquals(Float.valueOf(3.14f), result);
    }

    @Test
    @DisplayName("Date parses xml format")
    void dateParses() {
      Date result = converter.fromXML(Date.class, "2025-01-15T10:30:00.0Z");
      assertNotNull(result);
    }

    @Test
    @DisplayName("Timestamp parses xml format")
    void timestampParses() {
      Timestamp result = converter.fromXML(Timestamp.class, "2025-01-15T10:30:00.0Z");
      assertNotNull(result);
    }

    @Test
    @DisplayName("byte array decodes Base64")
    void byteArrayDecodes() {
      String base64 = "SGVsbG8="; // "Hello" in Base64
      byte[] result = converter.fromXML(byte[].class, base64);
      assertArrayEquals("Hello".getBytes(), result);
    }

    @Test
    @DisplayName("invalid BigDecimal throws EntityXMLException")
    void invalidBigDecimalThrows() {
      assertThrows(EntityXMLException.class,
          () -> converter.fromXML(BigDecimal.class, "not-a-number"));
    }

    @Test
    @DisplayName("unsupported class throws EntityXMLException")
    void unsupportedClassThrows() {
      assertThrows(EntityXMLException.class,
          () -> converter.fromXML(Integer.class, "42"));
    }
  }

  // ── toXMLSchemaType ────────────────────────────────────────────────

  @Nested
  @DisplayName("toXMLSchemaType")
  class ToXMLSchemaType {

    @Test
    @DisplayName("Date returns ob:dateTime")
    void dateType() {
      assertEquals("ob:dateTime", converter.toXMLSchemaType(Date.class));
    }

    @Test
    @DisplayName("Timestamp returns ob:dateTime")
    void timestampType() {
      assertEquals("ob:dateTime", converter.toXMLSchemaType(Timestamp.class));
    }

    @Test
    @DisplayName("String returns ob:string")
    void stringType() {
      assertEquals("ob:string", converter.toXMLSchemaType(String.class));
    }

    @Test
    @DisplayName("BigDecimal returns ob:decimal")
    void bigDecimalType() {
      assertEquals("ob:decimal", converter.toXMLSchemaType(BigDecimal.class));
    }

    @Test
    @DisplayName("Long returns ob:long")
    void longType() {
      assertEquals("ob:long", converter.toXMLSchemaType(Long.class));
    }

    @Test
    @DisplayName("boolean returns ob:boolean")
    void booleanPrimitiveType() {
      assertEquals("ob:boolean", converter.toXMLSchemaType(boolean.class));
    }

    @Test
    @DisplayName("Boolean returns ob:boolean")
    void booleanType() {
      assertEquals("ob:boolean", converter.toXMLSchemaType(Boolean.class));
    }

    @Test
    @DisplayName("Float returns ob:float")
    void floatType() {
      assertEquals("ob:float", converter.toXMLSchemaType(Float.class));
    }

    @Test
    @DisplayName("byte[] returns ob:base64Binary")
    void byteArrayType() {
      assertEquals("ob:base64Binary", converter.toXMLSchemaType(byte[].class));
    }

    @Test
    @DisplayName("Object returns xs:anyType")
    void objectType() {
      assertEquals("xs:anyType", converter.toXMLSchemaType(Object.class));
    }

    @Test
    @DisplayName("null returns NULL")
    void nullType() {
      assertEquals("NULL", converter.toXMLSchemaType(null));
    }

    @Test
    @DisplayName("unsupported class throws OBException")
    void unsupportedThrows() {
      assertThrows(OBException.class,
          () -> converter.toXMLSchemaType(Integer.class));
    }
  }
}
