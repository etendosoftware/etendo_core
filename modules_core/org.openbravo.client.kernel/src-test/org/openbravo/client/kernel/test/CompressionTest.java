/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2009-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.kernel.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openbravo.client.kernel.JSCompressor;
import org.openbravo.test.base.Issue;

/**
 * Test the compression of a static js file.
 * 
 * @author mtaal
 */

public class CompressionTest {

  @Test
  public void testCompression() throws IOException {
    CompressedResource compressedResource = new CompressedResource("test-compression.js");

    assertThat("Original size is at least twice bigger than original",
        compressedResource.original.length(),
        greaterThan(2 * compressedResource.compressed.length()));
  }

  @Test
  @Issue("42475")
  public void compressedTemplateLiteralSpaces() throws IOException {
    CompressedResource compressedResource = new CompressedResource(
        "test-compession-spaces-template-literal.js");

    assertThat(compressedResource.compressed,
        containsString("`this is a \"string literal\" with spaces`"));
  }

  @Test
  @Issue("42475")
  public void compressedTemplateLiteralMultiLine() throws IOException {
    CompressedResource compressedResource = new CompressedResource(
        "test-compession-multiline-template-literal.js");

    assertThat("Number of lines after compression", compressedResource.getNumberOfLines(), is(8));
  }

  private static class CompressedResource {
    private String original;
    private String compressed;

    CompressedResource(String resource) throws IOException {
      try (
          InputStreamReader is = new InputStreamReader(
              this.getClass().getResourceAsStream(resource));
          BufferedReader reader = new BufferedReader(is)) {
        original = reader.lines().collect(Collectors.joining("\n"));
        compressed = new JSCompressor().compress(original);
      }
    }

    public int getNumberOfLines() {
      return compressed.split("\n").length - 1;
    }
  }
}
