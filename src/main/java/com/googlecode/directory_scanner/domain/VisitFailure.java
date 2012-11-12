/**
 * 
 */
package com.googlecode.directory_scanner.domain;

import java.util.Date;

/**
 * @author kaefert
 *
 */
public class VisitFailure {

    private int failureId;
    private String path;
    private Date scanDate;
    private long size, sizeRead;
    private String error;
    
//    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY \
//    ,path VARCHAR(1000) NOT NULL \
//	,scanDir_id INT NULL \
//	,scandate DateTime NOT NULL \
//	,size BIGINT NULL \
//	,sizeRead BIGINT NULL \
//	,error VARCHAR(1000) NOT NULL \
    
    public VisitFailure(int id, String path, Date scanDate, long size, long sizeRead, String error) {
	this.failureId = id;
	this.path = path;
	this.size = size;
	this.sizeRead = sizeRead;
	this.error = error;
    }

    public int getFailureId() {
        return failureId;
    }

    public String getPath() {
        return path;
    }

    public Date getScanDate() {
        return scanDate;
    }

    public long getSize() {
        return size;
    }

    public long getSizeRead() {
        return sizeRead;
    }

    public String getError() {
        return error;
    }
    
    private boolean isDirectory;

    public boolean isDirectory() {
	return isDirectory;
    }

    public void setIsDirectory(boolean isDirectory) {
	this.isDirectory = isDirectory;
    }


    private VisitFailure() {
    };

    public static VisitFailure endOfQueue = new VisitFailure();
}
