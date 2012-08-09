/**
 * 
 */
package com.googlecode.directory_scanner.ui;

import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.ConfigLoader;
import com.googlecode.directory_scanner.Worker;

/**
 * @author kaefert
 * 
 */
public class CLI {

    private CLI() {
	// this class does only exist for the main method, private constructor
	// prevents instantiation
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

	if (args != null && args.length > 0) {

	    Logger logger = Logger.getLogger("directory-scanner-console-logger_" + args[0].hashCode());
	    Properties config = new ConfigLoader(logger).getConfig();
	    Worker worker = new Worker(logger, config);

	    String commmand = null;
	    int num = 0;

	    for (String s : args) {
		logger.log(Level.INFO, "processing args[" + num + "]=" + s);

		if (commmand == null) {
		    if ("scan".equals(s) || "rescan".equals(s) || "check_exists".equals(s)) {
			commmand = s;
		    } else if ("reset_done_directories".equals(s)) {
			worker.forgetDoneDirectories();
		    } else
			printHelp(true);
		} else if ("scan".equals(commmand)) {
		    worker.scanPath(s);
		} else if ("rescan".equals(commmand)) {
		    worker.forgetPath(s);
		    worker.scanPath(s);
		} else if ("check_exists".equals(commmand)) {
		    worker.printInfoOn(s);
		} else {
		    logger.log(Level.ERROR, "unhandled stated, please share the call that caused this output with the developers!");
		}

		num++;
	    }

	    System.out.println("");
	    System.out.println("");
	    logger.log(Level.INFO, "end of DirectoryScanner -> static void main");

	} else
	    printHelp(false);
    }

    private static void printHelp(boolean error) {
	System.out.println("Error: given parameters did not match the specification below:");
	System.out.println("");
	System.out.println("Welcome to DirectoryScanner Version 0.0.2 Snapshot");
	System.out.println("");
	System.out.println("This is a command line program, you need to pass it");
	System.out.println("options so that it knows what to do.");
	System.out.println("");
	System.out.println("scan path");
	System.out.println("--> scans the path");
	System.out.println("");
	System.out.println("rescan path");
	System.out.println("--> removes path% from the doneDirectories + scans the path");
	System.out.println("");
	System.out.println("check_exists path");
	System.out.println("--> goes through the paths in the database that are below the given path");
//	System.out.println("--> and removes them from ");
	System.out.println("");
	System.out.println("reset_done_directories");
	System.out.println("--> delete all rows of the done directories table");
	System.out.println("");
	System.out.println("");
    }

}
