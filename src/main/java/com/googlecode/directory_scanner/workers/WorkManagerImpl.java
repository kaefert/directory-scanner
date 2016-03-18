/**
 * 
 */
package com.googlecode.directory_scanner.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipDirectoryDecider;
import com.googlecode.directory_scanner.contracts.SkipFileDecider;
import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.PathVisit.Type;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ScanJob;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;
import com.googlecode.directory_scanner.domain.VisitFailure;

/**
 * @author kaefert
 * 
 */
public class WorkManagerImpl implements WorkManager {

	private Logger logger;
	private AppConfig config;
	private DatabaseWorker db;

	private DirectoryTreeWalker walker = null;
	private BlockingQueue<PathVisit> walkerOutputQueue = null;
	private BlockingQueue<ScanJob> walkerInputQueue = null;
	private PathVisitProcessor visitProcessor = null;

	private BlockingQueue<PathVisit> getWalkerOutputQueue() {
		if (walkerOutputQueue == null) {
			walkerOutputQueue = new ArrayBlockingQueue<>(config.getQueueLength());
			SkipFileDecider decider = null;

			if ("1".equals(config.getProperty("doneFiles")))
				decider = new SkipFileDeciderImpl(logger, db);

			visitProcessor = new PathVisitProcessor(walkerOutputQueue, logger, db, decider, config);
		}
		return walkerOutputQueue;
	}

	private BlockingQueue<ScanJob> getWalkerInputQueue() {
		if (walkerInputQueue == null) {
			walkerInputQueue = new ArrayBlockingQueue<>(config.getQueueLength());
			getWalker(); // also create the processor for this queue
		}
		return walkerInputQueue;
	}

	private boolean creatingWalker = false;

	private DirectoryTreeWalker getWalker() {
		if (walker == null && !creatingWalker) {
			creatingWalker = true;
			walker = new DirectoryTreeWalker(logger, getWalkerOutputQueue(), getWalkerInputQueue());
		}
		return walker;
	}

	/**
	 * 
	 */
	public WorkManagerImpl(Logger logger, AppConfig config, DatabaseWorker db) {
		this.logger = logger;
		this.config = config;
		this.db = db;
	}

	@Override
	public String getProfileStats() {
		return db.getProfileStats();
	}

	@Override
	public void setProfile(String name) {
		config.addProfile(name);
		db.setProfile(name);
	}

	@Override
	public List<String> getProfileList() {
		return config.getProfileList();
	}

	/**
	 * @see com.googlecode.directory_scanner.contracts.WorkManager#scanPath(java.lang.String)
	 */
	@Override
	public void scanPath(String path) {

		if ("1".equals(config.getProperty("dropIndexesBeforeScan")))
			db.indexesInsertingMode();

		db.forgetFailuresBelow(path);

		Date after = config.getSkipDirectoriesDoneAfter();
		int cacheDoneDirectories = Integer.parseInt(config.getProperty("cacheDoneDirectories"));
		SkipDirectoryDecider skipDecider = new SkipDirectoryDeciderImpl(path, after, cacheDoneDirectories, db, logger);
		Integer scanDirId = db.getDirectoryId(path, true);

		try {
			getWalkerInputQueue().put(new ScanJob(path, scanDirId, skipDecider));
		} catch (InterruptedException e) {
			logger.error("could not put ScanJob into walkerInputQueue!", e);
		}
	}

