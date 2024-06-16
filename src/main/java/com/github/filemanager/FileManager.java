/*
The MIT License

Copyright (c) 2015-2023 Valentyn Kolesnikov (https://github.com/javadev/file-manager)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package com.github.filemanager;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.*;
import javax.swing.tree.*;
import org.apache.commons.io.FileUtils;

/**
 * A basic File Manager. Requires 1.6+ for the Desktop &amp; SwingWorker
 * classes, amongst other minor things.
 *
 * <p>
 * Includes support classes FileTableModel &amp; FileTreeCellRenderer.
 *
 * <p>
 * TODO Bugs
 *
 * <ul>
 * <li>Still throws occasional AIOOBEs and NPEs, so some update on the EDT must
 * have been missed.
 * <li>Fix keyboard focus issues - especially when functions like rename/delete
 * etc. are called that update nodes &amp; file lists.
 * <li>Needs more testing in general.
 * <p>
 * TODO Functionality
 * <li>Implement Read/Write/Execute checkboxes
 * <li>Implement Copy
 * <li>Extra prompt for directory delete (camickr suggestion)
 * <li>Add File/Directory fields to FileTableModel
 * <li>Double clicking a directory in the table, should update the tree
 * <li>Move progress bar?
 * <li>Add other file display modes (besides table) in CardLayout?
 * <li>Menus + other cruft?
 * <li>Implement history/back
 * <li>Allow multiple selection
 * <li>Add file search
 * </ul>
 */
public class FileManager {

	/** Title of the application */
	public static final String APP_TITLE = "FileMan";

	/** Used to open/edit/print files. */
	private Desktop osDesktop = Desktop.getDesktop();

	/** Provides nice icons and names for files. */
	private FileSystemView osFileSystemView = FileSystemView.getFileSystemView();;

	/** currently selected File. */
	private File currentFile;

	/** Main GUI container */
	private JPanel mainGuiPanel;

	/** File-system tree. Built Lazily */
	private JTree fileSystemJTree;

	private DefaultTreeModel treeModel;

	/** Directory listing */
	private JTable dirContentFilesTable;

	private JProgressBar progressBar;
	/** Table model for File[]. */
	private FileTableModel fileTableModel;

	private ListSelectionListener fileListSelectionListener;
	private boolean cellSizesSet = false;
	private int rowIconPadding = 6;

	/* File controls. */
	private JButton openFileBtn;
	private JButton printFileBtn;
	private JButton editFileBtn;
	private JButton deleteFileBtn;
	private JButton newFileBtn;
	private JButton copyFileBtn;
	/* File details. */
	private JLabel fileNameLb;
	private JTextField pathTF;
	private JLabel fdateLb;
	private JLabel fileSizeLB;
	private JCheckBox readCBox;
	private JCheckBox writeCBox;
	private JCheckBox execCBox;
	private JRadioButton isDirRadio;
	private JRadioButton isFileRadio;

	/* GUI options/containers for new File/Directory creation. Created lazily. */
	private JPanel newFilePanel;
	private JRadioButton newTypeFile;
	private JTextField name;

