package com.googlecode.directory_scanner.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
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
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.googlecode.directory_scanner.contracts.WorkManager;
import com.googlecode.directory_scanner.domain.ReportMatch;
import com.googlecode.directory_scanner.workers.AppConfig;

public class GUI {

    private JFrame frmDirectoryScanner;

    private JLabel lblPath1, lblPath2;
    private JTextField txtPath1, txtPath2, txtDate;

    private JScrollPane statusScrollPane;
    private JScrollBar scrollPaneVerticalBar;
    private JTextArea txtrLog, txtrFileList;

    private JRadioButtonMenuItem sortNot, sortSize, sortCount, sortSizeCount, reportAll, reportPath1, report1stPath1, reportNotPath1, reportAllBut1stPath1,
    reportPath2, report1stPath2, reportNotPath2, reportAllBut1stPath2;
    private JCheckBoxMenuItem reportSha1andSize;

    private JCheckBox chckbxAutoscroll;

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
	frmDirectoryScanner.setVisible(true);
	timer.start();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

	frmDirectoryScanner = new JFrame("Directory Scanner");
	// frmDirectoryScanner.setTitle("Directory Scanner");
	frmDirectoryScanner.setSize(800, 600);
	frmDirectoryScanner.setPreferredSize(new Dimension(800, 600));
	frmDirectoryScanner.setMinimumSize(new Dimension(440, 320));
	// frmDirectoryScanner.setUndecorated(true);
	// frmDirectoryScanner.setShape(new Polygon(new int[]{0,300,600,300,0},
	// new int[]{300,600,300,0,300}, 5));
	// frmDirectoryScanner.setBounds(100, 100, 800, 600);
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

	// lblDate = new JLabel("Date:  ");
	// GridBagConstraints gbc_lblDate = new GridBagConstraints();
	// gbc_lblDate.anchor = GridBagConstraints.WEST;
	// gbc_lblDate.gridx = 0;
	// gbc_lblDate.gridy = 2;
	// northFormPanel.add(lblDate, gbc_lblDate);

	txtPath1 = new JTextField("/");
	GridBagConstraints gbc_txtPath1 = new GridBagConstraints();
	gbc_txtPath1.fill = GridBagConstraints.HORIZONTAL;
	gbc_txtPath1.gridx = 1;
	gbc_txtPath1.gridy = 0;
	northFormPanel.add(txtPath1, gbc_txtPath1);

	txtPath2 = new JTextField("/");
	GridBagConstraints gbc_txtPath2 = new GridBagConstraints();
	gbc_txtPath2.fill = GridBagConstraints.HORIZONTAL;
	gbc_txtPath2.gridx = 1;
	gbc_txtPath2.gridy = 1;
	northFormPanel.add(txtPath2, gbc_txtPath2);

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

	JPanel fileListOptionsPanel = new JPanel();
	fileListOptionsPanel.setLayout(new WrapLayout());

	JPanel fileListPanel = new JPanel();
	fileListPanel.setLayout(new BorderLayout());
	fileListPanel.add(fileListOptionsPanel, BorderLayout.SOUTH);
	fileListPanel.add(fileListScrollPane, BorderLayout.CENTER);

	JTabbedPane tabPane = new JTabbedPane();
	// centerPanel.setLayout(new CardLayout());
	tabPane.add(statusPanel, "Status");
	tabPane.add(fileListPanel, "Files");

