package com.googlecode.directory_scanner.domain;

import com.googlecode.directory_scanner.contracts.SkipDirectoryDecider;

public class ScanJob {

	private String path;
	private Integer scanDirId;
	private SkipDirectoryDecider skipDecider;

	public ScanJob(String path, Integer scanDirId, SkipDirectoryDecider skipDecider) {
		this.path = path;
		this.scanDirId = scanDirId;
		this.skipDecider = skipDecider;
	}

	public String getPath() {
		return path;
	}

	public Integer getScanDirId() {
		return scanDirId;
	}

	public SkipDirectoryDecider getSkipDecider() {
		return skipDecider;
	}

	private ScanJob() {
	};

	public static ScanJob endOfQueue = new ScanJob();
}
