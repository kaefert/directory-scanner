/**
 * 
 */
package com.googlecode.directory_scanner.directory_scanner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author kaefert
 * 
 */
public class DatabaseHandler {

    private Properties configFile;
    private Logger logger;

    public DatabaseHandler(Properties configFile, Logger logger) {
	this.configFile = configFile;
	this.logger = logger;
    }

    private Map<String, Connection> connections = null;

    private Connection getConnection() {
	return getConnection(configFile.getProperty("databaseName"));
    }

    private Connection getConnection(String databaseName) {
	if (connections == null) {
	    connections = new HashMap<String, Connection>();
	}
	if (connections.get(databaseName) == null) {

	    try {
		Class.forName(configFile.getProperty("databaseDriver")).newInstance();
		Connection connection = DriverManager.getConnection(configFile.getProperty("databaseUrl") + databaseName,
		configFile.getProperty("databaseUser"), configFile.getProperty("databasePassword"));
		connections.put(databaseName, connection);
		logger.log(Level.INFO, "Connected to the database \"" + databaseName + "\"");

	    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
		logger.fatal("Could not load Database Driver! This application cannot do anything useful without this!", e);
		e.printStackTrace();
	    } catch (SQLException e) {
		if (databaseName != "") {
		    logger.warn("Could not open database \"" + databaseName + "\", will try to log in to global and create it", e);
		    createDatabase(databaseName);
		    return getConnection(databaseName);
		} else {
		    logger.error("Failed to connect database-server without supplying a database-name.", e);
		}

		e.printStackTrace();
	    }

	}
	return connections.get(databaseName);
    }

    @Override
    protected void finalize() throws Throwable {

	if (connections != null) {

	    for (Map.Entry<String, Connection> entry : connections.entrySet()) {

		entry.getValue().close();
		logger.log(Level.INFO, "Disconnected from database \"" + entry.getKey() + "\".");
	    }

	    for (Connection connection : connections.values()) {
		connection.close();
	    }
	    logger.log(Level.INFO, "Disconnected all open database-connections.");
	} else {
	    logger.log(Level.INFO, "No database-connections have been opened.");
	}

	super.finalize();
    }

    private void createDatabase(String databaseName) {
	try {
	    Connection global = getConnection("");
	    if (global != null) {
		PreparedStatement createDB = global.prepareStatement("create Database " + databaseName + ";");
		// createDB.setString(1, databaseName);
		createDB.execute();
		createDB.close();
	    }
	} catch (SQLException e) {
	    logger.error("Could not create database", e);
	}
    }

    private boolean createDirectoriesTableFailed = false;

    private void createDirectoriesTable() {
	if (createDirectoriesTableFailed)
	    return;
	try {
	    Connection global = getConnection();
	    if (global != null) {
		PreparedStatement createTable = global.prepareStatement(configFile.getProperty("sql_createTableDirectories"));
		createTable.execute();
	    }
	} catch (SQLException e) {
	    logger.error("Could not create table directories", e);
	    createDirectoriesTableFailed = true;
	}
    }

    private boolean createFilesTableFailed = false;

    private void createFilesTable() {
	if (createFilesTableFailed)
	    return;
	try {
	    Connection global = getConnection();
	    if (global != null) {
		PreparedStatement createTable = global.prepareStatement(configFile.getProperty("sql_createTableFiles"));
		createTable.execute();
	    }
	} catch (SQLException e) {
	    logger.error("Could not create table Files", e);
	    createFilesTableFailed = true;
	}
    }

    private HashMap<String, Integer> directories;
    private HashMap<Integer, Date> directoriesDone;

    // private Map<String, Integer> directories;

    public Map<String, Integer> getDirectories() {
	if (directories == null) {
	    try {
		PreparedStatement stmt = getConnection().prepareStatement("SELECT path, id, finished FROM directories");
		ResultSet result = stmt.executeQuery();

		directories = new HashMap<>();
		directoriesDone = new HashMap<>();

		while (result.next()) {
		    directories.put(result.getString("path"), result.getInt("id"));
		    if (result.getDate("finished") != null)
			directoriesDone.put(result.getInt("id"), result.getDate("finished"));
		}

	    } catch (SQLException e) {
		directories = null;
		if (!createDirectoriesTableFailed) {
		    logger.warn("selecting directories failed, will try to create table", e);
		    createDirectoriesTable();
		    if (!createDirectoriesTableFailed)
			return getDirectories();
		} else {
		    logger.error("selecting directories failed, but table creation already failed previously", e);
		}
	    }
	}
	return directories;
    }

