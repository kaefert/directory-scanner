package com.googlecode.directory_scanner.directory_scanner;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class TreeWalker extends SimpleFileVisitor<Path> {

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		if (attr.isSymbolicLink()) {
			System.out.format("Symbolic link: %s ", file);
		} else if (attr.isRegularFile()) {
			System.out.format("Regular file: %s ", file);
		} else {
			System.out.format("Other: %s ", file);
		}

		// try {
		// System.out.println("sha1 = " + ChecksumSHA1.getSHA1Checksum(file));
		// } catch (NoSuchAlgorithmException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		System.out.println("(" + attr.size() + "bytes)");

		DatabaseWorker.writeFile(file, attr);

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
			throws IOException {
		if (DatabaseWorker.skipDirectory(dir, attrs)) {
			return FileVisitResult.SKIP_SUBTREE;
		} else {
			return FileVisitResult.CONTINUE;
		}

	}

	// Print each directory visited.
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		// System.out.format("Directory: %s%n", dir);

		DatabaseWorker.finishedDirectory(dir);
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
}