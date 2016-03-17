/**
 * 
 */
package com.googlecode.directory_scanner.workers;

import java.nio.file.attribute.FileTime;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;
import com.googlecode.directory_scanner.domain.VisitFailure;

/**
 * @author kaefert
 * 
 */
public class DatabaseWorkerImpl implements DatabaseWorker {

	Logger logger;
	private AppConfig config;
	private DatabaseConnectionHandler db;
	private String profile;

	/**
	 * 
	 */
	public DatabaseWorkerImpl(Logger logger, AppConfig config) {
		this.logger = logger;
		this.config = config;
		this.profile = config.getProperty("ProfileName1");
		this.db = new DatabaseConnectionHandler(logger, config);
	}

	@Override
	public String getProfileStats() {
		String stats = "Current: " + profile + " - ";
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement("SELECT count(id) FROM files");
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				stats += Integer.toString(result.getInt(1)) + " files & ";
			}
			PreparedStatement stmt2 = db.getConnection().prepareStatement("SELECT count(id) FROM directories");
			ResultSet result2 = stmt2.executeQuery();
			if (result2.next()) {
				stats += result2.getInt(1) + " dirs";
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return stats;
	}

	@Override
	public void setProfile(String name) {
		this.profile = name;
		db.setProfile(name);
	}

	/**
	 * @see com.googlecode.directory_scanner.contracts.DatabaseWorker#getDirectoriesDoneBelowAfterIfLessThen(java.lang.String,
	 *      java.util.Date, int)
	 */
	@Override
	public HashSet<String> getDirectoriesDoneBelowAfterIfLessThen(String below, Date after, int limit) {
		try {
			String sql = config.readSqlFromClasspath("selectDoneDirectories.sql");
			PreparedStatement stmt = db.getConnection().prepareStatement(sql);
			stmt.setTimestamp(1, new java.sql.Timestamp(after.getTime()));
			stmt.setString(2, below + "%");
			// stmt.setInt(3, limit + 1);
			logger.info("start executing selectDoneDirectories.sql");
			ResultSet result = stmt.executeQuery();
			logger.info("finished executing selectDoneDirectories.sql, start iterating");
			HashSet<String> returnVal = new HashSet<>();

			String addedLast = null;
			String current = null;
			while (result.next()) {
				current = result.getString(1);
				if (addedLast == null || !current.contains(addedLast)) {
					addedLast = current;
					returnVal.add(current);
				}
				if (returnVal.size() > limit)
					return null;
			}
			stmt.close();
			logger.info("finished iterating selectDoneDirectories.sql");

			// stmt =
			// db.getConnection().prepareStatement("SELECT path FROM directories
			// WHERE finished IS NOT NULL AND path LIKE ?");
			// stmt.setString(1, "/media/kaefert/%");

			if (returnVal.size() <= limit)
				return returnVal;
			else
				return null;

		} catch (SQLException e) {
			db.createDirectoriesTable(e);
			return getDirectoriesDoneBelowAfterIfLessThen(below, after, limit);
		}
	}

	/**
	 * @see com.googlecode.directory_scanner.contracts.DatabaseWorker#getDirectoryDoneAfter(java.lang.String,
	 *      java.util.Date)
	 */
	@Override
	public boolean getDirectoryDoneAfter(String path, Date after) {
		try {
			PreparedStatement stmt = db.getConnection()
					.prepareStatement("SELECT count(id) FROM directories WHERE finished > ? AND path = ?");
			stmt.setTimestamp(1, new java.sql.Timestamp(after.getTime()));
			stmt.setString(2, path);
			ResultSet result = stmt.executeQuery();
			result.next();
			boolean returnVal = result.getInt(1) > 0;
			stmt.close();
			return returnVal;

		} catch (SQLException e) {
			db.createDirectoriesTable(e);
			return getDirectoryDoneAfter(path, after);
		}
	}

