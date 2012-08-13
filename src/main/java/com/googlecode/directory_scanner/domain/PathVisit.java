package com.googlecode.directory_scanner.domain;

import java.nio.file.Path;

public class PathVisit {

    private Path path;
    private long size, bytesRead;
    private int scanRoot;
    private byte[] sha1;

    public enum Type {
	FILE, FINISHED_DIRECTORY, FAILURE
    }
    private Type type;
    
    public PathVisit(int scanRoot, Path path, long size, Type type) {
	this.scanRoot = scanRoot;
	this.path = path;
	this.size = size;
	this.type = type;
    }
    
    public int getScanRoot() {
        return scanRoot;
    }

    public Path getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }
    
    public Type getType() {
	return type;
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

    private PathVisit() {};
    public static PathVisit endOfQueue = new PathVisit();
}
