package com.googlecode.directory_scanner.workers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class DatabaseConnectionHandler {

    private Logger logger;
    private Properties appConfig;
    private boolean useFallback = false;
    
    public boolean isUsingFallback() {
	return useFallback;
    }

    public DatabaseConnectionHandler(Logger logger, Properties config) {
	this.logger = logger;
	this.appConfig = config;
    }

    private Map<String, Connection> connections = null;

    public Connection getConnection() {
	return getConnection(appConfig.getProperty("databaseName"));
    }

    private Connection getConnection(String databaseName) {
	if (connections == null) {
	    connections = new HashMap<String, Connection>();
	}
	
	if(useFallback)
	    return connections.get("");
	
	if("1".equals(appConfig.getProperty("dbFallBackUseAsDefault")))
		return getFallbackDatabase();
	
	if (connections.get(databaseName) == null) {

	    try {
		Class.forName(appConfig.getProperty("databaseDriver")).newInstance();
		Connection connection = DriverManager.getConnection(appConfig.getProperty("databaseUrl") + databaseName, appConfig.getProperty("databaseUser"),
		appConfig.getProperty("databasePassword"));
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
		    logger.error("Failed to connect database-server without supplying a database-name, using fallback db", e);
		    return getFallbackDatabase();
		}
	    }

	}
	return connections.get(databaseName);
    }

    private Connection getFallbackDatabase() {
	try {
	    Class.forName(appConfig.getProperty("dbFallBackDriver")).newInstance();
	    Connection connection = DriverManager.getConnection(appConfig.getProperty("dbFallBackURL"), appConfig.getProperty("dbFallBackUser"),
	    appConfig.getProperty("dbFallBackPassword"));
	    connections.put("", connection);
	    useFallback = true;
	    createDirectoriesTable(null);
	    createFilesTable(null);
	    createFailuresTable(null);
	    return connection;

	} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
	    logger.error("Failed to load fallback-database-drivers!", e);
	} catch (SQLException e) {
	    logger.error("Failed to connect fallback-database-server!", e);
	}
	return null;
    }

    private void createDatabase(String databaseName) {
	try {
	    Connection global = getConnection("");
	    if (global != null && !useFallback) { //fallback is a single database, doesn't need create database statement
		PreparedStatement createDB = global.prepareStatement("create Database " + databaseName + ";");
		// createDB.setString(1, databaseName);
		createDB.execute();
		createDB.close();
	    }
	} catch (SQLException e) {
	    logger.error("Could not create database", e);
	}
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

    private boolean createDirectoriesTableFailed = false;

    public void createDirectoriesTable(SQLException e) {
	if (createDirectoriesTableFailed)
	    throw new IllegalStateException("database cannot be used, could not create directory table", e);

	try {
	    Connection global = getConnection();
	    if (global != null) {
		String sql = appConfig.getProperty("sql_createTableDirectories");
		if(useFallback)
		    sql = sql.replace(" ENGINE=InnoDB", "");
		PreparedStatement createTable = global.prepareStatement(sql);
		createTable.execute();
		createTable.close();
	    }
	} catch (SQLException e2) {
	    createDirectoriesTableFailed = true;
	    throw new IllegalStateException("database cannot be used, could not create directory table", e2);
	}
    }

    private boolean createFilesTableFailed = false;

    public void createFilesTable(SQLException e) {
	if (createFilesTableFailed)
	    throw new IllegalStateException("database cannot be used, could not create files table", e);

	try {
	    Connection global = getConnection();
	    if (global != null) {
		String sql = appConfig.getProperty("sql_createTableFiles");
		if(useFallback)
		    sql = sql.replace(" ENGINE=InnoDB", "");
		PreparedStatement createTable = global.prepareStatement(sql);
		createTable.execute();
		createTable.close();
	    }
	} catch (SQLException e2) {
	    createFilesTableFailed = true;
	    throw new IllegalStateException("database cannot be used, could not create files table", e2);
	}
    }

    private boolean createFailuresTableFailed = false;

    public void createFailuresTable(SQLException e) {
	if (createFailuresTableFailed)
	    throw new IllegalStateException("database cannot be used, could not create files table", e);

	try {
	    Connection global = getConnection();
	    if (global != null) {
		String sql = appConfig.getProperty("sql_createTableFailures");
		if(useFallback)
		    sql = sql.replace(" ENGINE=InnoDB", "");
		PreparedStatement createTable = global.prepareStatement(sql);
		createTable.execute();
		createTable.close();
	    }
	} catch (SQLException e2) {
	    createFailuresTableFailed = true;
	    throw new IllegalStateException("database cannot be used, could not create files table", e2);
	}
    }

}
