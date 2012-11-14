/**
 * 
 */
package com.googlecode.directory_scanner.workers;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipDirectoryDecider;
import com.googlecode.directory_scanner.contracts.SkipFileDecider;
import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.PathVisit.Type;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.Sort;
import com.googlecode.directory_scanner.domain.ScanJob;
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
			walkerOutputQueue = new ArrayBlockingQueue<>(
					config.getQueueLength());
			SkipFileDecider decider = null;

			if ("1".equals(config.getProperty("doneFiles")))
				decider = new SkipFileDeciderImpl(logger, db);

			visitProcessor = new PathVisitProcessor(walkerOutputQueue, logger,
					db, decider, config);
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
			walker = new DirectoryTreeWalker(logger, getWalkerOutputQueue(),
					getWalkerInputQueue());
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
		int cacheDoneDirectories = Integer.parseInt(config
				.getProperty("cacheDoneDirectories"));
		SkipDirectoryDecider skipDecider = new SkipDirectoryDeciderImpl(path,
				after, cacheDoneDirectories, db, logger);
		Integer scanDirId = db.getDirectoryId(path, true);

		try {
			getWalkerInputQueue()
					.put(new ScanJob(path, scanDirId, skipDecider));
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
					logger.warn("directory does not exist any more! deleting from db --> "
							+ dbPath);
					db.forgetDirectoryTree(dbPath);
				}
				// else {
				// logger.trace("directory from db still exists in filesystem (do nothing) -> "
				// + dbPath);
				// }

			} catch (InterruptedException e) {
				logger.error(
						"interrupted while taking Path String from dirQueue (or putting into scanQeue)",
						e);
			}
		}
		//

		BlockingQueue<ReportMatch> fileQueue = db.findFiles(path, null, false,
				Sort.NOSORT);

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
						BasicFileAttributes attr = Files.readAttributes(ioPath,
								BasicFileAttributes.class, new LinkOption[] {});

						String problem = "";
						if (attr.size() != stored.getSize()) {
							problem = "different sizes";
							logger.warn("different sizes: " + attr.size()
									+ " ; " + stored.getSize());
						}
						if (stored.getLastModified() == null) {
							problem += " no lastModified Date stored";
						} else if (attr.lastModifiedTime().toMillis() != stored
								.getLastModified().getTime()) {
							problem += " lastModified Date differs";
							logger.warn("lastModified differs: "
									+ config.getDateFormatter().format(
											new Date(attr.lastModifiedTime()
													.toMillis()))
									+ " ; "
									+ config.getDateFormatter().format(
											stored.getLastModified()));
						}
						if (problem.length() > 0) {
							logger.warn("will rescan file, because: " + problem
									+ " -> " + stored.getFullPath());

							getWalkerOutputQueue().put(
									new PathVisit(scanDir, ioPath, attr,
											Type.FILE));

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
						logger.warn("file does not exist anymore -> "
								+ stored.getFullPath());
						db.forgetFile(
								db.getDirectoryId(stored.getDirPath(), false),
								stored.getFileName());
					}
					// }
				}

			} catch (InterruptedException e) {
				logger.error(
						"interrupted while taking ReportMatch from queue (or putting into scanQeue)",
						e);
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
		int cacheDoneDirectories = Integer.parseInt(config
				.getProperty("cacheDoneDirectories"));
		SkipDirectoryDecider skipDecider = new SkipDirectoryDeciderImpl(path,
				after, cacheDoneDirectories, db, logger);

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
						if (lastDirectory == null
								|| !lastDirectory.contains(visit.getPath())) {
							ScanJob scanJob = new ScanJob(visit.getPath(),
									scanDirId, skipDecider);
							getWalkerInputQueue().put(scanJob);
						}
					} else {
						Path nioPath = FileSystems.getDefault().getPath(
								visit.getPath());
						PathVisit pathVisit = new PathVisit(scanDirId, nioPath,
								null, Type.FILE);
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
	public BlockingQueue<ReportMatch> findFiles(String path1, String path2,
			boolean duplicates, Sort sort) {
		waitForWalkers();
		return db.findFiles(path1, path2, duplicates, sort);
	}

	@Override
	public BlockingQueue<ReportMatch> findProblems() {
		waitForWalkers();
		return db.findProblems();
	}

	@Override
	public void moveOrCopyMatches(final BlockingQueue<ReportMatch> queue,
			final String from, final String to, final boolean copy,
			final boolean flatten) {
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
						logger.error("reportMatchesProcessor was interrupted",
								e);
					}
				}
				// finished queue
			}
		});
		processor.start();
	}
	
	private String attachSlashIfNotThere(String source) {
		if(source.endsWith("/"))
			return source;
		else
			return source + "/";
	}

	private void moveOrCopySingleMatch(ReportMatch match, String from,
			String to, boolean copy, boolean flatten) {
		for (StoredFile stored : match.getStore()) {
			if (stored.getDirPath().startsWith(from)) {

				String target = flatten ? attachSlashIfNotThere(to) + stored.getFileName() 
						: stored.getFullPath().replace(from, to);

				if (copy) {
					try {
						FileChannel sourceChannel = FileChannel
								.open(FileSystems.getDefault().getPath(
										stored.getFullPath()));
						FileChannel targetChannel = FileChannel
								.open(FileSystems.getDefault().getPath(target), StandardOpenOption.CREATE);
						sourceChannel.transferTo(0, Long.MAX_VALUE,
								targetChannel);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					File file = new File(stored.getFullPath());
					file.renameTo(new File(target));
				}

			}
		}
	}

	private void waitForWalkers() {
		while ((walkerInputQueue != null && (!walkerInputQueue.isEmpty() || getWalker()
				.isWorking()))
				&& (walkerOutputQueue != null && (!walkerOutputQueue.isEmpty() || visitProcessor
						.isWorking()))) {
			try {
				this.wait(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
