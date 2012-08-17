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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;

/**
 * @author kaefert
 * 
 */
public class DatabaseWorkerImpl implements DatabaseWorker {

    private Logger logger;
    private AppConfig config;
    private DatabaseConnectionHandler db;

    /**
     * 
     */
    public DatabaseWorkerImpl(Logger logger, AppConfig config) {
	this.logger = logger;
	this.config = config;
	this.db = new DatabaseConnectionHandler(logger, config);
    }

    /**
     * @see com.googlecode.directory_scanner.contracts.DatabaseWorker#getDirectoriesDoneBelowAfterIfLessThen(java.lang.String,
     *      java.util.Date, int)
     */
    @Override
    public HashSet<String> getDirectoriesDoneBelowAfterIfLessThen(String below, Date after, int limit) {
	try {
	    PreparedStatement stmt = db.getConnection().prepareStatement("SELECT path FROM directories WHERE finished > ? AND path LIKE ? LIMIT ?");
	    stmt.setTimestamp(1, new java.sql.Timestamp(after.getTime()));
	    stmt.setString(2, below);
	    stmt.setInt(3, limit + 1);
	    ResultSet result = stmt.executeQuery();

	    HashSet<String> returnVal = new HashSet<>();

	    while (result.next()) {
		returnVal.add(result.getString(1));
	    }
	    stmt.close();

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
	    PreparedStatement stmt = db.getConnection().prepareStatement("SELECT count(id) FROM directories WHERE finished > ? AND path = ?");
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
		PreparedStatement stmt = db.getConnection().prepareStatement("SELECT id FROM directories where path = ?");
		stmt.setString(1, key);
		ResultSet result = stmt.executeQuery();
		Integer id = null;

		if (result.next()) {
		    id = result.getInt(1);
		}
		stmt.close();

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
	    dirIdCache = CacheBuilder.newBuilder().maximumSize(Long.valueOf(config.getProperty("cacheDirIds"))).build(dirIdLoader);
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
	try {
	    if (dirId != null) {
		PreparedStatement stmt = null;
		if (finished)
		    stmt = db.getConnection().prepareStatement("UPDATE directories SET scanDir_id = ?, finished = ? WHERE id = ?");
		else
		    stmt = db.getConnection().prepareStatement("UPDATE directories SET scanDir_id = ?, scandate = ? WHERE id = ?");

		if (scanDir == null)
		    stmt.setNull(1, java.sql.Types.INTEGER);
		else
		    stmt.setInt(1, scanDir);

		stmt.setTimestamp(2, new java.sql.Timestamp(new java.util.Date().getTime()));
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

    @Override
    public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir, FileTime lastModified, long size, byte[] sha1) {
	return insertFile(fullPath, fileName, getDirectoryId(containingDir, true), scanDir, lastModified, size, sha1);
    }

    private Integer insertFile(String fullPath, String fileName, int containingDir, int scanDir, FileTime lastModified, long size, byte[] sha1) {
	try {
	    PreparedStatement stmt = db.getConnection().prepareStatement("SELECT id FROM files WHERE dir_id = ? and filename = ?");
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

    private Integer insertFile(String fullPath, String fileName, int containingDir, int scanDir, FileTime lastModified, long size, byte[] sha1, Integer fileId) {

	logger.info((fileId == null ? "inserting" : "updating") + "file; size=" + size + "; path=" + fullPath);

	try {
	    if (fileId != null) {
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
	    } else {
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
	    }
	} catch (SQLException e) {
	    logger.warn("inserting file failed, will try to create table", e);
	    db.createFilesTable(e);
	    return insertFile(fullPath, fileName, containingDir, scanDir, lastModified, size, sha1);
	}
	return fileId;
    }

    @Override
    public void insertFailure(String fullPath, int scanRoot, long size, long bytesRead, String error) {

	logger.info("inserting failure, size=" + size + ", bytesRead=" + bytesRead + ", path=" + fullPath + ", error=" + error);

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
	    PreparedStatement stmt = db.getConnection().prepareStatement("DELETE FROM files WHERE dir_id = ? AND filename = ?");
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
    public BlockingQueue<ReportMatch> findFiles(final String path1, final String path2, final boolean duplicates, final Sort sort) {

	if (path1 == null)
	    throw new IllegalArgumentException("path1 must not be null!");

	final BlockingQueue<ReportMatch> queue = new ArrayBlockingQueue<>(config.getQueueLength());

	new Thread(new Runnable() {
	    @Override
	    public void run() {

		try {
		    createIndexes();
		    String sql;
		    if (duplicates) {
			if (path2 == null)
			    sql = config.getProperty("sql_selectFilesWithDuplicatesBelowPath");
			else if (path1.equals(path2))
			    sql = config.getProperty("sql_selectFilesWithMoreDuplicatesBelowPath");
			else
			    sql = config.getProperty("sql_selectFilesWithDuplicatesBelowTwoPaths");
		    } else {
			if (path2 == null)
			    sql = config.getProperty("sql_selectFilesBelowPath");
			else
			    sql = config.getProperty("sql_selectFilesBelowPath1NotBelowPath2");
		    }

		    PreparedStatement stmt = db.getConnection().prepareStatement(sql + sort.getSQL());
		    stmt.setString(1, path1 + "%");

		    if (path2 != null) {
			if (duplicates && path1.equals(path2))
			    stmt.setInt(2, 2);
			else
			    stmt.setString(2, path2 + "%");
		    }

		    ResultSet result = stmt.executeQuery();
		    ReportMatch current = null;
		    while (result.next()) {

			String dirPath = result.getString(1);
			// int dirId = result.getInt(2);
			String filename = result.getString(3);
			// int fileId = result.getInt(4);
			long size = result.getLong(5);
			Date scandate = result.getTimestamp(6);
			byte[] sha1 = result.getBytes(7);
			Date lastmodified = result.getTimestamp(8);

			if (current == null || !Arrays.equals(current.getSha1(), sha1)) {
			    if (current != null) {
				try {
				    queue.put(current);
				} catch (InterruptedException e) {
				    logger.error("could not put ReportMatch into queue", e);
				}
			    }
			    current = new ReportMatch(sha1, size);
			}

			current.getStore().add(new StoredFile(dirPath, filename, size, lastmodified, scandate));
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

		} catch (SQLException e) {
		    logger.error("forgetDirectoryTree failed - SQLException", e);
		}

	    }
	}).start();

	return queue;
    }

    @Override
    public BlockingQueue<ReportMatch> findSha1Collisions() {

	final BlockingQueue<ReportMatch> queue = new ArrayBlockingQueue<>(config.getQueueLength());

	new Thread(new Runnable() {
	    @Override
	    public void run() {

		try {
		    createIndexes();
		    PreparedStatement stmt = db.getConnection().prepareStatement(config.getProperty("sql_selectSha1Collisions"));

		    ResultSet result = stmt.executeQuery();
		    ReportMatch current = null;
		    while (result.next()) {

			String dirPath = result.getString(1);
			// int dirId = result.getInt(2);
			String filename = result.getString(3);
			// int fileId = result.getInt(4);
			long size = result.getLong(5);
			Date scandate = result.getTimestamp(6);
			byte[] sha1 = result.getBytes(7);
			Date lastmodified = result.getTimestamp(8);

			if (current != null) {
			    try {
				queue.put(current);
			    } catch (InterruptedException e) {
				logger.error("could not put ReportMatch into queue", e);
			    }
			}
			current = new ReportMatch(sha1, size);

			current.getStore().add(new StoredFile(dirPath, filename, size, lastmodified, scandate));
			// current.getPaths().add(dirPath + "/" + filename);
			// current.getFileIds().add(fileId);

		    }
		    stmt.close();

		    try {
			if (current != null)
			    queue.put(current);
			queue.put(ReportMatch.endOfQueue);
		    } catch (InterruptedException e) {
			logger.error("could not put ReportMatch into queue", e);
		    }

		} catch (SQLException e) {
		    logger.error("forgetDirectoryTree failed - SQLException", e);
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
		    PreparedStatement stmt = db.getConnection().prepareStatement("SELECT path FROM directories WHERE path LIKE ?");
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
    public StoredFile getFile(String dir, String fileName) {
	Integer dirId = getDirectoryId(dir, false);
	if (dirId == null)
	    return null;

	try {
	    PreparedStatement stmt = db.getConnection().prepareStatement(config.getProperty("sql_selectFileDetails"));
	    stmt.setString(1, dir);
	    stmt.setString(2, fileName);
	    ResultSet result = stmt.executeQuery();

	    StoredFile storedFile = null;
	    if (result.next()) {
		storedFile = new StoredFile(dir, fileName, result.getLong(1), result.getTimestamp(2), result.getTimestamp(3));
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

    private void createIndexes() {
	if ("1".equals(config.getProperty("createIndexes"))) {
	    try {
		PreparedStatement stmt = db.getConnection().prepareStatement("SHOW INDEX FROM files");
		ResultSet result = stmt.executeQuery();
		boolean sizeIndexExists = false, sha1IndexExists = false;
		while (result.next()) {
		    String column = result.getString("Column_name");
		    String table = result.getString("Table");
		    if ("files".equals(table) && "size".equals(column))
			sizeIndexExists = true;
		    else if ("files".equals(table) && "sha1".equals(column))
			sha1IndexExists = true;
		}
		stmt.close();
		if (!sha1IndexExists) {
		    stmt = db.getConnection().prepareStatement("CREATE INDEX files_sha1 ON files (sha1)");
		    stmt.execute();
		    stmt.close();
		}
		if (!sizeIndexExists) {
		    stmt = db.getConnection().prepareStatement("CREATE INDEX files_size ON files (size)");
		    stmt.execute();
		    stmt.close();
		}
	    } catch (SQLException e) {
		logger.error("problem while creating indexes");
	    }
	}
    }
}