	/**
	 * @see com.googlecode.directory_scanner.contracts.WorkManager#checkExistence(java.lang.String)
	 */
	@Override
	public void checkExistence(String path) {

		BlockingQueue<String> dirQueue = db.findDirectoriesBelow(path);

		while (true) {
			try {
				String dbPath = dirQueue.take();

				if (config.getProperty("EndOfStringQueue").equals(dbPath))
					break;

				File file = new File(dbPath);
				if (!file.exists()) {
					logger.warn("directory does not exist any more! deleting from db --> " + dbPath);
					db.forgetDirectoryTree(dbPath);
				}
				// else {
				// logger.trace("directory from db still exists in filesystem
				// (do nothing) -> "
				// + dbPath);
				// }

			} catch (InterruptedException e) {
				logger.error("interrupted while taking Path String from dirQueue (or putting into scanQeue)", e);
			}
		}
		//

		BlockingQueue<ReportMatch> fileQueue = db.findFiles(path, null, false, Sort.SHA1, FindFilter.UNFILTERED);

		final Integer scanDir = db.getDirectoryId(path, true);

		while (true) {
			try {
				ReportMatch reportMatch = fileQueue.take();

				if (reportMatch == ReportMatch.endOfQueue) {
					// scanQueue.put(PathVisit.endOfQueue);
					break;
				}

				for (StoredFile stored : reportMatch.getStore()) {

					Path ioPath = Paths.get(stored.getFullPath());

					try {
						BasicFileAttributes attr = Files.readAttributes(ioPath, BasicFileAttributes.class,
								new LinkOption[] {});

						String problem = "";
						if (attr.size() != stored.getSize()) {
							problem = "different sizes";
							logger.warn("different sizes: " + attr.size() + " ; " + stored.getSize());
						}
						if (stored.getLastModified() == null) {
							problem += " no lastModified Date stored";
						} else if (attr.lastModifiedTime().toMillis() != stored.getLastModified().getTime()) {
							problem += " lastModified Date differs";
							logger.warn("lastModified differs: "
									+ config.getDateFormatter().format(new Date(attr.lastModifiedTime().toMillis()))
									+ " ; " + config.getDateFormatter().format(stored.getLastModified()));
						}
						if (problem.length() > 0) {
							logger.warn("will rescan file, because: " + problem + " -> " + stored.getFullPath());

							getWalkerOutputQueue().put(new PathVisit(scanDir, ioPath, attr, Type.FILE));

							// Sha1WithSize sha1WithSize =
							// ChecksumSHA1Calculator.getSHA1Checksum(stored.getFullPath());
							// pathVisit.fileScanned(sha1WithSize.getBytesRead(),
							// sha1WithSize.getSha1());
						}
						// else {
						// logger.info("db is up to date for file -> " +
						// stored.getFullPath());
						// }

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

	@Override
	public BlockingQueue<VisitFailure> getFailuresBelow(String path) {
		return db.loadFailuresBelow(path);
	}

	@Override
	public void checkFailuresBelow(String path) {
		BlockingQueue<VisitFailure> queue = db.loadFailuresBelow(path);
		Integer scanDirId = db.getDirectoryId(path, true);

		Date after = config.getSkipDirectoriesDoneAfter();
		int cacheDoneDirectories = Integer.parseInt(config.getProperty("cacheDoneDirectories"));
		SkipDirectoryDecider skipDecider = new SkipDirectoryDeciderImpl(path, after, cacheDoneDirectories, db, logger);

		String lastDirectory = null;
		while (true) {
			try {
				VisitFailure visit = queue.take();

				if (visit == VisitFailure.endOfQueue) {
					break;
				}

				File file = new File(visit.getPath());

				if (!file.exists()) {
					db.forgetFailure(visit.getFailureId());
				} else {

					if (file.isDirectory()) {
						if (lastDirectory == null || !lastDirectory.contains(visit.getPath())) {
							ScanJob scanJob = new ScanJob(visit.getPath(), scanDirId, skipDecider);
							getWalkerInputQueue().put(scanJob);
						}
					} else {
						Path nioPath = FileSystems.getDefault().getPath(visit.getPath());
						PathVisit pathVisit = new PathVisit(scanDirId, nioPath, null, Type.FILE);
						getWalkerOutputQueue().put(pathVisit);
					}
				}

			} catch (InterruptedException e) {
				logger.error(
						"interrupted while taking VisitFailure from queue, or putting into walkerInput or walkerOutput queue",
						e);
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
	public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort,
			FindFilter filter) {
		waitForWalkers();
		return db.findFiles(path1, path2, duplicates, sort, filter);
	}

	@Override
	public BlockingQueue<ReportMatch> findProblems() {
		waitForWalkers();
		return db.findProblems();
	}

	@Override
	public void moveOrCopyMatches(final BlockingQueue<ReportMatch> queue, final String from, final String to,
			final boolean copy, final boolean flatten) {
		Thread processor = new Thread(new Runnable() {
			@Override
			public void run() {
				logger.info("started moveOrCopyMatches Thread processor");
				while (true) {
					try {
						ReportMatch match = queue.take();
						if (match == ReportMatch.endOfQueue)
							break;
						moveOrCopySingleMatch(match, from, to, copy, flatten);
					} catch (InterruptedException e) {
						logger.error("reportMatchesProcessor was interrupted", e);
					}
				}
				// finished queue
			}
		});
		processor.start();
	}

	private String attachSlashIfNotThere(String source) {
		if (source.endsWith("/"))
			return source;
		else
			return source + "/";
	}

	private void moveOrCopySingleMatch(ReportMatch match, String from, String to, boolean copy, boolean flatten) {
		for (StoredFile stored : match.getStore()) {
			if (stored.getDirPath().startsWith(from)) {

				String name = stored.getFileName();
				String dir = attachSlashIfNotThere(to);

				if (!flatten) {
					dir = stored.getFullPath().replace(from, to).replace(name, "");
				}
				ensureDirExists(dir);
				String target = dir + name;

				if (copy) {
					copyFile(stored.getFullPath(), target);
					// copyFile(new File(stored.getFullPath()), new
					// File(target));
				} else {
					moveFile(stored.getFullPath(), target);
				}
			}
		}
	}

	private String ensureDirExists(String path) {
		ensureDirExists(new File(path));
		return path;
	}

	private File ensureDirExists(File target) {
		try {
			FileUtils.forceMkdir(target);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return target;
	}

	private void moveFile(String source, String target) {
		File fileSource = new File(source);
		File fileTarget = new File(target);
		boolean success = fileSource.renameTo(fileTarget);
		if (!success)
			if (copyFile(source, target))
				fileSource.delete();
	}

	private boolean copyFile(String source, String target) {

		// FileSystem fs = FileSystems.getDefault();
		// Path sourcePath = fs.getPath(source);
		// Path targetPath = fs.getPath(target);

		// Method1 : use java.nio.file.Files.copy
		Path srcPath = Paths.get(source);
		Path targePath = Paths.get(target);

		// configure how to copy or move a file.
		CopyOption[] options = new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES };

		// Copy srcFile to targetFile
		try {
			Files.copy(srcPath, targePath, options);
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Method2 : use apache commons.io
		File sourceFile = new File(source);
		File destFile = new File(target);
		try {
			FileUtils.copyFile(sourceFile, destFile, true);
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// try {
		// OutputStream targetStream = new FileOutputStream(target);
		// Files.copy(sourcePath, targetStream);
		// } catch (FileNotFoundException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// Method3 : using filechannels (doesn't preserve timestamps!)
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			if (!destFile.exists()) {
				destFile.createNewFile();
			}
			fileInputStream = new FileInputStream(sourceFile);
			fileOutputStream = new FileOutputStream(destFile);
			FileChannel sourceChnl = fileInputStream.getChannel();
			fileOutputStream.getChannel().transferFrom(sourceChnl, 0, sourceChnl.size());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}

	private void waitForWalkers() {
		while ((walkerInputQueue != null && (!walkerInputQueue.isEmpty() || getWalker().isWorking()))
				&& (walkerOutputQueue != null && (!walkerOutputQueue.isEmpty() || visitProcessor.isWorking()))) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IllegalMonitorStateException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void importFilesFromOtherProfile(String otherProfile, String pathToImport) {
		DatabaseWorkerImpl otherDB = new DatabaseWorkerImpl(logger, config);
		otherDB.setProfile(otherProfile);
		BlockingQueue<ReportMatch> fileQueue = otherDB.findFiles(pathToImport, null, false, Sort.SHA1, FindFilter.UNFILTERED);
		int importDirId = db.getDirectoryId(pathToImport, true);
		while (true) {
			try {
				ReportMatch reportMatch = fileQueue.take();

				if (reportMatch == ReportMatch.endOfQueue) {
					// scanQueue.put(PathVisit.endOfQueue);
					break;
				}

				for (StoredFile stored : reportMatch.getStore()) {
					Timestamp lastModified = new Timestamp(stored.getLastModified().getTime());
					db.insertFile(stored.getFullPath(), stored.getFileName(), stored.getDirPath(), importDirId, lastModified, stored.getSize(), reportMatch.getSha1());
				}

			} catch (InterruptedException e) {
				logger.error("interrupted while taking ReportMatch from queue (or putting into scanQeue)", e);
			}
		}
	}

	@Override
	public void quitWhenFinished() {
		if (walkerInputQueue != null) {
			walkerInputQueue.add(ScanJob.endOfQueue);
		}
		if (visitProcessor != null) {
			visitProcessor.quitWhenFinished();
		}
	}
}
