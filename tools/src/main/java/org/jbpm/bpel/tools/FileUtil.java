/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A collection of utility methods on {@linkplain File files}.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2008/02/05 05:37:34 $
 */
public class FileUtil {

  // suppress default constructor, ensuring non-instantiability
  private FileUtil() {
  }

  /**
   * The default temporary file path, provided by the <code>java.io.tmpdir</code> system property.
   */
  public static final File TEMPORARY_DIRECTORY = new File(System.getProperty("java.io.tmpdir"));

  /**
   * The user's current working directory, provided by the <code>user.dir</code> system property.
   */
  public static final File CURRENT_DIRECTORY = new File(System.getProperty("user.dir"));

  /**
   * Returns the default charset of this virtual machine. The default charset is determined by
   * examining the <code>file.encoding</code> system property. If there is no such property, the
   * actual mechanism used to obtain the default charset is unspecified.
   */
  public static final Charset DEFAULT_CHARSET = getDefaultCharset();

  private static Charset getDefaultCharset() {
    String charsetName;
    try {
      // examine the de facto system property
      charsetName = System.getProperty("file.encoding");
    }
    catch (SecurityException e) {
      charsetName = null;
    }
    if (charsetName == null) {
      // take the long route
      charsetName = new InputStreamReader(System.in).getEncoding();
    }
    return Charset.forName(charsetName);
  }

  /**
   * Copies data from one file to another.
   * @param toFile the file to which data will be written
   * @param fromFile the file from which data will be read
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File toFile, File fromFile) throws IOException {
    FileChannel source = new FileInputStream(fromFile).getChannel();
    try {
      FileChannel sink = new FileOutputStream(toFile).getChannel();
      try {
        for (long position = 0, size = source.size(); position < size;)
          position += source.transferTo(position, size - position, sink);
      }
      finally {
        sink.close();
      }
    }
    finally {
      source.close();
    }
  }

  /**
   * Copies files that satisfy the specified filter from one directory to another.
   * @param toDir the directory to which files will be written
   * @param fromDir the directory from which files will be read
   * @param filter a file filter
   * @throws IOException if an I/O error occurs
   */
  public static void copy(File toDir, File fromDir, FileFilter filter) throws IOException {
    if (!toDir.exists() && !toDir.mkdir())
      throw new IOException("directory making failed: " + toDir);

    File[] files = fromDir.listFiles(filter);
    if (files == null)
      throw new IOException("file listing failed: " + fromDir);

    // copy files in the directory
    for (int i = 0; i < files.length; i++) {
      File fromFile = files[i];
      File toFile = new File(toDir, fromFile.getName());

      if (fromFile.isDirectory())
        copy(toFile, fromFile, filter);
      else
        copy(toFile, fromFile);
    }
  }

  /**
   * Cleans the given file or directory from the file system. If <code>path</code> is a directory,
   * then the files it contains are recursively deleted. After deleting <code>path</code>, an
   * attempt is made to purge empty parent directories.
   * @param path the file or directory to clean
   * @return <code>true</code> if and only if <code>path</code> is successfully deleted;
   * <code>false</code> otherwise
   */
  public static boolean clean(File path) {
    // delete any child files
    if (path.isDirectory())
      deleteFiles(path);

    // delete target
    if (!path.delete())
      return false;

    // purge empty parent directories
    for (File parentDir = path.getParentFile(); parentDir != null; parentDir = parentDir.getParentFile()) {
      if (!parentDir.delete())
        break;
    }

    return true;
  }

  private static void deleteFiles(File dir) {
    File[] files = dir.listFiles();
    if (files == null)
      return; // I/O error

    // delete files in the directory
    for (int i = 0; i < files.length; i++) {
      File file = files[i];

      // delete any child files
      if (file.isDirectory())
        deleteFiles(file);

      file.delete();
    }
  }

  /**
   * Adds the contents of the given file or directory to the zip stream. If <code>path</code> is a
   * directory, then the files it contains are recursively zipped, preserving the relative path
   * hierarchy. The given prefix is prepended to the name of the file or directory to construct the
   * zip entry name.
   * @param path the file or directory to zip
   * @param sink the stream where data is written
   * @param prefix the zip entry prefix
   * @throws IOException if an I/O error occurs
   */
  public static void zip(File path, ZipOutputStream sink, String prefix) throws IOException {
    byte[] buffer = new byte[512];

    if (path.isDirectory())
      zipDirectory(path, sink, prefix, buffer);
    else
      zipFile(path, sink, prefix, buffer);
  }

  /**
   * Recursively adds the contents of the given directory to the zip stream. The given prefix is
   * prepended to the directory name.
   * @param dir the directory to zip
   * @param sink the stream where data is written
   * @param prefix the zip entry prefix
   * @param buffer a byte array for efficient data transfer
   * @throws IOException if an I/O error occurs
   */
  private static void zipDirectory(File dir, ZipOutputStream sink, String prefix, byte[] buffer)
      throws IOException {
    File[] files = dir.listFiles();
    if (files == null)
      throw new IOException("file listing failed: " + dir);

    // add files in the directory
    for (int i = 0; i < files.length; i++) {
      File file = files[i];

      if (file.isDirectory())
        zipDirectory(file, sink, prefix + WebModuleBuilder.SEPARATOR + file.getName(), buffer);
      else
        zipFile(file, sink, prefix, buffer);
    }
  }

  /**
   * Adds the content of the given file to the zip stream. The given prefix is prepended to the file
   * name.
   * @param file the file to zip
   * @param sink the stream where data is written
   * @param prefix the zip entry prefix
   * @param buffer a byte array for efficient data transfer
   * @throws IOException if an I/O error occurs
   */
  private static void zipFile(File file, ZipOutputStream sink, String prefix, byte[] buffer)
      throws IOException {
    InputStream fileSource = new FileInputStream(file);
    try {
      sink.putNextEntry(new ZipEntry(prefix + WebModuleBuilder.SEPARATOR + file.getName()));

      // transfer bytes from the file to the archive using the given buffer
      for (int length; (length = fileSource.read(buffer)) != -1;)
        sink.write(buffer, 0, length);

      sink.closeEntry();
    }
    finally {
      fileSource.close();
    }
  }

  /**
   * Creates an empty directory in the default temporary-file directory, using the given prefix and
   * suffix to generate its name.
   * @param prefix the prefix to be used in generating the directory's name; must be at least three
   * characters long
   * @param suffix the suffix to be used in generating the directory's name; may be
   * <code>null</code>, in which case the suffix <code>.tmp</code> will be used
   * @return an abstract pathname denoting a newly-created empty directory
   * @throws IOException if the directory could not be created
   */
  public static File makeTempDirectory(String prefix, String suffix) throws IOException {
    File dir = File.createTempFile(prefix, suffix);

    // delete temp file
    if (!dir.delete())
      throw new IOException("could not delete temporary file: " + dir);

    // make temp directory
    if (!dir.mkdir())
      throw new IOException("could not make temporary directory: " + dir);

    return dir;
  }

  /**
   * Converts the URL of the specified resource into an abstract pathname.
   * @param c class to be used to find the resource
   * @param resource name of the desired resource
   * @return the abstract pathname denoted by the specified resource
   * @throws IllegalArgumentException if the URL of the specified resource does not adhere to the
   * <code>file</code> scheme
   */
  public static File toFile(Class c, String resource) {
    URL resourceUrl = c.getResource(resource);
    if (resourceUrl == null)
      return null;
    try {
      return new File(new URI(resourceUrl.toExternalForm()));
    }
    catch (URISyntaxException e) {
      // should not happen
      throw new AssertionError(e);
    }
  }
}
