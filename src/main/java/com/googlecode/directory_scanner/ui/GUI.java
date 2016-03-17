package com.googlecode.directory_scanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.FindFilter;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.domain.Sort;
import com.googlecode.directory_scanner.domain.StoredFile;
import com.googlecode.directory_scanner.domain.VisitFailure;
import com.googlecode.directory_scanner.workers.AppConfig;

@SuppressWarnings("serial")
public class GUI {

	private JFrame frmDirectoryScanner;

	private JLabel lblPath1, lblPath2, lblPath3;
	private JTextField txtPath1, txtPath2, txtPath3, txtDate;

	private JScrollPane statusScrollPane;
	private JScrollBar scrollPaneVerticalBar;
	private JTextArea txtrLog, txtrFileList;

	private JLabel projectStatLabel = new JLabel("Current: Default");
	private JRadioButtonMenuItem sortSha1, sortSize, sortCount, sortSizeCount, reportAll, reportPath1, report1stPath1,
			reportNotPath1, reportAllBut1stPath1, reportPath2, report1stPath2, reportNotPath2, reportAllBut1stPath2,
			actionScopeAll, actionScopeOlder, actionScopeOlderEquals, performActionForEquals, performActionForNewerEquals, performActionForNewer;
	private JCheckBoxMenuItem reportMetadata, autoScanReport, autoScanAction, flattenTarget;

	private JCheckBox chckbxAutoscroll;
	private JTabbedPane tabPane;

	private Logger logger;
	private AppConfig config;
	private WorkManager worker;

	private Date lastLogChange;
	private Level printLogToStatus;

	/**
	 * Create the application.
	 */
	public GUI(Logger logger, AppConfig config, WorkManager worker) {

		this.logger = logger;
		this.config = config;
		this.worker = worker;

		// logger = Logger.getLogger("directory-scanner-gui-logger_" +
		// this.hashCode());
		// config = new ConfigLoader(logger).getConfig();
		printLogToStatus = Level.toLevel(config.getProperty("status-log-level"));

		logger.addAppender(new AppenderSkeleton() {

			@Override
			public boolean requiresLayout() {
				return false;
			}

			@Override
			public void close() {
			}

			@Override
			protected void append(LoggingEvent event) {
				if (GUI.this.txtrLog != null && event.getLevel().toInt() >= printLogToStatus.toInt()) {
					String timeStamp = GUI.this.config.getDateFormatter().format(new Date(event.timeStamp));
					GUI.this.txtrLog.append(timeStamp + " - " + event.getRenderedMessage() + "\n");
					String[] t = event.getThrowableStrRep();
					if (t != null) {
						for (String s : t) {
							GUI.this.txtrLog.append(s + "\n");
						}
						GUI.this.txtrLog.append("\n");
					}
					GUI.this.lastLogChange = new Date();
				}
			}
		});

		initialize();

		frmDirectoryScanner.setJMenuBar(createMenuBar());
		projectStatLabel.setText(worker.getProfileStats());
		frmDirectoryScanner.setVisible(true);
		autoScrollTimer.start();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {

		frmDirectoryScanner = new JFrame("Directory Scanner");
		// frmDirectoryScanner.setTitle("Directory Scanner");
		// frmDirectoryScanner.setSize(800, 600);
		// frmDirectoryScanner.setPreferredSize(new Dimension(800, 600));
		// frmDirectoryScanner.setMinimumSize(new Dimension(440, 320));
		// frmDirectoryScanner.setUndecorated(true);
		// frmDirectoryScanner.setShape(new Polygon(new int[]{0,300,600,300,0},
		// new int[]{300,600,300,0,300}, 5));
		frmDirectoryScanner.setBounds(50, 50, 800, 600);
		frmDirectoryScanner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Container contentPane = frmDirectoryScanner.getContentPane();
		contentPane.setLayout(new BorderLayout());

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));

		JPanel northFormPanel = new JPanel();
		GridBagLayout gbl_northFormPanel = new GridBagLayout();
		gbl_northFormPanel.columnWeights = new double[] { 0, 1 };
		northFormPanel.setLayout(gbl_northFormPanel);

		lblPath1 = new JLabel("Path 1:  ");
		GridBagConstraints gbc_lblPath1 = new GridBagConstraints();
		gbc_lblPath1.anchor = GridBagConstraints.WEST;
		gbc_lblPath1.gridx = 0;
		gbc_lblPath1.gridy = 0;
		northFormPanel.add(lblPath1, gbc_lblPath1);

