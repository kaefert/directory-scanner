package com.googlecode.directory_scanner.directory_scanner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Worker {

    private Logger logger;
    private Properties config;
    private DatabaseHandler databaseHandler;

    private Integer checkDoneFiles;
    private Long skipDirectoriesScannedAgo;
    private Date skipDirectoriesDoneAfter = null;

    public Worker(Logger logger, Properties config) {
	this.logger = logger;
	this.config = config;
	this.databaseHandler = new DatabaseHandler(this.config, logger);

	checkDoneFiles = Integer.valueOf(config.getProperty("doneFiles"));
	// checkDoneDirectories =
	// Integer.valueOf(config.getProperty("doneDirectories"));
	skipDirectoriesScannedAgo = Long.valueOf(config.getProperty("skipDirectoriesScannedAgo"));
	if (skipDirectoriesScannedAgo >= 0)
	    skipDirectoriesDoneAfter = new Date(new Date().getTime() + skipDirectoriesScannedAgo);
	// if(checkDoneDirectories.equals(
    }

    public void scanPath(String pathString) {
	logger.info("scanning path: " + pathString);
	databaseHandler.insertDirectory(pathString, null, false);
	new PathWalker(logger, this, pathString);

	if (failedVisits != null && failedVisits.size() > 0) {
	    logger.info("PathWalker finished, printing failedVisits errors now:");
	    for (Map.Entry<Path, IOException> entry : failedVisits.entrySet()) {
		logger.warn(entry.getKey(), entry.getValue());
	    }
	    failedVisits.clear();
	} else {
	    logger.info("PathWalker finished, no failedVisits recorded.");
	}

    }

    public boolean preScanDirectory(Path dir, BasicFileAttributes attrs, String scanPath) {

	if (skipDirectoriesScannedAgo.equals(-1)) {
	    return false;
	} else {
	    Date finished = databaseHandler.getDirectoryDoneTime(dir.toString(), databaseHandler.getDirectory(scanPath));

	    if (finished != null && (skipDirectoriesScannedAgo < -1 || finished.after(skipDirectoriesDoneAfter))) {
		logger.log(Level.INFO, "skipping done directory: " + dir.toString());
		return true;
	    } else {
		logger.log(Level.INFO, "conditions for directory-skip not met, continue processing -> " + dir.toString());
		return false;
	    }
	}
    }

    public void postScanDirectory(Path dir, IOException exc, String scanPath) {
	databaseHandler.insertDirectory(dir.toString(), databaseHandler.getDirectory(scanPath), true);
    }

    public void scanFile(Path file, BasicFileAttributes attr, String scanPath) {

	String fullPath = file.toString();
	String fileName = file.getFileName().toString();
	String directory = file.getParent().toString();

	Integer dirId = databaseHandler.getDirectory(directory);

	boolean scanned = false, sized = false;

	if (checkDoneFiles.equals(1)) {
	    Long scannedSize = databaseHandler.getFileSize(fullPath);
	    if (scannedSize != null) {
		scanned = true;
		if (scannedSize.equals(attr.size()))
		    sized = true;
	    }
	}

	if (!scanned || !sized) {
	    try {
		String sha1 = ChecksumSHA1.getSHA1Checksum(file);
		logger.log(Level.INFO, "sha1 = " + sha1);
		databaseHandler.insertFile(fullPath, fileName, dirId, databaseHandler.getDirectory(scanPath), attr.size(), sha1);

	    } catch (NoSuchAlgorithmException | IOException e) {
		logger.error("failed to calculate sha1 checksum", e);
	    }

	} else {
	    logger.log(Level.INFO, "has already been scanned and has not changed size. Will ignore this file.");
	}

    }

    private HashMap<Path, IOException> failedVisits = new HashMap<>();

    public void visitFileFailed(Path path, IOException exc) {
	failedVisits.put(path, exc);
	logger.warn(path, exc);
    }

    public void setSkipDirectoriesDoneAfter(Date date) {
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	logger.trace("Setting skipDirectoriesDoneAfter = " + formatter.format(date));
	skipDirectoriesDoneAfter = date;
	skipDirectoriesScannedAgo = Math.max(0, new Date().getTime() - date.getTime());
    }

    public void forgetDoneDirectories() {
	databaseHandler.forgetDoneDirectories();
    }

    public void forgetPath(String s) {
	databaseHandler.forgetDirectoryTree(s);
    }

    public void printInfoOn(String text) {

	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	for (Map.Entry<String, Integer> entry : databaseHandler.getFileIds().entrySet()) {
	    if (entry.getKey().startsWith(text)) {
		Integer id = entry.getValue();
		Long filesize = databaseHandler.getFileSizes().get(id);
		Date scandate = databaseHandler.getFileScandates().get(id);
		logger.info("found file: id=\"" + id + "\", scandate=\"" + scandate == null ? "null" : formatter.format(scandate) + "\", size=\"" + filesize
		+ "\", path=\"" + entry.getKey() + "\"");
	    }
	}
    }

    public class ReportMatch {
	public String sha1;
	public long size;
	public HashSet<String> paths;
	public HashSet<Integer> fileIds;
	public int sawPath1, sawPath2;
	
	public ReportMatch(String sha1) {
	    this.sha1 = sha1;
	    this.fileIds = databaseHandler.getFileSha1s().get(sha1);
	    this.paths = new HashSet<>(2);
	    this.size = -1;
	    this.sawPath1 = 0;
	    this.sawPath2 = 0;
	}
    }
    
    /**
     * 
     * @param path1
     *            if not null only duplicates that have at least 1 instance
     *            below this path are printed
     * 
     * @param path2
     *            see path1. if equal path1 only duplicates that have at least 2
     *            instances below given path are printed.
     * 
     * @param opposite
     *            if false behaviour as drescribed in description for path1&2 if
     *            true, path1 & path2 must not be null, and any files are
     *            printed, that exist in path1 but not in path2
     * 
     */
    public HashSet<ReportMatch> findDuplicates(String path1, String path2, boolean opposite) {
	Set<String> sha1s = opposite ? databaseHandler.getFileSha1s().keySet() : databaseHandler.getNonUniqueSha1s();
	HashSet<ReportMatch> matches = new HashSet<>();
	for (String sha1 : sha1s) {
	    ReportMatch match = new ReportMatch(sha1);
	    for (Integer fileId : match.fileIds) {

		String filePath = databaseHandler.getFilePaths().get(fileId);
		long fileSizeCurrent = databaseHandler.getFileSizes().get(fileId);
		if (match.size == -1) {
		    match.size = fileSizeCurrent;
		} else if (match.size != fileSizeCurrent) {
		    logger.fatal("FOUND COLLISION - FILES WITH DIFFERENT SIZE BUT SAME SHA1 HASH = " + sha1 + "; size1=" + match.size + "; size2="
		    + fileSizeCurrent + "; path1=" + match.paths.iterator().next() + "; path2=" + filePath);
		}

		if (path1 != null && filePath.startsWith(path1))
		    match.sawPath1++;
		if (path2 != null && filePath.startsWith(path2)) {
		    match.sawPath2++;
		    if (opposite)
			break;
		}

		match.paths.add(filePath);
	    }

	    boolean trueMatch = false;
	    if (opposite) {
		if (match.sawPath1 > 0 && match.sawPath2 == 0) {
		    trueMatch = true;
		}
	    } else {
		if ((path1 == null || match.sawPath1 > 0) && (path2 == null || match.sawPath2 > 0)
		&& (path1 == null || path2 == null || !path1.equals(path2) || match.sawPath1 > 1)) {
		    trueMatch = true;
		}
	    }

	    if (trueMatch) {
		logger.info("found matching sha1=" + sha1 + "; size=" + match.size + "; existing @ paths:");
		for (String path : match.paths) {
		    logger.info(path);
		}
		matches.add(match);
	    }

	}
	logger.info("finished Worker.findDuplicates");
	return matches;
    }
}
