/**
 * 
 */
package com.googlecode.directory_scanner.directory_scanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import org.apache.log4j.Level;

/**
 * @author kaefert
 *
 */
public class DirectoryScanner {

	private DirectoryScanner() {
		// this class does only exist for the main method, private constructor prevents instantiation
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("creating treewalker");
		TreeWalker fileWalker = new TreeWalker();
		
		
		if(args != null) {
			String commmand = null;
			int num = 0;
			
			for(String s : args) {				
				DatabaseWorker.getLogger().log(Level.INFO, "processing args["+num+"]="+s);
				
				if(commmand == null) {
					if("scan".equals(s) || "rescan".equals(s) || "check_exists".equals(s)) {
						commmand = s;
					}
					else if ("create_tables".equals(s)) {
						DatabaseWorker.createTables();
					}
					else if("reset_done_directories".equals(s)) {
						DatabaseWorker.resetDoneDirectories();
					}
					else printHelp(true);
				}
				else if("scan".equals(commmand)) {
					scanPath(s, fileWalker);
				}
				else if("rescan".equals(commmand)) {
					DatabaseWorker.prepareRescan(s);
					scanPath(s, fileWalker);
				}
				else if("check_exists".equals(commmand)) {
					DatabaseWorker.checkExistence(s);
				}
				else {
					DatabaseWorker.getLogger().log(Level.ERROR, "unhandled stated, please share the call that caused this output with the developers!");
				}

				num++;
			}
		}
		else
			printHelp(false);
		
		
		System.out.println("");
		System.out.println("");
		DatabaseWorker.getLogger().log(Level.INFO ,"end of DirectoryScanner -> static void main");
	}
	
	private static void scanPath(String pathString, TreeWalker fileWalker) {
		try {
			Path path = FileSystems.getDefault().getPath(pathString);
			Files.walkFileTree(path, fileWalker);
		
		} catch (InvalidPathException e) {
			DatabaseWorker.getLogger().log(Level.ERROR, "invalid path='"+pathString+"'", e);
		} catch (IOException e) {
			DatabaseWorker.getLogger().log(Level.ERROR, "could not read path='"+pathString+"'", e);
		}
	}
	
	private static void printHelp(boolean error) {
		System.out.println("Error: given parameters did not match the specification below:");
		System.out.println("");
		System.out.println("Welcome to DirectoryScanner Version 0.0.1 Snapshot");
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
		System.out.println("create_tables");
		System.out.println("--> run the create_table statements set in DirectoryScanner.properties");
		System.out.println("");
		System.out.println("check_exists path");
		System.out.println("--> goes through the paths in the database that are below the given path");
		System.out.println("--> and removes them from ");
		System.out.println("");
		System.out.println("reset_done_directories");
		System.out.println("--> delete all rows of the done directories table");
		System.out.println("");
		System.out.println("");
	}

}
