/**
 * 
 */
package com.googlecode.directory_scanner.ui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.ReportMatch.ReportMode;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.workers.AppConfig;

/**
 * @author kaefert
 * 
 */
public class CLI {

	AppConfig config;
	Logger logger;
	ReportMode reportMode = ReportMode.ALL;

	private static final String[] optionCommands = { "profile", "report_mode", "scan", "rescan", "check_exists",
			"report_any", "report_duplicates1", "report_duplicates2", "import" };

	public CLI(String[] args, Logger logger, AppConfig config, WorkManager worker) {

		this.config = config;
		this.logger = logger;

		String command = null;
		String path1 = null;
		int num = 0;

		for (String s : args) {
			logger.log(Level.INFO, "processing args[" + num + "]=" + s);

			if (command == null) {
				for (String c : optionCommands) {
					if (c.equals(s)) {
						command = s;
						break;
					}
				}
				if (command == null) {
					if ("reset_done_directories".equals(s))
						config.setProperty("skipDirectoriesScannedAgo", "-2");
					else if ("help".equals(s))
						printHelp(false);
					else
						printHelp(true);
				}
			} else {

				if ("scan".equals(command)) {
					worker.scanPath(s);
				} else if ("rescan".equals(command)) {
					config.setProperty("skipDirectoriesScannedAgo", "-2");
					worker.scanPath(s);
				} else if ("check_exists".equals(command)) {
					worker.checkExistence(s);
				} else if ("report_any".equals(command)) {
					doReport(worker.findFiles(s, null, false, Sort.SIZETIMESCOUNT, FindFilter.UNFILTERED), s, null);
				} else if ("report_duplicates1".equals(command)) {
					doReport(worker.findFiles(s, null, true, Sort.SIZETIMESCOUNT, FindFilter.UNFILTERED), s, null);
				} else if ("report_duplicates2".equals(command)) {
					if (path1 == null)
						path1 = s;
					else {
						doReport(worker.findFiles(path1, s, true, Sort.SIZETIMESCOUNT, FindFilter.UNFILTERED), path1, s);
						path1 = null;
					}
				} else if ("profile".equals(command)) {
					worker.setProfile(s);
					logger.log(Level.INFO, "switched to profile " + s + ", stats: " + worker.getProfileStats());
				} else if ("import".equals(command)) {
					if (path1 == null)
						path1 = s;
					else {
						worker.importFilesFromOtherProfile(s, path1);
						path1 = null;
					}
				} else if ("report_mode".equals(command)) {
					reportMode = ReportMode.valueOf(s);
				} else {
					logger.log(Level.ERROR,
							"unhandled state!, please share the call that caused this output with the developers!");
				}
				
				// preserve command if it needs another cycle for its second sub-parmeter (path, profilename, ..)
				if (path1 == null)
					command = null;
			}

			num++;
		}

		System.out.println("");
		System.out.println("");
		logger.log(Level.INFO, "end of DirectoryScanner -> static void main");

	}

	private void printHelp(boolean error) {
		if (error) {
			System.out.println("Error: given parameters did not match the CLI specifications, see help:");
		}
		System.out.println("");
		String help = config.readFileFromClasspath("com/googlecode/directory_scanner/help.txt");
		String version = getClass().getPackage().getImplementationVersion();
		help = help.replace("[[VERSION]]", version != null ? version : "???");
		System.out.println(help);
	}

	private void doReport(final BlockingQueue<ReportMatch> matches, final String path1, final String path2) {

		System.out.println("");
		System.out.println("");

		final BufferedWriter reportFile;
		try {
			String now = new SimpleDateFormat("yyyy-MM-dd_HHmm-ss").format(new Date());
			String filename = "directory-scanner-report_" + now + "." + (reportMode.equals(ReportMode.PreserveTimestampScript) ? "sh" : "txt");
			reportFile = Files.newBufferedWriter(Paths.get(filename), Charset.defaultCharset());
		} catch (IOException e) {
			logger.error("cant open reportfile!", e);
			return;
		}

		Thread processor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						ReportMatch match = matches.take();
						if (match == ReportMatch.endOfQueue) {
							reportFile.flush();
							reportFile.close();
							break;
						}

						String matchReport = match.getReport(true, reportMode, path1, path2);
						System.out.println(matchReport);
						reportFile.write(matchReport);
					} catch (InterruptedException e) {
						logger.error("reportMatchesProcessor was interrupted", e);
					} catch (IOException e) {
						logger.error("could not close / flush / write to reportfile", e);
					}
				}
				System.out.println("");
				System.out.println("############### end of report ###############");
				System.out.println("");
			}
		});
		processor.start();
	}
}