		lblPath2 = new JLabel("Path 2:  ");
		GridBagConstraints gbc_lblPath2 = new GridBagConstraints();
		gbc_lblPath2.anchor = GridBagConstraints.WEST;
		gbc_lblPath2.gridx = 0; 
		gbc_lblPath2.gridy = 1;
		northFormPanel.add(lblPath2, gbc_lblPath2);

		lblPath3 = new JLabel("Path 3:  ");
		GridBagConstraints gbc_lblPath3 = new GridBagConstraints();
		gbc_lblPath3.anchor = GridBagConstraints.WEST;
		gbc_lblPath3.gridx = 0;
		gbc_lblPath3.gridy = 2;
		northFormPanel.add(lblPath3, gbc_lblPath3);

		// lblDate = new JLabel("Date:  ");
		// GridBagConstraints gbc_lblDate = new GridBagConstraints();
		// gbc_lblDate.anchor = GridBagConstraints.WEST;
		// gbc_lblDate.gridx = 0;
		// gbc_lblDate.gridy = 2;
		// northFormPanel.add(lblDate, gbc_lblDate);

		txtPath1 = new JTextField("");
		GridBagConstraints gbc_txtPath1 = new GridBagConstraints();
		gbc_txtPath1.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPath1.gridx = 1;
		gbc_txtPath1.gridy = 0;
		northFormPanel.add(txtPath1, gbc_txtPath1);

		txtPath2 = new JTextField("");
		GridBagConstraints gbc_txtPath2 = new GridBagConstraints();
		gbc_txtPath2.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPath2.gridx = 1;
		gbc_txtPath2.gridy = 1;
		northFormPanel.add(txtPath2, gbc_txtPath2);

		txtPath3 = new JTextField("");
		GridBagConstraints gbc_txtPath3 = new GridBagConstraints();
		gbc_txtPath3.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtPath3.gridx = 1;
		gbc_txtPath3.gridy = 2;
		northFormPanel.add(txtPath3, gbc_txtPath3);

		// GridBagConstraints gbc_txtDate = new GridBagConstraints();
		// gbc_txtDate.fill = GridBagConstraints.HORIZONTAL;
		// gbc_txtDate.gridx = 1;
		// gbc_txtDate.gridy = 2;
		// northFormPanel.add(txtDate, gbc_txtDate);

		// northFormPanel.setLayout(new FormLayout(new ColumnSpec[] {}, new
		// RowSpec[] {}));

		// JButton btnScanPath = new JButton("Scan Path1");
		// btnScanPath.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// worker.scanPath(txtPath1.getText());
		// }
		// }).start();
		//
		// txtrLog.append("started scan thread");
		// }
		// });
		//
		// JButton btnInfoPath = new JButton("info Path1");
		// btnInfoPath.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.printInfoOn(txtPath1.getText());
		// }
		// }).start();
		// }
		// });
		//
		// JButton btnSetDirectorySkipLimit = new
		// JButton("Set Directory-skip limit");
		// btnSetDirectorySkipLimit.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// try {
		// worker.setSkipDirectoriesDoneAfter(formatter.parse(txtDate.getText()));
		// } catch (ParseException e1) {
		// logger.error("what you entered in the date filed differed from my format (look at startup example value)",
		// e1);
		// }
		// }
		// });
		//
		// JButton btnForgetPath = new JButton("Forget Path1%");
		// btnForgetPath.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent arg0) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.forgetPath(txtPath1.getText());
		// }
		// }).start();
		// }
		// });
		//
		// JButton btnSwapPaths = new JButton("Swap Path1&2");
		// btnSwapPaths.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// String path1 = txtPath1.getText();
		// txtPath1.setText(txtPath2.getText());
		// txtPath2.setText(path1);
		// }
		// });
		//
		// JButton btnDateNow = new JButton("Date Now");
		// btnDateNow.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// txtDate.setText(formatter.format(new Date()));
		// }
		// });
		//
		// JButton btnDuplicates = new JButton("Duplicates in Path1");
		// btnDuplicates.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.findDuplicates(txtPath1.getText(), txtPath1.getText(), false);
		// }
		// }).start();
		// }
		// });
		//
		// JButton btnDuplicatesOutside = new
		// JButton("Files of Path1 with Duplicates");
		// btnDuplicatesOutside.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.findDuplicates(txtPath1.getText(), null, false);
		// }
		// }).start();
		// }
		// });
		//
		// JButton btnDiffs = new JButton("Files in Path1 not in Path2");
		// btnDiffs.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.findDuplicates(txtPath1.getText(), txtPath2.getText(), true);
		// }
		// }).start();
		// }
		// });
		//
		// JButton btnShared = new JButton("Files in both Paths");
		// btnShared.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// new Thread(new Runnable() {
		// @Override
		// public void run() {
		// worker.findDuplicates(txtPath1.getText(), txtPath2.getText(), false);
		// }
		// }).start();
		// }
		// });
		//
		// JPanel buttonPanel = new JPanel();
		// buttonPanel.setLayout(new WrapLayout());
		// buttonPanel.add(btnScanPath);
		// buttonPanel.add(btnSetDirectorySkipLimit);
		// buttonPanel.add(btnInfoPath);
		// buttonPanel.add(btnForgetPath);
		// buttonPanel.add(btnSwapPaths);
		// buttonPanel.add(btnDateNow);
		// buttonPanel.add(btnDuplicates);
		// buttonPanel.add(btnDuplicatesOutside);
		// buttonPanel.add(btnDiffs);
		// buttonPanel.add(btnShared);
		//
		northPanel.add(northFormPanel);
		// northPanel.add(buttonPanel);

