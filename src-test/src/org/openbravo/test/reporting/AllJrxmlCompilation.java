package org.openbravo.test.reporting;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalContextListener;

import jakarta.servlet.ServletContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 * Compiles all jrxml templates present in the sources directory ensuring they can be compiled with
 * the current combination of jdk + jasperreports.
 *
 * Adapted for JasperReports 7.x which uses Jackson XML parser.
 */
public class AllJrxmlCompilation extends WeldBaseTest {

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @BeforeEach
  public void setupServletContext() {
    ServletContext ctx = createMockedServletContext();
    DalContextListener.setServletContext(ctx);
    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
  }

  private ServletContext createMockedServletContext() {
    ServletContext ctx = mock(ServletContext.class);

    String basePath = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("source.path");

    when(ctx.getRealPath(anyString())).thenAnswer(invocation -> {
      String path = invocation.getArgument(0);
      if ("/".equals(path)) {
        return basePath;
      }
      return Paths.get(basePath, path).toString();
    });

    when(ctx.getInitParameter(eq("BaseConfigPath"))).thenReturn("config");
    when(ctx.getInitParameter(eq("BaseDesignPath"))).thenReturn("src");
    when(ctx.getInitParameter(eq("DefaultDesignPath")))
        .thenReturn("org/openbravo/erpCommon/reportCache");
    when(ctx.getInitParameter(eq("AttachmentDirectory"))).thenReturn(basePath + "/attachments");

    return ctx;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("parameters")
  public void jrxmlShouldCompile(Path report) throws JRException, IOException {
    if (DalContextListener.getServletContext() == null) {
      setupServletContext();
    }

    ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

      File file = report.toFile();
      if (!file.exists()) {
        throw new JRException(report + " (No such jasper template file)");
      }
      String xmlContent = Files.readString(file.toPath(), StandardCharsets.UTF_8);

      try (InputStream is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
        JasperDesign jasperDesign = JRXmlLoader.load(is);
        JasperCompileManager.compileReport(jasperDesign);
      }

    } catch (JRException e) {
      throw new JRException("Failed to compile: " + report.getFileName(), e);
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  private static Stream<Path> parameters() throws IOException {
    final List<Path> allJasperFiles = new ArrayList<>();
    allJasperFiles.addAll(getJrxmlTemplates("src"));
    allJasperFiles.addAll(getJrxmlTemplates("modules"));
    return allJasperFiles.stream();
  }

  private static Collection<Path> getJrxmlTemplates(String dir) throws IOException {
    final Collection<Path> allJasperFiles = new ArrayList<>();

    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:.*\\.jrxml");
    Path basePath = Paths.get(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path"),
        dir);
    Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (matcher.matches(file)) {
          allJasperFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return allJasperFiles;
  }
}
