/**
 * 
 */
package com.googlecode.directory_scanner.workers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipDecider;
import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.PathVisit.Type;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;

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

	Date after = config.getSkipDirectoriesDoneAfter();
	int cacheDoneDirectories = Integer.parseInt(config.getProperty("cacheDoneDirectories"));
	int cacheFiles = Integer.parseInt(config.getProperty("cacheDoneDirectories"));
	final SkipDecider skipDecider = new SkipDeciderImpl(pathString, after, cacheDoneDirectories, cacheFiles, db);

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		Integer scanDirId = db.getDirectoryId(pathString, true);
		new DirectoryTreeWalker(logger, pathString, scanDirId, queue, skipDecider);
	    }
	}).start();

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		new PathVisitProcessor(queue, logger, db, "1".equals(config.getProperty("doneFiles")) ? skipDecider : null);
	    }
	}).start();
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#checkExistence(java.lang.String)
     */
    @Override
    public void checkExistence(String path) {
	
	BlockingQueue<String> dirQueue = db.findDirectoriesBelow(path);

	while(true) {
	    try {
		String dbPath = dirQueue.take();
		
		if(config.getProperty("EndOfStringQueue").equals(dbPath))
		    break;
		
		if(!new File(dbPath).exists()) {
		    logger.warn("directory does not exist any more! deleting from db --> " + dbPath);
		    db.forgetDirectoryTree(dbPath);
		}
		else {
		    logger.info("directory from db still exists in filesystem (do nothing) -> " + dbPath);
		}
		
	    } catch (InterruptedException e) {
		logger.error("interrupted while taking Path String from dirQueue (or putting into scanQeue)", e);
	    }
	}
//	
	
	BlockingQueue<ReportMatch> fileQueue = db.findFiles(path, null, false, Sort.NOSORT);
	
	final Integer scanDir = db.getDirectoryId(path, true);
	final BlockingQueue<PathVisit> scanQueue = new ArrayBlockingQueue<>(1000);
	new Thread(new Runnable() {
	    @Override
	    public void run() {
		new PathVisitProcessor(scanQueue, logger, db, null);
	    }
	}).start();

	while (true) {
	    try {
		ReportMatch reportMatch = fileQueue.take();

		if (reportMatch == ReportMatch.endOfQueue) {
		    scanQueue.put(PathVisit.endOfQueue);
		    break;
		}
		    

		for (StoredFile stored : reportMatch.getStore()) {

		    Path ioPath = Paths.get(stored.getFullPath());
		    // if (Files.exists(ioPath, (LinkOption) null)) {
		    // logger.warn("file does not exist anymore -> " +
		    // stored.getFullPath());
		    // // TODO: remove file from database
		    // } else {

		    try {
			BasicFileAttributes attr = Files.readAttributes(ioPath, BasicFileAttributes.class, new LinkOption[] {});

			String problem = "";
			if (attr.size() != stored.getSize()) {
			    problem += " different sizes";
			    logger.warn("different sizes: " + attr.size() + " ; " + stored.getSize());
			}
			if (stored.getLastModified() == null) {
			    problem += " no lastModified Date stored";
			} else if (attr.lastModifiedTime().toMillis() != stored.getLastModified().getTime()) {
			    problem += " lastModified Date differs";
			    logger.warn("lastModified differs: " + config.getDateFormatter().format(new Date(attr.lastModifiedTime().toMillis())) + " ; "
			    + config.getDateFormatter().format(stored.getLastModified()));
			}
			if (problem.length() > 0) {
			    logger.warn("will rescan file, because: " + problem + " -> " + stored.getFullPath());

			    scanQueue.put(new PathVisit(scanDir, ioPath, attr, Type.FILE));

			    // Sha1WithSize sha1WithSize =
			    // ChecksumSHA1Calculator.getSHA1Checksum(stored.getFullPath());
			    // pathVisit.fileScanned(sha1WithSize.getBytesRead(),
			    // sha1WithSize.getSha1());
			}
			else {
			    logger.info("db is up to date for file -> " + stored.getFullPath());
			}

		    } catch (IOException e) {
			logger.warn("file does not exist anymore -> " + stored.getFullPath());
			db.forgetFile(db.getDirectoryId(stored.getDirPath(), false), stored.getFileName());
		    }
		    // }
		}

	    } catch (InterruptedException e) {
		logger.error("interrupted while taking ReportMatch from queue (or putting into scanQeue)", e);
	    }
	}
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#forgetPath(java.lang.String)
     */
    @Override
    public void forgetPath(String path) {
	db.forgetDirectoryTree(path);
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.WorkManager#findFiles(java.lang.String,
     *      java.lang.String, boolean,
     *      com.googlecode.directory_scanner.domain.ReportMatch.Sort)
     */
    @Override
    public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort) {
	return db.findFiles(path1, path2, duplicates, sort);
    }

    @Override
    public BlockingQueue<ReportMatch> findSha1Collisions() {
	return db.findSha1Collisions();
    }

}
