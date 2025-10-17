package org.openbravo.dal.service;

import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Helper para migrar de FileItem (Apache Commons FileUpload) a Part (Jakarta Servlet).
 * Proporciona métodos de compatibilidad para el código existente.
 */
public class FileItemHelper {
  
  /**
   * Obtiene el contenido del Part como array de bytes.
   * Reemplaza FileItem.get()
   */
  public static byte[] getBytes(Part part) throws IOException {
    if (part == null) {
      return new byte[0];
    }
    
    try (InputStream is = part.getInputStream();
         ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    }
  }
  
  /**
   * Obtiene el InputStream del Part.
   * Reemplaza FileItem.getInputStream()
   */
  public static InputStream getInputStream(Part part) throws IOException {
    return part != null ? part.getInputStream() : new ByteArrayInputStream(new byte[0]);
  }
  
  /**
   * Obtiene el nombre del archivo del Part.
   * Reemplaza FileItem.getName()
   */
  public static String getFileName(Part part) {
    if (part == null) {
      return null;
    }
    
    String contentDisposition = part.getHeader("content-disposition");
    if (contentDisposition != null) {
      String[] tokens = contentDisposition.split(";");
      for (String token : tokens) {
        if (token.trim().startsWith("filename")) {
          String filename = token.substring(token.indexOf('=') + 1).trim();
          return filename.replaceAll("\"", "");
        }
      }
    }
    return null;
  }
  
  /**
   * Obtiene el tamaño del Part.
   * Reemplaza FileItem.getSize()
   */
  public static long getSize(Part part) {
    return part != null ? part.getSize() : 0;
  }
  
  /**
   * Escribe el contenido del Part a un archivo.
   * Reemplaza FileItem.write(File)
   */
  public static void write(Part part, File file) throws IOException {
    if (part == null) {
      return;
    }
    
    try (InputStream is = part.getInputStream();
         FileOutputStream fos = new FileOutputStream(file)) {
      
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = is.read(buffer)) != -1) {
        fos.write(buffer, 0, bytesRead);
      }
    }
  }
  
  /**
   * Escribe el contenido del Part a un Path.
   * Para compatibility con nuevas APIs
   */
  public static void write(Part part, Path path) throws IOException {
    write(part, path.toFile());
  }
  
  /**
   * Verifica si el Part está vacío.
   * Reemplaza FileItem.getSize() == 0
   */
  public static boolean isEmpty(Part part) {
    return part == null || part.getSize() == 0;
  }
}