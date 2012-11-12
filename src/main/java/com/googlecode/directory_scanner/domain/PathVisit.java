package com.googlecode.directory_scanner.domain;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class PathVisit {

    private Path path;
    private long bytesRead;
    private BasicFileAttributes attr;
    private int scanRoot;
    private byte[] sha1;
    
    private boolean checkedDB = false;
    private Integer dbId = null;

    public enum Type {
	FILE, FINISHED_DIRECTORY, FAILURE
    }
    private Type type;
    
    public PathVisit(int scanRoot, Path path, BasicFileAttributes attr, Type type) {
	this.scanRoot = scanRoot;
	this.path = path;
	this.attr = attr;
	this.type = type;
    }
    
    public int getScanRoot() {
        return scanRoot;
    }

    public Path getPath() {
        return path;
    }

    public long getSize() {
	if(attr != null)
	    return attr.size();
	else
	    return -1L;
    }
    
    public Type getType() {
	return type;
    }
    
    public BasicFileAttributes getAttributes() {
	return attr;
    }
    
    public void checkedDB(Integer dbId) {
	this.checkedDB = true;
	this.dbId = dbId;
    }
    
    public void fileScanned(long bytesRead, byte[] sha1) {
	this.bytesRead = bytesRead;
	this.sha1 = sha1;
    }

    public long getBytesRead() {
        return bytesRead;
    }

    public byte[] getSha1() {
        return sha1;
    }

    public boolean haveCheckedDB() {
        return checkedDB;
    }

    public Integer getDBId() {
        return dbId;
    }

    private PathVisit() {};
    public static PathVisit endOfQueue = new PathVisit();

    public FileTime getLastModified() {
	return attr.lastModifiedTime();
    }
}
