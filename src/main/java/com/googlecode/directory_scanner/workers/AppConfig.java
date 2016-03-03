package com.googlecode.directory_scanner.workers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class AppConfig extends Properties {

	/**
     * 
     */
	private static final long serialVersionUID = 2402741572495774673L;

	public static String getSha1HexString(byte[] sha1) {
		if (sha1 == null)
			return "NULL";

		String result = "";
		for (int i = 0; i < sha1.length; i++) {
			result += Integer.toString((sha1[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	private Logger logger;
	private String[] propertyPaths = new String[] { "./DirectoryScanner.properties", "/DirectoryScanner.properties",
			"DirectoryScanner.properties" };
	private Thread loader;
	private SimpleDateFormat dateFormatter;

	public AppConfig(Logger logger) {
		this.logger = logger;

		loader = new Thread(new Runnable() {
			@Override
			public void run() {
				loadConfig();
				dateFormatter = new SimpleDateFormat(AppConfig.super.getProperty("dateFormat"));
				loader = null;
			}
		});
		loader.start();
	}

	public SimpleDateFormat getDateFormatter() {
		finishLoading();
		return dateFormatter;
	}

	public int getQueueLength() {
		return Integer.valueOf(getProperty("queueLength"));
	}

	public Date getSkipDirectoriesDoneAfter() {
		String dateProp = getProperty("skipDirectoriesDoneAfter");
		Date returnValue = null;
		if (dateProp != null && dateProp.length() > 0) {
			try {
				returnValue = dateFormatter.parse(dateProp);
			} catch (ParseException e) {
				logger.warn("could not parse skipDirectoriesDoneAfter", e);
			}
		}
		if (returnValue == null) {
			String ago = getProperty("skipDirectoriesScannedAgo");
			long skipDirectoriesScannedAgo = Long.valueOf(ago);

			if (skipDirectoriesScannedAgo == -1)
				returnValue = new Date(new Date().getTime() + 315600000000L);
			else if (skipDirectoriesScannedAgo < -1)
				returnValue = new Date(0L);
			else
				returnValue = new Date(new Date().getTime() - skipDirectoriesScannedAgo);
		}
		return returnValue;
	}

	@Override
	public String getProperty(String key) {
		if (!foundFile)
			finishLoading();
		return super.getProperty(key);
	}

	private void finishLoading() {
		if (loader != null)
			try {
				loader.join();
			} catch (InterruptedException e) {
				logger.fatal("interrupted while trying to wait for the config file to finish loading", e);
			}
	}

	private boolean foundFile = false;

	private void loadConfig() {

		if (!foundFile)
			for (String path : propertyPaths) {
				loadConfigFile(path, false);
				if (foundFile)
					break;
			}

		if (!foundFile)
			for (String path : propertyPaths) {
				loadConfigFile(path, true);
				if (foundFile)
					break;
			}

		if (!foundFile)
			logger.log(
					Level.FATAL,
					"Could not load properties! something is seriously wrong here! (cannot connect to database without properties, therefore cannot do anything at all)");
	}

	private void loadConfigFile(String path, boolean useClassloader) {

		InputStream inputStream = null;
		try {

			if (useClassloader)
				inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
			else
				inputStream = new FileInputStream(path);

			if (inputStream != null) {
				this.load(inputStream);
				foundFile = true;
				logger.log(Level.INFO, "successfully loaded properties from path=\"" + path + "\" & useClassloader="
						+ useClassloader);
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
	}

	@Override
	protected void finalize() throws Throwable {
		writePropertiesFile();
		super.finalize();
	}

	private void writePropertiesFile() {
		try {
			OutputStream outputStream = new FileOutputStream(propertyPaths[0]);
			this.store(outputStream, null);
			outputStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addProfile(String name) {
		int i = 1;
		while (true) {
			String existingProfile = this.getProperty("ProfileName" + i);
			if (existingProfile == null || existingProfile.length() == 0) {
				this.setProperty("ProfileName" + i, name);
				writePropertiesFile();
				return;
			} else if (existingProfile.equals(name)) {
				return;
			}
			i++;
		}
	}

	public List<String> getProfileList() {
		ArrayList<String> profiles = new ArrayList<>();
		int i = 1;
		while (true) {
			String existingProfile = this.getProperty("ProfileName" + i);
			if (existingProfile == null || existingProfile.length() == 0) {
				return profiles;
			} else
				profiles.add(existingProfile);
			i++;
		}
	}
}
