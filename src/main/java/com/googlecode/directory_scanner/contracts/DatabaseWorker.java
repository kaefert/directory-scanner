package com.googlecode.directory_scanner.contracts;

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;

import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.Sort;

public interface DatabaseWorker {

    public HashSet<String> getDirectoriesDoneBelowAfterIfLessThen(String below, Date after, int limit);

    public boolean getDirectoryDoneAfter(String path, Date after);
    
    /**
     * This method will check if the Directory exists already, and then call the one below with this info.
     */
    public int insertDirectory(String path, Integer scanDir, boolean finished);

    /**
     * Only use this method if you either already have the id of the directory and only want to update it,
     * or if you are certain that the directory does not exist jet in the database you can pass a null as @dirId
     * @return dirId
     */
    public int insertDirectory(String path, Integer dirId, Integer scanDir, boolean finished);
    
    public Integer getDirectoryId(String path, boolean createIfNotExists);

    public Integer insertFile(String fullPath, String fileName, String containingDir, int scanDir, long size, byte[] sha1);
    
    public Integer insertFile(String fullPath, String fileName, int containingDir, int scanDir, long size, byte[] sha1);

    public void insertFailure(String fullPath, int scanRoot, long size, long bytesRead, String failure);

    public void forgetDirectoryTree(String path);

    public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort);
}
