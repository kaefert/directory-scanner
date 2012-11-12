package com.googlecode.directory_scanner.workers;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipFileDecider;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.PathVisit.Type;
import com.googlecode.directory_scanner.workers.ChecksumSHA1Calculator.Sha1WithSize;

public class PathVisitProcessor {

    private BlockingQueue<PathVisit> queue, dbInsertQueue;
    private DatabaseWorker db;
    private Logger logger;
    private SkipFileDecider skipDecider;
    private AppConfig config;
    private boolean working = true;

    public PathVisitProcessor(BlockingQueue<PathVisit> queue, Logger logger, DatabaseWorker db, SkipFileDecider skipDecider, AppConfig config) {
	this.queue = queue;
	this.logger = logger;
	this.db = db;
	this.skipDecider = skipDecider;
	this.config = config;

	dbInsertQueue = new ArrayBlockingQueue<>(this.config.getQueueLength());

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		process();
	    }
	}).start();

	new Thread(new Runnable() {
	    @Override
	    public void run() {
		processDbQueue();
	    }
	}).start();
    }

    private void process() {
	while (true) {
	    try {
		working = false;
		PathVisit pathVisit = queue.take();
		working = true;
		handlePathVisit(pathVisit);

		if (pathVisit == PathVisit.endOfQueue)
		    break;

	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    private void handlePathVisit(PathVisit pathVisit) {

	logger.debug("start handlePathVisit, path=" + pathVisit.getPath());
	boolean doDbInsertion = true;
	
	if (pathVisit.getType() == Type.FILE) {

	    if (skipDecider != null && skipDecider.decideFileSkip(pathVisit)) {
		doDbInsertion = false;
		logger.debug("skipping unchanged     file=" + pathVisit.getPath());
	    } else {
		try {
		    // although this is a long running action, we will not fork
		    // a thread for it, because:
		    // 1.) it is not healthy for disks to read multiple files at
		    //     ones.
		    // 2.) we do not want to insert directories as finished
		    //     before files within it
		    //     - the order of the queue must be kept

		    Sha1WithSize sha1WithSize = ChecksumSHA1Calculator.getSHA1Checksum(pathVisit.getPath());
		    pathVisit.fileScanned(sha1WithSize.getBytesRead(), sha1WithSize.getSha1());
		    logger.debug("finished sha1 calculation for file=" + pathVisit.getPath());

		} catch (NoSuchAlgorithmException | IOException e) {
		    logger.warn("could not calculate sha1 for file: " + pathVisit.getPath());
		}
	    }
	}

	if (doDbInsertion)
	    try {
		dbInsertQueue.put(pathVisit);
	    } catch (InterruptedException e) {
		logger.error("Interupted while inserting into dbInsertQueue", e);
	    }
    }

    private void processDbQueue() {
	while (true) {
	    try {
		PathVisit pathVisit = dbInsertQueue.take();

		if (pathVisit == PathVisit.endOfQueue)
		    break;

		doDbInsertion(pathVisit);

	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    private void doDbInsertion(PathVisit pathVisit) {

	String fullPath = pathVisit.getPath().toString();
	logger.trace("starting DbInsertion: path=" + fullPath + " // Type=" + pathVisit.getType().toString() + " ; dbid=" + pathVisit.getDBId());

	if (pathVisit.getType() == Type.FINISHED_DIRECTORY) {
	    db.insertDirectory(fullPath, pathVisit.getScanRoot(), true);
	} else if (pathVisit.getType() == Type.FILE) {

	    if (pathVisit.getBytesRead() == pathVisit.getSize()) {
		// sanity check succeeded, insert using the supplied data
		String fileName = pathVisit.getPath().getFileName().toString();
		String directory = pathVisit.getPath().getParent().toString();
		if(pathVisit.haveCheckedDB())
		    db.insertFile(fullPath, fileName, directory, pathVisit.getScanRoot(), pathVisit.getLastModified(), pathVisit.getSize(), pathVisit.getSha1(), pathVisit.getDBId());
		else
		    db.insertFile(fullPath, fileName, directory, pathVisit.getScanRoot(), pathVisit.getLastModified(), pathVisit.getSize(), pathVisit.getSha1());
	    } else {
		// sanity check failed. insert file as FAILURE
		db.insertFailure(fullPath, pathVisit.getScanRoot(), pathVisit.getSize(), pathVisit.getBytesRead(), "different sizes!");
	    }

	} else if (pathVisit.getType() == Type.FAILURE) {
	    // insert FAILURE
	    db.insertFailure(fullPath, pathVisit.getScanRoot(), pathVisit.getSize(), -1L, "Failed Visit");
	} else {
	    db.insertFailure(fullPath, pathVisit.getScanRoot(), pathVisit.getSize(), -1L, "unknown PathVisit type");
	}

	logger.trace("finished DbInsertion: path=" + fullPath + " // Type=" + pathVisit.getType().toString() + " ; dbid=" + pathVisit.getDBId());
    }

    public boolean isWorking() {
	return working;
    }
}
