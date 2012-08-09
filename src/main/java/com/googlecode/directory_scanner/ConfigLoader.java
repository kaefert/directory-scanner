package com.googlecode.directory_scanner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ConfigLoader {

    private Logger logger;
    private Properties config;

    public ConfigLoader(Logger logger) {
	this.logger = logger;
    }

    public Properties getConfig() {
	if (config == null) {
	    config = loadConfig();
	}
	return config;
    }

    private String[] propertyPaths = new String[] { "./DirectoryScanner.properties", "/DirectoryScanner.properties", "DirectoryScanner.properties" };

    private Properties loadConfig() {

	Properties config = null;

	for (String path : propertyPaths) {
	    config = loadConfigFile(path, false);
	    if (config != null) {
		return config;
	    }
	}

	for (String path : propertyPaths) {
	    config = loadConfigFile(path, true);
	    if (config != null) {
		return config;
	    }
	}

	logger.log(Level.FATAL,
	"Could not load properties! something is seriously wrong here! (cannot connect to database without properties, therefore cannot do anything at all)");
	return null;
    }

    private Properties loadConfigFile(String path, boolean useClassloader) {

	InputStream inputStream = null;
	try {

	    if (useClassloader)
		inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
	    else
		inputStream = new FileInputStream(path);

	    if (inputStream != null) {
		Properties configFile = new Properties();
		configFile.load(inputStream);
		logger.log(Level.INFO, "successfully loaded properties from path=\"" + path + "\" & useClassloader=" + useClassloader);
		return configFile;
	    }
	} catch (FileNotFoundException e) {
	    logger.warn("FileNotFoundException for path=" + path, e);
	} catch (IOException e) {
	    logger.warn("IOException for path=" + path, e);
	} finally {
	    try {
		if (inputStream != null)
		    inputStream.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

	return null;
    }
}
