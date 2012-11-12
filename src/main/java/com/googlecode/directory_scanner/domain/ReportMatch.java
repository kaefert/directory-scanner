package com.googlecode.directory_scanner.domain;

import java.util.HashSet;

import com.googlecode.directory_scanner.workers.AppConfig;

public class ReportMatch {

    private byte[] sha1;
    private long size;
    private HashSet<StoredFile> store;
    private String metaData;
//    private HashSet<Integer> fileIds;

    public ReportMatch(byte[] sha1, long size) {
	this.sha1 = sha1;
	// TO DO: rethink reporting
	// this.fileIds = databaseHandler.getFileSha1s().get(sha1);
	this.store = new HashSet<>(2);
	this.size = size;
    }

    public byte[] getSha1() {
	return sha1;
    }

    public long getSize() {
	return size;
    }

    public HashSet<StoredFile> getStore() {
        return store;
    }
    
    public void setMetadata(String metadata) {
	this.metaData = metadata;
    }

    public String getMetadata() {
	if(metaData != null)
	    return metaData;
	else
	    return "Match sha1=" + AppConfig.getSha1HexString(getSha1()) + "; size=" + getSize() + "; count=" + getStore().size() + "; totalSize=" + getSize()*getStore().size();
    }

    public static enum Sort {
	NOSORT {
	    @Override
	    public String getSQL() {
		return " ORDER BY f.sha1";
	    }
	},
	SIZE {
	    @Override
	    public String getSQL() {
		return " ORDER BY f.size DESC, f.sha1";
	    }
	},
	COUNT {
	    @Override
	    public String getSQL() {
		return " ORDER BY (SELECT count(f2.id) FROM files WHERE f2.sha1 = f.sha1) DESC, f.sha1";
	    }
	},
	SIZETIMESCOUNT {
	    @Override
	    public String getSQL() {
		return " ORDER BY f.size*(SELECT count(f2.id) FROM files f2 WHERE f2.sha1 = f.sha1) DESC, f.sha1";
	    }
	},
	
	PATH {
	    @Override
	    public String getSQL() {
		return " ORDER BY d.path, f.filename";
	    }
	};

	public abstract String getSQL();
    };

    private ReportMatch() {
    };

    public static ReportMatch endOfQueue = new ReportMatch();
}
