/**
 * 
 */
package com.googlecode.directory_scanner.directory_scanner;

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
		Walker fileWalker = new Walker();
		
		
		if(args != null) {
			String commmand = null;
			int num = 0;
			
			for(String s : args) {				
				Worker.getLogger().log(Level.INFO, "processing args["+num+"]="+s);
				
				if(commmand == null) {
					if("scan".equals(s) || "rescan".equals(s) || "check_exists".equals(s)) {
						commmand = s;
					}
					else if ("create_tables".equals(s)) {
						Worker.getSingelton().createTables();
					}
					else if("reset_done_directories".equals(s)) {
						Worker.getSingelton().resetDoneDirectories();
					}
					else printHelp(true);
				}
				else if("scan".equals(commmand)) {
					fileWalker.scanPath(s);
				}
				else if("rescan".equals(commmand)) {
					Worker.getSingelton().forgetPath(s);
					fileWalker.scanPath(s);
				}
				else if("check_exists".equals(commmand)) {
					Worker.getSingelton().checkExistence(s);
				}
				else {
					Worker.getLogger().log(Level.ERROR, "unhandled stated, please share the call that caused this output with the developers!");
				}

				num++;
			}
		}
		else
			printHelp(false);
		
		
		System.out.println("");
		System.out.println("");
		Worker.getLogger().log(Level.INFO ,"end of DirectoryScanner -> static void main");
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
