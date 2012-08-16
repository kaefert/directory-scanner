package com.googlecode.directory_scanner.workers;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashSet;

import com.googlecode.directory_scanner.contracts.DatabaseWorker;
import com.googlecode.directory_scanner.contracts.SkipDecider;
import com.googlecode.directory_scanner.domain.StoredFile;

public class SkipDeciderImpl implements SkipDecider {

    private HashSet<String> cache = null;
    private DatabaseWorker db = null;
    private Date after = null;
    private Thread modeDecider;

    public SkipDeciderImpl(final String below, final Date after, final int dirLimit, final int fileLimit, final DatabaseWorker db) {
	
	this.db = db;
	this.after = after;
	
	modeDecider = new Thread(new Runnable() {
	    @Override
	    public void run() {
		cache = db.getDirectoriesDoneBelowAfterIfLessThen(below, after, dirLimit);
//		if(cache == null) {
//		    SkipDeciderImpl.this.db = db;
//		    SkipDeciderImpl.this.after = after;
//		}   
		SkipDeciderImpl.this.modeDecider = null;
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

    @Override
    public boolean decideFileSkip(Path path, BasicFileAttributes attrs) {
	
	String fileName = path.getFileName().toString();
	String directory = path.getParent().toString();
	
	
	StoredFile storedFile = db.getFile(directory, fileName);
	
	if(storedFile == null || storedFile.getLastModified() == null)
	    return false;
	
	if(storedFile.getLastModified().getTime() == attrs.lastModifiedTime().toMillis() && storedFile.getSize() == attrs.size())
	    return true;
	
	// return true if this file with the same filesize already exists in the database
	// and maybe check dates? lastmodified date?
//	attrs.lastModifiedTime()
	
	return false;
    }

}
