package com.googlecode.directory_scanner.contracts;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;
import com.googlecode.directory_scanner.domain.VisitFailure;

public interface DatabaseWorker {

    public HashSet<String> getDirectoriesDoneBelowAfterIfLessThen(String below, Date after, int limit);

    public boolean getDirectoryDoneAfter(String path, Date after);
    
    public String getProfileStats();
    
    public void setProfile(String profile);
    
    /**
     * This method will check if the Directory exists already, and then call the one below with this info.
     */
    public int insertDirectory(String path, Integer scanDir, boolean finished);

    public Integer getDirectoryId(String path, boolean createIfNotExists);

    public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir, Timestamp lastModified, long size, byte[] sha1);
    
    public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir, Timestamp lastModified, long size, byte[] sha1, Integer fileId);
    
    public void insertFailure(String fullPath, int scanRoot, long size, long bytesRead, String failure);

    public void forgetDirectoryTree(String path);

    public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort, FindFilter filter);
    
    public BlockingQueue<ReportMatch> findProblems();

    public BlockingQueue<String> findDirectoriesBelow(String path);

    public BlockingQueue<VisitFailure> loadFailuresBelow(final String path);
    
    public StoredFile getFile(String dir, String fileName);

    public void forgetFile(int dirId, String filename);

    public void indexesInsertingMode();

    void forgetFailure(int failureId);

    void forgetFailuresBelow(String path);
}
