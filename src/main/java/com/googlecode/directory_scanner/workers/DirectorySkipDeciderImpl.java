package com.googlecode.directory_scanner.workers;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.DirectorySkipDecider;

public class DirectorySkipDeciderImpl implements DirectorySkipDecider {

    private HashSet<String> cache = null;
    private DatabaseWorker db = null;
    private Date after = null;
    private Thread modeDecider;

    public DirectorySkipDeciderImpl(final String below, final Date after, final int limit, final DatabaseWorker db) {
	
	modeDecider = new Thread(new Runnable() {
	    @Override
	    public void run() {
		cache = db.getDirectoriesDoneBelowAfterIfLessThen(below, after, limit);
		if(cache == null) {
		    DirectorySkipDeciderImpl.this.db = db;
		    DirectorySkipDeciderImpl.this.after = after;
		}   
		DirectorySkipDeciderImpl.this.modeDecider = null;
	    }
	});
	modeDecider.start();
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
	
	if(cache != null) {
	    for(String s : cache) {
		if(s.equals(path.toString())) 
		    return true;
	    }
	    return false;
	}
	else {
	    return db.getDirectoryDoneAfter(path.toString(), after);
	}
    }

}
