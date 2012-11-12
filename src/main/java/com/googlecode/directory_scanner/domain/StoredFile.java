/**
 * 
 */
package com.googlecode.directory_scanner.domain;

import java.util.Date;

/**
 * @author kaefert
 *
 */
public class StoredFile {

    private String dirPath;
    private String fileName;
    private long size;
    private Date lastModified, scanDate;
    private Integer fileId;

    public StoredFile(String dirPath, String fileName, long size, Date lastModified, Date scanDate, Integer fileId) {
	this(dirPath, fileName, size, lastModified, scanDate);
	this.fileId = fileId;
    }
    
    public StoredFile(String dirPath, String fileName, long size, Date lastModified, Date scanDate) {
	this.dirPath = dirPath;
	this.fileName = fileName;
	this.size = size;
	this.lastModified = lastModified;
	this.scanDate = scanDate;
    }
    
    public Integer getFileId() {
        return fileId;
    }

    public String getDirPath() {
        return dirPath;
    }

    public String getFileName() {
        return fileName;
    }
    
    public String getFullPath() {
	return dirPath + "/" + fileName;
    }

    public long getSize() {
        return size;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public Date getScanDate() {
        return scanDate;
    }
}