    public HashMap<Integer, Date> getDirectoriesDone() {
	getDirectories();
	return directoriesDone;
    }

    public Date getDirectoryDoneTime(String path, Integer scanDir) {
	Integer id = getDirectories().get(path);
	if (id != null) {
	    return getDirectoriesDone().get(id);
	    // return getDoneDirectories().contains(id);
	} else {
	    insertDirectory(path, scanDir, false);
	    return null;
	}
    }

    public Integer getDirectory(String path) {
	Integer id = getDirectories().get(path);
	if (id != null) {
	    return id;
	} else {
	    return insertDirectory(path, null, false);
	}
    }

    public void forgetDoneDirectories() {
	// TODO: maybe we should make this permanent in the database, but for
	// rescanning in the currently running instance this should suffice.
	getDirectoriesDone().clear();
    }

    public Integer insertDirectory(String path, Integer scanDir, boolean finished) {
	getDirectories();
	Integer dirId = (Integer) directories.get(path);

	try {
	    if (dirId != null) {
		PreparedStatement stmt = null;
		if (finished)
		    stmt = getConnection().prepareStatement("UPDATE directories SET scanDir_id = ?, finished = ? WHERE id = ?");
		else
		    stmt = getConnection().prepareStatement("UPDATE directories SET scanDir_id = ?, scandate = ? WHERE id = ?");

		if (scanDir == null)
		    stmt.setNull(1, java.sql.Types.INTEGER);
		else
		    stmt.setInt(1, scanDir);

		stmt.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
		stmt.setInt(3, dirId);
		stmt.execute();
	    } else {
		PreparedStatement stmt = getConnection().prepareStatement("INSERT INTO directories (path, scanDir_id, scandate, finished) VALUES (?, ?, ?, ?)");
		stmt.setString(1, path);

		if (scanDir == null)
		    stmt.setNull(2, java.sql.Types.INTEGER);
		else
		    stmt.setInt(2, scanDir);

		stmt.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
		stmt.setTimestamp(4, (finished ? new java.sql.Timestamp(new java.util.Date().getTime()) : null));
		boolean resultSetExists = stmt.execute();
		stmt.close();

		if (resultSetExists) {
		    logger.info("need to find the way!");
		}

		stmt = getConnection().prepareStatement("select LAST_INSERT_ID()");
		ResultSet result = stmt.executeQuery();
		while (result.next()) {
		    getDirectories().put(path, result.getInt(1));

		    if (finished)
			getDirectoriesDone().put(result.getInt(1), new Date());

		    return result.getInt(1);
		}
	    }
	} catch (SQLException e) {
	    if (!createDirectoriesTableFailed) {
		createDirectoriesTable();
		insertDirectory(path, scanDir, finished);
	    }
	    e.printStackTrace();
	}

	return dirId;
    }

    // private Map<Integer, HashSet<String>> filesInDir;

    private Map<String, Integer> fileIds;
    private Map<Integer, String> filePaths;
    private Map<Integer, Long> fileSizes;
    private Map<Integer, Date> fileScandates;
    private Map<String, HashSet<Integer>> fileSha1s;
    private HashSet<String> nonUniqueSha1s;
    