	/**
	 * The hierarchy of GUI components in the FileManager class is as follows:
	 * <ul>
	 * <li>mainGuiPanel (JPanel)
	 * <ul>
	 * <li>splitPane (JSplitPane)
	 * <ul>
	 * <li>treeScroll (JScrollPane)
	 * <ul>
	 * <li>fileSystemTree (JTree)</li>
	 * </ul>
	 * </li>
	 * <li>detailView (JPanel)
	 * <ul>
	 * <li>tabScrollJSPfiles (JScrollPane)
	 * <ul>
	 * <li>dirListingTable (JTable)</li>
	 * </ul>
	 * </li>
	 * <li>fileView (JPanel)
	 * <ul>
	 * <li>toolBar (JToolBar)
	 * <ul>
	 * <li>openFileBtn (JButton)</li>
	 * <li>editFileBtn (JButton)</li>
	 * <li>printFileBtn (JButton)</li>
	 * <li>newFileBtn (JButton)</li>
	 * <li>copyFileBtn (JButton)</li>
	 * <li>renameFile (JButton)</li>
	 * <li>deleteFileBtn (JButton)</li>
	 * <li>readCBox (JCheckBox)</li>
	 * <li>writeCBox (JCheckBox)</li>
	 * <li>execCBox (JCheckBox)</li>
	 * </ul>
	 * </li>
	 * <li>fileMainDetails (JPanel)
	 * <ul>
	 * <li>fileDetailsLabels (JPanel)
	 * <ul>
	 * <li>Various JLabels</li>
	 * </ul>
	 * </li>
	 * <li>fileDetailsValues (JPanel)
	 * <ul>
	 * <li>fileNameLb (JLabel)</li>
	 * <li>pathTF (JTextField)</li>
	 * <li>date (JLabel)</li>
	 * <li>fileSizeLB (JLabel)</li>
	 * <li>flags (JPanel)
	 * <ul>
	 * <li>isDirRadio (JRadioButton)</li>
	 * <li>isFileRadio (JRadioButton)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * <li>simpleOutput (JPanel)
	 * <ul>
	 * <li>progressBar (JProgressBar)</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * </ul>
	 * Please note that this hierarchy is based on the getGui() method in the
	 * FileManager class. The newFilePanel (JPanel) is created lazily and is not
	 * part of the initial hierarchy. It contains newTypeFile (JRadioButton) and
	 * name (JTextField) among other components.
	 */

	public Container getGui() {
		if (mainGuiPanel == null) {
			mainGuiPanel = new JPanel(new BorderLayout(3, 3));
			mainGuiPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

			JPanel detailView = new JPanel(new BorderLayout(3, 3));
			// fileTableModel = new FileTableModel();

			dirContentFilesTable = createFilesTable();

			JScrollPane dirContJScrPan = createFilesTableScrollPane(dirContentFilesTable);
			
			detailView.add(dirContJScrPan, BorderLayout.CENTER);
			
			treeModel = createTreeModel();
						
			fileSystemJTree = createFileSystemJtree(treeModel);//
			
			JScrollPane treeScroll = new JScrollPane(fileSystemJTree);
			
			adjustFileTreeScroll(treeScroll);

			// details for a File

			JPanel fileMainDetJPanel = createFileMainDetailsPanel();
		
			//  toolBar creation and adjustment BEGIN
			JToolBar toolBar = createToolBar(); 
			// toolBar creation and adjustment END

			JPanel fileView = new JPanel(new BorderLayout(3, 3));

			fileView.add(toolBar, BorderLayout.NORTH);
			fileView.add(fileMainDetJPanel, BorderLayout.CENTER);

			detailView.add(fileView, BorderLayout.SOUTH);

			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
			mainGuiPanel.add(splitPane, BorderLayout.CENTER);

			JPanel simpleOutput = new JPanel(new BorderLayout(3, 3));
			progressBar = new JProgressBar();
			simpleOutput.add(progressBar, BorderLayout.EAST);
			progressBar.setVisible(false);

			mainGuiPanel.add(simpleOutput, BorderLayout.SOUTH);
		}
		return mainGuiPanel;
	}

	private JPanel createFileMainDetailsPanel() {
		

    JPanel localFileMainDetJPanel = new JPanel(new BorderLayout(4, 2));
    localFileMainDetJPanel.setBorder(new EmptyBorder(0, 6, 0, 6));

    JPanel localFileDetLabJPanel = new JPanel(new GridLayout(0, 1, 2, 2));
    localFileMainDetJPanel.add(localFileDetLabJPanel, BorderLayout.WEST);

    JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
    localFileMainDetJPanel.add(fileDetailsValues, BorderLayout.CENTER);

    localFileDetLabJPanel.add(new JLabel("File", JLabel.TRAILING));
    fileNameLb = new JLabel();
    fileDetailsValues.add(fileNameLb);
    localFileDetLabJPanel.add(new JLabel("Path/name", JLabel.TRAILING));
    pathTF = new JTextField(5);
    pathTF.setEditable(false);
    fileDetailsValues.add(pathTF);
    localFileDetLabJPanel.add(new JLabel("Last Modified", JLabel.TRAILING));
    fdateLb = new JLabel();
    fileDetailsValues.add(fdateLb);
    localFileDetLabJPanel.add(new JLabel("File size", JLabel.TRAILING));
    fileSizeLB = new JLabel();
    fileDetailsValues.add(fileSizeLB);
    localFileDetLabJPanel.add(new JLabel("Type", JLabel.TRAILING));

    JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
    isDirRadio = new JRadioButton("Directory");
    isDirRadio.setEnabled(false);
    flags.add(isDirRadio);

    isFileRadio = new JRadioButton("File");
    isFileRadio.setEnabled(false);
    flags.add(isFileRadio);
    fileDetailsValues.add(flags);

    int count = localFileDetLabJPanel.getComponentCount();
    for (int ii = 0; ii < count; ii++) {
        localFileDetLabJPanel.getComponent(ii).setEnabled(false);
    }

    return localFileMainDetJPanel;


	}

