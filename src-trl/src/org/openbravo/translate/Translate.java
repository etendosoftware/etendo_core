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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.translate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.xerces.parsers.SAXParser;
import org.openbravo.database.CPStandAlone;
import org.openbravo.database.SessionInfo;
import org.openbravo.exception.NoConnectionAvailableException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Looks for translatable elements in html, fo, srpt and jrxml files inserting them in
 * ad_textinterfaces table.
 * 
 * @author Fernando Iriazabal
 **/
public class Translate extends DefaultHandler {
  private static CPStandAlone pool;
  private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
  private static final List<String> translatableExtensions = Arrays.asList("html", "fo", "srpt",
      "jrxml");
  private static final List<String> EXCLUDED_TAGS = Arrays.asList("script", "style");
  private static final Logger log = LogManager.getLogger();

  private XMLReader parser;
  private String extension;

  private String actualTag;
  private String actualFile;
  private String actualPrefix;
  private StringBuilder translationText;
  private int count = 0;

  private String moduleLang = "";
  private String moduleID = "";
  private Path moduleBasePath;
  private Connection conn;
  private boolean canTranslateModule = true;

  private static Map<String, List<TranslateData>> allLabels;
  private static HashSet<String> modsInDev = new HashSet<>();

  private static void init(String xmlPoolFile) {
    pool = new CPStandAlone(xmlPoolFile);
  }

  private Translate() {
  }

  /**
   * @param _fileTermination
   *          File extension to filter.
   */
  public Translate(String _fileTermination) throws ServletException {
    extension = _fileTermination;
    boolean isHtml = extension.toLowerCase().endsWith("html");
    if (isHtml) {
      parser = new org.cyberneko.html.parsers.SAXParser();
    } else {
      parser = new SAXParser();
    }
    parser.setEntityResolver(new LocalEntityResolver());
    parser.setContentHandler(this);
  }

  /**
   * Command Line method.
   * 
   * @param argv
   *          List of arguments. There is 2 call ways, with 2 arguments; the first one is the
   *          attribute to indicate if the AD_TEXTINTERFACES must be cleaned ("clean") and the
   *          second one is the Openbravo.properties path. The other way is with more arguments,
   *          where: 0- Openbravo.properties path. 1- Path where are the files to translate.
   */
  public static void main(String argv[]) throws Exception {
    if (argv.length != 2) {
      log.error("Usage: Translate Openbravo.properties [clean|remove|sourceDir]");
      log.error("Received: " + Arrays.asList(argv));
      return;
    }

    init(argv[0]);
    Translate translate;
    switch (argv[1]) {
    case "clean":
      log.debug("clean AD_TEXTINTERFACES");
      translate = new Translate();
      translate.clean();
      return;
    case "remove":
      log.debug("remove AD_TEXTINTERFACES");
      translate = new Translate();
      translate.remove();
      return;
    }

    Path obPath = Paths.get(argv[1]).normalize();

    TranslateData[] mods = TranslateData.getModulesInDevelopment(pool);
    for (TranslateData mod : mods) {
      modsInDev.add(mod.id);
    }
    for (TranslateData mod : mods) {
      Path path;
      if ("0".equals(mod.id)) {
        path = obPath.resolve("src");
      } else {
        path = obPath.resolve(Paths.get("modules", mod.javapackage, "src"));
        if (!Files.exists(path)) {
          continue;
        }
      }
      log.info("Looking for translatable elements in " + mod.javapackage + " (" + mod.name + ")");

      for (String extension : translatableExtensions) {
        translate = new Translate(extension);
        translate.moduleLang = mod.lang;
        translate.moduleID = mod.id;
        translate.moduleBasePath = path;

        translate.execute();
        if (translate.count > 0) {
          log.info("  parsed " + translate.count + " " + extension + " files");
        }

        if (!translate.canTranslateModule) {
          break;
        }
      }
    }

    if (mods.length == 0) {
      log.info("No modules in development to look for translatable elements.");
    }
    destroy();
  }

  /**
   * Executes the clean of the AD_TEXTINTERFACES table.
   */
  private void clean() {
    try {
      TranslateData.clean(pool);
    } catch (final Exception e) {
      log.error("clean error", e);
    }
  }

  private void remove() {
    try {
      int n = TranslateData.remove(pool);
      if (n > 0) {
        log.info("Removed " + n + " unused elements");
      }
    } catch (final Exception e) {
      log.error("remove error", e);
    }
  }

