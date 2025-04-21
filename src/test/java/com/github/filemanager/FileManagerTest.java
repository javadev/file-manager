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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class FileManagerTest {

    private FileManager fileManager;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        fileManager = new FileManager();
    }

    @Test
    void testAppTitleConstant() {
        assertEquals("FileMan", FileManager.APP_TITLE, "APP_TITLE constant should be 'FileMan'");
    }

    @Test
    void testGetGuiNotNull() {
        Container gui = fileManager.getGui();
        assertNotNull(gui, "GUI container should not be null");
    }

    @Test
    void testGetGuiReturnsSameInstance() {
        Container gui1 = fileManager.getGui();
        Container gui2 = fileManager.getGui();
        assertSame(gui1, gui2, "getGui() should return the same instance on multiple calls");
    }

    @Test
    void testShowRootFileDoesNotThrowException() {
        fileManager.getGui(); // Initialize GUI components
        assertDoesNotThrow(
                () -> fileManager.showRootFile(), "showRootFile() should not throw exceptions");
    }

    @Test
    void testCopyFileCreatesNewFile(@TempDir Path tempDir) throws IOException {
        // Create source file
        File sourceFile = tempDir.resolve("source.txt").toFile();
        Files.write(sourceFile.toPath(), "test content".getBytes());

        // Target file
        File targetFile = tempDir.resolve("target.txt").toFile();

        boolean result = FileManager.copyFile(sourceFile, targetFile);

        assertTrue(targetFile.exists(), "Target file should exist after copying");
        assertEquals(
                "test content",
                new String(Files.readAllBytes(targetFile.toPath())),
                "Target file should have the same content as source file");
    }

    @Test
    void testFileTableModelColumnCount() {
        FileTableModel model = new FileTableModel();
        assertEquals(10, model.getColumnCount(), "FileTableModel should have 10 columns");
    }

    @Test
    void testFileTableModelGetColumnClass() {
        FileTableModel model = new FileTableModel();

        assertEquals(
                ImageIcon.class, model.getColumnClass(0), "Column 0 should be ImageIcon class");
        assertEquals(String.class, model.getColumnClass(1), "Column 1 should be String class");
        assertEquals(String.class, model.getColumnClass(2), "Column 2 should be String class");
        assertEquals(Long.class, model.getColumnClass(3), "Column 3 should be Long class");
        assertEquals(Date.class, model.getColumnClass(4), "Column 4 should be Date class");
        assertEquals(Boolean.class, model.getColumnClass(5), "Column 5 should be Boolean class");
        assertEquals(Boolean.class, model.getColumnClass(6), "Column 6 should be Boolean class");
        assertEquals(Boolean.class, model.getColumnClass(7), "Column 7 should be Boolean class");
        assertEquals(Boolean.class, model.getColumnClass(8), "Column 8 should be Boolean class");
        assertEquals(Boolean.class, model.getColumnClass(9), "Column 9 should be Boolean class");
    }

    @Test
    void testFileTableModelRowCount() throws IOException {
        // Create test files
        File file1 = Files.createFile(tempDir.resolve("file1.txt")).toFile();
        File file2 = Files.createFile(tempDir.resolve("file2.txt")).toFile();

        File[] files = {file1, file2};
        FileTableModel model = new FileTableModel(files);

        assertEquals(2, model.getRowCount(), "Row count should match the number of files");
    }

    @Test
    void testFileTableModelGetFile() throws IOException {
        // Create test file
        File file1 = Files.createFile(tempDir.resolve("file1.txt")).toFile();
        File[] files = {file1};

        FileTableModel model = new FileTableModel(files);

        assertEquals(file1, model.getFile(0), "getFile(0) should return the first file");
    }

    @Test
    void testFileTableModelSetFiles() throws IOException {
        FileTableModel model = new FileTableModel();

        // Create test files
        File file1 = Files.createFile(tempDir.resolve("file1.txt")).toFile();
        File file2 = Files.createFile(tempDir.resolve("file2.txt")).toFile();

        File[] files = {file1, file2};
        model.setFiles(files);

        assertEquals(2, model.getRowCount(), "Row count should be updated after setFiles");
        assertEquals(file1, model.getFile(0), "First file should match after setFiles");
    }

    @Test
    void testFileTreeCellRendererNotNull() {
        FileTreeCellRenderer renderer = new FileTreeCellRenderer();
        JTree tree = new JTree();
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tempDir.toFile());

        Component component =
                renderer.getTreeCellRendererComponent(tree, node, false, false, true, 0, false);

        assertNotNull(component, "Tree cell renderer component should not be null");
    }

    @Test
    void testSetFileDetailsUpdatesUI() throws Exception {
        // This test uses reflection to access private method and fields
        fileManager.getGui(); // Initialize GUI components

        Method setFileDetailsMethod =
                FileManager.class.getDeclaredMethod("setFileDetails", File.class);
        setFileDetailsMethod.setAccessible(true);

        File testFile = Files.createFile(tempDir.resolve("test.txt")).toFile();
        setFileDetailsMethod.invoke(fileManager, testFile);

        // Access the private fields to verify they were updated
        Field currentFileField = FileManager.class.getDeclaredField("currentFile");
        currentFileField.setAccessible(true);
        File currentFile = (File) currentFileField.get(fileManager);

        assertEquals(testFile, currentFile, "currentFile should be updated to the test file");
    }

    @Test
    void testFindTreePathReturnsNullForNonExistentFile() throws Exception {
        // Initialize GUI components
        fileManager.getGui();

        Method findTreePathMethod = FileManager.class.getDeclaredMethod("findTreePath", File.class);
        findTreePathMethod.setAccessible(true);

        File nonExistentFile = new File("/this/file/does/not/exist");
        TreePath result = (TreePath) findTreePathMethod.invoke(fileManager, nonExistentFile);

        assertNull(result, "findTreePath should return null for non-existent file");
    }

    @Test
    void testShowErrorMessage() throws Exception {
        // Mock JOptionPane to avoid actual dialog
        try (MockedStatic<JOptionPane> mockedJOptionPane = mockStatic(JOptionPane.class)) {
            Method showErrorMessageMethod =
                    FileManager.class.getDeclaredMethod(
                            "showErrorMessage", String.class, String.class);
            showErrorMessageMethod.setAccessible(true);

            showErrorMessageMethod.invoke(fileManager, "Error message", "Error title");

            mockedJOptionPane.verify(
                    () ->
                            JOptionPane.showMessageDialog(
                                    any(),
                                    eq("Error message"),
                                    eq("Error title"),
                                    eq(JOptionPane.ERROR_MESSAGE)),
                    times(1));
        }
    }

    @Test
    void testShowThrowable() throws Exception {
        // Mock JOptionPane to avoid actual dialog
        try (MockedStatic<JOptionPane> mockedJOptionPane = mockStatic(JOptionPane.class)) {
            Method showThrowableMethod =
                    FileManager.class.getDeclaredMethod("showThrowable", Throwable.class);
            showThrowableMethod.setAccessible(true);

            fileManager.getGui();
            Exception testException = new Exception("Test exception");
            showThrowableMethod.invoke(fileManager, testException);

            mockedJOptionPane.verify(
                    () ->
                            JOptionPane.showMessageDialog(
                                    any(),
                                    eq(testException.toString()),
                                    eq(testException.getMessage()),
                                    eq(JOptionPane.ERROR_MESSAGE)),
                    times(1));
        }
    }
}