		this.txtrLog = new JTextArea(
				"warning: JTextArea is a piece of junk which will completly fill your memory with trash-data\n(if you want to display reaaaally lots of text in it, like a detailed log..).\nTherefore I set the level that will be printed to this field to 'ERROR'.\nIf you want to see what the application is doing,\nrun it from a terminal and look at the output there\n(or modify the properties file but be aware of the 'java.lang.OutOfMemoryError: Java heap space').\n\n");
		this.txtrLog.setEditable(false);
		this.statusScrollPane = new JScrollPane(this.txtrLog);
		this.scrollPaneVerticalBar = statusScrollPane.getVerticalScrollBar();

		this.chckbxAutoscroll = new JCheckBox("autoscroll");
		this.chckbxAutoscroll.setSelected(true);

		JPanel chkbxPanel = new JPanel();
		chkbxPanel.add(chckbxAutoscroll);

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(chkbxPanel, BorderLayout.SOUTH);
		statusPanel.add(statusScrollPane, BorderLayout.CENTER);

		this.txtrFileList = new JTextArea();
		// this.txtrFileList.setEditable(false);
		JScrollPane fileListScrollPane = new JScrollPane(this.txtrFileList);

		// JPanel fileListOptionsPanel = new JPanel();
		// fileListOptionsPanel.setLayout(new WrapLayout());
		//
		// JPanel fileListPanel = new JPanel();
		// fileListPanel.setLayout(new BorderLayout());
		// fileListPanel.add(fileListOptionsPanel, BorderLayout.SOUTH);
		// fileListPanel.add(fileListScrollPane, BorderLayout.CENTER);

		tabPane = new JTabbedPane();
		// centerPanel.setLayout(new CardLayout());
		tabPane.add(statusPanel, "Status");
		tabPane.add(fileListScrollPane, "Files");