  /** Looks for translatable elements for a given module and extension */
  private void execute() throws IOException {
    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:.*\\." + extension);
    Files.walkFileTree(moduleBasePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (matcher.matches(file)) {
          if (!canTranslateModule()) {
            log.error("  Module is not set as translatable or has not defined language, but has translatable elements");
            log.error("  No translations will be inserted. Set the module as 'is translation requiered' and select a language");
            log.error("  then execute translation again.");
            canTranslateModule = false;
            return FileVisitResult.TERMINATE;
          }
          parseFile(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });

    if (conn != null) {
      try {
        pool.releaseCommitConnection(conn);
      } catch (SQLException e) {
        log.error("Error commiting changes to database", e);
      }
    }
  }

  private boolean canTranslateModule() {
    return !(moduleLang == null || moduleLang.equals(""));
  }

  /**
   * Parse each file searching the text to translate.
   * 
   * @param fileParsing
   *          File to parse.
   */
  private void parseFile(Path fileParsing) {
    actualFile = "/" + moduleBasePath.relativize(fileParsing).toString().replace("\\", "/");

    log.debug("File: " + actualFile);

    try (InputStream in = Files.newInputStream(fileParsing);
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      parser.parse(new InputSource(reader));
      count++;
    } catch (final Exception e) {
      log.error("file: " + actualFile, e);
    }
  }

  /**
   * Parse each attribute of each element in the file. This method decides which ones must been
   * translated.
   * 
   * @param amap
   *          Attributes of the element.
   */
  private void parseAttributes(Attributes amap) {
    String type = "";
    String value = "";
    for (int i = 0; i < amap.getLength(); i++) {
      String strAux = amap.getValue(i);
      if (amap.getQName(i).equalsIgnoreCase("type")) {
        type = strAux;
      } else if (amap.getQName(i).equalsIgnoreCase("value")) {
        value = strAux;
      } else if (amap.getQName(i).equalsIgnoreCase("onMouseOver")) {
        if (strAux.toLowerCase().startsWith("window.status='")) {
          int j = strAux.lastIndexOf("';");
          int aux = j;
          while ((j != -1) && (aux = strAux.lastIndexOf("';", j - 1)) != -1) {
            j = aux;
          }
          translate(strAux.substring(15, j));
        }
      } else if (amap.getQName(i).equalsIgnoreCase("alt")) {
        translate(strAux);
      } else if (amap.getQName(i).equalsIgnoreCase("title")) {
        translate(strAux);
      }
    }
    if (value != null && !value.equals("") && type.equalsIgnoreCase("button")) {
      translate(value);
    }
  }

  /**
   * The start of the document to translate.
   */
  @Override
  public void startDocument() {
  }

  /**
   * The prefix mapping for the file.
   */
  @Override
  public void startPrefixMapping(String prefix, String uri) {
    actualPrefix = " xmlns:" + prefix + "=\"" + uri + "\"";
  }

  /**
   * Method to know if a specific element in the file is parseable or not.
   * 
   * @param tagname
   *          Name of the element.
   * @return True if the element is parseable, false if not.
   */
  private boolean isParseable(String tagname) {
    String tag = tagname.toLowerCase();
    if (EXCLUDED_TAGS.contains(tag)) {
      return false;
    } else if (extension.equalsIgnoreCase("jrxml") && !tag.equals("text")
        && !tag.equals("textfieldexpression")) {
      return false;
    }
    return true;
  }

  /**
   * Start of an element of the file. When the parser finds a new element in the file, it calls to
   * this method.
   */
  @Override
  public void startElement(String uri, String name, String qName, Attributes amap) {
    log.debug("startElement element name=" + qName + " actualtag" + actualTag + " trlTxt"
        + translationText);
    if (actualTag != null && isParseable(actualTag) && translationText != null) {
      translate(translationText.toString());
    }
    translationText = null;
    parseAttributes(amap);
    if (actualPrefix != null && !actualPrefix.equals("")) {
      actualPrefix = "";
    }
    actualTag = name.trim().toUpperCase();
  }

  /**
   * End of an element of the file. When the parser finds the end of an element in the file, it
   * calls to this method.
   */
  @Override
  public void endElement(String uri, String name, String qName) {
    log.debug("endElement : " + qName);

    if (isParseable(actualTag) && translationText != null) {
      translate(translationText.toString());
    }
    translationText = null;
    actualTag = "";
  }

  /**
   * This method is called by the parser when it finds any content between the start and end
   * element's tags.
   */
  @Override
  public void characters(char[] ch, int start, int length) {
    final String chars = new String(ch, start, length);
    log.debug("characters: " + chars);
    if (translationText == null) {
      translationText = new StringBuilder();
    }
    translationText.append(chars);
  }

  /**
   * This method is the one in charge of the translation of the found text.
   * 
   * @param txt
   *          String with the text to translate.
   */
  private void translate(String txt) {
    translate(txt, false);
  }

  /**
   * This method is the one in charge of the translation of the found text.
   * 
   * @param input
   *          String with the text to translate.
   * @param isPartial
   *          Indicates if the text passed is partial text or the complete one found in the element
   *          content.
   */
  private void translate(String input, boolean isPartial) {
    String txt = input.replace("\r", "").replace("\n", " ").replaceAll(" ( )+", " ").trim();

    if (!isPartial && actualTag.equalsIgnoreCase("textFieldExpression")) {
      int pos = txt.indexOf("\"");
      while (pos != -1) {
        txt = txt.substring(pos + 1);
        pos = txt.indexOf("\"");
        if (pos != -1) {
          translate(txt.substring(0, pos), true);
          txt = txt.substring(pos + 1);
        } else {
          break;
        }
        pos = txt.indexOf("\"");
      }
      return;
    }
    boolean translatableTxt = !(txt.equals("") || txt.toLowerCase().startsWith("xx") || isNumeric(txt));
    if (!translatableTxt) {
      return;
    }

    log.debug("Checking [" + txt + "] from file" + actualFile + " - language: " + moduleLang);
    TranslateData t = getLabel(txt);
    if (t != null) {
      if ("N".equals(t.tr) && t.module.equals(moduleID) && t.id != null) {
        try {
          TranslateData.update(getConnection(), pool, t.id);
          t.tr = "Y";
        } catch (ServletException e) {
          log.error("Could not set label as used [" + txt + "]");
        }
      }
    } else {
      createLabel(txt);
    }
  }

  /** Returns a suitable text interface element for the given txt or null if there is none. */
  private TranslateData getLabel(String txt) {
    if (allLabels == null) {
      // lazy initialization: in case none of the modules in development has elements to translate
      // it won't be initialized
      initializeAllLabels();
    }

    List<TranslateData> labels = allLabels.get(txt);
    if (labels == null) {
      return null;
    }

    TranslateData selected = null;
    for (TranslateData label : labels) {
      if (!moduleLang.equals(label.lang)) {
        continue;
      }
      if (actualFile.equals(label.filename)) {
        return label;
      } else if (label.filename == null || "".equals(label.filename)) {
        selected = label;
      }
    }
    return selected;
  }

  /**
   * Loads in memory all text interfaces. Typically this is faster than querying them each time a
   * translatable element is found in a file by reducing significantly the number of queries.
   */
  private void initializeAllLabels() {
    TranslateData[] all;
    try {
      all = TranslateData.getTextInterfaces(pool);
    } catch (ServletException e) {
      throw new RuntimeException("Error loading text interfaces from DB", e);
    }
    allLabels = new HashMap<>(all.length);
    String currentTxt = null;
    List<TranslateData> currentList = null;
    for (TranslateData label : all) {
      if (!label.text.equals(currentTxt)) {
        currentTxt = label.text;
        currentList = new ArrayList<>();
        allLabels.put(currentTxt, currentList);
      }
      currentList.add(label);
    }
  }

  private void createLabel(String txt) {
    try {
      TranslateData.insert(getConnection(), pool, txt, actualFile, moduleID);
      log.info("    New label found [" + txt + "] in " + actualFile);
    } catch (ServletException e) {
      log.error("   Could not insert label [" + txt + "]", e);
    }

    List<TranslateData> labelsForTxt = allLabels.get(txt);
    if (labelsForTxt == null) {
      labelsForTxt = new ArrayList<>();
      allLabels.put(txt, labelsForTxt);
    }

    TranslateData newElement = new TranslateData();
    newElement.text = txt;
    newElement.filename = actualFile;
    newElement.lang = moduleLang;

    labelsForTxt.add(newElement);
  }

  private Connection getConnection() {
    if (conn == null) {
      try {
        conn = pool.getTransactionConnection();
      } catch (NoConnectionAvailableException | SQLException e) {
        throw new RuntimeException("Could not get a DB connection", e);
      }
    }
    return conn;
  }

  /**
   * To know if a text is numeric or not.
   * 
   * @param ini
   *          String with the text.
   * @return True if has no letter in the text or false if has any letter.
   */
  private static boolean isNumeric(String ini) {
    return !LETTER_PATTERN.matcher(ini).find();
  }

  /** Closes database connection. */
  private static void destroy() {
    // remove cached connection from thread local
    SessionInfo.init();
    pool.destroy();
  }
}
