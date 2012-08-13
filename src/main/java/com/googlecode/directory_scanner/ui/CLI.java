/**
 * 
 */
package com.googlecode.directory_scanner.ui;

import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.WorkManager;

/**
 * @author kaefert
 * 
 */
public class CLI {

    public CLI(String[] args, Logger logger, Properties config, WorkManager worker) {

	// Logger logger = Logger.getLogger("directory-scanner-console-logger_"
	// + args[0].hashCode());
	// Properties config = new ConfigLoader(logger).getConfig();
	// Worker worker = new Worker(logger, config);

	String command = null;
	int num = 0;

	for (String s : args) {
	    logger.log(Level.INFO, "processing args[" + num + "]=" + s);

	    if (command == null) {
		if ("scan".equals(s) || "rescan".equals(s) || "check_exists".equals(s)) {
		    command = s;
		} else if ("reset_done_directories".equals(s)) {
		    config.setProperty("skipDirectoriesScannedAgo", "-2");
		} else
		    printHelp(true);
	    } else {

		if ("scan".equals(command)) {
		    worker.scanPath(s);
		} else if ("rescan".equals(command)) {
		    config.setProperty("skipDirectoriesScannedAgo", "-2");
		    worker.scanPath(s);
		} else if ("check_exists".equals(command)) {
		    worker.checkExistence(s);
		} else {
		    logger.log(Level.ERROR, "unhandled stated, please share the call that caused this output with the developers!");
		}
		command = null;
	    }

	    num++;
	}

	System.out.println("");
	System.out.println("");
	logger.log(Level.INFO, "end of DirectoryScanner -> static void main");

    }

    private void printHelp(boolean error) {
	if (error) {
	    System.out.println("Error: given parameters did not match the specification below:");
	    System.out.println("");
	}
	System.out.println("Welcome to DirectoryScanner Version 0.0.2 Snapshot");
	System.out.println("");
	System.out.println("If you want to use the the GUI, don't pass arguments. ");
	System.out.println("If you want to use the CLI, adjust your arguments to the description below:");
	System.out.println("");
	System.out.println("scan path");
	System.out.println("--> scans the path");
	System.out.println("");
	System.out.println("rescan path");
	System.out.println("--> removes path% from the doneDirectories + scans the path");
	System.out.println("");
	System.out.println("check_exists path");
	System.out.println("--> goes through the paths in the database that are below the given path");
	// System.out.println("--> and removes them from ");
	System.out.println("");
	System.out.println("reset_done_directories");
	System.out.println("--> dont skip directories");
	System.out.println("");
	System.out.println("");
    }

}
