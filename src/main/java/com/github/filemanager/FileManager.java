/*
The MIT License

Copyright (c) 2015-2025 Valentyn Kolesnikov (https://github.com/javadev/file-manager)

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

import org.apache.commons.io.FileUtils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * An improved basic File Manager. Requires Java 8+.
 *
 * <p>Includes support classes FileTableModel &amp; FileTreeCellRenderer.
 */
public class FileManager {

    /** Title of the application */
    public static final String APP_TITLE = "FileMan";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Used to open/edit/print files. */
    private Desktop desktop;

    /** Provides nice icons and names for files. */
    private FileSystemView fileSystemView;

    /** currently selected File. */
    private File currentFile;

    /** Main GUI container */
    private JPanel gui;

    /** File-system tree. Built Lazily */
    private JTree tree;

    private DefaultTreeModel treeModel;

    /** Directory listing */
    private JTable table;

    private TableRowSorter<FileTableModel> tableSorter;
    private JTextField filterField;

    private JProgressBar progressBar;

    /** Table model for File[]. */
    private FileTableModel fileTableModel;

    private ListSelectionListener listSelectionListener;
    private boolean cellSizesSet = false;
    private int rowIconPadding = 6;

    /* File controls. */
    private JButton openFile;
    private JButton printFile;
    private JButton editFile;
    private JButton deleteFile;
    private JButton newFile;
    private JButton copyFile;
    private JButton renameFile;
    private JButton refreshFile;

    /* File details. */
    private JLabel fileName;
    private JTextField path;
    private JLabel date;
    private JLabel size;
    private JCheckBox readable;
    private JCheckBox writable;
    private JCheckBox executable;
    private JRadioButton isDirectory;
    private JRadioButton isFile;

    /* Status bar */
    private JLabel statusLabel;

    /* GUI options/containers for new File/Directory creation.  Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    /** Clipboard for copy operations */
    private File clipboardFile;

