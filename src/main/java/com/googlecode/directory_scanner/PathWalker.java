package com.googlecode.directory_scanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PathWalker extends SimpleFileVisitor<Path> {

    private Logger logger;
    private Worker worker;
    private String scanPath;

    public PathWalker(Logger logger, Worker worker, String scanPath) {
	this.logger = logger;
	this.worker = worker;
	this.scanPath = scanPath;
	
	try {
	    Path path = FileSystems.getDefault().getPath(scanPath);
	    logger.info("starting scan with scanroot=\"" + scanPath + "\"");
	    Files.walkFileTree(path, this);
	    logger.info("finished scan with scanroot=\"" + scanPath + "\"");

	} catch (InvalidPathException e) {
	    logger.log(Level.ERROR, "invalid path='" + scanPath + "'", e);
	} catch (IOException e) {
	    logger.log(Level.ERROR, "could not read path='" + scanPath + "'", e);
	}
    }

    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
	if (attr.isSymbolicLink()) {
	    logger.info("Found Symbolic link: " + file);
	}

	if (attr.isRegularFile()) {
	    logger.info("Found Regular file: " + file);
	}

	if (attr.isOther()) {
	    logger.info("Found Other file: " + file);
	}

	if (attr.isDirectory()) {
	    logger.info("path given to visitFile method is a directory! --> " + file);
	} else {
	    logger.info("size = " + attr.size() + " bytes)");
	    worker.scanFile(file, attr, scanPath);
	}

	// try {
	// System.out.println("sha1 = " + ChecksumSHA1.getSHA1Checksum(file));
	// } catch (NoSuchAlgorithmException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }

	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
	if (worker.preScanDirectory(dir, attrs, scanPath)) {
	    return FileVisitResult.SKIP_SUBTREE;
	} else {
	    return FileVisitResult.CONTINUE;
	}

    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
	// System.out.format("Directory: %s%n", dir);

	worker.postScanDirectory(dir, exc, scanPath);
	return FileVisitResult.CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
	worker.visitFileFailed(file, exc);
	return FileVisitResult.CONTINUE;
    }
}