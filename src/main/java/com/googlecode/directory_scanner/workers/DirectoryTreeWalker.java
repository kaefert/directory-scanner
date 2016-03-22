package com.googlecode.directory_scanner.workers;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.SkipDirectoryDecider;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.ScanJob;

public class DirectoryTreeWalker extends SimpleFileVisitor<Path> {

	private Logger logger;
	private Integer scanPathId;
	private BlockingQueue<PathVisit> outputQueue;
	private BlockingQueue<ScanJob> inputQueue;
	private boolean working = true;

	private SkipDirectoryDecider skipDecider;

	// private AppConfig config;

	public DirectoryTreeWalker(Logger logger, BlockingQueue<PathVisit> outputQueue, BlockingQueue<ScanJob> inputQueue) {
		this.logger = logger;
		this.outputQueue = outputQueue;
		this.inputQueue = inputQueue;
		// this.skipDecider = skipDecider;
		// this.config = config;

		startInputQueueProcessor();
	}

	private void startInputQueueProcessor() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				while (true) {
					try {
						working = false;
						ScanJob scanJob = inputQueue.take();
						working = true;

						if (ScanJob.endOfQueue.equals(scanJob)) {
							outputQueue.add(PathVisit.endOfQueue);
							break;
						}	

						walkDirectory(scanJob);

					} catch (InterruptedException e) {
						logger.error("interrupted while taking Path String from dirQueue (or putting into scanQeue)", e);
					}
				}
			}
		}).start();
	}

	private void walkDirectory(ScanJob scanJob) {
		String scanPath = scanJob.getPath();
		skipDecider = scanJob.getSkipDecider();
		try {
			Path path = FileSystems.getDefault().getPath(scanPath);
			this.scanPathId = scanJob.getScanDirId();
			logger.info("starting scan with scanroot=\"" + path.toString() + "\"");
			Files.walkFileTree(path, this);

			// try {
			// outputQueue.put(PathVisit.endOfQueue);
			// } catch (InterruptedException e) {
			// logger.error("interrupted while trying to put FileVisit into queue", e);
			// }

			logger.info("finished scan with scanroot=\"" + scanPath + "\"");

		} catch (InvalidPathException e) {
			logger.log(Level.ERROR, "invalid path='" + scanPath + "'", e);
		} catch (IOException e) {
			logger.log(Level.ERROR, "could not read path='" + scanPath + "'", e);
		} finally {
			skipDecider = null;
		}
	}

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
		if (attr.isSymbolicLink()) {
			logger.info("Found Symbolic link (WONT PROCESS IT AS FILE): " + file + "; size=" + attr.size());
			return FileVisitResult.CONTINUE;
		}

		if (attr.isRegularFile()) {
			logger.info("Found Regular           file=" + file + "; size=" + attr.size());
		}

		if (attr.isOther()) {
			logger.info("Found Other             file=" + file + "; size=" + attr.size());
		}

		if (attr.isDirectory()) {
			logger.warn("path given to visitFile method is a directory, I think this will happen if a simlink links to a directory. I will not process this as file. path = "
					+ file);
		} else {
			try {
				outputQueue.put(new PathVisit(scanPathId, file, attr, PathVisit.Type.FILE));
			} catch (InterruptedException e) {
				logger.error("interrupted while trying to put FileVisit into queue", e);
			}
			// worker.scanFile(file, attr, scanPathId);
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
		if (skipDecider.decideDirectorySkip(dir, attrs)) {
			return FileVisitResult.SKIP_SUBTREE;
		} else {
			return FileVisitResult.CONTINUE;
		}
	}

	// Print each directory visited.
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		// System.out.format("Directory: %s%n", dir);

		try {
			outputQueue.put(new PathVisit(scanPathId, dir, null, PathVisit.Type.FINISHED_DIRECTORY));
		} catch (InterruptedException e) {
			logger.error("interrupted while trying to put FileVisit into queue", e);
		}

		// worker.postScanDirectory(dir, exc, scanPathId);
		return FileVisitResult.CONTINUE;
	}

	// If there is some error accessing
	// the file, let the user know.
	// If you don't override this method
	// and an error occurs, an IOException
	// is thrown.
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		try {
			outputQueue.put(new PathVisit(scanPathId, file, null, PathVisit.Type.FAILURE));
		} catch (InterruptedException e) {
			logger.error("interrupted while trying to put FileVisit into queue", e);
		}
		// worker.visitFileFailed(file, exc);
		return FileVisitResult.CONTINUE;
	}

	public boolean isWorking() {
		return working;
	}
}