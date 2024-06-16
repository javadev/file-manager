I want two panels exactly like detailView side by side. Depending on which one is focused on the choice made in fileSystemTree will be reflected in currently focused panel


Copilot4Eclipse


To achieve this, you can create two instances of the detailView panel and add them to the JSplitPane. You will also need to keep track of which panel is currently focused. Here's how you can do it:

Create two instances of detailView panel, let's call them detailView1 and detailView2.
Add a FocusListener to both detailView1 and detailView2. In the focusGained method, update a variable focusedPanel to the panel that gained focus.
In the TreeSelectionListener for fileSystemTree, update the focusedPanel instead of the detailView.
Here's the modified code:


// Step 1: Create two instances of detailView panel
JPanel detailView1 = createDetailView();
JPanel detailView2 = createDetailView();

// Step 2: Add a FocusListener to both detailView1 and detailView2
FocusListener focusListener = new FocusAdapter() {
    @Override
    public void focusGained(FocusEvent e) {
        focusedPanel = (JPanel) e.getSource();
    }
};
detailView1.addFocusListener(focusListener);
detailView2.addFocusListener(focusListener);

// Step 3: In the TreeSelectionListener for fileSystemTree, update the focusedPanel
TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
    public void valueChanged(TreeSelectionEvent tse) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tse.getPath().getLastPathComponent();
        showChildren(node);
        setFileDetails((File) node.getUserObject(), focusedPanel);
    }
};

// Add both detailView1 and detailView2 to a JSplitPane
JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, detailView1, detailView2);
mainGuiPanel.add(splitPane, BorderLayout.CENTER);
In the above code, createDetailView is a method that creates a new detailView panel. You would need to extract the code that creates the detailView panel into this new method. The setFileDetails method is also modified to take an additional parameter, the panel to update.

Please note that Swing components should be updated on the Event Dispatch Thread (EDT). You might need to use SwingUtilities.invokeLater or SwingWorker to ensure this.

Using 1 Reference
 What are the key features of the FileManager class?

wojtekmichalik

in the file referenced list all interactions with the instance of detailedView


Copilot4Eclipse


In the provided code, the detailView instance is interacted with in the following ways:

It is created as a new JPanel with a BorderLayout.

JPanel detailView = new JPanel(new BorderLayout(3, 3));
A JScrollPane named tabScrollJSPfiles is added to it.

detailView.add(tabScrollJSPfiles, BorderLayout.CENTER);
A JPanel named fileView is added to it.

detailView.add(fileView, BorderLayout.SOUTH);
It is added to a JSplitPane named splitPane.

JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailView);
Please note that the detailView instance itself does not have any event listeners attached to it or any methods invoked on it directly. It is mainly used as a container to hold other components.


- `mainGuiPanel` (JPanel)
  - `splitPane` (JSplitPane)
    - `treeScroll` (JScrollPane)
      - `fileSystemJTree` (JTree)
    - `detailView` (JPanel)
      - `dirContJScrPan` (JScrollPane)
        - `dirContentFilesTable` (JTable)
      - `fileView` (JPanel)
        - `toolBar` (JToolBar)
          - `openFileBtn` (JButton)
          - `editFileBtn` (JButton)
          - `printFileBtn` (JButton)
          - `newFileBtn` (JButton)
          - `copyFileBtn` (JButton)
          - `deleteFileBtn` (JButton)
          - `readCBox` (JCheckBox)
          - `writeCBox` (JCheckBox)
          - `execCBox` (JCheckBox)
        - `fileMainDetails` (JPanel)
          - `fileDetailsLabels` (JPanel)
            - Various JLabels
          - `fileDetailsValues` (JPanel)
            - `fileNameLb` (JLabel)
            - `pathTF` (JTextField)
            - `date` (JLabel)
            - `fileSizeLB` (JLabel)
            - `flags` (JPanel)
              - `isDirRadio` (JRadioButton)
              - `isFileRadio` (JRadioButton)
  - `simpleOutput` (JPanel)
    - `progressBar` (JProgressBar)

Please note that the `newFilePanel` (JPanel) is created lazily and is not part of the initial hierarchy. It contains `newTypeFile` (JRadioButton) and `name` (JTextField) among other components.
