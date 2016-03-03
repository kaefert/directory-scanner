package com.googlecode.directory_scanner.workers;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipDirectoryDecider;

public class SkipDirectoryDeciderImpl implements SkipDirectoryDecider {

	private HashSet<String> cache = null;
	private DatabaseWorker db = null;
	private Logger logger;
	private Date after = null;
	private Thread modeDecider = null;

	public SkipDirectoryDeciderImpl(final String below, final Date after, final int cacheLimit,
			final DatabaseWorker db, Logger logger) {

		this.db = db;
		this.after = after;
		this.logger = logger;

		if (cacheLimit > 0) {
			modeDecider = new Thread(new Runnable() {
				@Override
				public void run() {
					cache = db.getDirectoriesDoneBelowAfterIfLessThen(below, after, cacheLimit);
					// if(cache == null) {
					// SkipDeciderImpl.this.db = db;
					// SkipDeciderImpl.this.after = after;
					// }
					SkipDirectoryDeciderImpl.this.modeDecider = null;
				}
			});
			modeDecider.start();
		}
	}

	@Override
	public boolean decideDirectorySkip(Path path, BasicFileAttributes attrs) {
		if (modeDecider != null) {
			try {
				modeDecider.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		if (cache != null) {
			for (String s : cache) {
				if (s.equals(path.toString())) {
					logger.info("skipping doneDirectory " + s);
					return true;
				}
			}
			return false;
		} else {
			return db.getDirectoryDoneAfter(path.toString(), after);
		}
	}

}
