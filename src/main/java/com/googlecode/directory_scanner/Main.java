/**
 * 
 */
package com.googlecode.directory_scanner;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.ui.CLI;
import com.googlecode.directory_scanner.ui.GUI;
import com.googlecode.directory_scanner.workers.AppConfig;
import com.googlecode.directory_scanner.workers.DatabaseWorkerImpl;
import com.googlecode.directory_scanner.workers.WorkManagerImpl;

/**
 * @author kaefert
 *
 */
public class Main {    
    
    private Main() {
    }

    /**
     * @param args if empty, gui is started. else the cli is used
     */
    public static void main(String[] args) {
	
	Logger logger = Logger.getLogger("directory-scanner_logger_" + Thread.currentThread().toString());
	AppConfig config = new AppConfig(logger);
	DatabaseWorker db = new DatabaseWorkerImpl(logger, config);
	WorkManager workManager = new WorkManagerImpl(logger, config, db);
	
	if (args == null || args.length == 0) {
	    new GUI(logger, config, workManager);
	}
	else {
	    new CLI(args, logger, config, workManager);
	}
    }
}
