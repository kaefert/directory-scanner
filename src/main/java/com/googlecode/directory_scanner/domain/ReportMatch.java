package com.googlecode.directory_scanner.domain;

import java.util.ArrayList;
import java.util.Date;

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
	
	public enum ReportMode { ALL, PATH, NOTPATH, FIRSTPATH, ALLBUT1STPATH, PreserveTimestampScript };
	
	public String getReport(boolean reportMetadata, ReportMode mode, String path1, String path2) {
		String report = "\n";
		
		if(reportMetadata)
			report += "### " + getMetadata() + "\n";

		if(mode == ReportMode.PreserveTimestampScript) {
			// we assume:
			// 1.) path1 = the directory that contains the current and future structure
			// 2.) some of the files below path2 might have an older timestamp than the file
			//     with the same content in path1 and we want to preserve that older timestamp
			//     by outputting a call to touch that copies it to the path1 versions.
			
			boolean matchHasInstancesBelowPath1 = false;
			
			// subtask1: find the oldest timestamp in the ReportMatch:
			StoredFile earliest = null;
			for (StoredFile file : getStore()) {
				Date check = file.getLastModified();
				if(earliest == null || (earliest.getLastModified().after(check) && check.after(AppConfig.earliestDateAccepted)))
					earliest = file;
			}
			report += "# earliest timestamp = " + earliest.getLastModified() + " - from file: \"" + earliest.getFullPath() + "\"\n";

			// subtask2: output touch command for all later file instances below path1 to set to earliest timestamp:
			for (StoredFile file : getStore()) {
				if (file.getDirPath().startsWith(path1)) {
					matchHasInstancesBelowPath1 = true;
					if(file.getLastModified().after(earliest.getLastModified())) {
						report += "# overwrite timestamp = " + file.getLastModified() + " in path1: \n" +
								"touch -d \"$(date -R -r \"" + earliest.getFullPath() + "\")\" \"" + file.getFullPath() + "\"\n";
					}
				}
			}
			
			if(matchHasInstancesBelowPath1) {
				report += "# delete duplicates below path2: \n";
				// subtask3: output rm statement for all file instances below path2:
				for (StoredFile file : getStore()) {
					if (file.getDirPath().startsWith(path2)) {
						report += "rm \"" + file.getFullPath() + "\"\n";
					}
				}
			}
		}
		else {
			boolean firstSkipped = false;
			for (StoredFile file : getStore()) {
				if(mode == ReportMode.ALL
						|| ((mode == ReportMode.PATH && file.getDirPath().startsWith(path1)))
						|| ((mode == ReportMode.NOTPATH && !file.getDirPath().startsWith(path1)))
						)
					report += file.getFullPath() + "\n";
				else if(mode == ReportMode.FIRSTPATH && file.getDirPath().startsWith(path1)) {
					report += file.getFullPath() + "\n";
					break;
				}
				else if(mode == ReportMode.ALLBUT1STPATH) {
					if(!firstSkipped && file.getDirPath().startsWith(path1))
						firstSkipped = true;
					else
						report += file.getFullPath() + "\n";
				}
			}
		}
		
		return report;
	}
}
