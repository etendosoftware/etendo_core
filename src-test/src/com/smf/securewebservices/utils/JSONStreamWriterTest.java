package com.smf.securewebservices.utils;

import static org.junit.Assert.assertEquals;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link JSONStreamWriter}.
 */
public class JSONStreamWriterTest {

    private JSONStreamWriter jsonStreamWriter;

    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        jsonStreamWriter = new JSONStreamWriter();
    }

    /**
     * Tests the write method to ensure it adds JSONObjects to the JSONArray.
     *
     * @throws Exception if an error occurs during execution
     */
    @Test
    public void testWriteAddsJSONObjectToJSONArray() throws Exception {
        // Given
        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("key1", "value1");

        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("key2", "value2");

        // When
        jsonStreamWriter.write(jsonObject1);
        jsonStreamWriter.write(jsonObject2);

        // Then
        JSONArray result = jsonStreamWriter.getJSONArray();
        assertEquals(2, result.length());
        assertEquals("value1", result.getJSONObject(0).getString("key1"));
        assertEquals("value2", result.getJSONObject(1).getString("key2"));
    }

    /**
     * Tests the getJSONArray method to ensure it returns an empty JSONArray initially.
     */
    @Test
    public void testGetJSONArrayReturnsEmptyJSONArrayInitially() {
        // When
        JSONArray result = jsonStreamWriter.getJSONArray();

        // Then
        assertEquals(0, result.length());
    }
}
