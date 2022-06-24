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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.openbravo.client.kernel.JSCompressor;

/**
 * Ant task that minifies JavaScript using JSMin
 */
public class JSMinifyTask extends Task {

  static final Logger log = LogManager.getLogger();

  private List<FileSet> filesets = new ArrayList<>();
  private String outputDir;

  public void addFileset(FileSet fileset) {
    filesets.add(fileset);
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  @Override
  public void execute() throws BuildException {
    verifyParameters();
    for (FileSet fileSet : filesets) {
      DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
      String dir = scanner.getBasedir().getPath();
      String[] fileNames = scanner.getIncludedFiles();
      int totalMinifiedFiles = 0;
      for (String fileName : fileNames) {
        try {
          if (fileName.endsWith(".js")) {
            double compressionRatio = minifyJSFile(Path.of(dir, fileName),
                Path.of(outputDir, fileName));
            log.debug("File {} minified successfully. Compression ratio {}% of original.", fileName,
                String.format("%.2f", compressionRatio));
            totalMinifiedFiles++;
          }
        } catch (IOException ex) {
          log.error("Failed to minify file: {}", fileName, ex);
        }
      }
      log.info("Minified {} files from {} directory to {} directory", totalMinifiedFiles,
          fileSet.getDir().getPath(), outputDir);
    }
  }

  private void verifyParameters() {
    String errorMsg = "";

    if (outputDir == null || "".equals(outputDir)) {
      errorMsg += "Output directory is not specified\n";
    }

    if (!"".equals(errorMsg)) {
      throw new BuildException(errorMsg);
    }
  }

  /**
   * Minifies a JS file from source to destination
   * 
   * @param source
   *          Path to source input file
   * @param dest
   *          Path to destination output file
   * @return CompressionRatio if successful, 0 otherwise
   * @throws IOException
   *           If it fails to read/write from/to input/output files
   */
  private double minifyJSFile(Path source, Path dest) throws IOException {
    double compressionRatio = 0;
    try {
      String fileContent = Files.readString(source);
      String minifiedContent = JSCompressor.getInstance().compress(fileContent);
      Files.writeString(dest, minifiedContent);
      if (minifiedContent.length() != 0) {
        compressionRatio = minifiedContent.length() / (double) fileContent.length() * 100;
      }
    } catch (IOException ex) {
      throw new IOException("Failed to read/write JS file" + source, ex);
    }
    return compressionRatio;
  }
}