		contentPane.add(northPanel, BorderLayout.NORTH);
		contentPane.add(tabPane, BorderLayout.CENTER);
	}

	private JMenuBar createMenuBar() {

		JMenuBar jMenuBar = new JMenuBar();
		jMenuBar.add(createProjectMenu());
		jMenuBar.add(createScanMenu());
		jMenuBar.add(createToolsMenu());
		jMenuBar.add(createReportMenu());
		jMenuBar.add(createActionsMenu());
		jMenuBar.add(createFilterMenu());
		jMenuBar.add(createSortMenu());

		return jMenuBar;
	}

	private JMenu createProjectMenu() {
		final JMenu projMenu = new JMenu("Profile");
		projMenu.setMnemonic('P');
		projMenu.add(projectStatLabel);
		projMenu.addSeparator();
		JMenuItem np = new JMenuItem(new AbstractAction("New / Other Profile") {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				String s = (String) JOptionPane.showInputDialog(frmDirectoryScanner, "Enter the Profile-Name:",
						"Customized Dialog", JOptionPane.PLAIN_MESSAGE);

				// If a string was returned, say so.
				if ((s != null) && (s.length() > 0)) {
					worker.setProfile(s);
					projectStatLabel.setText(worker.getProfileStats());
					addProjSelectItem(projMenu, s);
				} else {
					JOptionPane.showMessageDialog(frmDirectoryScanner,
							"You did not enter a profile-name, will keep the current profile.");
				}
			}
		});
		projMenu.add(np);
		projMenu.addSeparator();

		for (final String profile : worker.getProfileList()) {
			addProjSelectItem(projMenu, profile);
		}
		return projMenu;
	}

	private void addProjSelectItem(JMenu projMenu, final String profile) {
		JMenuItem lp = new JMenuItem(new AbstractAction(profile) {
			private static final long serialVersionUID = 4604811901635863258L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				worker.setProfile(profile);
				projectStatLabel.setText(worker.getProfileStats());
			}
		});
		projMenu.add(lp);
	}

	private JMenu createScanMenu() {
		JMenu scanMenu = new JMenu("Scan");
		scanMenu.setMnemonic('S');
		JMenuItem sp1 = new JMenuItem(new AbstractAction("Scan Path1") {
			private static final long serialVersionUID = 4604811901635863259L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						GUI.this.worker.scanPath(txtPath1.getText());
					}
				}).start();
			}
		});
		sp1.setMnemonic('1');
		scanMenu.add(sp1);

		JMenuItem sp2 = new JMenuItem(new AbstractAction("Scan Path2") {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						GUI.this.worker.scanPath(txtPath2.getText());
					}
				}).start();
			}

		});
		sp2.setMnemonic('2');
		scanMenu.add(sp2);

		scanMenu.addSeparator();
		scanMenu.add(new JLabel("skip Directories already scanned after:"));

		Date skipNewer = config.getSkipDirectoriesDoneAfter();

		txtDate = new JTextField(config.getDateFormatter().format(skipNewer));
		txtDate.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// logger.info("txtDate keyTyped!" + txtDate.getText());
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// logger.info("txtDate keyReleased!" + txtDate.getText());
				try {
					config.getDateFormatter().parse(txtDate.getText());
					config.setProperty("skipDirectoriesDoneAfter", txtDate.getText());
					// worker.setSkipDirectoriesDoneAfter(formatter.parse(txtDate.getText()));
					txtDate.setBackground(Color.WHITE);
				} catch (ParseException e1) {
					txtDate.setBackground(new Color(255, 170, 170));
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				// logger.info("txtDate keyPressed!" + e);
			}
		});
		txtDate.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				logger.info("txtDate actionPerformed!");
			}
		});
		scanMenu.add(txtDate);

		JMenuItem scd = new JMenuItem(new AbstractAction("set current date") {

			private static final long serialVersionUID = -4763799767860486881L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Date now = new Date();
				String nowString = config.getDateFormatter().format(now);
				txtDate.setText(nowString);
				txtDate.setBackground(Color.WHITE);
				config.setProperty("skipDirectoriesDoneAfter", nowString);
				// worker.setSkipDirectoriesDoneAfter(now);
			}
		});
		scd.setMnemonic('c');
		scanMenu.add(scd);

		scanMenu.addSeparator();

		JMenuItem r1 = new JMenuItem(new AbstractAction("Remove Path1% from Database") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.forgetPath(txtPath1.getText());
					}
				}).start();
			}
		});
		r1.setMnemonic('r');
		scanMenu.add(r1);

		JMenuItem r2 = new JMenuItem(new AbstractAction("Remove Path2% from Database") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.forgetPath(txtPath2.getText());
					}
				}).start();
			}
		});
		r2.setMnemonic('e');
		scanMenu.add(r2);

		scanMenu.addSeparator();

		JMenuItem ce = new JMenuItem(new AbstractAction("Check Files stored in db below Path1%") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.checkExistence(txtPath1.getText());
					}
				}).start();
			}
		});
		ce.setToolTipText("files stored in the database are checked for existence and same size & lastmodifieddate");
		ce.setMnemonic('h');
		scanMenu.add(ce);

		JMenuItem ce1 = new JMenuItem(new AbstractAction("Check Files stored in db below Path2%") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.checkExistence(txtPath2.getText());
					}
				}).start();
			}
		});
		ce1.setToolTipText("files stored in the database are checked for existence and same size & lastmodifieddate");
		ce1.setMnemonic('k');
		scanMenu.add(ce1);

		JMenuItem cf = new JMenuItem(new AbstractAction("Check Failures below Path1%") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.checkFailuresBelow(txtPath1.getText());
					}
				}).start();
			}
		});
		cf.setToolTipText("failures that happened in previous scans are checked: removed if path does not exist, rescanned otherwise");
		cf.setMnemonic('f');
		scanMenu.add(cf);

		JMenuItem cf1 = new JMenuItem(new AbstractAction("Check Failures below Path2%") {
			private static final long serialVersionUID = -8229587970729576138L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						worker.checkFailuresBelow(txtPath2.getText());
					}
				}).start();
			}
		});
		cf1.setToolTipText("failures that happened in previous scans are checked: removed if path does not exist, rescanned otherwise");
		cf1.setMnemonic('a');
		scanMenu.add(cf1);
		return scanMenu;
	}

	private JMenu createToolsMenu() {
		JMenu toolsMenu = new JMenu("Tools");
		toolsMenu.setMnemonic('T');
		JMenuItem switch12 = new JMenuItem(new AbstractAction("Switch Path1&2") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String path1 = txtPath1.getText();
				txtPath1.setText(txtPath2.getText());
				txtPath2.setText(path1);
			}
		});
		switch12.setMnemonic('1');
		toolsMenu.add(switch12);

		JMenuItem switch23 = new JMenuItem(new AbstractAction("Switch Path2&3") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String path2 = txtPath2.getText();
				txtPath2.setText(txtPath3.getText());
				txtPath3.setText(path2);
			}
		});
		switch23.setMnemonic('2');

		toolsMenu.add(switch23);

		JMenuItem switch13 = new JMenuItem(new AbstractAction("Switch Path1&3") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String path1 = txtPath1.getText();
				txtPath1.setText(txtPath3.getText());
				txtPath3.setText(path1);
			}
		});
		switch13.setMnemonic('3');
		toolsMenu.add(switch13);
		return toolsMenu;
	}

	private JMenu createReportMenu() {
		JMenu reportMenu = new JMenu("Reports");
		reportMenu.setMnemonic('R');

		autoScanReport = new JCheckBoxMenuItem("autoscan involved directories", true);
		reportMenu.add(autoScanReport);
		reportMenu.addSeparator();

		reportMenu.add(new AbstractAction("Files in Path1") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1 });
						doReport(worker.findFiles(txtPath1.getText(), null, false, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Duplicates in Path1") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1 });
						doReport(worker.findFiles(txtPath1.getText(), txtPath1.getText(), true, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files of Path1 with Duplicates") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1 });
						doReport(worker.findFiles(txtPath1.getText(), null, true, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files in Path2") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 2 });
						doReport(worker.findFiles(txtPath2.getText(), null, false, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Duplicates in Path2") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 2 });
						doReport(worker.findFiles(txtPath2.getText(), txtPath2.getText(), true, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files of Path2 with Duplicates") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 2 });
						doReport(worker.findFiles(txtPath2.getText(), null, true, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files in Path1&2") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1, 2 });
						doReport(worker.findFiles(txtPath1.getText(), txtPath2.getText(), true, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files in Path1 not in Path2") {
			private static final long serialVersionUID = -2431148865166087753L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1, 2 });
						doReport(worker.findFiles(txtPath1.getText(), txtPath2.getText(), false, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("Files in Path2 not in Path1") {
			private static final long serialVersionUID = -2431148865166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						autoscan(true, new int[] { 1, 2 });
						doReport(worker.findFiles(txtPath2.getText(), txtPath1.getText(), false, getReportSortMode(), getFilter()));
					}
				}).start();
			}
		});
		reportMenu.add(new AbstractAction("sha1 Collisions & other problems") {
			private static final long serialVersionUID = -2431148865166087752L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						reportMetadata.setSelected(true);
						doReport(worker.findProblems());
					}
				}).start();
			}
		});

		reportMenu.add(new AbstractAction("failures below Path1%") {
			private static final long serialVersionUID = -2431148865166087752L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						reportFailures(worker.getFailuresBelow(txtPath1.getText()));
					}
				}).start();
			}
		});

		reportMenu.addSeparator();
		
		reportMetadata = new JCheckBoxMenuItem("report match metadata", true);
		reportMenu.add(reportMetadata);

		reportMenu.addSeparator();

		reportAll = new JRadioButtonMenuItem("report all", true);
		reportPath1 = new JRadioButtonMenuItem("report any Path1%");
		report1stPath1 = new JRadioButtonMenuItem("report first Path1%");
		reportNotPath1 = new JRadioButtonMenuItem("report all but Path1%");
		reportAllBut1stPath1 = new JRadioButtonMenuItem("report all but first Path1%");

		reportPath2 = new JRadioButtonMenuItem("report any Path2%");
		report1stPath2 = new JRadioButtonMenuItem("report first Path2%");
		reportNotPath2 = new JRadioButtonMenuItem("report all but Path2%");
		reportAllBut1stPath2 = new JRadioButtonMenuItem("report all but first Path2%");

		ButtonGroup reportOptions = new ButtonGroup();
		reportOptions.add(reportAll);
		reportOptions.add(reportPath1);
		reportOptions.add(report1stPath1);
		reportOptions.add(reportNotPath1);
		reportOptions.add(reportAllBut1stPath1);

		reportOptions.add(reportPath2);
		reportOptions.add(report1stPath2);
		reportOptions.add(reportNotPath2);
		reportOptions.add(reportAllBut1stPath2);

		reportMenu.add(reportAll);
		reportMenu.add(reportPath1);
		reportMenu.add(report1stPath1);
		reportMenu.add(reportNotPath1);
		reportMenu.add(reportAllBut1stPath1);

		reportMenu.add(reportPath2);
		reportMenu.add(report1stPath2);
		reportMenu.add(reportNotPath2);
		reportMenu.add(reportAllBut1stPath2);

		reportMenu.addSeparator();

		reportMenu.add(new AbstractAction("clear Reported") {
			private static final long serialVersionUID = 5774069469263470975L;

			@Override
			public void actionPerformed(ActionEvent e) {
				txtrFileList.setText("");
			}
		});
		return reportMenu;
	}

	private JMenu createActionsMenu() {
		JMenu actionsMenu = new JMenu("Actions");
		actionsMenu.setMnemonic('A');

		autoScanAction = new JCheckBoxMenuItem("autoscan involved directories", true);
		actionsMenu.add(autoScanAction);

		flattenTarget = new JCheckBoxMenuItem("flatten the directory when moving/copying to Path3", true);
		actionsMenu.add(flattenTarget);

		actionsMenu.addSeparator();

		JMenuItem move = new JMenuItem(
				new AbstractAction("Move Files that exist below Path1&2 from Path2% into Path3%") {
					private static final long serialVersionUID = -2431148565166087751L;

					@Override
					public void actionPerformed(ActionEvent e) {
						autoscan(false, new int[] { 1, 2 });
						BlockingQueue<ReportMatch> queue = worker.findFiles(txtPath1.getText(), txtPath2.getText(),
								true, getReportSortMode(), getFilter());
						worker.moveOrCopyMatches(queue, txtPath2.getText(), txtPath3.getText(), false,
								flattenTarget.isSelected());
					}
				});
		move.setMnemonic('m');
		actionsMenu.add(move);

		JMenuItem moveNeg = new JMenuItem(new AbstractAction(
				"Move Files that don't exist below Path1 from Path2% into Path3%") {
			private static final long serialVersionUID = -2431148565166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				autoscan(false, new int[] { 1, 2 });
				BlockingQueue<ReportMatch> queue = worker.findFiles(txtPath2.getText(), txtPath1.getText(), false,
						getReportSortMode(), getFilter());
				worker.moveOrCopyMatches(queue, txtPath2.getText(), txtPath3.getText(), false,
						flattenTarget.isSelected());
			}
		});
		moveNeg.setMnemonic('n');
		actionsMenu.add(moveNeg);

		JMenuItem copy = new JMenuItem(
				new AbstractAction("Copy Files that exist below Path1&2 from Path2% into Path3%") {
					private static final long serialVersionUID = -2431148565166087751L;

					@Override
					public void actionPerformed(ActionEvent e) {
						autoscan(false, new int[] { 1, 2 });
						BlockingQueue<ReportMatch> queue = worker.findFiles(txtPath1.getText(), txtPath2.getText(),
								true, getReportSortMode(), getFilter());
						worker.moveOrCopyMatches(queue, txtPath2.getText(), txtPath3.getText(), true,
								flattenTarget.isSelected());
					}
				});
		copy.setMnemonic('m');
		actionsMenu.add(copy);

		JMenuItem copyNeg = new JMenuItem(new AbstractAction(
				"Copy Files that don't exist below Path1 from Path2% into Path3%") {
			private static final long serialVersionUID = -2431148565166087751L;

			@Override
			public void actionPerformed(ActionEvent e) {
				autoscan(false, new int[] { 1, 2 });
				BlockingQueue<ReportMatch> queue = worker.findFiles(txtPath2.getText(), txtPath1.getText(), false,
						getReportSortMode(), getFilter());
				worker.moveOrCopyMatches(queue, txtPath2.getText(), txtPath3.getText(), true,
						flattenTarget.isSelected());
			}
		});
		copyNeg.setMnemonic('n');
		actionsMenu.add(copyNeg);

		return actionsMenu;
	}
	
	private JMenu createFilterMenu() {
		
		JMenu menu = new JMenu("Filter");
		menu.setMnemonic('F');
		
		actionScopeAll = new JRadioButtonMenuItem("Don't filter", true);
		actionScopeOlder = new JRadioButtonMenuItem("only files older in %path2");
		actionScopeOlderEquals = new JRadioButtonMenuItem("only files older/equals in %path2");
		performActionForEquals = new JRadioButtonMenuItem("only files with equals timestamps");
		performActionForNewerEquals = new JRadioButtonMenuItem("only files newer/equals in %path2");
		performActionForNewer = new JRadioButtonMenuItem("only files newer in %path2");
		
		ButtonGroup actionLimitation = new ButtonGroup();
		actionLimitation.add(actionScopeAll);
		actionLimitation.add(actionScopeOlder);
		actionLimitation.add(actionScopeOlderEquals);
		actionLimitation.add(performActionForEquals);
		actionLimitation.add(performActionForNewerEquals);
		actionLimitation.add(performActionForNewer);
		
		menu.add(actionScopeAll);
		menu.add(actionScopeOlder);
		menu.add(actionScopeOlderEquals);
		menu.add(performActionForEquals);
		menu.add(performActionForNewerEquals);
		menu.add(performActionForNewer);
		
		return menu;
	}

	private JMenu createSortMenu() {
		final JMenu menu = new JMenu("Sort");
		menu.setMnemonic('O');

		sortSha1 = new JRadioButtonMenuItem("sort by sha1");
		sortSize = new JRadioButtonMenuItem("sort by size");
		sortCount = new JRadioButtonMenuItem("sort by count");
		sortSizeCount = new JRadioButtonMenuItem("sort by size*count", true);

		ButtonGroup sortOptions = new ButtonGroup();
		sortOptions.add(sortSha1);
		sortOptions.add(sortSize);
		sortOptions.add(sortCount);
		sortOptions.add(sortSizeCount);

		menu.add(sortSha1);
		menu.add(sortSize);
		menu.add(sortCount);
		menu.add(sortSizeCount);
		
		return menu;
	}

	private void autoscan(boolean report, int[] paths) {
		if ((report && autoScanReport.isSelected()) || autoScanAction.isSelected()) {
			for (int i : paths) {
				switch (i) {
				case 1:
					worker.scanPath(txtPath1.getText());
					break;
				case 2:
					worker.scanPath(txtPath2.getText());
					break;
				case 3:
					worker.scanPath(txtPath3.getText());
					break;
				default:
					logger.error("invalid path #: " + i);
					break;
				}
			}
		}
	}

	private void doReport(final BlockingQueue<ReportMatch> matches) {

		tabPane.setSelectedIndex(1);

		if (txtrFileList.getText().length() > 2)
			txtrFileList.append("\n\n");

		Thread processor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						ReportMatch match = matches.take();
						if (match == ReportMatch.endOfQueue)
							break;
						reportSingleMatch(match);
					} catch (InterruptedException e) {
						logger.error("reportMatchesProcessor was interrupted", e);
					}
				}
				txtrFileList.append("end of report.");
			}
		});
		processor.start();
	}

	private void reportSingleMatch(ReportMatch match) {
		if (reportMetadata.isSelected()) {
			txtrFileList.append("\n" + match.getMetadata() + "\n");
			// Match sha1=" + AppConfig.getSha1HexString(match.getSha1()) + ";
			// size=" + match.getSize() + ";
			// count=" + match.getStore().size() + ";
			// totalSize=" + match.getSize()*match.getStore().size() + "\n");
		}
		if (reportAll.isSelected()) {
			for (StoredFile file : match.getStore()) {
				txtrFileList.append(file.getFullPath() + "\n");
			}
		} else if (reportPath1.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (file.getFullPath().startsWith(txtPath1.getText()))
					txtrFileList.append(file.getFullPath() + "\n");
			}
		} else if (reportPath2.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (file.getFullPath().startsWith(txtPath2.getText()))
					txtrFileList.append(file.getFullPath() + "\n");
			}
		} else if (reportNotPath1.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (!file.getFullPath().startsWith(txtPath1.getText()))
					txtrFileList.append(file.getFullPath() + "\n");
			}
		} else if (reportNotPath2.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (!file.getFullPath().startsWith(txtPath2.getText()))
					txtrFileList.append(file.getFullPath() + "\n");
			}
		} else if (report1stPath1.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (file.getFullPath().startsWith(txtPath1.getText())) {
					txtrFileList.append(file.getFullPath() + "\n");
					break;
				}
			}
		} else if (report1stPath2.isSelected()) {
			for (StoredFile file : match.getStore()) {
				if (file.getFullPath().startsWith(txtPath2.getText())) {
					txtrFileList.append(file.getFullPath() + "\n");
					break;
				}
			}
		} else if (reportAllBut1stPath1.isSelected()) {
			boolean firstSkipped = false;
			for (StoredFile file : match.getStore()) {
				if (firstSkipped) {
					txtrFileList.append(file.getFullPath() + "\n");
				} else {
					if (file.getFullPath().startsWith(txtPath1.getText())) {
						firstSkipped = true;
					} else {
						txtrFileList.append(file.getFullPath() + "\n");
					}
				}
			}
		} else if (reportAllBut1stPath2.isSelected()) {
			boolean firstSkipped = false;
			for (StoredFile file : match.getStore()) {
				if (firstSkipped) {
					txtrFileList.append(file.getFullPath() + "\n");
				} else {
					if (file.getFullPath().startsWith(txtPath2.getText())) {
						firstSkipped = true;
					} else {
						txtrFileList.append(file.getFullPath() + "\n");
					}
				}
			}
		}
	}

	private void reportFailures(final BlockingQueue<VisitFailure> failuresBelow) {

		if (txtrFileList.getText().length() > 2)
			txtrFileList.append("\n\n");

		Thread processor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						VisitFailure failure = failuresBelow.take();
						if (failure == VisitFailure.endOfQueue)
							break;

						String size = "; size=" + failure.getSize();
						if (failure.getError().contains("different sizes"))
							size += "; sizeRead=" + failure.getSizeRead();
						txtrFileList.append(failure.getPath() + size + "; error=" + failure.getError() + "\n");

					} catch (InterruptedException e) {
						logger.error("reportMatchesProcessor was interrupted", e);
					}
				}
				txtrFileList.append("end of report.");
			}
		});
		processor.start();
	}

	private Sort getReportSortMode() {
		if (sortSha1.isSelected()) {
			return Sort.SHA1;
		} else if (sortCount.isSelected()) {
			return Sort.COUNT;
		} else if (sortSize.isSelected()) {
			return Sort.SIZE;
		} else if (sortSizeCount.isSelected()) {
			return Sort.SIZETIMESCOUNT;
		}

		logger.error("inconsistent gui state - sortMode");
		return Sort.SHA1;
	}

	private FindFilter getFilter() {
		
		if (actionScopeAll.isSelected()) {
			return FindFilter.UNFILTERED;
		} else if (actionScopeOlder.isSelected()) {
			return FindFilter.OLDER;
		} else if (actionScopeOlderEquals.isSelected()) {
			return FindFilter.OLDEREQUALS;
		} else if (performActionForEquals.isSelected()) {
			return FindFilter.EQUALS;
		} else if (performActionForNewerEquals.isSelected()) {
			return FindFilter.NEWEREQUALS;
		} else if (performActionForNewer.isSelected()) {
			return FindFilter.NEWER;
		}

		logger.error("inconsistent gui state - actionScope");
		return FindFilter.UNFILTERED;
	}

	final javax.swing.Timer autoScrollTimer = new Timer(1000, new ActionListener() {

		private Date lastRefresh = new Date();

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (GUI.this != null && GUI.this.chckbxAutoscroll != null && GUI.this.chckbxAutoscroll.isSelected()
					&& lastLogChange != null && lastLogChange.after(lastRefresh)) {

				scrollPaneVerticalBar.setValue(scrollPaneVerticalBar.getMaximum());
				lastRefresh = new Date();
			}
			// System.gc(); // Explicit GC!
		}
	});
}
