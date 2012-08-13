package com.googlecode.directory_scanner.domain;

import java.util.HashSet;

public class ReportMatch {

    private byte[] sha1;
    private long size;
    private HashSet<String> paths;
//    private HashSet<Integer> fileIds;

    public ReportMatch(byte[] sha1, long size) {
	this.sha1 = sha1;
	// TO DO: rethink reporting
	// this.fileIds = databaseHandler.getFileSha1s().get(sha1);
	this.paths = new HashSet<>(2);
	this.size = size;
    }

    public byte[] getSha1() {
	return sha1;
    }

    public long getSize() {
	return size;
    }

    public HashSet<String> getPaths() {
	if(paths == null)
	    paths = new HashSet<>();
	return paths;
    }


    public enum Sort {
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
	};

	public abstract String getSQL();
    };

    private ReportMatch() {
    };

    public static ReportMatch endOfQueue = new ReportMatch();
}