    public Container getGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3, 3));
            gui.setBorder(new EmptyBorder(5, 5, 5, 5));

            fileSystemView = FileSystemView.getFileSystemView();
            desktop = Desktop.getDesktop();

            JPanel detailView = new JPanel(new BorderLayout(3, 3));

            table = new JTable();
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setAutoCreateRowSorter(false);
            table.setShowVerticalLines(false);
            table.setFillsViewportHeight(true);

            listSelectionListener =
                    new ListSelectionListener() {
                        @Override
                        public void valueChanged(ListSelectionEvent lse) {
                            if (lse.getValueIsAdjusting()) {
                                return;
                            }
                            int viewRow = table.getSelectionModel().getLeadSelectionIndex();
                            if (viewRow < 0 || viewRow >= table.getRowCount()) {
                                setFileDetails(null);
                                return;
                            }
                            int modelRow = table.convertRowIndexToModel(viewRow);
                            setFileDetails(((FileTableModel) table.getModel()).getFile(modelRow));
                        }
                    };
            table.getSelectionModel().addListSelectionListener(listSelectionListener);

            // Double-click on directory in table navigates into it
            table.addMouseListener(
                    new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getClickCount() == 2) {
                                int viewRow = table.getSelectedRow();
                                if (viewRow < 0) return;
                                int modelRow = table.convertRowIndexToModel(viewRow);
                                File f = ((FileTableModel) table.getModel()).getFile(modelRow);
                                if (f != null && f.isDirectory()) {
                                    navigateTo(f);
                                } else if (f != null && desktop.isSupported(Desktop.Action.OPEN)) {
                                    try {
                                        desktop.open(f);
                                    } catch (Throwable t) {
                                        showThrowable(t);
                                    }
                                }
                            }
                        }
                    });

            JScrollPane tableScroll = new JScrollPane(table);
            Dimension d = tableScroll.getPreferredSize();
            tableScroll.setPreferredSize(
                    new Dimension((int) d.getWidth(), (int) d.getHeight() / 2));

            // Filter panel above the table
            JPanel filterPanel = new JPanel(new BorderLayout(3, 3));
            filterPanel.setBorder(new EmptyBorder(0, 2, 2, 2));
            filterPanel.add(new JLabel("Filter: "), BorderLayout.WEST);
            filterField = new JTextField();
            filterField
                    .getDocument()
                    .addDocumentListener(
                            new DocumentListener() {
                                @Override
                                public void insertUpdate(DocumentEvent e) {
                                    applyFilter();
                                }

                                @Override
                                public void removeUpdate(DocumentEvent e) {
                                    applyFilter();
                                }

                                @Override
                                public void changedUpdate(DocumentEvent e) {
                                    applyFilter();
                                }
                            });
            filterPanel.add(filterField, BorderLayout.CENTER);

            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.add(filterPanel, BorderLayout.NORTH);
            tablePanel.add(tableScroll, BorderLayout.CENTER);

            detailView.add(tablePanel, BorderLayout.CENTER);

            // the File tree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode();
            treeModel = new DefaultTreeModel(root);

            TreeSelectionListener treeSelectionListener =
                    new TreeSelectionListener() {
                        public void valueChanged(TreeSelectionEvent tse) {
                            DefaultMutableTreeNode node =
                                    (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
                            showChildren(node);
                            Object userObj = node.getUserObject();
                            if (userObj instanceof File) {
                                setFileDetails((File) userObj);
                            }
                        }
                    };

            // show the file system roots.
            File[] roots = fileSystemView.getRoots();
            for (File fileSystemRoot : roots) {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileSystemRoot);
                root.add(node);
                File[] files = fileSystemView.getFiles(fileSystemRoot, true);
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory()) {
                            node.add(new DefaultMutableTreeNode(file));
                        }
                    }
                }
            }

            tree = new JTree(treeModel);
            tree.setRootVisible(false);
            tree.addTreeSelectionListener(treeSelectionListener);
            tree.setCellRenderer(new FileTreeCellRenderer());
            tree.expandRow(0);
            JScrollPane treeScroll = new JScrollPane(tree);

            tree.setVisibleRowCount(15);

            Dimension preferredSize = treeScroll.getPreferredSize();
            Dimension widePreferred = new Dimension(240, (int) preferredSize.getHeight());
            treeScroll.setPreferredSize(widePreferred);

            // details for a File
            JPanel fileMainDetails = new JPanel(new BorderLayout(4, 2));
            fileMainDetails.setBorder(new EmptyBorder(0, 6, 0, 6));

            JPanel fileDetailsLabels = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

            JPanel fileDetailsValues = new JPanel(new GridLayout(0, 1, 2, 2));
            fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

            fileDetailsLabels.add(new JLabel("File", JLabel.TRAILING));
            fileName = new JLabel();
            fileDetailsValues.add(fileName);
            fileDetailsLabels.add(new JLabel("Path/name", JLabel.TRAILING));
            path = new JTextField(5);
            path.setEditable(false);
            fileDetailsValues.add(path);
            fileDetailsLabels.add(new JLabel("Last Modified", JLabel.TRAILING));
            date = new JLabel();
            fileDetailsValues.add(date);
            fileDetailsLabels.add(new JLabel("File size", JLabel.TRAILING));
            size = new JLabel();
            fileDetailsValues.add(size);
            fileDetailsLabels.add(new JLabel("Type", JLabel.TRAILING));

            JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
            isDirectory = new JRadioButton("Directory");
            isDirectory.setEnabled(false);
            flags.add(isDirectory);

            isFile = new JRadioButton("File");
            isFile.setEnabled(false);
            flags.add(isFile);
            fileDetailsValues.add(flags);

            int count = fileDetailsLabels.getComponentCount();
            for (int ii = 0; ii < count; ii++) {
                fileDetailsLabels.getComponent(ii).setEnabled(false);
            }

            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            openFile = new JButton("Open");
            openFile.setMnemonic('o');
            openFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            if (currentFile == null) {
                                showErrorMessage("No file selected.", "Select File");
                                return;
                            }
                            try {
                                desktop.open(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                            gui.repaint();
                        }
                    });
            toolBar.add(openFile);

            editFile = new JButton("Edit");
            editFile.setMnemonic('e');
            editFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            if (currentFile == null) {
                                showErrorMessage("No file selected.", "Select File");
                                return;
                            }
                            try {
                                desktop.edit(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                        }
                    });
            toolBar.add(editFile);

            printFile = new JButton("Print");
            printFile.setMnemonic('p');
            printFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            if (currentFile == null) {
                                showErrorMessage("No file selected.", "Select File");
                                return;
                            }
                            try {
                                desktop.print(currentFile);
                            } catch (Throwable t) {
                                showThrowable(t);
                            }
                        }
                    });
            toolBar.add(printFile);

            // Check the actions are supported on this platform!
            openFile.setEnabled(desktop.isSupported(Desktop.Action.OPEN));
            editFile.setEnabled(desktop.isSupported(Desktop.Action.EDIT));
            printFile.setEnabled(desktop.isSupported(Desktop.Action.PRINT));

            toolBar.addSeparator();

            newFile = new JButton("New");
            newFile.setMnemonic('n');
            newFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            newFile();
                        }
                    });
            toolBar.add(newFile);

            copyFile = new JButton("Copy");
            copyFile.setMnemonic('c');
            copyFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            copyFileAction();
                        }
                    });
            toolBar.add(copyFile);

            renameFile = new JButton("Rename");
            renameFile.setMnemonic('r');
            renameFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            renameFile();
                        }
                    });
            toolBar.add(renameFile);

            deleteFile = new JButton("Delete");
            deleteFile.setMnemonic('d');
            deleteFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            deleteFile();
                        }
                    });
            toolBar.add(deleteFile);

            refreshFile = new JButton("Refresh");
            refreshFile.setMnemonic('f');
            refreshFile.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            refresh();
                        }
                    });
            toolBar.add(refreshFile);

            toolBar.addSeparator();

            readable = new JCheckBox("Read  ");
            readable.setMnemonic('a');
            readable.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            applyPermissions();
                        }
                    });
            toolBar.add(readable);

            writable = new JCheckBox("Write  ");
            writable.setMnemonic('w');
            writable.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            applyPermissions();
                        }
                    });
            toolBar.add(writable);

            executable = new JCheckBox("Execute");
            executable.setMnemonic('x');
            executable.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            applyPermissions();
                        }
                    });
            toolBar.add(executable);

            JPanel fileView = new JPanel(new BorderLayout(3, 3));

            fileView.add(toolBar, BorderLayout.NORTH);
            fileView.add(fileMainDetails, BorderLayout.CENTER);

            detailView.add(fileView, BorderLayout.SOUTH);

            JSplitPane splitPane =
                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
            gui.add(splitPane, BorderLayout.CENTER);

            // Status bar with progress
            JPanel statusPanel = new JPanel(new BorderLayout(3, 3));
            statusLabel = new JLabel(" Ready");
            statusPanel.add(statusLabel, BorderLayout.CENTER);
            progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(150, 18));
            statusPanel.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);

            gui.add(statusPanel, BorderLayout.SOUTH);

            // Keyboard shortcuts
            installKeyboardShortcuts();

            // Initially disable file-specific buttons
            updateButtonsState();
        }
        return gui;
    }

    private void installKeyboardShortcuts() {
        InputMap im = gui.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = gui.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
        am.put(
                "rename",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        renameFile();
                    }
                });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put(
                "delete",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        deleteFile();
                    }
                });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refresh");
        am.put(
                "refresh",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        refresh();
                    }
                });

        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask), "copy");
        am.put(
                "copy",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        copyToClipboard();
                    }
                });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask), "paste");
        am.put(
                "paste",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        pasteFromClipboard();
                    }
                });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, menuMask), "new");
        am.put(
                "new",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        newFile();
                    }
                });
    }

    public JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem newItem = new JMenuItem("New...", 'N');
        newItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        newFile();
                    }
                });
        fileMenu.add(newItem);

        JMenuItem refreshItem = new JMenuItem("Refresh", 'R');
        refreshItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        refreshItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        refresh();
                    }
                });
        fileMenu.add(refreshItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit", 'x');
        exitItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        System.exit(0);
                    }
                });
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');

        JMenuItem copyItem = new JMenuItem("Copy", 'C');
        copyItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        copyToClipboard();
                    }
                });
        editMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Paste", 'P');
        pasteItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        pasteFromClipboard();
                    }
                });
        editMenu.add(pasteItem);

        editMenu.addSeparator();

        JMenuItem renameItem = new JMenuItem("Rename...", 'R');
        renameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        renameItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        renameFile();
                    }
                });
        editMenu.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("Delete", 'D');
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        deleteItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        deleteFile();
                    }
                });
        editMenu.add(deleteItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        JMenuItem aboutItem = new JMenuItem("About", 'A');
        aboutItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JOptionPane.showMessageDialog(
                                gui,
                                APP_TITLE
                                        + "\n\nA simple file manager written in Java Swing.\n"
                                        + "Shortcuts: F2=Rename, Delete=Delete, F5=Refresh\n"
                                        + "Ctrl+C=Copy, Ctrl+V=Paste, Ctrl+N=New",
                                "About " + APP_TITLE,
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void applyFilter() {
        if (tableSorter == null || filterField == null) {
            return;
        }
        String text = filterField.getText();
        if (text == null || text.trim().length() == 0) {
            tableSorter.setRowFilter(null);
        } else {
            try {
                tableSorter.setRowFilter(
                        RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1));
            } catch (Exception ex) {
                tableSorter.setRowFilter(null);
            }
        }
    }

    private void updateButtonsState() {
        boolean hasFile = currentFile != null;
        boolean fileExists = hasFile && currentFile.exists();
        if (editFile != null)
            editFile.setEnabled(fileExists && desktop.isSupported(Desktop.Action.EDIT));
        if (openFile != null)
            openFile.setEnabled(fileExists && desktop.isSupported(Desktop.Action.OPEN));
        if (printFile != null)
            printFile.setEnabled(fileExists && desktop.isSupported(Desktop.Action.PRINT));
        if (deleteFile != null) deleteFile.setEnabled(fileExists);
        if (renameFile != null) renameFile.setEnabled(fileExists);
        if (copyFile != null) copyFile.setEnabled(fileExists);
    }

    private void copyToClipboard() {
        if (currentFile == null) {
            showErrorMessage("No file selected.", "Select File");
            return;
        }
        clipboardFile = currentFile;
        setStatus("Copied to clipboard: " + clipboardFile.getName());
    }

    private void pasteFromClipboard() {
        if (clipboardFile == null) {
            showErrorMessage("Clipboard is empty.", "Paste");
            return;
        }
        if (currentFile == null) {
            showErrorMessage("No target location selected.", "Paste");
            return;
        }
        File targetDir = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
        if (targetDir == null) {
            showErrorMessage("Invalid target directory.", "Paste");
            return;
        }
        File dest = new File(targetDir, clipboardFile.getName());
        if (dest.exists()) {
            int res =
                    JOptionPane.showConfirmDialog(
                            gui,
                            "File '" + dest.getName() + "' already exists. Overwrite?",
                            "Overwrite?",
                            JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            if (clipboardFile.isDirectory()) {
                FileUtils.copyDirectory(clipboardFile, dest);
            } else {
                FileUtils.copyFile(clipboardFile, dest);
            }
            setStatus("Pasted: " + dest.getName());
            TreePath parentPath = findTreePath(targetDir);
            if (parentPath != null) {
                DefaultMutableTreeNode parentNode =
                        (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                showChildren(parentNode);
            }
        } catch (Throwable t) {
            showThrowable(t);
        }
    }

    private void copyFileAction() {
        if (currentFile == null) {
            showErrorMessage("No file selected.", "Select File");
            return;
        }
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(gui);
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setDialogTitle("Copy to directory");
        chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        if (currentFile.getParentFile() != null) {
            chooser.setCurrentDirectory(currentFile.getParentFile());
        }
        int result = chooser.showDialog(parent, "Copy Here");
        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            File destDir = chooser.getSelectedFile();
            File dest = new File(destDir, currentFile.getName());
            if (dest.exists()) {
                int res =
                        JOptionPane.showConfirmDialog(
                                gui,
                                "File '" + dest.getName() + "' already exists. Overwrite?",
                                "Overwrite?",
                                JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                if (currentFile.isDirectory()) {
                    FileUtils.copyDirectory(currentFile, dest);
                } else {
                    FileUtils.copyFile(currentFile, dest);
                }
                setStatus("Copied to: " + dest.getAbsolutePath());
                TreePath parentPath = findTreePath(destDir);
                if (parentPath != null) {
                    DefaultMutableTreeNode parentNode =
                            (DefaultMutableTreeNode) parentPath.getLastPathComponent();
                    showChildren(parentNode);
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
    }

    private void applyPermissions() {
        if (currentFile == null || !currentFile.exists()) {
            return;
        }
        try {
            currentFile.setReadable(readable.isSelected());
            currentFile.setWritable(writable.isSelected());
            currentFile.setExecutable(executable.isSelected());
            setStatus("Updated permissions for: " + currentFile.getName());
        } catch (Throwable t) {
            showThrowable(t);
        }
    }

    private void refresh() {
        if (currentFile == null) {
            return;
        }
        File dir = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
        if (dir == null) {
            return;
        }
        TreePath treePath = findTreePath(dir);
        if (treePath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            showChildren(node);
            setStatus("Refreshed: " + dir.getAbsolutePath());
        }
    }

    private void navigateTo(File dir) {
        TreePath path = findTreePath(dir);
        if (path != null) {
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        } else {
            // Not in tree yet - just display
            DefaultMutableTreeNode tempNode = new DefaultMutableTreeNode(dir);
            showChildren(tempNode);
        }
    }

    public void showRootFile() {
        tree.setSelectionInterval(0, 0);
    }

    private TreePath findTreePath(File find) {
        if (find == null) return null;
        for (int ii = 0; ii < tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            Object userObj = node.getUserObject();
            if (userObj instanceof File) {
                File nodeFile = (File) userObj;
                if (nodeFile.equals(find)) {
                    return treePath;
                }
            }
        }
        return null;
    }

    private void renameFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected to rename.", "Select File");
            return;
        }

        String renameTo =
                (String)
                        JOptionPane.showInputDialog(
                                gui,
                                "New Name",
                                "Rename",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                null,
                                currentFile.getName());
        if (renameTo != null && renameTo.trim().length() > 0) {
            try {
                boolean directory = currentFile.isDirectory();
                File parentFile = currentFile.getParentFile();
                if (parentFile == null) {
                    showErrorMessage("Cannot rename root file.", "Rename Failed");
                    return;
                }
                TreePath parentPath = findTreePath(parentFile);
                DefaultMutableTreeNode parentNode =
                        parentPath != null
                                ? (DefaultMutableTreeNode) parentPath.getLastPathComponent()
                                : null;

                File newFile = new File(parentFile, renameTo);
                if (newFile.exists()) {
                    showErrorMessage("A file with that name already exists.", "Rename Failed");
                    return;
                }
                boolean renamed = currentFile.renameTo(newFile);
                if (renamed) {
                    if (directory) {
                        TreePath currentPath = findTreePath(currentFile);
                        if (currentPath != null) {
                            DefaultMutableTreeNode currentNode =
                                    (DefaultMutableTreeNode) currentPath.getLastPathComponent();
                            treeModel.removeNodeFromParent(currentNode);
                        }
                    }
                    if (parentNode != null) {
                        showChildren(parentNode);
                    }
                    setStatus("Renamed to: " + newFile.getName());
                } else {
                    String msg = "The file '" + currentFile + "' could not be renamed.";
                    showErrorMessage(msg, "Rename Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void deleteFile() {
        if (currentFile == null) {
            showErrorMessage("No file selected for deletion.", "Select File");
            return;
        }

        String message =
                currentFile.isDirectory()
                        ? "Are you sure you want to delete this directory and all its contents?\n"
                                + currentFile.getAbsolutePath()
                        : "Are you sure you want to delete this file?\n"
                                + currentFile.getAbsolutePath();

        int result =
                JOptionPane.showConfirmDialog(
                        gui,
                        message,
                        "Delete File",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            try {
                File parentFile = currentFile.getParentFile();
                TreePath parentPath = findTreePath(parentFile);
                DefaultMutableTreeNode parentNode =
                        parentPath != null
                                ? (DefaultMutableTreeNode) parentPath.getLastPathComponent()
                                : null;

                boolean directory = currentFile.isDirectory();
                File toDelete = currentFile;
                if (FileUtils.deleteQuietly(toDelete)) {
                    if (directory) {
                        TreePath currentPath = findTreePath(toDelete);
                        if (currentPath != null) {
                            DefaultMutableTreeNode currentNode =
                                    (DefaultMutableTreeNode) currentPath.getLastPathComponent();
                            treeModel.removeNodeFromParent(currentNode);
                        }
                    }
                    if (parentNode != null) {
                        showChildren(parentNode);
                    }
                    setStatus("Deleted: " + toDelete.getName());
                    currentFile = null;
                    updateButtonsState();
                } else {
                    String msg = "The file '" + currentFile + "' could not be deleted.";
                    showErrorMessage(msg, "Delete Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
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
        name.setText("");

        int result =
                JOptionPane.showConfirmDialog(
                        gui, newFilePanel, "Create File", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                if (name.getText() == null || name.getText().trim().length() == 0) {
                    showErrorMessage("Name cannot be empty.", "Create Failed");
                    return;
                }
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File(parentFile, name.getText());
                if (file.exists()) {
                    showErrorMessage("A file with that name already exists.", "Create Failed");
                    return;
                }
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {
                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                            (DefaultMutableTreeNode) parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);
                        treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    showChildren(parentNode);
                    setStatus("Created: " + file.getName());
                } else {
                    String msg = "The file '" + file + "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch (Throwable t) {
                showThrowable(t);
            }
        }
        gui.repaint();
    }

    private void setStatus(String text) {
        if (statusLabel != null) {
            statusLabel.setText(" " + text);
        }
    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(gui, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(gui, t.toString(), t.getMessage(), JOptionPane.ERROR_MESSAGE);
        gui.repaint();
    }

    /** Update the table on the EDT */
    private void setTableData(final File[] files) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        if (fileTableModel == null) {
                            fileTableModel = new FileTableModel();
                            table.setModel(fileTableModel);
                            tableSorter = new TableRowSorter<FileTableModel>(fileTableModel);
                            table.setRowSorter(tableSorter);
                        }
                        table.getSelectionModel()
                                .removeListSelectionListener(listSelectionListener);
                        fileTableModel.setFiles(files);
                        table.getSelectionModel().addListSelectionListener(listSelectionListener);
                        applyFilter();
                        if (!cellSizesSet && files != null && files.length > 0) {
                            Icon icon = fileSystemView.getSystemIcon(files[0]);
                            if (icon != null) {
                                table.setRowHeight(icon.getIconHeight() + rowIconPadding);
                            }
                            setColumnWidth(0, -1);
                            setColumnWidth(3, 80);
                            table.getColumnModel().getColumn(3).setMaxWidth(120);
                            setColumnWidth(4, -1);
                            setColumnWidth(5, -1);
                            setColumnWidth(6, -1);
                            setColumnWidth(7, -1);
                            setColumnWidth(8, -1);
                            setColumnWidth(9, -1);

                            cellSizesSet = true;
                        }
                        int shown = (files == null) ? 0 : files.length;
                        setStatus(shown + " item(s)");
                    }
                });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width < 0) {
            JLabel label = new JLabel((String) tableColumn.getHeaderValue());
            Dimension preferred = label.getPreferredSize();
            width = (int) preferred.getWidth() + 14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    /** Add the files that are contained within the directory of this node. */
    private void showChildren(final DefaultMutableTreeNode node) {
        tree.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        SwingWorker<Void, File> worker =
                new SwingWorker<Void, File>() {
                    @Override
                    public Void doInBackground() {
                        Object userObj = node.getUserObject();
                        if (!(userObj instanceof File)) {
                            return null;
                        }
                        File file = (File) userObj;
                        if (file.isDirectory()) {
                            File[] files = fileSystemView.getFiles(file, true);
                            if (files == null) {
                                files = new File[0];
                            }
                            if (node.isLeaf()) {
                                for (File child : files) {
                                    if (child.isDirectory()) {
                                        publish(child);
                                    }
                                }
                            }
                            setTableData(files);
                        }
                        return null;
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
                        tree.setEnabled(true);
                    }
                };
        worker.execute();
    }

    /** Update the File details view with the details of this File. */
    private void setFileDetails(File file) {
        currentFile = file;
        if (file == null) {
            fileName.setIcon(null);
            fileName.setText("");
            path.setText("");
            date.setText("");
            size.setText("");
            readable.setSelected(false);
            writable.setSelected(false);
            executable.setSelected(false);
            isDirectory.setSelected(false);
            isFile.setSelected(false);
            updateButtonsState();
            gui.repaint();
            return;
        }
        Icon icon = fileSystemView.getSystemIcon(file);
        fileName.setIcon(icon);
        fileName.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(DATE_FORMAT.format(new Date(file.lastModified())));
        size.setText(formatSize(file.length()) + " (" + file.length() + " bytes)");
        readable.setSelected(file.canRead());
        writable.setSelected(file.canWrite());
        executable.setSelected(file.canExecute());
        isDirectory.setSelected(file.isDirectory());
        isFile.setSelected(file.isFile());

        JFrame f = (JFrame) gui.getTopLevelAncestor();
        if (f != null) {
            f.setTitle(APP_TITLE + " :: " + fileSystemView.getSystemDisplayName(file));
        }

        updateButtonsState();
        gui.repaint();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static boolean copyFile(File from, File to) throws IOException {
        boolean created = to.createNewFile();
        if (created) {
            FileChannel fromChannel = null;
            FileChannel toChannel = null;
            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(from);
                fos = new FileOutputStream(to);
                fromChannel = fis.getChannel();
                toChannel = fos.getChannel();

                toChannel.transferFrom(fromChannel, 0, fromChannel.size());

                to.setReadable(from.canRead());
                to.setWritable(from.canWrite());
                to.setExecutable(from.canExecute());
            } finally {
                if (fromChannel != null) {
                    try {
                        fromChannel.close();
                    } catch (IOException ignored) {
                    }
                }
                if (toChannel != null) {
                    try {
                        toChannel.close();
                    } catch (IOException ignored) {
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException ignored) {
                    }
                }
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return created;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        try {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        } catch (Exception weTried) {
                        }
                        JFrame f = new JFrame(APP_TITLE);
                        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                        FileManager fileManager = new FileManager();
                        f.setContentPane(fileManager.getGui());
                        f.setJMenuBar(fileManager.createMenuBar());

                        try {
                            URL urlBig = fileManager.getClass().getResource("fm-icon-32x32.png");
                            URL urlSmall = fileManager.getClass().getResource("fm-icon-16x16.png");
                            ArrayList<Image> images = new ArrayList<Image>();
                            if (urlBig != null) images.add(ImageIO.read(urlBig));
                            if (urlSmall != null) images.add(ImageIO.read(urlSmall));
                            if (!images.isEmpty()) f.setIconImages(images);
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
}

/** A TableModel to hold File[]. */
class FileTableModel extends AbstractTableModel {

    private File[] files;
    private FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private String[] columns = {
        "Icon", "File", "Path/name", "Size", "Last Modified", "R", "W", "E", "D", "F",
    };

    FileTableModel() {
        this(new File[0]);
    }

    FileTableModel(File[] files) {
        this.files = files;
    }

    public Object getValueAt(int row, int column) {
        File file = files[row];
        switch (column) {
            case 0:
                return fileSystemView.getSystemIcon(file);
            case 1:
                return fileSystemView.getSystemDisplayName(file);
            case 2:
                return file.getPath();
            case 3:
                return file.length();
            case 4:
                return new Date(file.lastModified());
            case 5:
                return file.canRead();
            case 6:
                return file.canWrite();
            case 7:
                return file.canExecute();
            case 8:
                return file.isDirectory();
            case 9:
                return file.isFile();
            default:
                System.err.println("Logic Error");
        }
        return "";
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0:
                return ImageIcon.class;
            case 3:
                return Long.class;
            case 4:
                return Date.class;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
                return Boolean.class;
        }
        return String.class;
    }

    public String getColumnName(int column) {
        return columns[column];
    }

    public int getRowCount() {
        return files == null ? 0 : files.length;
    }

    public File getFile(int row) {
        if (files == null || row < 0 || row >= files.length) {
            return null;
        }
        return files[row];
    }

    public void setFiles(File[] files) {
        this.files = files == null ? new File[0] : files;
        fireTableDataChanged();
    }
}

/** A TreeCellRenderer for a File. */
class FileTreeCellRenderer extends DefaultTreeCellRenderer {

    private FileSystemView fileSystemView;
    private JLabel label;

    FileTreeCellRenderer() {
        label = new JLabel();
        label.setOpaque(true);
        fileSystemView = FileSystemView.getFileSystemView();
    }

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObj = node.getUserObject();
        if (userObj instanceof File) {
            File file = (File) userObj;
            try {
                label.setIcon(fileSystemView.getSystemIcon(file));
                label.setText(fileSystemView.getSystemDisplayName(file));
                label.setToolTipText(file.getPath());
            } catch (Exception ex) {
                label.setIcon(null);
                label.setText(file.getName());
                label.setToolTipText(file.getPath());
            }
        } else {
            label.setIcon(null);
            label.setText(userObj == null ? "" : userObj.toString());
            label.setToolTipText(null);
        }

        if (selected) {
            label.setBackground(backgroundSelectionColor);
            label.setForeground(textSelectionColor);
        } else {
            label.setBackground(backgroundNonSelectionColor);
            label.setForeground(textNonSelectionColor);
        }

        return label;
    }
}
