package com.googlecode.directory_scanner.directory_scanner;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import javax.swing.JCheckBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Gui {

	private JFrame frmDirectoryScanner;
	private JTextField path1Field;
	private JTextField path2Field;
	private JTextArea txtrStatus;

	private Walker fileWalker = new Walker();
	private JScrollPane scrollPane;
	private JCheckBox chckbxAutoscroll;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					final Gui window = new Gui();

					Worker.getLogger().addAppender(new AppenderSkeleton() {

						SimpleDateFormat formatter = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss.SSS - ");

						@Override
						public boolean requiresLayout() {
							return false;
						}

						@Override
						public void close() {
						}

						@Override
						protected void append(LoggingEvent event) {
							if (window.txtrStatus != null) {
								window.txtrStatus.append(formatter
										.format(new Date(event.timeStamp))
										+ event.getRenderedMessage() + "\n");

								if (window.chckbxAutoscroll.isSelected()) {
									JScrollBar vertical = window.scrollPane
											.getVerticalScrollBar();
									vertical.setValue(vertical.getMaximum());
								}
							}
						}
					});

					window.frmDirectoryScanner.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Gui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmDirectoryScanner = new JFrame();
		frmDirectoryScanner.setTitle("Directory Scanner");
		frmDirectoryScanner.setBounds(100, 100, 494, 300);
		frmDirectoryScanner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 1.0, 0.0,
				Double.MIN_VALUE };
		frmDirectoryScanner.getContentPane().setLayout(gridBagLayout);

		JPanel pathEntryPanel = new JPanel();
		GridBagConstraints gbc_pathEntryPanel = new GridBagConstraints();
		gbc_pathEntryPanel.insets = new Insets(0, 0, 5, 0);
		gbc_pathEntryPanel.fill = GridBagConstraints.BOTH;
		gbc_pathEntryPanel.gridx = 0;
		gbc_pathEntryPanel.gridy = 0;
		frmDirectoryScanner.getContentPane().add(pathEntryPanel,
				gbc_pathEntryPanel);
		GridBagLayout gbl_pathEntryPanel = new GridBagLayout();
		gbl_pathEntryPanel.columnWidths = new int[] { 0, 0, 0 };
		gbl_pathEntryPanel.rowHeights = new int[] { 0, 0, 0 };
		gbl_pathEntryPanel.columnWeights = new double[] { 0.0, 1.0,
				Double.MIN_VALUE };
		gbl_pathEntryPanel.rowWeights = new double[] { 0.0, 0.0,
				Double.MIN_VALUE };
		pathEntryPanel.setLayout(gbl_pathEntryPanel);

		JLabel lblPath = new JLabel("Path1:");
		GridBagConstraints gbc_lblPath = new GridBagConstraints();
		gbc_lblPath.anchor = GridBagConstraints.EAST;
		gbc_lblPath.insets = new Insets(0, 0, 5, 5);
		gbc_lblPath.gridx = 0;
		gbc_lblPath.gridy = 0;
		pathEntryPanel.add(lblPath, gbc_lblPath);

		path1Field = new JTextField();
		GridBagConstraints gbc_path1Field = new GridBagConstraints();
		gbc_path1Field.insets = new Insets(0, 0, 5, 0);
		gbc_path1Field.fill = GridBagConstraints.HORIZONTAL;
		gbc_path1Field.gridx = 1;
		gbc_path1Field.gridy = 0;
		pathEntryPanel.add(path1Field, gbc_path1Field);
		path1Field.setColumns(10);

		JLabel lblPath_1 = new JLabel("Path2:");
		GridBagConstraints gbc_lblPath_1 = new GridBagConstraints();
		gbc_lblPath_1.anchor = GridBagConstraints.EAST;
		gbc_lblPath_1.insets = new Insets(0, 0, 0, 5);
		gbc_lblPath_1.gridx = 0;
		gbc_lblPath_1.gridy = 1;
		pathEntryPanel.add(lblPath_1, gbc_lblPath_1);

		path2Field = new JTextField();
		GridBagConstraints gbc_path2Field = new GridBagConstraints();
		gbc_path2Field.fill = GridBagConstraints.HORIZONTAL;
		gbc_path2Field.gridx = 1;
		gbc_path2Field.gridy = 1;
		pathEntryPanel.add(path2Field, gbc_path2Field);
		path2Field.setColumns(10);

		JPanel buttonPanel = new JPanel();
		GridBagConstraints gbc_buttonPanel = new GridBagConstraints();
		gbc_buttonPanel.insets = new Insets(0, 0, 5, 0);
		gbc_buttonPanel.fill = GridBagConstraints.BOTH;
		gbc_buttonPanel.gridx = 0;
		gbc_buttonPanel.gridy = 1;
		frmDirectoryScanner.getContentPane().add(buttonPanel, gbc_buttonPanel);
		GridBagLayout gbl_buttonPanel = new GridBagLayout();
		// gbl_buttonPanel.columnWidths = new int[]{148, 148, 148, 0};
		// gbl_buttonPanel.rowHeights = new int[]{25, 25, 0};
		// gbl_buttonPanel.columnWeights = new double[]{0.0, 0.0, 0.0,
		// Double.MIN_VALUE};
		// gbl_buttonPanel.rowWeights = new double[]{0.0, 0.0,
		// Double.MIN_VALUE};
		buttonPanel.setLayout(gbl_buttonPanel);

		JButton btnScanPath = new JButton("Scan Path1");
		btnScanPath.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						fileWalker.scanPath(path1Field.getText());
					}
				}).start();

				txtrStatus.append("started scan thread");
			}
		});
		GridBagConstraints gbc_btnScanPath = new GridBagConstraints();
		gbc_btnScanPath.fill = GridBagConstraints.BOTH;
		gbc_btnScanPath.insets = new Insets(0, 0, 5, 5);
		gbc_btnScanPath.gridx = 0;
		gbc_btnScanPath.gridy = 0;
		buttonPanel.add(btnScanPath, gbc_btnScanPath);

		JButton btnForgetPath = new JButton("Forget Path1%");
		btnForgetPath.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

			}
		});

		JButton btnCreateDb = new JButton("Create DB");
		btnCreateDb.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Worker.getSingelton().createDatabase();
			}
		});

		GridBagConstraints gbc_btnCreateDb = new GridBagConstraints();
		gbc_btnCreateDb.fill = GridBagConstraints.BOTH;
		gbc_btnCreateDb.insets = new Insets(0, 0, 5, 0);
		gbc_btnCreateDb.gridx = 2;
		gbc_btnCreateDb.gridy = 0;
		buttonPanel.add(btnCreateDb, gbc_btnCreateDb);
		GridBagConstraints gbc_btnForgetPath = new GridBagConstraints();
		gbc_btnForgetPath.fill = GridBagConstraints.BOTH;
		gbc_btnForgetPath.insets = new Insets(0, 0, 5, 5);
		gbc_btnForgetPath.gridx = 0;
		gbc_btnForgetPath.gridy = 1;
		buttonPanel.add(btnForgetPath, gbc_btnForgetPath);

		JButton btnCreateTables = new JButton("Create Tables");
		btnCreateTables.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				Worker.getSingelton().createTables();
			}
		});

		GridBagConstraints gbc_btnCreateTables = new GridBagConstraints();
		gbc_btnCreateTables.insets = new Insets(0, 0, 5, 0);
		gbc_btnCreateTables.fill = GridBagConstraints.BOTH;
		gbc_btnCreateTables.gridx = 2;
		gbc_btnCreateTables.gridy = 1;
		buttonPanel.add(btnCreateTables, gbc_btnCreateTables);

		JButton btnDropTables = new JButton("Drop Tables");
		btnDropTables.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Worker.getSingelton().dropTables();
			}
		});

		JButton btnInfoPath = new JButton("info Path1");
		btnInfoPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Worker.getSingelton().checkExistence(path1Field.getText());
					}
				}).start();
			}
		});
		GridBagConstraints gbc_btnInfoPath = new GridBagConstraints();
		gbc_btnInfoPath.insets = new Insets(0, 0, 0, 5);
		gbc_btnInfoPath.fill = GridBagConstraints.BOTH;
		gbc_btnInfoPath.gridx = 0;
		gbc_btnInfoPath.gridy = 2;
		buttonPanel.add(btnInfoPath, gbc_btnInfoPath);

		GridBagConstraints gbc_btnDropTables = new GridBagConstraints();
		gbc_btnDropTables.insets = new Insets(0, 0, 0, 0);
		gbc_btnDropTables.fill = GridBagConstraints.BOTH;
		gbc_btnDropTables.gridx = 2;
		gbc_btnDropTables.gridy = 2;
		buttonPanel.add(btnDropTables, gbc_btnDropTables);

		JPanel statusPanel = new JPanel();
		GridBagConstraints gbc_statusPanel = new GridBagConstraints();
		gbc_statusPanel.insets = new Insets(0, 0, 5, 0);
		gbc_statusPanel.fill = GridBagConstraints.BOTH;
		gbc_statusPanel.gridx = 0;
		gbc_statusPanel.gridy = 2;
		frmDirectoryScanner.getContentPane().add(statusPanel, gbc_statusPanel);
		statusPanel.setLayout(new BorderLayout(0, 0));

		txtrStatus = new JTextArea();
		txtrStatus.setEditable(false);

		scrollPane = new JScrollPane(txtrStatus);
		statusPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		frmDirectoryScanner.getContentPane().add(panel, gbc_panel);

		chckbxAutoscroll = new JCheckBox("autoscroll");
		chckbxAutoscroll.setSelected(true);
		panel.add(chckbxAutoscroll);
	}
}