    public Map<String, Integer> getFileIds() {
	if (fileIds == null) {
	    logger.log(Level.INFO, "Start building map of existing files");
	    try {
		PreparedStatement stmt = getConnection().prepareStatement(
		"SELECT d.path, f.dir_id, f.filename, f.id, f.size, f.scandate, f.sha1 FROM files f INNER JOIN directories d ON d.id = f.dir_id");

		fileIds = new HashMap<>();
		filePaths = new HashMap<>();
		fileSizes = new HashMap<>();
		fileScandates = new HashMap<>();
		fileSha1s = new HashMap<>();
		nonUniqueSha1s = new HashSet<>();

		ResultSet result = stmt.executeQuery();
		while (result.next()) {
		    fileIds.put(result.getString("path") + "/" + result.getString("filename"), result.getInt("id"));
		    filePaths.put(result.getInt("id"), result.getString("path") + "/" + result.getString("filename"));
		    fileSizes.put(result.getInt("id"), result.getLong("size"));
		    fileScandates.put(result.getInt("id"), result.getTimestamp("scandate"));

		    HashSet<Integer> sha1Ids = fileSha1s.get(result.getString("sha1"));
		    if (sha1Ids == null) {
			sha1Ids = new HashSet<Integer>(1);
			sha1Ids.add(result.getInt("id"));
			fileSha1s.put(result.getString("sha1"), sha1Ids);
		    } else {
			sha1Ids.add(result.getInt("id"));
			if(sha1Ids.size() > 1)
			    nonUniqueSha1s.add(result.getString("sha1"));
		    }
		}
		logger.log(Level.INFO, "Finished building scannedFiles map");

	    } catch (SQLException e) {

		fileIds = null;
		fileSizes = null;

		if (!createFilesTableFailed) {
		    logger.warn("selecting files failed, will try to create table", e);
		    createFilesTable();
		    if (!createFilesTableFailed)
			return getFileIds();
		} else {
		    logger.error("selecting files failed, but table creation already failed previously", e);
		    logger.log(Level.ERROR, "could not build file maps", e);
		}
	    }

	}
	return fileIds;
    }
    
    public Map<Integer, String> getFilePaths() {
	getFileIds();
	return filePaths;
    }

    public Map<Integer, Long> getFileSizes() {
	getFileIds();
	return fileSizes;
    }

    public Map<Integer, Date> getFileScandates() {
	getFileIds();
	return fileScandates;
    }

    public Map<String, HashSet<Integer>> getFileSha1s() {
	getFileIds();
	return fileSha1s;
    }

    public HashSet<String> getNonUniqueSha1s() {
	getFileIds();
	return nonUniqueSha1s;
    }

    public Long getFileSize(String fullPath) {
	return getFileSizes().get(getFileIds().get(fullPath));
    }

    public Integer insertFile(String fullPath, String fileName, Integer containingDir, Integer scanDir, long size, String sha1) {
	Integer fileId = getFileIds().get(fullPath);

	try {
	    if (fileId != null) {
		PreparedStatement stmt = getConnection().prepareStatement("UPDATE files SET scanDir_id = ?, size = ?, sha1 = ?, scandate = ? WHERE id = ?");

		if (scanDir == null)
		    stmt.setNull(1, java.sql.Types.INTEGER);
		else
		    stmt.setInt(1, scanDir);

		stmt.setLong(2, size);
		stmt.setString(3, sha1);

		stmt.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));

		stmt.setInt(5, fileId);
		stmt.execute();
	    } else {
		PreparedStatement stmt = getConnection().prepareStatement(
		"INSERT INTO files (dir_id, scanDir_id, filename, sha1, scandate, size) VALUES (?, ?, ?, ?, ?, ?)");
		stmt.setInt(1, containingDir);

		if (scanDir == null)
		    stmt.setNull(2, java.sql.Types.INTEGER);
		else
		    stmt.setInt(2, scanDir);

		stmt.setString(3, fileName);
		stmt.setString(4, sha1);
		Timestamp timestamp = new Timestamp(new java.util.Date().getTime());
		stmt.setTimestamp(5, timestamp);
		stmt.setLong(6, size);
		stmt.execute();

		stmt = getConnection().prepareStatement("select LAST_INSERT_ID()");
		ResultSet result = stmt.executeQuery();
		while (result.next()) {
		    
		    getFileIds().put(fullPath, result.getInt(1));
		    getFilePaths().put(result.getInt(1), fullPath);
		    getFileSizes().put(result.getInt(1), size);
		    getFileScandates().put(result.getInt(1), timestamp);
		    return result.getInt(1);
		}
	    }
	} catch (SQLException e) {

	    if (!createFilesTableFailed) {
		logger.warn("inserting file failed, will try to create table", e);
		createFilesTable();
		if (!createFilesTableFailed)
		    return insertFile(fullPath, fileName, containingDir, scanDir, size, sha1);
	    } else {
		logger.error("inserting file failed, but table creation already failed previously", e);
		logger.log(Level.ERROR, "could not insert file", e);
	    }
	}
	return fileId;
    }

    public void forgetDirectoryTree(String s) {
	try {
	    PreparedStatement stmt = getConnection().prepareStatement("DELETE directories WHERE path LIKE ?");
	    stmt.setString(1, s + "%");
	    stmt.execute();
	} catch (SQLException e) {
	    logger.error("forgetDirectoryTree failed - SQLException", e);
	}
    }
}
