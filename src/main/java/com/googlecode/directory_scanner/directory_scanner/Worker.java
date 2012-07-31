package com.googlecode.directory_scanner.directory_scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Worker {

    public static Logger getLogger() {
	return getSingelton().logger;
    }

    private static Worker singelton;

    public static Worker getSingelton() {
	if (singelton == null)
	    singelton = new Worker();
	return singelton;
    }

    private Integer checkDoneFiles, checkDoneDirectories;

    private Logger logger;

    private Connection connection = null;

    private PreparedStatement insertScannedFile, selectSizeForPath, updateScannedFile, selectAllPathSize, insertDirectoryDone, selectAllDoneDirectories,
    deleteSubDirectories, selectFilesLike, deleteSingleFile;

    private Map<String, Long> scannedFiles;

    private Set<String> doneDirectories;

    Properties configFile;

    private PreparedStatement getDeleteSubDirectories() throws SQLException {
	if (deleteSubDirectories == null) {
	    deleteSubDirectories = getConnection().prepareStatement(configFile.getProperty("sqlStatement_delete_done_directories_below"));
	}
	return deleteSubDirectories;
    }

    private PreparedStatement getDeleteSingleFile() throws SQLException {
	if (deleteSingleFile == null) {
	    deleteSingleFile = getConnection().prepareStatement(configFile.getProperty("sqlStatement_delete_single_file"));
	}
	return deleteSingleFile;
    }

    private Map<String, Long> getScannedFiles() {
	if (scannedFiles == null) {
	    logger.log(Level.INFO, "Start building map of existing files");
	    try {
		selectAllPathSize = getConnection().prepareStatement(configFile.getProperty("sqlStatement_select_all_files"));
		scannedFiles = new HashMap<String, Long>();
		ResultSet result = selectAllPathSize.executeQuery();
		while (result.next()) {
		    scannedFiles.put(result.getString("path"), result.getLong("size"));
		}
		logger.log(Level.INFO, "Finished building scannedFiles map");

	    } catch (SQLException e) {
		logger.log(Level.ERROR, "could not build scannedFiles map", e);
	    }

	}
	return scannedFiles;
    }

    private Set<String> getDoneDirectories() {
	if (doneDirectories == null) {
	    logger.log(Level.INFO, "Will now load set of doneDirectories");
	    doneDirectories = new HashSet<String>();
	    try {
		selectAllDoneDirectories = getConnection().prepareStatement(configFile.getProperty("sqlStatement_select_all_done_directories"));
		ResultSet result = selectAllDoneDirectories.executeQuery();
		while (result.next()) {
		    doneDirectories.add(result.getString("path"));
		}
	    } catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }

	    logger.log(Level.INFO, "finished loading doneDirectories");
	}
	return doneDirectories;
    }

    private Connection getConnection() {
	return getConnection(configFile.getProperty("databaseName"));
    }

    private Connection getConnection(String databaseName) {
	if (connection == null) {
	    try {
		Class.forName(configFile.getProperty("databaseDriver")).newInstance();
		connection = DriverManager.getConnection(configFile.getProperty("databaseUrl") + databaseName, configFile.getProperty("databaseUser"),
		configFile.getProperty("databasePassword"));
		logger.log(Level.INFO, "Connected to the database \"" + databaseName + "\"");

	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	return connection;
    }

    private Worker() {

	logger = Logger.getLogger("DatabaseWorkerLogger");

	// tried out java.util.logging... seems like crap to me
	// getLogger("DatabaseWorkerLogger");
	// logger.setLevel(Level.FINEST);
	// logger.entering(this.getClass().getName(), "Constructor");
	// logger.info("logger.info");
	// logger.fine("logger.fine");
	// logger.finer("logger.finer");
	// logger.finest("logger.finest");
	// logger.log(Level.FINEST, "logger.log FINEST");
	// logger.log(Level.FINER, "logger.log FINER");
	// logger.log(Level.FINE, "logger.log FINE");
	// logger.log(Level.INFO, "logger.log INFO");
	// logger.log(Level.WARNING, "logger.log WARNING");
	// logger.log(Level.SEVERE, "logger.log SEVERE");

	// logger.log(Level.INFO, "logger.log INFO");

	configFile = findConfigFile();

	checkDoneFiles = Integer.valueOf(configFile.getProperty("doneFiles"));
	checkDoneDirectories = Integer.valueOf(configFile.getProperty("doneDirectories"));

	logger.log(Level.INFO, "finished DatabaseWriter constructor");
    }

    public void createDatabase() {
	try {
	    getConnection("").prepareStatement(configFile.getProperty("createDatabase")).execute();
	    logger.info("created database");
	} catch (SQLException e) {
	    logger.error("could not create tables", e);
	}
    }

    public void createTables() {
	try {
	    getConnection().prepareStatement(configFile.getProperty("createTableStatement_Files")).execute();
	    getConnection().prepareStatement(configFile.getProperty("createTableStatement_directories_done")).execute();
	    logger.info("created tables");
	} catch (SQLException e) {
	    logger.error("could not create tables", e);
	}
    }

    public void dropTables() {
	try {
	    getConnection().prepareStatement(configFile.getProperty("dropTableStatement_Files")).execute();
	    getConnection().prepareStatement(configFile.getProperty("dropTableStatement_directories_done")).execute();
	    logger.info("droped tables");
	} catch (SQLException e) {
	    logger.error("could not drop tables", e);
	}
    }

    public void scanFile(Path file, BasicFileAttributes attr) {

	String path = file.toString();
	String fileName = file.getFileName().toString();
	String directory = file.getParent().toString();

	boolean scanned = false, sized = false;

	if (checkDoneFiles.equals(1)) {
	    Long scannedSize = getScannedFiles().get(path);
	    if (scannedSize != null) {
		scanned = true;
		if (scannedSize.equals(attr.size()))
		    sized = true;
	    }
	} else if (checkDoneFiles.equals(2)) {
	    // much to slow (check the database for each file if it already has
	    // been
	    // scanned)

	    try {
		if (selectSizeForPath == null) {
		    selectSizeForPath = getConnection().prepareStatement(configFile.getProperty("sqlStatement_select_size"));
		}

		selectSizeForPath.setString(1, file.toString());

		ResultSet result = selectSizeForPath.executeQuery();

		if (result.next()) {
		    scanned = true;
		    if (attr.size() == result.getLong("size")) {
			sized = true;
		    }
		}
		if (result.next()) {
		    logger.log(Level.WARN, "WARNING: path stored multiple times! This code does not process this");
		}

	    } catch (SQLException e) {
		logger.error("could not check if file has been scanned previously", e);
	    }

	}

	// if(result.
	if (!sized) {
	    try {
		String sha1 = ChecksumSHA1.getSHA1Checksum(file);

		logger.log(Level.INFO, "sha1 = " + sha1);

		if (scanned) {
		    logger.log(Level.INFO, "has been scanned but changed filesize. scanned -> updating.");

		    if (updateScannedFile == null) {
			updateScannedFile = getConnection().prepareStatement(configFile.getProperty("sqlStatement_update_file"));
		    }

		    updateScannedFile.setString(1, sha1);
		    updateScannedFile.setTimestamp(2, new Timestamp(new java.util.Date().getTime()));
		    updateScannedFile.setLong(3, attr.size());
		    updateScannedFile.setString(4, path);

		    updateScannedFile.execute();

		} else {
		    logger.log(Level.INFO, "has not been scanned yed. scanned -> inserting.");

		    if (insertScannedFile == null) {
			insertScannedFile = getConnection().prepareStatement(configFile.getProperty("sqlStatement_insert_file"));
		    }

		    insertScannedFile.setString(1, sha1);
		    insertScannedFile.setTimestamp(2, new Timestamp(new java.util.Date().getTime()));
		    // statement.setDate(2, new java.sql.Date(new
		    // java.util.Date().getTime()));
		    insertScannedFile.setString(3, path);
		    insertScannedFile.setLong(4, attr.size());

		    insertScannedFile.execute();
		}
		if (checkDoneFiles.equals(1))
		    getScannedFiles().put(path, attr.size());

	    } catch (NoSuchAlgorithmException | IOException e) {
		logger.error("failed to calculate sha1 checksum", e);
	    } catch (SQLException e) {
		logger.error("failed to write row for this file into database. This is expected behaviour if you deactivated the check for doneFiles", e);
	    }

	} else {
	    logger.log(Level.INFO, "has already been scanned and has not changed size. Will ignore this file.");
	}
    }

    public boolean decideDirectorySkip(Path dir, BasicFileAttributes attrs) {
	if (checkDoneDirectories.equals(1) && getDoneDirectories().contains(dir.toString())) {
	    logger.log(Level.INFO, "skipping done directory: " + dir.toString());
	    return true;
	} else {
	    logger.log(Level.INFO, "directory not yet done, continue processing -> " + dir.toString());
	    return false;
	}
    }

    public void finishedDirectory(Path dir) {

	logger.log(Level.INFO, "finished processing directory: " + dir.toString());

	try {
	    getDeleteSubDirectories().setString(1, dir.toString() + "%");
	    getDeleteSubDirectories().execute();

	    if (insertDirectoryDone == null) {
		insertDirectoryDone = getConnection().prepareStatement(configFile.getProperty("sqlStatement_insert_done_directory"));
	    }
	    insertDirectoryDone.setString(1, dir.toString());
	    insertDirectoryDone.execute();

	} catch (SQLException e) {
	    logger.error("failed to delete/insert finishedDirectory", e);
	    e.printStackTrace();
	}

	if (checkDoneDirectories == 1)
	    getDoneDirectories().add(dir.toString());
    }

    public void infoOnPath(String path) {
	logger.log(Level.INFO, "printing what I got on the path '" + path + "%' ");
    }

    public void forgetPath(String s) {
	logger.log(Level.INFO, "Deleting '" + s + "%' from doneDirectories");
	try {
	    getDeleteSubDirectories().setString(1, s + "%");
	    getDeleteSubDirectories().execute();
	} catch (SQLException e) {
	    logger.error("failed to delete directories", e);
	}
    }

    public void resetDoneDirectories() {
	try {
	    getConnection().prepareStatement(configFile.getProperty("sqlStatement_delete_all_done_directories")).execute();
	} catch (SQLException e) {
	    logger.log(Level.ERROR, "delete done directories failed", e);
	}
    }

    public void checkExistence(String s) {
	logger.log(Level.INFO, "Checking existence of files like '" + s + "%'");
	try {
	    if (selectFilesLike == null) {
		selectFilesLike = getConnection().prepareStatement(configFile.getProperty("sqlStatement_select_all_files_like"));
	    }

	    selectFilesLike.setString(1, s + "%");
	    ResultSet result = selectFilesLike.executeQuery();
	    while (result.next()) {
		String pathString = result.getString("path");
		long size = result.getLong("size");
		try {
		    Path path = FileSystems.getDefault().getPath(pathString);
		    File file = path.toFile();
		    if (!file.exists()) {
			logger.log(Level.WARN, "the file@path='" + pathString + "' --> does not exist!");
//			deletePathFromDB(pathString);
		    } else if (file.length() != size) {
			logger.log(Level.WARN, "the file@path='" + pathString + "' has changed size since it was last scanned. please use rescan to update!");
		    } else {
			logger.log(Level.INFO, "file@path='" + pathString + "' still exists, and has not changed in size.");
		    }
		} catch (InvalidPathException e) {
		    logger.log(Level.WARN, "invalid path='" + pathString + "' --> deleting from db", e);
		    deletePathFromDB(pathString);
		}
	    }

	} catch (SQLException e) {
	    logger.error("failed to read paths from database", e);
	}
	// "sqlStatement_select_all_files_like";
    }

    private void deletePathFromDB(String pathString) {
	logger.log(Level.INFO, "deleting path '" + pathString + "' from database.");
	try {
	    getDeleteSingleFile().setString(1, pathString);
	    getDeleteSingleFile().execute();
	} catch (SQLException e) {
	    logger.error("failed to delete path '" + pathString + "' from db", e);
	}
    }

    @Override
    protected void finalize() throws Throwable {

	if (connection != null)
	    connection.close();

	logger.log(Level.INFO, "Disconnected from database");

	super.finalize();
    }

    private Properties findConfigFile() {
	Properties configFile = new Properties();

	InputStream inputStream = null;

	try {
	    inputStream = new FileInputStream("./DirectoryScanner.properties");
	    if (inputStream != null)
		logger.log(Level.INFO, "successfully loaded \"DirectoryScanner.properties\" file with new FileInputStream(\"./DirectoryScanner.properties\")");
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
	if (inputStream == null) {
	    try {
		inputStream = new FileInputStream("/DirectoryScanner.properties");
		if (inputStream != null)
		    logger.log(Level.INFO,
		    "successfully loaded \"DirectoryScanner.properties\" file with new FileInputStream(\"/DirectoryScanner.properties\")");
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	}
	if (inputStream == null) {
	    try {
		inputStream = new FileInputStream("DirectoryScanner.properties");
		if (inputStream != null)
		    logger
		    .log(Level.INFO, "successfully loaded \"DirectoryScanner.properties\" file with new FileInputStream(\"DirectoryScanner.properties\")");
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    }
	}
	if (inputStream == null) {
	    try {
		inputStream = this.getClass().getClassLoader().getResourceAsStream("./DirectoryScanner.properties");
		if (inputStream != null)
		    logger
		    .log(Level.INFO,
		    "successfully loaded \"DirectoryScanner.properties\" file with this.getClass().getClassLoader().getResourceAsStream(\"./DirectoryScanner.properties\");");
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
	}
	if (inputStream == null) {
	    try {
		inputStream = this.getClass().getClassLoader().getResourceAsStream("/DirectoryScanner.properties");
		if (inputStream != null)
		    logger
		    .log(Level.INFO,
		    "successfully loaded \"DirectoryScanner.properties\" file with this.getClass().getClassLoader().getResourceAsStream(\"/DirectoryScanner.properties\");");
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
	}
	if (inputStream == null) {
	    try {
		inputStream = this.getClass().getClassLoader().getResourceAsStream("DirectoryScanner.properties");
		if (inputStream != null)
		    logger
		    .log(Level.INFO,
		    "successfully loaded \"DirectoryScanner.properties\" file with this.getClass().getClassLoader().getResourceAsStream(\"DirectoryScanner.properties\");");
	    } catch (Throwable t) {
		t.printStackTrace();
	    }
	}

	try {
	    configFile.load(inputStream);
	    inputStream.close();
	} catch (IOException e1) {
	    e1.printStackTrace();
	} catch (Throwable t) {
	    t.printStackTrace();
	}

	finally {
	    try {
		if (inputStream != null)
		    inputStream.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}

	return configFile;
    }
}
