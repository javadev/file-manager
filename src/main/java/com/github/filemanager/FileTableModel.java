package com.github.filemanager;

import java.io.File;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;

/** 
 * A TableModel to hold File[]. This class extends AbstractTableModel and is used to represent 
 * a table of files with various attributes displayed in each column.
 */
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

    /**
     * Returns the value to be displayed in the specified cell in the table.
     * @param row The row index of the cell.
     * @param column The column index of the cell.
     * @return The value to be displayed in the specified cell.
     */
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
                return file.lastModified();
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

    /**
     * Returns the number of columns in the table.
     * @return The number of columns in the table.
     */
    public int getColumnCount() {
        return columns.length;
    }

    /**
     * Returns the class of the values in the specified column.
     * @param column The column index.
     * @return The class of the values in the specified column.
     */
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

    /**
     * Returns the name of the specified column.
     * @param column The column index.
     * @return The name of the specified column.
     */
    public String getColumnName(int column) {
        return columns[column];
    }

    /**
     * Returns the number of rows in the table.
     * @return The number of rows in the table.
     */
    public int getRowCount() {
        return files.length;
    }

    /**
     * Returns the file at the specified row in the table.
     * @param row The row index.
     * @return The file at the specified row.
     */
    public File getFile(int row) {
        return files[row];
    }

    /**
     * Sets the array of files to be displayed in the table and notifies all listeners that the table data has changed.
     * @param files The new array of files to be displayed in the table.
     */
    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }
}
