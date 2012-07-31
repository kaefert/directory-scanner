package com.googlecode.directory_scanner.directory_scanner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Level;

public class Walker extends SimpleFileVisitor<Path> {

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		if (attr.isSymbolicLink()) {
			Worker.getLogger().info("Found Symbolic link: " + file);
		} else if (attr.isRegularFile()) {
			Worker.getLogger().info("Found Regular file: " + file);
		} else {
			Worker.getLogger().info("Found Other file: " + file);
		}

		// try {
		// System.out.println("sha1 = " + ChecksumSHA1.getSHA1Checksum(file));
		// } catch (NoSuchAlgorithmException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		Worker.getLogger().info("size = " + attr.size() + " bytes)");

		Worker.getSingelton().scanFile(file, attr);

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		if (Worker.getSingelton().decideDirectorySkip(dir, attrs)) {
			return FileVisitResult.SKIP_SUBTREE;
		} else {
			return FileVisitResult.CONTINUE;
		}

	}

	// Print each directory visited.
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		// System.out.format("Directory: %s%n", dir);

		Worker.getSingelton().finishedDirectory(dir);
		return FileVisitResult.CONTINUE;
	}

	// If there is some error accessing
	// the file, let the user know.
	// If you don't override this method
	// and an error occurs, an IOException
	// is thrown.
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		System.err.println(exc);
		return FileVisitResult.CONTINUE;
	}
	
	
	public void scanPath(String pathString) {
		try {
			Path path = FileSystems.getDefault().getPath(pathString);
			Files.walkFileTree(path, this);
		
		} catch (InvalidPathException e) {
			Worker.getLogger().log(Level.ERROR, "invalid path='"+pathString+"'", e);
		} catch (IOException e) {
			Worker.getLogger().log(Level.ERROR, "could not read path='"+pathString+"'", e);
		}
	}
}