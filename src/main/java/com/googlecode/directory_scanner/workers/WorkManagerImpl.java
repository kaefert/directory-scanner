/**
 * 
 */
package com.googlecode.directory_scanner.workers;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.DirectorySkipDecider;
import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.Sort;

/**
 * @author kaefert
 *
 */
public class WorkManagerImpl implements WorkManager {

    Logger logger;
    AppConfig config;
    DatabaseWorker db;
    
    /**
     * 
     */
    public WorkManagerImpl(Logger logger, AppConfig config, DatabaseWorker db) {
	this.logger = logger;
	this.config = config;
	this.db = db;
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#scanPath(java.lang.String)
     */
    @Override
    public void scanPath(String path) {
	
	final ArrayBlockingQueue<PathVisit> queue = new ArrayBlockingQueue<>(1000, false);
	final String pathString = path.toString();
		
	new Thread(new Runnable() {
	    @Override
	    public void run() {
		Integer scanDirId = db.getDirectoryId(pathString, true);
		Date after = config.getSkipDirectoriesDoneAfter();
		DirectorySkipDecider skipDecider = new DirectorySkipDeciderImpl(pathString, after, 1000, db);
		new DirectoryTreeWalker(logger, pathString, scanDirId, queue, skipDecider);
	    }
	}).start();
	
	new Thread(new Runnable() {
	    @Override
	    public void run() {
		new PathVisitProcessor(queue, logger, db);
	    }
	}).start();
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#checkExistence(java.lang.String)
     */
    @Override
    public void checkExistence(String path) {
	// TODO Auto-generated method stub

    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#forgetPath(java.lang.String)
     */
    @Override
    public void forgetPath(String path) {
	db.forgetDirectoryTree(path);
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#findFiles(java.lang.String, java.lang.String, boolean, com.googlecode.directory_scanner.domain.ReportMatch.Sort)
     */
    @Override
    public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort) {
	return db.findFiles(path1, path2, duplicates, sort);
    }

}