	private JToolBar createToolBar() {
		JToolBar freshJToolBar = new JToolBar();
		// mnemonics stop working in a floated toolbar
		freshJToolBar.setFloatable(false);

		openFileBtn = new JButton("Open");
		openFileBtn.setMnemonic('o');

		openFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					osDesktop.open(currentFile);
				} catch (Throwable t) {
					showThrowable(t);
				}
				mainGuiPanel.repaint();
			}
		});
		freshJToolBar.add(openFileBtn);

		editFileBtn = new JButton("Edit");
		editFileBtn.setMnemonic('e');
		editFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					osDesktop.edit(currentFile);
				} catch (Throwable t) {
					showThrowable(t);
				}
			}
		});
		freshJToolBar.add(editFileBtn);

		printFileBtn = new JButton("Print");
		printFileBtn.setMnemonic('p');
		printFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				try {
					osDesktop.print(currentFile);
				} catch (Throwable t) {
					showThrowable(t);
				}
			}
		});
		freshJToolBar.add(printFileBtn);

		// Check the actions are supported on this platform!
		openFileBtn.setEnabled(osDesktop.isSupported(Desktop.Action.OPEN));
		editFileBtn.setEnabled(osDesktop.isSupported(Desktop.Action.EDIT));
		printFileBtn.setEnabled(osDesktop.isSupported(Desktop.Action.PRINT));

		freshJToolBar.addSeparator();

		newFileBtn = new JButton("New");
		newFileBtn.setMnemonic('n');
		newFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				newFile();
			}
		});
		freshJToolBar.add(newFileBtn);

		copyFileBtn = new JButton("Copy");
		copyFileBtn.setMnemonic('c');
		copyFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showErrorMessage("'Copy' not implemented.", "Not implemented.");
			}
		});
		freshJToolBar.add(copyFileBtn);

		JButton renameFileBtn = new JButton("Rename");
		renameFileBtn.setMnemonic('r');
		renameFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				renameFile();
			}
		});
		freshJToolBar.add(renameFileBtn);

		deleteFileBtn = new JButton("Delete");
		deleteFileBtn.setMnemonic('d');
		deleteFileBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				deleteMarkedFiles();
			}
		});
		freshJToolBar.add(deleteFileBtn);

		freshJToolBar.addSeparator();

		readCBox = new JCheckBox("Read  ");
		readCBox.setMnemonic('a');
		// readable.setEnabled(false);
		freshJToolBar.add(readCBox);

		writeCBox = new JCheckBox("Write  ");
		writeCBox.setMnemonic('w');
		// writable.setEnabled(false);
		freshJToolBar.add(writeCBox);

		execCBox = new JCheckBox("Execute");
		execCBox.setMnemonic('x');
		// executable.setEnabled(false);
		freshJToolBar.add(execCBox);
		return freshJToolBar;
	}

	private void adjustFileTreeScroll(JScrollPane treeScroll) {
		Dimension preferredSize = treeScroll.getPreferredSize();
		Dimension widePreferred = new Dimension(200, (int) preferredSize.getHeight());
		treeScroll.setPreferredSize(widePreferred);
	}

	private JTree createFileSystemJtree(DefaultTreeModel pramJTreeModel) {
		
		TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) fileSystemJTree.getLastSelectedPathComponent();
				if (node == null) {
					return;
				}
				showChildren(node);
				File file = (File) node.getUserObject();
				setFileDetails(file);
			}
		};
		JTree freshJtree = new JTree(pramJTreeModel);
		freshJtree.setRootVisible(false);
		freshJtree.addTreeSelectionListener(treeSelectionListener);
		freshJtree.setCellRenderer(new FileTreeCellRenderer());
		freshJtree.expandRow(0);
		// as per trashgod tip
		freshJtree.setVisibleRowCount(15);

		return freshJtree;
	}

	private DefaultTreeModel createTreeModel() {
		// the File tree
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		DefaultTreeModel localtreeModel = new DefaultTreeModel(root);

		
		// show the file system roots.
		File[] roots = osFileSystemView.getRoots();
		for (File fileSystemRoot : roots) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
			root.add(node);
			// showChildren(node);
			//
			File[] files = osFileSystemView.getFiles(fileSystemRoot, true);
			for (File file : files) {
				if (file.isDirectory()) {
					node.add(new DefaultMutableTreeNode(file));
				}
			}
			//
		}
		return localtreeModel;
	}

	private JScrollPane createFilesTableScrollPane(JTable dirContentFilesTable2) {
		JScrollPane freshJSScrollPane = new JScrollPane(dirContentFilesTable2);
		Dimension d = freshJSScrollPane.getPreferredSize();
		freshJSScrollPane.setPreferredSize(new Dimension((int) d.getWidth(), (int) d.getHeight() / 2));
		return freshJSScrollPane;
	}

	private JTable createFilesTable() {
		JTable freshDirContentFilesTable = new JTable();
		freshDirContentFilesTable = new JTable();
		freshDirContentFilesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		freshDirContentFilesTable.setAutoCreateRowSorter(true);
		freshDirContentFilesTable.setShowVerticalLines(false);

		/* code reacting on selection events in */
		fileListSelectionListener = new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent lse) {

				int frowInd = dirContentFilesTable.getSelectionModel().getLeadSelectionIndex();
				// lse.getFirstIndex(); //TODO - get the first index of the selection
				File f = ((FileTableModel) dirContentFilesTable.getModel()).getFile(frowInd);
				setFileDetails(f);
			}
		};

		freshDirContentFilesTable.getSelectionModel().addListSelectionListener(fileListSelectionListener);
		return freshDirContentFilesTable;
	}

	public void showRootFile() {
		// ensure the main files are displayed
		fileSystemJTree.setSelectionInterval(0, 0);
	}

	private TreePath findTreePath(File find) {
		for (int ii = 0; ii < fileSystemJTree.getRowCount(); ii++) {
			TreePath treePath = fileSystemJTree.getPathForRow(ii);
			Object object = treePath.getLastPathComponent();
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
			File nodeFile = (File) node.getUserObject();

			if (nodeFile.equals(find)) {
				return treePath;
			}
		}
		// not found!
		return null;
	}

	private void renameFile() {
		if (currentFile == null) {
			showErrorMessage("No file selected to rename.", "Select File");
			return;
		}

		String renameTo = JOptionPane.showInputDialog(mainGuiPanel, "New Name");
		if (renameTo != null) {
			try {
				boolean directory = currentFile.isDirectory();
				TreePath parentPath = findTreePath(currentFile.getParentFile());
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

				boolean renamed = currentFile.renameTo(new File(currentFile.getParentFile(), renameTo));
				if (renamed) {
					if (directory) {
						// rename the node..

						// delete the current node..
						TreePath currentPath = findTreePath(currentFile);
						System.out.println(currentPath);
						DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
								.getLastPathComponent();

						treeModel.removeNodeFromParent(currentNode);

						// add a new node..
					}

					showChildren(parentNode);
				} else {
					String msg = "The file '" + currentFile + "' could not be renamed.";
					showErrorMessage(msg, "Rename Failed");
				}
			} catch (Throwable t) {
				showThrowable(t);
			}
		}
		mainGuiPanel.repaint();
	}

	private void deleteMarkedFiles() {
		if (currentFile == null) {
			showErrorMessage("No file selected for deletion.", "Select File");
			return;
		}

		int result = JOptionPane.showConfirmDialog(mainGuiPanel, "Are you sure you want to delete this file?",
				"Delete File", JOptionPane.ERROR_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			try {
				System.out.println("currentFile: " + currentFile);
				TreePath parentPath = findTreePath(currentFile.getParentFile());
				System.out.println("parentPath: " + parentPath);
				DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
				System.out.println("parentNode: " + parentNode);

				boolean directory = currentFile.isDirectory();
				if (FileUtils.deleteQuietly(currentFile)) {
					if (directory) {
						// delete the node..
						TreePath currentPath = findTreePath(currentFile);
						System.out.println(currentPath);
						DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
								.getLastPathComponent();

						treeModel.removeNodeFromParent(currentNode);
					}

					showChildren(parentNode);
				} else {
					String msg = "The file '" + currentFile + "' could not be deleted.";
					showErrorMessage(msg, "Delete Failed");
				}
			} catch (Throwable t) {
				showThrowable(t);
			}
		}
		mainGuiPanel.repaint();
	}

	private void newFile() {
		if (currentFile == null) {
			showErrorMessage("No location selected for new file.", "Select Location");
			return;
		}

		if (newFilePanel == null) {
			newFilePanel = new JPanel(new BorderLayout(3, 3));

			JPanel southRadio = new JPanel(new GridLayout(1, 0, 2, 2));
			newTypeFile = new JRadioButton("File", true);
			JRadioButton newTypeDirectory = new JRadioButton("Directory");
			ButtonGroup bg = new ButtonGroup();
			bg.add(newTypeFile);
			bg.add(newTypeDirectory);
			southRadio.add(newTypeFile);
			southRadio.add(newTypeDirectory);

			name = new JTextField(15);

			newFilePanel.add(new JLabel("Name"), BorderLayout.WEST);
			newFilePanel.add(name);
			newFilePanel.add(southRadio, BorderLayout.SOUTH);
		}

		int result = JOptionPane.showConfirmDialog(mainGuiPanel, newFilePanel, "Create File",
				JOptionPane.OK_CANCEL_OPTION);
		if (result == JOptionPane.OK_OPTION) {
			try {
				boolean created;
				File parentFile = currentFile;
				if (!parentFile.isDirectory()) {
					parentFile = parentFile.getParentFile();
				}
				File file = new File(parentFile, name.getText());
				if (newTypeFile.isSelected()) {
					created = file.createNewFile();
				} else {
					created = file.mkdir();
				}
				if (created) {

					TreePath parentPath = findTreePath(parentFile);
					DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();

					if (file.isDirectory()) {
						// add the new node..
						DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

						TreePath currentPath = findTreePath(currentFile);
						DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentPath
								.getLastPathComponent();

						treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
					}

					showChildren(parentNode);
				} else {
					String msg = "The file '" + file + "' could not be created.";
					showErrorMessage(msg, "Create Failed");
				}
			} catch (Throwable t) {
				showThrowable(t);
			}
		}
		mainGuiPanel.repaint();
	}

	/** Update the table on the EDT */
	private void setTableData(final File[] files) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (fileTableModel == null) {
					fileTableModel = new FileTableModel();
					dirContentFilesTable.setModel(fileTableModel);
				}
				dirContentFilesTable.getSelectionModel().removeListSelectionListener(fileListSelectionListener);
				fileTableModel.setFiles(files);
				dirContentFilesTable.getSelectionModel().addListSelectionListener(fileListSelectionListener);
				if (!cellSizesSet) {
					Icon icon = osFileSystemView.getSystemIcon(files[0]);

					// size adjustment to better account for icons
					dirContentFilesTable.setRowHeight(icon.getIconHeight() + rowIconPadding);

					setColumnWidth(0, -1, dirContentFilesTable);
					setColumnWidth(3, 60, dirContentFilesTable);
					dirContentFilesTable.getColumnModel().getColumn(3).setMaxWidth(120);
					setColumnWidth(4, -1, dirContentFilesTable);
					setColumnWidth(5, -1, dirContentFilesTable);
					setColumnWidth(6, -1, dirContentFilesTable);
					setColumnWidth(7, -1, dirContentFilesTable);
					setColumnWidth(8, -1, dirContentFilesTable);
					setColumnWidth(9, -1, dirContentFilesTable);

					cellSizesSet = true;
				}
			}
		});
	}

	private void setColumnWidth(int column, int width, JTable table) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		if (width < 0) {
			// use the preferred width of the header..
			JLabel label = new JLabel((String) tableColumn.getHeaderValue());
			Dimension preferred = label.getPreferredSize();
			// altered 10->14 as per camickr comment.
			width = (int) preferred.getWidth() + 14;
		}
		tableColumn.setPreferredWidth(width);
		tableColumn.setMaxWidth(width);
		tableColumn.setMinWidth(width);
	}

	/**
	 * Add the files that are contained within the directory of this node. Thanks to
	 * Hovercraft Full Of Eels.
	 */
	private void showChildren(final DefaultMutableTreeNode node) {
		fileSystemJTree.setEnabled(false);
		progressBar.setVisible(true);
		progressBar.setIndeterminate(true);

		SwingWorker<Void, File> worker = new SwingWorker<Void, File>() {
			@Override
			public Void doInBackground() {
				File file = (File) node.getUserObject();
				if (file.isDirectory()) {
					File[] filesFromChosenDir = osFileSystemView.getFiles(file, true); // !!
					if (node.isLeaf()) {
						for (File child : filesFromChosenDir) {
							if (child.isDirectory()) {
								publish(child);
							}
						}
					}
					setTableData(filesFromChosenDir);
				}
				return null;// In case of plain file do nothing
			}

			@Override
			protected void process(List<File> chunks) {
				for (File child : chunks) {
					node.add(new DefaultMutableTreeNode(child));
				}
			}

			@Override
			protected void done() {
				progressBar.setIndeterminate(false);
				progressBar.setVisible(false);
				fileSystemJTree.setEnabled(true);
			}
		};
		worker.execute();
	}

	/** Update the File details view with the details of this File. */
	private void setFileDetails(File file) {
		currentFile = file;
		Icon icon = osFileSystemView.getSystemIcon(file);
		fileNameLb.setIcon(icon);
		fileNameLb.setText(osFileSystemView.getSystemDisplayName(file));
		pathTF.setText(file.getPath());
		fdateLb.setText(new Date(file.lastModified()).toString());
		fileSizeLB.setText(file.length() + " bytes");
		readCBox.setSelected(file.canRead());
		writeCBox.setSelected(file.canWrite());
		execCBox.setSelected(file.canExecute());
		isDirRadio.setSelected(file.isDirectory());

		isFileRadio.setSelected(file.isFile());

		JFrame f = (JFrame) mainGuiPanel.getTopLevelAncestor();
		if (f != null) {
			f.setTitle(APP_TITLE + " :: " + osFileSystemView.getSystemDisplayName(file));
		}

		mainGuiPanel.repaint();
	}

	public static boolean copyFile(File from, File to) throws IOException {

		boolean created = to.createNewFile();

		if (created) {
			FileChannel fromChannel = null;
			FileChannel toChannel = null;
			try {
				fromChannel = new FileInputStream(from).getChannel();
				toChannel = new FileOutputStream(to).getChannel();

				toChannel.transferFrom(fromChannel, 0, fromChannel.size());

				// set the flags of the to the same as the from
				to.setReadable(from.canRead());
				to.setWritable(from.canWrite());
				to.setExecutable(from.canExecute());
			} finally {
				if (fromChannel != null) {
					fromChannel.close();
				}
				if (toChannel != null) {
					toChannel.close();
				}
				return false;
			}
		}
		return created;
	}// end of copyFile

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					// Significantly improves the look of the output in
					// terms of the file names returned by FileSystemView!
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception weTried) {
				}
				JFrame f = new JFrame(APP_TITLE);
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

				FileManager fileManager = new FileManager();
				f.setContentPane(fileManager.getGui());

				try {
					URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
					URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
					ArrayList<Image> images = new ArrayList<Image>();
					images.add(ImageIO.read(urlBig));
					images.add(ImageIO.read(urlSmall));
					f.setIconImages(images);
				} catch (Exception weTried) {
				}

				f.pack();
				f.setLocationByPlatform(true);
				f.setMinimumSize(f.getSize());
				f.setVisible(true);

				fileManager.showRootFile();
			}
		});
	}

	private void showErrorMessage(String errorMessage, String errorTitle) {
		JOptionPane.showMessageDialog(mainGuiPanel, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
	}

	private void showThrowable(Throwable t) {
		t.printStackTrace();
		JOptionPane.showMessageDialog(mainGuiPanel, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
		mainGuiPanel.repaint();
	}

}// end of FileManager.main
