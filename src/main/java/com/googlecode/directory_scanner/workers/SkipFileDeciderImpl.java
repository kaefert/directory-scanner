package com.googlecode.directory_scanner.workers;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipFileDecider;
import com.googlecode.directory_scanner.domain.PathVisit;
import com.googlecode.directory_scanner.domain.PathVisit.Type;
import com.googlecode.directory_scanner.domain.StoredFile;

public class SkipFileDeciderImpl implements SkipFileDecider {

	@SuppressWarnings("unused")
	private Logger logger;
	private DatabaseWorker db;

	public SkipFileDeciderImpl(Logger logger, DatabaseWorker db) {
		this.logger = logger;
		this.db = db;
	}

	@Override
	public boolean decideFileSkip(PathVisit visit) {

		assert (visit.getType() == Type.FILE);

		Path path = visit.getPath();
		BasicFileAttributes attrs = visit.getAttributes();
		String fileName = path.getFileName().toString();
		String directory = path.getParent().toString();

		StoredFile storedFile = db.getFile(directory, fileName);

		if (storedFile != null) {
			visit.checkedDB(storedFile.getFileId());

			if (storedFile.getLastModified() == null || attrs == null)
				return false;

			if (storedFile.getLastModified().getTime() == attrs.lastModifiedTime().toMillis()
					&& storedFile.getSize() == attrs.size()) {
				// logger.info("skipping unchanged file " + storedFile.getFullPath());
				return true;
			}

		}
		visit.checkedDB(null);
		return false;

		// return true if this file with the same filesize already exists in the
		// database
		// and maybe check dates? lastmodified date?
		// attrs.lastModifiedTime()

	}
}