	contentPane.add(northPanel, BorderLayout.NORTH);
	contentPane.add(tabPane, BorderLayout.CENTER);
    }

    private JMenuBar createMenuBar() {

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
	sp1.setAccelerator(KeyStroke.getKeyStroke("1"));
	scanMenu.add(sp1);

	JMenuItem sp2 = new JMenuItem(new AbstractAction("Scan Path2") {
	    private static final long serialVersionUID = 5505858980619274011L;

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
	sp2.setAccelerator(KeyStroke.getKeyStroke("2"));
	scanMenu.add(sp2);

	scanMenu.addSeparator();
	scanMenu.add(new JLabel("skip Directories done after:"));

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
		worker.forgetPath(txtPath1.getText());
	    }
	});
	r1.setMnemonic('r');
	scanMenu.add(r1);

	JMenuItem r2 = new JMenuItem(new AbstractAction("Remove Path2% from Database") {
	    private static final long serialVersionUID = -8229587970729576138L;

	    @Override
	    public void actionPerformed(ActionEvent e) {
		worker.forgetPath(txtPath2.getText());
	    }
	});
	r2.setMnemonic('e');
	scanMenu.add(r2);

	JMenu toolsMenu = new JMenu("Tools");
	toolsMenu.setMnemonic('T');
	JMenuItem switc = new JMenuItem(new AbstractAction("Switch Path1&2") {
	    private static final long serialVersionUID = -2431148865166087751L;

	    @Override
	    public void actionPerformed(ActionEvent e) {
		String path1 = txtPath1.getText();
		txtPath1.setText(txtPath2.getText());
		txtPath2.setText(path1);
	    }
	});
	switc.setMnemonic('S');
	toolsMenu.add(switc);

	JMenu reportMenu = new JMenu("Reports");
	reportMenu.setMnemonic('R');
	reportMenu.add(new AbstractAction("Duplicates in Path1") {
	    private static final long serialVersionUID = -2431148865166087751L;

	    @Override
	    public void actionPerformed(ActionEvent e) {
		new Thread(new Runnable() {
		    @Override
		    public void run() {
			doReport(worker.findFiles(txtPath1.getText(), txtPath1.getText(), true, getReportSortMode()));
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
			doReport(worker.findFiles(txtPath1.getText(), null, true, getReportSortMode()));
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
			doReport(worker.findFiles(txtPath2.getText(), txtPath2.getText(), true, getReportSortMode()));
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
			doReport(worker.findFiles(txtPath2.getText(), null, true, getReportSortMode()));
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
			doReport(worker.findFiles(txtPath1.getText(), txtPath2.getText(), true, getReportSortMode()));
		    }
		}).start();
	    }
	});
	reportMenu.add(new AbstractAction("Files in Path1 not in Path2") {
	    private static final long serialVersionUID = -2431148865166087751L;

	    @Override
	    public void actionPerformed(ActionEvent e) {
		new Thread(new Runnable() {
		    @Override
		    public void run() {
			doReport(worker.findFiles(txtPath1.getText(), txtPath2.getText(), false, getReportSortMode()));
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
			doReport(worker.findFiles(txtPath2.getText(), txtPath1.getText(), false, getReportSortMode()));
		    }
		}).start();
	    }
	});

	reportMenu.addSeparator();

	sortNot = new JRadioButtonMenuItem("don't sort", true);
	sortSize = new JRadioButtonMenuItem("sort by size");
	sortCount = new JRadioButtonMenuItem("sort by count");
	sortSizeCount = new JRadioButtonMenuItem("sort by size*count");

	ButtonGroup sortOptions = new ButtonGroup();
	sortOptions.add(sortNot);
	sortOptions.add(sortSize);
	sortOptions.add(sortCount);
	sortOptions.add(sortSizeCount);
	// sortNot.

	reportMenu.add(sortNot);
	reportMenu.add(sortSize);
	reportMenu.add(sortCount);
	reportMenu.add(sortSizeCount);

	reportMenu.addSeparator();

	reportSha1andSize = new JCheckBoxMenuItem("report sha1 & size");
	reportMenu.add(reportSha1andSize);

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

	JMenuBar jMenuBar = new JMenuBar();
	jMenuBar.add(scanMenu);
	jMenuBar.add(toolsMenu);
	jMenuBar.add(reportMenu);

	return jMenuBar;
    }

    private void doReport(final BlockingQueue<ReportMatch> matches) {
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
	if (reportSha1andSize.isSelected()) {
	    txtrFileList.append("\nMatch sha1=" + config.getSha1HexString(match.getSha1()) + "; size=" + match.getSize() + "; count=" + match.getPaths().size() + "; totalSize=" + match.getSize()*match.getPaths().size() + "\n");
	}
	if (reportAll.isSelected()) {
	    for (String path : match.getPaths()) {
		txtrFileList.append(path + "\n");
	    }
	} else if (reportPath1.isSelected()) {
	    for (String path : match.getPaths()) {
		if (path.startsWith(txtPath1.getText()))
		    txtrFileList.append(path + "\n");
	    }
	} else if (reportPath2.isSelected()) {
	    for (String path : match.getPaths()) {
		if (path.startsWith(txtPath2.getText()))
		    txtrFileList.append(path + "\n");
	    }
	} else if (reportNotPath1.isSelected()) {
	    for (String path : match.getPaths()) {
		if (!path.startsWith(txtPath1.getText()))
		    txtrFileList.append(path + "\n");
	    }
	} else if (reportNotPath2.isSelected()) {
	    for (String path : match.getPaths()) {
		if (!path.startsWith(txtPath2.getText()))
		    txtrFileList.append(path + "\n");
	    }
	} else if (report1stPath1.isSelected()) {
	    for (String path : match.getPaths()) {
		if (path.startsWith(txtPath1.getText())) {
		    txtrFileList.append(path + "\n");
		    break;
		}
	    }
	} else if (report1stPath2.isSelected()) {
	    for (String path : match.getPaths()) {
		if (path.startsWith(txtPath2.getText())) {
		    txtrFileList.append(path + "\n");
		    break;
		}
	    }
	} else if (reportAllBut1stPath1.isSelected()) {
	    boolean firstSkipped = false;
	    for (String path : match.getPaths()) {
		if (firstSkipped) {
		    txtrFileList.append(path + "\n");
		} else {
		    if (path.startsWith(txtPath1.getText())) {
			firstSkipped = true;
		    } else {
			txtrFileList.append(path + "\n");
		    }
		}
	    }
	} else if (reportAllBut1stPath2.isSelected()) {
	    boolean firstSkipped = false;
	    for (String path : match.getPaths()) {
		if (firstSkipped) {
		    txtrFileList.append(path + "\n");
		} else {
		    if (path.startsWith(txtPath2.getText())) {
			firstSkipped = true;
		    } else {
			txtrFileList.append(path + "\n");
		    }
		}
	    }
	}
    }

    private ReportMatch.Sort getReportSortMode() {
	if (sortNot.isSelected()) {
	    return ReportMatch.Sort.NOSORT;
	} else if (sortCount.isSelected()) {
	    return ReportMatch.Sort.COUNT;
	} else if (sortSize.isSelected()) {
	    return ReportMatch.Sort.SIZE;
	} else if (sortSizeCount.isSelected()) {
	    return ReportMatch.Sort.SIZETIMESCOUNT;
	}

	logger.error("inconsistent gui state - sortMode");
	return ReportMatch.Sort.NOSORT;
    }

    final javax.swing.Timer timer = new Timer(1000, new ActionListener() {

	private Date lastRefresh = new Date();

	@Override
	public void actionPerformed(ActionEvent arg0) {
	    if (GUI.this != null && GUI.this.chckbxAutoscroll != null && GUI.this.chckbxAutoscroll.isSelected() && lastLogChange != null
	    && lastLogChange.after(lastRefresh)) {

		scrollPaneVerticalBar.setValue(scrollPaneVerticalBar.getMaximum());
		lastRefresh = new Date();
	    }
	    // System.gc(); // Explicit GC!
	}
    });
}
