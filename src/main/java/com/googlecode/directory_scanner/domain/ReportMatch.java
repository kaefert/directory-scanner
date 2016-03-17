package com.googlecode.directory_scanner.domain;

import java.util.ArrayList;

import com.googlecode.directory_scanner.workers.AppConfig;

/**
 * A ReportMatch represents multiple files with the same sha1 hash and the same size (so duplicates)
 * 
 * @author kaefert
 *
 */
public class ReportMatch {

	private byte[] sha1;
	private long size;
	private ArrayList<StoredFile> store;
	private String metaData;

	// private HashSet<Integer> fileIds;

	public ReportMatch(byte[] sha1, long size) {
		this.sha1 = sha1;
		// TO DO: rethink reporting
		// this.fileIds = databaseHandler.getFileSha1s().get(sha1);
		this.store = new ArrayList<>(2);
		this.size = size;
	}

	public byte[] getSha1() {
		return sha1;
	}

	public long getSize() {
		return size;
	}

	public ArrayList<StoredFile> getStore() {
		return store;
	}

	public void setMetadata(String metadata) {
		this.metaData = metadata;
	}

	public String getMetadata() {
		if (metaData != null)
			return metaData;
		else
			return "Match sha1=" + AppConfig.getSha1HexString(getSha1())
					+ "; size=" + getSize() + "; count=" + getStore().size()
					+ "; totalSize=" + getSize() * getStore().size();
	}

	private ReportMatch() {
	};

	public static ReportMatch endOfQueue = new ReportMatch();
}
