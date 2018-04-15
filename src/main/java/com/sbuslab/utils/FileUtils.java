package com.sbuslab.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;


public class FileUtils {

    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * Returns file by its location
     * <ul>
     * <li>classpath:location</li>
     * <li>URL</li>
     * <li>system path</li>
     * </ul>
     *
     * @param location the location
     * @return {@code File} instance
     * @throws FileNotFoundException
     * @throws IllegalArgumentException if location is in classpath and the file has not been found
     */
    public static URL getFileUrl(String location) throws FileNotFoundException {
        URL url = null;
        if (location.startsWith(CLASSPATH_PREFIX)) {
            location = location.substring(CLASSPATH_PREFIX.length());

            if (location.startsWith(File.separator)) {
                location = location.substring(1);
            }
            url = getDefaultClassLoader().getResource(location);
        } else if (location.startsWith(File.separator)
                || (System.getProperty("os.name").contains("Windows") && location.charAt(1) == ':')) {
            try {
                url = new URL(location);
            } catch (MalformedURLException e) {
                // No URL -> resolve as resource path.
                try {
                    return new File(location).toURI().toURL();

                } catch (MalformedURLException ex) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
        } else {
            url = getDefaultClassLoader().getResource(location);
        }

        if (url == null) {
            throw new FileNotFoundException("File does not exist: " + location);
        }
        return url;
    }

    public static boolean isFileCompletelyWritten(File file) {
        RandomAccessFile stream = null;
        try {
            stream = new RandomAccessFile(file, "rw");
            return true;
        } catch (Exception ignored) {
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Returns a class loader but never null.
     *
     * @return the class loader
     */
    private static ClassLoader getDefaultClassLoader() {

        try {
            return Thread.currentThread().getContextClassLoader();

        } catch (Exception ex) {
            // Cannot access thread context ClassLoader - falling back to system one
            return FileUtils.class.getClassLoader();
        }
    }
}
