package com.googlecode.directory_scanner.contracts;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.domain.VisitFailure;

public interface WorkManager {

    public String getProfileStats();
    
    public void setProfile(String name);
    
    public List<String> getProfileList();
    
    /**
     * Creates a PathWalker that walks the filesystem tree below the given path.
     * its findings are incooperated into the db according to the config.
     * 
     * @param path
     */
    public void scanPath(String path);

    /**
     * Goes through files below the given path in the database and checks if
     * they exist. if they don't, they are removed (or marked as non existent,
     * implementation detail..)
     * 
     * @param path
     */
    public void checkExistence(String path);

    /**
     * Deletes all files below the given path from the database
     */
    public void forgetPath(String path);

    /**
     * 
     * @param path1
     * 		  must not be null!
     *            only duplicates that have at least 1 instance
     *            below this path are returned
     * 
     * @param path2
     *            if not null, same as path1. 
     *            if equal path1 only duplicates that have at least 2
     *            instances below the given path are returned.
     * 
     * @param duplicates
     *            if true behaviour as drescribed in description for path1&2
     * 
     *            if false and path2 is null, any files below path1 is reported
     * 
     *            if false & path1 & path2 are not null, any files are printed,
     *            that exist in path1 but not in path2
     * 
     * @return
     */
    public BlockingQueue<ReportMatch> findFiles(String path1, String path2, boolean duplicates, Sort sort, FindFilter filter);
    
    public BlockingQueue<ReportMatch> findProblems();
    
    public void checkFailuresBelow(String path);

    public BlockingQueue<VisitFailure> getFailuresBelow(String path);
    
    public void moveOrCopyMatches(BlockingQueue<ReportMatch> queue, String from, String to, boolean copy, boolean flatten);
}