	private CacheLoader<String, Optional<Integer>> dirIdLoader = new CacheLoader<String, Optional<Integer>>() {

		@Override
		public Optional<Integer> load(String key) throws Exception {

			try {
				logger.debug("starting loading directoryId for path = " + key);
				PreparedStatement stmt = db.getConnection()
						.prepareStatement("SELECT id FROM directories where path = ?");
				stmt.setString(1, key);
				ResultSet result = stmt.executeQuery();
				Integer id = null;

				if (result.next()) {
					id = result.getInt(1);
				}
				stmt.close();

				logger.debug("finished loading directoryId for path = " + key);

				// if(id != null)
				return Optional.fromNullable(id);
				// return id;

			} catch (SQLException e) {
				logger.warn("selecting directories failed, will try to create table", e);
				db.createDirectoriesTable(e);
				return load(key);
			}
		}
	};

	private LoadingCache<String, Optional<Integer>> dirIdCache = null;

	@Override
	public Integer getDirectoryId(String path, boolean createIfNotExists) {
		if (dirIdCache == null) {
			dirIdCache = CacheBuilder.newBuilder().maximumSize(Long.valueOf(config.getProperty("cacheDirIds")))
					.build(dirIdLoader);
		}
		try {
			Integer dirId = dirIdCache.get(path).orNull();
			if (dirId == null && createIfNotExists)
				return insertDirectory(path, null, null, false);
			return dirId;
		} catch (ExecutionException e) {
			logger.error("failed to load DirId using DirIdCache", e);
			return getDirectoryId(path, createIfNotExists);
		}
	}

	@Override
	public int insertDirectory(String path, Integer scanDir, boolean finished) {
		return insertDirectory(path, getDirectoryId(path, false), scanDir, finished);
	}

	/**
	 * Only use this method if you either already have the id of the directory
	 * and only want to update it, or if you are certain that the directory does
	 * not exist jet in the database you can pass a null as @dirId
	 * 
	 * @return dirId
	 */
	private int insertDirectory(String path, Integer dirId, Integer scanDir, boolean finished) {
		logger.info((dirId == null ? "inserting" : "updating") + (finished ? " finished " : " ") + "directory " + path);

		Timestamp finishedTimestamp = null;
		if (finished && !doFailuresExist(scanDir, path)) {
			// only mark a directory as finished, if no scanfailure happened
			// inside it
			finishedTimestamp = new java.sql.Timestamp(new java.util.Date().getTime());
		}

		try {
			if (dirId != null) {
				PreparedStatement stmt = null;

				if (finished) {
					stmt = db.getConnection()
							.prepareStatement("UPDATE directories SET scanDir_id = ?, finished = ? WHERE id = ?");
				} else {
					finishedTimestamp = new java.sql.Timestamp(new java.util.Date().getTime());
					stmt = db.getConnection()
							.prepareStatement("UPDATE directories SET scanDir_id = ?, scandate = ? WHERE id = ?");
				}

				if (scanDir == null)
					stmt.setNull(1, java.sql.Types.INTEGER);
				else
					stmt.setInt(1, scanDir);

				stmt.setTimestamp(2, finishedTimestamp);
				stmt.setInt(3, dirId);
				stmt.execute();
				stmt.close();
			} else {
				PreparedStatement stmt = db.getConnection().prepareStatement(
						"INSERT INTO directories (path, scanDir_id, scandate, finished) VALUES (?, ?, ?, ?)");
				stmt.setString(1, path);

				if (scanDir == null)
					stmt.setNull(2, java.sql.Types.INTEGER);
				else
					stmt.setInt(2, scanDir);

				stmt.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
				stmt.setTimestamp(4, (finished ? new java.sql.Timestamp(new java.util.Date().getTime()) : null));
				boolean resultSetExists = stmt.execute();
				stmt.close();

				if (resultSetExists) {
					logger.info("need to find the way!");
				}

				stmt = db.getConnection().prepareStatement("select LAST_INSERT_ID()");
				ResultSet result = stmt.executeQuery();
				if (result.next()) {
					dirId = result.getInt(1);
				}
				stmt.close();
			}

			dirIdCache.put(path, Optional.fromNullable(dirId));
			return dirId;

		} catch (SQLException e) {
			db.createDirectoriesTable(e);
			return insertDirectory(path, dirId, scanDir, finished);
		}
	}

	private boolean doFailuresExist(int scanDirId, String path) {
		String sql = "SELECT count(*) FROM failures WHERE scanDir_Id = ? AND path LIKE ?";
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement(sql);
			stmt.setInt(1, scanDirId);
			stmt.setString(2, path + "%");
			ResultSet result = stmt.executeQuery();
			if (result.next()) {
				if (result.getInt(1) == 0)
					return false;
				else
					return true;
			}

		} catch (SQLException e) {
			logger.error("could not get failures count!");
		}
		return false;
	}

	@Override
	public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir,
			FileTime lastModified, long size, byte[] sha1) {
		return insertFile(fullPath, fileName, getDirectoryId(containingDir, true), scanDir, lastModified, size, sha1);
	}

	@Override
	public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir,
			FileTime lastModified, long size, byte[] sha1, Integer fileId) {
		return insertFile(fullPath, fileName, getDirectoryId(containingDir, true), scanDir, lastModified, size, sha1,
				fileId);
	}

	private Integer insertFile(String fullPath, String fileName, int containingDir, int scanDir, FileTime lastModified,
			long size, byte[] sha1) {
		try {
			PreparedStatement stmt = db.getConnection()
					.prepareStatement("SELECT id FROM files WHERE dir_id = ? and filename = ?");
			stmt.setInt(1, containingDir);
			stmt.setString(2, fileName);
			ResultSet result = stmt.executeQuery();
			Integer fileId = null;
			if (result.next()) {
				fileId = result.getInt(1);
			}
			stmt.close();
			return insertFile(fullPath, fileName, containingDir, scanDir, lastModified, size, sha1, fileId);

		} catch (SQLException e) {
			db.createFilesTable(e);
			return insertFile(fullPath, fileName, containingDir, scanDir, lastModified, size, sha1);
		}
	}

	private void updateFile(int fileId, int scanDir, FileTime lastModified, long size, byte[] sha1) {
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement(
					"UPDATE files SET scanDir_id = ?, size = ?, sha1 = ?, scandate = ?, lastmodified = ? WHERE id = ?");

			stmt.setInt(1, scanDir);

			stmt.setLong(2, size);
			stmt.setBytes(3, sha1);

			stmt.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
			stmt.setTimestamp(5, new Timestamp(lastModified.toMillis()));
			stmt.setInt(6, fileId);
			stmt.execute();
			stmt.close();

		} catch (SQLException e) {
			logger.error(
					"updating file failed, cannot be a db config problem since to reach this point we have to have already read a fileid successfully from the db.",
					e);
		}
	}

	private Integer insertFile(String fullPath, String fileName, int containingDir, int scanDir, FileTime lastModified,
			long size, byte[] sha1, Integer fileId) {

		logger.info((fileId == null ? "inserting" : "updating") + " file; size=" + size + "; path=" + fullPath);

		if (fileId != null) {
			updateFile(fileId, scanDir, lastModified, size, sha1);
		} else {

			try {
				PreparedStatement stmt = db.getConnection().prepareStatement(
						"INSERT INTO files (dir_id, scanDir_id, filename, sha1, scandate, lastmodified, size) VALUES (?, ?, ?, ?, ?, ?, ?)");

				stmt.setInt(1, containingDir);
				stmt.setInt(2, scanDir);
				stmt.setString(3, fileName);
				stmt.setBytes(4, sha1);
				Timestamp timestamp = new Timestamp(new java.util.Date().getTime());
				stmt.setTimestamp(5, timestamp);
				stmt.setTimestamp(6, new Timestamp(lastModified.toMillis()));
				stmt.setLong(7, size);
				stmt.execute();
				stmt.close();

				stmt = db.getConnection().prepareStatement("select LAST_INSERT_ID()");
				ResultSet result = stmt.executeQuery();
				if (result.next()) {
					fileId = result.getInt(1);
				}
				stmt.close();

			} catch (SQLException e) {
				logger.warn("inserting file failed, will try to create table", e);
				db.createFilesTable(e);
				return insertFile(fullPath, fileName, containingDir, scanDir, lastModified, size, sha1);
			}
		}
		return fileId;
	}

	@Override
	public void insertFailure(String fullPath, int scanRoot, long size, long bytesRead, String error) {

		logger.info("inserting failure, size=" + size + ", bytesRead=" + bytesRead + ", path=" + fullPath + ", error="
				+ error);

		try {
			PreparedStatement stmt = db.getConnection().prepareStatement(
					"INSERT INTO failures (path, scanDir_id, scandate, size, sizeRead, error) VALUES (?, ?, ?, ?, ?, ?)");
			stmt.setString(1, fullPath);
			stmt.setInt(2, scanRoot);
			stmt.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
			stmt.setLong(4, size);
			stmt.setLong(5, bytesRead);
			stmt.setString(6, error);
			stmt.execute();
			stmt.close();

		} catch (SQLException e) {
			logger.warn("SQLException while trying to insert failure, trying to create table", e);
			db.createFailuresTable(e);
			insertFailure(fullPath, scanRoot, size, bytesRead, error);
		}
	}

	@Override
	public void forgetDirectoryTree(String path) {
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement("DELETE FROM directories WHERE path LIKE ?");
			stmt.setString(1, path + "%");
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			logger.error("forgetDirectoryTree failed - SQLException", e);
		}
	}

	@Override
	public void forgetFile(int dirId, String filename) {
		try {
			PreparedStatement stmt = db.getConnection()
					.prepareStatement("DELETE FROM files WHERE dir_id = ? AND filename = ?");
			stmt.setInt(1, dirId);
			stmt.setString(2, filename);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			logger.error("forgetDirectoryTree failed - SQLException", e);
		}
	}

	/**
	 * @see com.googlecode.directory_scanner.contracts.WorkManager#findFiles(java.lang.String,
	 *      java.lang.String, boolean,
	 *      com.googlecode.directory_scanner.domain.ReportMatch.Sort)
	 */
	@Override
	public BlockingQueue<ReportMatch> findFiles(final String path1F, final String path2F, final boolean duplicates,
			final Sort sort, final FindFilter filter) {

		if (path1F == null)
			throw new IllegalArgumentException("path1 must not be null!");

		final BlockingQueue<ReportMatch> queue = new ArrayBlockingQueue<>(config.getQueueLength());

		new Thread(new Runnable() {
			@Override
			public void run() {

				String path1 = path1F;
				String path2 = path2F;
				
				if(path2 != null && path2.contains(path1)) {
					/* switch paths because if overlapping, path1 must be the more general one (see selectFilesWithDuplicatesBelowPath.sql)*/
					path1 = path2F;
					path2 = path1F;
				}
				
				try {
					indexesReportingMode();
					
					String sql;
					if (duplicates) {
						if (path2 == null)
							sql = config.readSqlFromClasspath("selectFilesWithDuplicatesBelowPath.sql");
						else if (path1.equals(path2))
							sql = config.readSqlFromClasspath("selectFilesWithMoreDuplicatesBelowPath.sql");
						else {
							sql = config.readSqlFromClasspath("selectFilesWithDuplicatesBelowTwoPaths.sql");
							sql += "\n" + filter.getSQL();
						}

					} else {
						if (path2 == null)
							sql = config.readSqlFromClasspath("selectFilesBelowPath.sql");
						else
							sql = config.readSqlFromClasspath("selectFilesBelowPath1NotBelowPath2.sql");
					}

					logger.info("Starting findFiles Select Query");
					sql += "\n" + sort.getSQL();
					logger.debug("findFiles sql: \n\n" + sql);
					PreparedStatement stmt = db.getConnection().prepareStatement(sql);
					stmt.setString(1, path1 + "%");

					if (path2 != null) {
						if (duplicates && path1.equals(path2))
							stmt.setInt(2, 2);
						else {
							stmt.setString(2, path2 + "%");
						}
					}

					ResultSet result = stmt.executeQuery();
					logger.info("finished findFiles Select Query - starting to iterate result set");
					ReportMatch current = null;
					while (result.next()) {
						String dirPath = result.getString(1);
						// int dirId = result.getInt(2);
						String filename = result.getString(3);
						Integer fileId = result.getInt(4);
						long size = result.getLong(5);
						Date scandate = result.getTimestamp(6);
						byte[] sha1 = result.getBytes(7);
						Date lastmodified = result.getTimestamp(8);

						if (current == null || !Arrays.equals(current.getSha1(), sha1)) {
							if (current != null) {
								queue.put(current);
							}
							current = new ReportMatch(sha1, size);
						}

						current.getStore().add(new StoredFile(dirPath, filename, size, lastmodified, scandate, fileId));
						// current.getFileIds().add(fileId);

					}
					stmt.close();

					try {
						if (current != null)
							queue.put(current);
						queue.put(ReportMatch.endOfQueue);
					} catch (InterruptedException e) {
						logger.error("could not put ReportMatch.endOfQueue into queue", e);
					}
					logger.info("finished findFiles iterating result set");

				} catch (SQLException e) {
					logger.error("findFiles failed - SQLException", e);
				} catch (InterruptedException e) {
					logger.error("could not put ReportMatch into queue", e);
				}

			}
		}).start();

		return queue;
	}

	@Override
	public BlockingQueue<ReportMatch> findProblems() {

		final BlockingQueue<ReportMatch> queue = new ArrayBlockingQueue<>(config.getQueueLength());

		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					logger.debug("creating index sha1_size");
					tryExecute("CREATE INDEX sha1_size ON files (sha1, size)");

					logger.debug("finished creating index, starting findSha1Collisions");
					PreparedStatement s1cPS = db.getConnection()
							.prepareStatement(config.readSqlFromClasspath("selectSha1Collisions.sql"));
					ResultSet s1cRS = s1cPS.executeQuery();
					while (s1cRS.next()) {
						byte[] sha1 = s1cRS.getBytes(1);
						PreparedStatement s1dPS = db.getConnection()
								.prepareStatement(config.readSqlFromClasspath("selectSha1Details.sql"));
						s1dPS.setBytes(1, sha1);
						ResultSet s1dRS = s1dPS.executeQuery();
						ReportMatch rm = new ReportMatch(sha1, -1);
						rm.setMetadata("SHA1 Collision! sha1=" + AppConfig.getSha1HexString(sha1));
						while (s1dRS.next()) {
							rm.getStore()
									.add(new StoredFile(s1dRS.getString("path"), s1dRS.getString("filename"),
											s1dRS.getLong("size"), s1dRS.getTimestamp("lastmodified"),
											s1dRS.getTimestamp("scandate")));
						}
						s1dPS.close();
						queue.put(rm);
					}
					s1cPS.close();
					logger.debug(
							"finished itterating sha1Collisions and loading details for them. starting to check for directories that have been inserted multiple times");

					PreparedStatement ddPS = db.getConnection()
							.prepareStatement(config.readSqlFromClasspath("selectDirectoryDuplicates.sql"));
					ResultSet ddRS = ddPS.executeQuery();
					logger.debug("finished query selectDirectoryDuplicates, start iterating");
					while (ddRS.next()) {
						ReportMatch rm = new ReportMatch(null, -1);
						rm.setMetadata("duplicate directories");
						rm.getStore().add(new StoredFile(ddRS.getString(1), "", -1, null, null));
						queue.put(rm);
					}
					logger.debug(
							"finished iterating DirectoryDuplicates, will now try to create unique constraint ON files (filename, dir_id)");
					ddPS.close();

					PreparedStatement dfPS = db.getConnection()
							.prepareStatement("CREATE UNIQUE INDEX filename_dir ON files (filename, dir_id);");

					try {
						dfPS.execute();
					} catch (SQLException e) {
						if (e.getMessage().contains("Duplicate key name")
								|| e.getMessage().contains("already exists")) {
							logger.info(
									"unique constraint ON files (filename, dir_id) already exists, therefore this problem cannot exist; SQLException="
											+ e.getMessage());
						} else {
							logger.warn("could not create unique index, exception = " + e.getMessage()
									+ " (starting selectWrongFileDuplicates.sql)");

							ReportMatch rm = new ReportMatch(null, -1);
							rm.setMetadata("files that have the same dir_id & filename exist!");
							rm.getStore().add(new StoredFile(e.getMessage(), "", -1, null, null));
							queue.put(rm);

							PreparedStatement dfdPS = db.getConnection()
									.prepareStatement(config.readSqlFromClasspath("selectWrongFileDuplicates.sql"));
							ResultSet dfdRS = dfdPS.executeQuery();
							logger.debug(
									"finished selectWrongFileDuplicates.sql executeQuery, start iterating resultset");
							while (dfdRS.next()) {
								int dirId = dfdRS.getInt("dir_id");
								String filename = dfdRS.getString("filename");
								int fileId = dfdRS.getInt("id");
								ReportMatch rm2 = new ReportMatch(null, -1);
								rm2.setMetadata("WrongFileDuplicate, dirid=" + dirId + ", filename=" + filename
										+ ", file_id=" + fileId);
								queue.put(rm2);
							}
							logger.debug("finished iterating resultset of selectWrongFileDuplicates.sql");

						}
					} finally {
						dfPS.close();
					}

					queue.put(ReportMatch.endOfQueue);
					logger.info("finished DatabaseWorkerImpl.findProblems(..)");

				} catch (SQLException e) {
					logger.error("findProblems failed unexpectedly - SQLException", e);
				} catch (InterruptedException e) {
					logger.error("interrupted while putting reportmatch into queue!");
				}
			}
		}).start();

		return queue;
	}

	@Override
	public BlockingQueue<String> findDirectoriesBelow(final String path) {
		final BlockingQueue<String> queue = new ArrayBlockingQueue<>(config.getQueueLength());

		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					PreparedStatement stmt = db.getConnection()
							.prepareStatement("SELECT path FROM directories WHERE path LIKE ?");
					stmt.setString(1, path + '%');
					ResultSet result = stmt.executeQuery();

					try {
						while (result.next()) {
							queue.put(result.getString(1));
						}
						queue.put(config.getProperty("EndOfStringQueue"));
					} catch (InterruptedException e) {
						logger.error("could not put String into queue", e);
					} finally {
						stmt.close();
					}

				} catch (SQLException e) {
					logger.error("findDirectoriesBelow failed - SQLException", e);
				}

			}
		}).start();

		return queue;
	}

	@Override
	public BlockingQueue<VisitFailure> loadFailuresBelow(final String path) {
		final BlockingQueue<VisitFailure> queue = new ArrayBlockingQueue<>(config.getQueueLength());

		new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					deleteOldFailures();

					PreparedStatement stmt = db.getConnection().prepareStatement(
							"SELECT id, path, scandate, size, sizeRead, error FROM failures WHERE path LIKE ? ORDER BY path");
					stmt.setString(1, path + '%');
					ResultSet result = stmt.executeQuery();

					try {
						while (result.next()) {
							int id = result.getInt(1);
							String path = result.getString(2);
							Date scandate = result.getTimestamp(3);
							long size = result.getLong(4);
							long sizeRead = result.getLong(5);
							String error = result.getString(6);
							queue.put(new VisitFailure(id, path, scandate, size, sizeRead, error));
						}
						queue.put(VisitFailure.endOfQueue);
					} catch (InterruptedException e) {
						logger.error("could not put VisitFailure into queue", e);
					} finally {
						stmt.close();
					}

				} catch (SQLException e) {
					logger.error("loadFailuresBelow failed - SQLException", e);
				}

			}
		}).start();

		return queue;
	}

	private void deleteOldFailures() {
		try {
			PreparedStatement stmt = db.getConnection()
					.prepareStatement(config.readSqlFromClasspath("deleteOldFailureDuplicates.sql"));
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			logger.error("deleteOldFailures failed - SQLException", e);
		}
	}

	@Override
	public void forgetFailure(int failureId) {
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement("DELETE FROM failures WHERE id = ?");
			stmt.setInt(1, failureId);
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			logger.error("forgetFailure failed - SQLException", e);
		}
	}

	@Override
	public void forgetFailuresBelow(String path) {
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement("DELETE FROM failures WHERE path LIKE ?");
			stmt.setString(1, path + "%");
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			logger.error("forgetFailuresBelow failed - SQLException", e);
		}
	}

	@Override
	public StoredFile getFile(String dir, String fileName) {
		Integer dirId = getDirectoryId(dir, false);
		if (dirId == null)
			return null;

		try {
			PreparedStatement stmt = db.getConnection()
					.prepareStatement(config.readSqlFromClasspath("selectFileDetails.sql"));
			stmt.setString(1, dir);
			stmt.setString(2, fileName);
			ResultSet result = stmt.executeQuery();

			StoredFile storedFile = null;
			if (result.next()) {
				Integer fileId = result.getInt(1);
				Long size = result.getLong(2);
				Date modified = result.getTimestamp(3);
				Date scan = result.getTimestamp(4);
				storedFile = new StoredFile(dir, fileName, size, modified, scan, fileId);
			}
			stmt.close();
			return storedFile;

			// if(id != null)
			// return Optional.fromNullable(id);
			// return id;

		} catch (SQLException e) {
			logger.warn("selecting directories failed, will try to create table", e);
			db.createDirectoriesTable(e);
			db.createFilesTable(e);
			return getFile(dir, fileName);
		}
	}

	private Set<String> getIndexNames() {

		HashSet<String> returnVal = new HashSet<>();
		String sql = db.isUsingFallback() ? "SELECT * FROM information_schema.indexes WHERE table_name='FILES'"
				: "SHOW INDEX FROM files";
		try {
			PreparedStatement stmt = db.getConnection().prepareStatement(sql);
			ResultSet result = stmt.executeQuery();
			while (result.next()) {
				String indexName = result.getString(db.isUsingFallback() ? "INDEX_NAME" : "Key_name");
				returnVal.add(indexName);
			}
		} catch (SQLException e) {
			logger.error("could not load index names", e);
		}

		return returnVal;
	}

	private void indexesReportingMode() {
		try {

			logger.debug("starting switch to indexesReportingMode");

			Set<String> existingIndexes = getIndexNames();
			boolean sha1IndexExists = false, sizeIndexExists = false;
			for (String index : existingIndexes) {
				if ("size".equals(index))
					sizeIndexExists = true;
				else if ("sha1".equals(index))
					sha1IndexExists = true;
			}
			if (!sha1IndexExists) {
				PreparedStatement stmt = db.getConnection().prepareStatement("CREATE INDEX files_sha1 ON files (sha1)");
				stmt.execute();
				stmt.close();
			}
			if (!sizeIndexExists) {
				PreparedStatement stmt = db.getConnection().prepareStatement("CREATE INDEX files_size ON files (size)");
				stmt.execute();
				stmt.close();
			}
		} catch (SQLException e) {
			logger.error("problem while creating indexes");
		}
	}

	@Override
	public void indexesInsertingMode() {

		logger.debug("starting switch to indexesInsertingMode");

		// if (!db.isUsingFallback())
		tryExecute("CREATE INDEX dirid_filename ON files (dir_id, filename)");

		// tryExecute("CREATE INDEX ");

		Set<String> existingIndexes = getIndexNames();
		HashSet<String> dontDropOrAlreadyDropped = new HashSet<>();
		dontDropOrAlreadyDropped.add("dirid_filename");

		for (String indexName : existingIndexes) {
			if (!dontDropOrAlreadyDropped.contains(indexName)) {
				if (db.isUsingFallback()) {
					if (!(indexName.contains("CONSTRAINT_INDEX") || indexName.equals("PRIMARY_KEY"))) {
						dropSingleIndex(indexName);
						dontDropOrAlreadyDropped.add(indexName);
					}
				} else {
					if (!("PRIMARY".equals(indexName) || "scanDir_id".equals(indexName))) {
						// || "dir_id".equals(indexName)
						dropSingleIndex(indexName);
						dontDropOrAlreadyDropped.add(indexName);
					}
				}
			}
		}

		logger.debug("finished switch to indexesInsertingMode");
	}

	private boolean dropSingleIndex(String indexName) {
		try {
			PreparedStatement stmt;
			if (db.isUsingFallback()) {
				stmt = db.getConnection().prepareStatement("DROP INDEX " + indexName);
			} else {
				stmt = db.getConnection().prepareStatement("DROP INDEX ? ON files");
				stmt.setString(1, indexName);
			}
			return tryExecute(stmt);
		} catch (SQLException e) {
			logger.warn("could not drop index " + indexName, e);
			return false;
		}
	}

	private boolean tryExecute(String sql) {
		try {
			return tryExecute(db.getConnection().prepareStatement(sql));
		} catch (SQLException e) {
			logger.warn("could not create stmt with sql: " + sql);
			return false;
		}
	}

	private boolean tryExecute(PreparedStatement stmt) {
		try {
			return stmt.execute();
		} catch (SQLException e) {
			logger.warn("could not execute stmt: " + stmt.toString().substring(db.isUsingFallback() ? 7 : 48)
					+ " ; SQLException=" + e.getMessage());
			return false;
		}
	}
}
