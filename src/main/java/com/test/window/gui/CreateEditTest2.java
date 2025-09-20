package com.test.window.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CreateEditTest2 {
    public static File lastUsedFile = null;
    public static boolean isTableModified = false;
    private static List<String> previousElementNames = new ArrayList<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            JFrame frame = new JFrame("Test Table Application");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setResizable(false);
            frame.setLayout(new BorderLayout(10, 10));
            frame.getContentPane().setBackground(new Color(30, 30, 30));

            JLabel titleLabel = new JLabel("Create Test Steps", SwingConstants.LEFT);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
            titleLabel.setForeground(new Color(200, 200, 200));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            frame.add(titleLabel, BorderLayout.NORTH);

            String[] columnNames1 = {"Test Id", "Test Step", "Test Action", "Test Element", "Test Data", "Description"};
            DefaultTableModel tableModel1 = new DefaultTableModel(columnNames1, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 1;
                }
            };
            JTable table1 = new JTable(tableModel1);

            table1.setRowHeight(25);
            table1.setFont(new Font("Arial", Font.PLAIN, 12));
            table1.setBackground(new Color(50, 50, 50));
            table1.setForeground(new Color(200, 200, 200));
            table1.setGridColor(new Color(100, 100, 100));
            table1.setShowGrid(true);
            table1.setIntercellSpacing(new Dimension(1, 1));
            table1.setSelectionBackground(new Color(100, 149, 237));
            table1.setSelectionForeground(new Color(255, 255, 255));

            TableColumn testIdColumn1 = table1.getColumnModel().getColumn(0);
            testIdColumn1.setPreferredWidth(60);
            testIdColumn1.setMaxWidth(60);
            testIdColumn1.setMinWidth(60);

            TableColumn testStepColumn1 = table1.getColumnModel().getColumn(1);
            testStepColumn1.setPreferredWidth(80);
            testStepColumn1.setMaxWidth(80);
            testStepColumn1.setMinWidth(80);

            class TestIdCellEditor extends DefaultCellEditor {
                private JTable table;
                private int row;
                private String oldValue;
                private JTextField textField;

                public TestIdCellEditor(JTextField textField, JTable table) {
                    super(textField);
                    this.textField = textField;
                    this.table = table;
                    setClickCountToStart(1); // Enable single-click editing
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    this.row = row;
                    oldValue = value.toString();
                    textField.setText(oldValue);
                    return textField;
                }

                @Override
                public boolean stopCellEditing() {
                    String newValue = (String) getCellEditorValue();
                    if (!newValue.isEmpty() && !newValue.equals("#")) {
                        for (int i = 0; i < table.getRowCount(); i++) {
                            if (i != row) {
                                String existingId = String.valueOf(table.getValueAt(i, 0));
                                if (newValue.equals(existingId)) {
                                    JOptionPane.showMessageDialog(table, "Duplicate Test Id not allowed", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                                    textField.setText(oldValue);
                                    return false;
                                }
                            }
                        }
                    }
                    // Removed: isTableModified = true; (rely on table model listener instead)
                    return super.stopCellEditing();
                }
            }

            JTextField testIdTextField = new JTextField();
            AbstractDocument testIdDoc = (AbstractDocument) testIdTextField.getDocument();
            testIdDoc.setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                    if (string == null) return;
                    String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                    String newText = currentText.substring(0, offset) + string + currentText.substring(offset);
                    if (isValidInput(newText)) {
                        super.insertString(fb, offset, string, attr);
                    }
                }

                @Override
                public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                    if (text == null) return;
                    String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                    String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
                    if (isValidInput(newText)) {
                        super.replace(fb, offset, length, text, attrs);
                    }
                }

                private boolean isValidInput(String text) {
                    if (text.equals("#")) return true;
                    return text.matches("[0-9]*");
                }
            });
            testIdColumn1.setCellEditor(new TestIdCellEditor(testIdTextField, table1));

            JTextField defaultTextField = new JTextField();
            DefaultCellEditor defaultEditor = new DefaultCellEditor(defaultTextField);
            defaultEditor.setClickCountToStart(1);
            table1.getColumnModel().getColumn(4).setCellEditor(defaultEditor);
            table1.getColumnModel().getColumn(5).setCellEditor(defaultEditor);

            JTableHeader header1 = table1.getTableHeader();
            header1.setFont(new Font("Arial", Font.BOLD, 12));
            header1.setBackground(new Color(70, 130, 180));
            header1.setForeground(Color.BLACK);
            header1.setReorderingAllowed(false);

            DefaultTableCellRenderer leftRenderer1 = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setForeground(isSelected ? Color.WHITE : new Color(200, 200, 200));
                    return c;
                }
            };
            leftRenderer1.setHorizontalAlignment(JLabel.LEFT);
            for (int i = 0; i < table1.getColumnCount(); i++) {
                table1.getColumnModel().getColumn(i).setCellRenderer(leftRenderer1);
            }

            DefaultTableCellRenderer testActionRenderer1 = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setForeground(isSelected ? Color.WHITE : Color.ORANGE);
                    return c;
                }
            };
            testActionRenderer1.setHorizontalAlignment(JLabel.LEFT);
            TableColumn testActionColumn1 = table1.getColumnModel().getColumn(2);
            testActionColumn1.setCellRenderer(testActionRenderer1);

            DefaultTableCellRenderer testElementRenderer1 = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setForeground(isSelected ? Color.WHITE : new Color(255, 102, 102));
                    return c;
                }
            };
            testElementRenderer1.setHorizontalAlignment(JLabel.LEFT);
            TableColumn testElementColumn1 = table1.getColumnModel().getColumn(3);
            testElementColumn1.setCellRenderer(testElementRenderer1);

            DefaultTableCellRenderer testDataRenderer1 = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setForeground(isSelected ? Color.WHITE : new Color(135, 206, 250));
                    return c;
                }
            };
            testDataRenderer1.setHorizontalAlignment(JLabel.LEFT);
            TableColumn testDataColumn1 = table1.getColumnModel().getColumn(4);
            testDataColumn1.setCellRenderer(testDataRenderer1);

            String[] testActionOptions = {"", "APP_ID", "SET", "CLICK", "TAKE_SCREENSHOT"};
            JComboBox<String> testActionComboBox = new JComboBox<>(testActionOptions);
            testActionComboBox.setBackground(new Color(50, 50, 50));
            testActionComboBox.setForeground(new Color(200, 200, 200));
            testActionComboBox.setFont(new Font("Arial", Font.PLAIN, 12));
            testActionColumn1.setCellEditor(new DefaultCellEditor(testActionComboBox));

            String[] columnNames2 = {"Element Name", "Automation ID", "Name", "Xpath"};
            DefaultTableModel tableModel2 = new DefaultTableModel(columnNames2, 0);
            JTable table2 = new JTable(tableModel2);

            JTextField table2TextField = new JTextField();
            DefaultCellEditor table2Editor = new DefaultCellEditor(table2TextField);
            table2Editor.setClickCountToStart(1);
            for (int i = 0; i < table2.getColumnCount(); i++) {
                table2.getColumnModel().getColumn(i).setCellEditor(table2Editor);
            }

            for (int i = 0; i < tableModel2.getRowCount(); i++) {
                previousElementNames.add(String.valueOf(tableModel2.getValueAt(i, 0)));
            }

            JComboBox<String> testElementComboBox = new JComboBox<>();
            testElementComboBox.setEditable(false);
            testElementComboBox.setBackground(new Color(50, 50, 50));
            testElementComboBox.setForeground(new Color(200, 200, 200));
            testElementComboBox.setFont(new Font("Arial", Font.PLAIN, 12));
            testElementComboBox.addItem("");

            class TestElementCellEditor extends DefaultCellEditor {
                public TestElementCellEditor(JComboBox<String> comboBox) {
                    super(comboBox);
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    JComboBox<?> editorCombo = (JComboBox<?>) editorComponent;
                    editorCombo.setSelectedItem(value != null ? value.toString() : "");
                    return editorCombo;
                }

                @Override
                public boolean stopCellEditing() {
                    // Removed: isTableModified = true; (rely on table model listener instead)
                    return super.stopCellEditing();
                }
            }

            for (int i = 0; i < tableModel2.getRowCount(); i++) {
                String elementName = String.valueOf(tableModel2.getValueAt(i, 0));
                if (!elementName.isEmpty()) {
                    testElementComboBox.addItem(elementName);
                }
            }
            testElementColumn1.setCellEditor(new TestElementCellEditor(testElementComboBox));

            tableModel2.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 0) {
                        int row = e.getFirstRow();
                        String newElementName = String.valueOf(tableModel2.getValueAt(row, 0));
                        String oldElementName = (row < previousElementNames.size()) ? previousElementNames.get(row) : "";
                        if (!oldElementName.isEmpty() && !oldElementName.equals(newElementName)) {
                            for (int i = 0; i < tableModel1.getRowCount(); i++) {
                                String testElement = String.valueOf(tableModel1.getValueAt(i, 3));
                                if (testElement.equals(oldElementName)) {
                                    tableModel1.setValueAt(newElementName, i, 3);
                                    isTableModified = true;
                                }
                            }
                        }
                        while (previousElementNames.size() <= row) {
                            previousElementNames.add("");
                        }
                        previousElementNames.set(row, newElementName);
                    } else if (e.getType() == TableModelEvent.INSERT) {
                        for (int i = e.getFirstRow(); i <= e.getLastRow(); i++) {
                            while (previousElementNames.size() <= i) {
                                previousElementNames.add("");
                            }
                            previousElementNames.add(i, "");
                        }
                    } else if (e.getType() == TableModelEvent.DELETE) {
                        for (int i = e.getLastRow(); i >= e.getFirstRow(); i--) {
                            if (i < previousElementNames.size()) {
                                previousElementNames.remove(i);
                            }
                        }
                    }
                    if (e.getColumn() == 0 || e.getType() == TableModelEvent.INSERT || e.getType() == TableModelEvent.DELETE) {
                        testElementComboBox.removeAllItems();
                        testElementComboBox.addItem("");
                        Set<String> uniqueElements = new TreeSet<>();
                        for (int i = 0; i < tableModel2.getRowCount(); i++) {
                            String elementName = String.valueOf(tableModel2.getValueAt(i, 0));
                            if (!elementName.isEmpty()) {
                                uniqueElements.add(elementName);
                            }
                        }
                        for (String elementName : uniqueElements) {
                            testElementComboBox.addItem(elementName);
                        }
                        isTableModified = true;
                    }
                }
            });

            JScrollPane scrollPane2 = new JScrollPane(table2);
            scrollPane2.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            scrollPane2.getViewport().setBackground(new Color(30, 30, 30));

            table2.setRowHeight(25);
            table2.setFont(new Font("Arial", Font.PLAIN, 12));
            table2.setBackground(new Color(50, 50, 50));
            table2.setForeground(new Color(200, 200, 200));
            table2.setGridColor(new Color(100, 100, 100));
            table2.setShowGrid(true);
            table2.setIntercellSpacing(new Dimension(1, 1));
            table2.setSelectionBackground(new Color(100, 149, 237));
            table2.setSelectionForeground(new Color(255, 255, 255));

            int totalWidth = 600; // Estimate or dynamically get table2.getWidth() after layout
            table2.getColumnModel().getColumn(0).setPreferredWidth((int) (totalWidth * 0.15)); // Element Name: 20% = 120 pixels
            table2.getColumnModel().getColumn(1).setPreferredWidth((int) (totalWidth * 0.10)); // Automation ID: 15% = 90 pixels
            table2.getColumnModel().getColumn(2).setPreferredWidth((int) (totalWidth * 0.10)); // Name: 15% = 90 pixels
            table2.getColumnModel().getColumn(3).setPreferredWidth((int) (totalWidth * 0.65)); // Xpath: 50% = 300 pixels
            for (int i = 0; i < table2.getColumnCount(); i++) {
                table2.getColumnModel().getColumn(i).setMinWidth(50);
                table2.getColumnModel().getColumn(i).setMaxWidth(Integer.MAX_VALUE);
            }

            class ElementNameCellEditor extends DefaultCellEditor {
                private JTable table;
                private int row;
                private String oldValue;
                private JTextField textField;

                public ElementNameCellEditor(JTextField textField, JTable table) {
                    super(textField);
                    this.textField = textField;
                    this.table = table;
                    setClickCountToStart(1); // Enable single-click editing
                    AbstractDocument doc = (AbstractDocument) textField.getDocument();
                    doc.setDocumentFilter(new DocumentFilter() {
                        @Override
                        public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                            if (string == null) return;
                            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                            String newText = currentText.substring(0, offset) + string + currentText.substring(offset);
                            if (isValidInput(newText)) {
                                super.insertString(fb, offset, string, attr);
                            }
                        }

                        @Override
                        public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                            if (text == null) return;
                            String currentText = fb.getDocument().getText(0, fb.getDocument().getLength());
                            String newText = currentText.substring(0, offset) + text + currentText.substring(offset + length);
                            if (isValidInput(newText)) {
                                super.replace(fb, offset, length, text, attrs);
                            }
                        }

                        private boolean isValidInput(String text) {
                            return text.isEmpty() || text.matches("[a-zA-Z][a-zA-Z0-9-_]*");
                        }
                    });
                }

                @Override
                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    this.row = row;
                    oldValue = value.toString();
                    textField.setText(oldValue);
                    return textField;
                }

                public boolean stopCellEditing() {
                    String newValue = (String) getCellEditorValue();
                    if (!newValue.isEmpty()) {
                        if (!newValue.matches("[a-zA-Z][a-zA-Z0-9-_]*")) {
                            JOptionPane.showMessageDialog(table, "Element Name must start with a character (a-z or A-Z) and can only contain alphabets, numbers, '-' and '_'", "Invalid Input", JOptionPane.ERROR_MESSAGE);
                            textField.setText(oldValue);
                            return false;
                        }
                        for (int i = 0; i < table.getRowCount(); i++) {
                            if (i != row) {
                                String existingName = String.valueOf(table.getValueAt(i, 0));
                                if (newValue.equalsIgnoreCase(existingName) && !existingName.isEmpty()) {
                                    JOptionPane.showMessageDialog(table, "Duplicate Element Name not allowed (case-insensitive)", "Duplicate Error", JOptionPane.ERROR_MESSAGE);
                                    textField.setText(oldValue);
                                    return false;
                                }
                            }
                        }
                    }
                    // Removed: isTableModified = true; (rely on table model listener instead)
                    return super.stopCellEditing();
                }
            }

            JTextField elementNameTextField = new JTextField();
            TableColumn elementNameColumn = table2.getColumnModel().getColumn(0);
            elementNameColumn.setCellEditor(new ElementNameCellEditor(elementNameTextField, table2));

            JTableHeader header2 = table2.getTableHeader();
            header2.setFont(new Font("Arial", Font.BOLD, 12));
            header2.setBackground(new Color(70, 130, 180));
            header2.setForeground(Color.BLACK);
            header2.setReorderingAllowed(false);

            DefaultTableCellRenderer leftRenderer2 = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setForeground(isSelected ? Color.WHITE : new Color(200, 200, 200));
                    return c;
                }
            };
            leftRenderer2.setHorizontalAlignment(JLabel.LEFT);
            for (int i = 0; i < table2.getColumnCount(); i++) {
                table2.getColumnModel().getColumn(i).setCellRenderer(leftRenderer2);
            }

            JScrollPane scrollPane1 = new JScrollPane(table1);
            scrollPane1.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            scrollPane1.getViewport().setBackground(new Color(30, 30, 30));

            JPanel tablePanel = new JPanel(new GridBagLayout());
            tablePanel.setBackground(new Color(30, 30, 30));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.weighty = 0.7;
            gbc.gridx = 0;
            gbc.gridy = 0;
            tablePanel.add(scrollPane1, gbc);

            gbc.weighty = 0.3;
            gbc.gridy = 1;
            tablePanel.add(scrollPane2, gbc);

            frame.add(tablePanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            buttonPanel.setBackground(new Color(30, 30, 30));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

            Dimension buttonSize = new Dimension(100, 20);

            Runnable stopEditingAndTabOut = () -> {
                if (table1.isEditing()) {
                    int editingRow = table1.getEditingRow();
                    int editingColumn = table1.getEditingColumn();
                    table1.getCellEditor(editingRow, editingColumn).stopCellEditing();
                    for (int col = editingColumn + 1; col < table1.getColumnCount(); col++) {
                        if (table1.isCellEditable(editingRow, col)) {
                            table1.setRowSelectionInterval(editingRow, editingRow);
                            table1.setColumnSelectionInterval(col, col);
                            table1.requestFocus();
                            return;
                        }
                    }
                    if (editingRow + 1 < table1.getRowCount()) {
                        for (int col = 0; col < table1.getColumnCount(); col++) {
                            if (table1.isCellEditable(editingRow + 1, col)) {
                                table1.setRowSelectionInterval(editingRow + 1, editingRow + 1);
                                table1.setColumnSelectionInterval(col, col);
                                table1.requestFocus();
                                return;
                            }
                        }
                    }
                }
                if (table2.isEditing()) {
                    int editingRow = table2.getEditingRow();
                    int editingColumn = table2.getEditingColumn();
                    table2.getCellEditor(editingRow, editingColumn).stopCellEditing();
                    for (int col = editingColumn + 1; col < table2.getColumnCount(); col++) {
                        if (table2.isCellEditable(editingRow, col)) {
                            table2.setRowSelectionInterval(editingRow, editingRow);
                            table2.setColumnSelectionInterval(col, col);
                            table2.requestFocus();
                            return;
                        }
                    }
                    if (editingRow + 1 < table2.getRowCount()) {
                        for (int col = 0; col < table2.getColumnCount(); col++) {
                            if (table2.isCellEditable(editingRow + 1, col)) {
                                table2.setRowSelectionInterval(editingRow + 1, editingRow + 1);
                                table2.setColumnSelectionInterval(col, col);
                                table2.requestFocus();
                                return;
                            }
                        }
                    }
                }
            };

            MouseAdapter tableMouseListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JTable targetTable = (JTable) e.getSource();
                    int clickedRow = targetTable.rowAtPoint(e.getPoint());
                    int clickedColumn = targetTable.columnAtPoint(e.getPoint());
                    if (targetTable.isEditing()) {
                        int editingRow = targetTable.getEditingRow();
                        int editingColumn = targetTable.getEditingColumn();
                        if (clickedRow != editingRow || clickedColumn != editingColumn || clickedRow == -1 || clickedColumn == -1) {
                            stopEditingAndTabOut.run();
                        }
                    } else {
                        JTable otherTable = (targetTable == table1) ? table2 : table1;
                        if (otherTable.isEditing()) {
                            stopEditingAndTabOut.run();
                        }
                    }
                }
            };
            table1.addMouseListener(tableMouseListener);
            table2.addMouseListener(tableMouseListener);

            JButton addRowButton = new JButton("Add Row");
            addRowButton.setFont(new Font("Arial", Font.BOLD, 10));
            addRowButton.setBackground(new Color(50, 168, 82));
            addRowButton.setForeground(Color.BLACK);
            addRowButton.setFocusPainted(false);
            addRowButton.setBorderPainted(false);
            addRowButton.setContentAreaFilled(true);
            addRowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addRowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            addRowButton.setPreferredSize(buttonSize);
            addRowButton.setMaximumSize(buttonSize);
            addRowButton.setMinimumSize(buttonSize);
            addRowButton.setToolTipText("Add a new test step at the end of the test case table");
            addRowButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    addRowButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    addRowButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton addAboveButton = new JButton("Add Above");
            addAboveButton.setFont(new Font("Arial", Font.BOLD, 10));
            addAboveButton.setBackground(new Color(50, 168, 82));
            addAboveButton.setForeground(Color.BLACK);
            addAboveButton.setFocusPainted(false);
            addAboveButton.setBorderPainted(false);
            addAboveButton.setContentAreaFilled(true);
            addAboveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addAboveButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            addAboveButton.setPreferredSize(buttonSize);
            addAboveButton.setMaximumSize(buttonSize);
            addAboveButton.setMinimumSize(buttonSize);
            addAboveButton.setToolTipText("Insert a new test step above the selected row");
            addAboveButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    addAboveButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    addAboveButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton addBelowButton = new JButton("Add Below");
            addBelowButton.setFont(new Font("Arial", Font.BOLD, 10));
            addBelowButton.setBackground(new Color(50, 168, 82));
            addBelowButton.setForeground(Color.BLACK);
            addBelowButton.setFocusPainted(false);
            addBelowButton.setBorderPainted(false);
            addBelowButton.setContentAreaFilled(true);
            addBelowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addBelowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            addBelowButton.setPreferredSize(buttonSize);
            addBelowButton.setMaximumSize(buttonSize);
            addBelowButton.setMinimumSize(buttonSize);
            addBelowButton.setToolTipText("Insert a new test step below the selected row");
            addBelowButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    addBelowButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    addBelowButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton deleteRowButton = new JButton("Delete Row");
            deleteRowButton.setFont(new Font("Arial", Font.BOLD, 10));
            deleteRowButton.setBackground(new Color(50, 168, 82));
            deleteRowButton.setForeground(Color.BLACK);
            deleteRowButton.setFocusPainted(false);
            deleteRowButton.setBorderPainted(false);
            deleteRowButton.setContentAreaFilled(true);
            deleteRowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteRowButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            deleteRowButton.setPreferredSize(buttonSize);
            deleteRowButton.setMaximumSize(buttonSize);
            deleteRowButton.setMinimumSize(buttonSize);
            deleteRowButton.setToolTipText("Delete the selected test step");
            deleteRowButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    deleteRowButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    deleteRowButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton moveUpButton1 = new JButton("Move Up");
            moveUpButton1.setFont(new Font("Arial", Font.BOLD, 10));
            moveUpButton1.setBackground(new Color(50, 168, 82));
            moveUpButton1.setForeground(Color.BLACK);
            moveUpButton1.setFocusPainted(false);
            moveUpButton1.setBorderPainted(false);
            moveUpButton1.setContentAreaFilled(true);
            moveUpButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
            moveUpButton1.setAlignmentX(Component.LEFT_ALIGNMENT);
            moveUpButton1.setPreferredSize(buttonSize);
            moveUpButton1.setMaximumSize(buttonSize);
            moveUpButton1.setMinimumSize(buttonSize);
            moveUpButton1.setToolTipText("Move the selected test step up");
            moveUpButton1.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    moveUpButton1.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    moveUpButton1.setBackground(new Color(50, 168, 82));
                }
            });

            JButton moveDownButton1 = new JButton("Move Down");
            moveDownButton1.setFont(new Font("Arial", Font.BOLD, 10));
            moveDownButton1.setBackground(new Color(50, 168, 82));
            moveDownButton1.setForeground(Color.BLACK);
            moveDownButton1.setFocusPainted(false);
            moveDownButton1.setBorderPainted(false);
            moveDownButton1.setContentAreaFilled(true);
            moveDownButton1.setCursor(new Cursor(Cursor.HAND_CURSOR));
            moveDownButton1.setAlignmentX(Component.LEFT_ALIGNMENT);
            moveDownButton1.setPreferredSize(buttonSize); // Fixed: Changed setPreferredWidth to setPreferredSize
            moveDownButton1.setMaximumSize(buttonSize);
            moveDownButton1.setMinimumSize(buttonSize);
            moveDownButton1.setToolTipText("Move the selected test step down");
            moveDownButton1.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) {
                    moveDownButton1.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(MouseEvent evt) {
                    moveDownButton1.setBackground(new Color(50, 168, 82));
                }
            });

            JButton saveJsonButton = new JButton("Save to JSON");
            saveJsonButton.setFont(new Font("Arial", Font.BOLD, 10));
            saveJsonButton.setBackground(new Color(50, 168, 82));
            saveJsonButton.setForeground(Color.BLACK);
            saveJsonButton.setFocusPainted(false);
            saveJsonButton.setBorderPainted(false);
            saveJsonButton.setContentAreaFilled(true);
            saveJsonButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            saveJsonButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            saveJsonButton.setPreferredSize(buttonSize);
            saveJsonButton.setMaximumSize(buttonSize);
            saveJsonButton.setMinimumSize(buttonSize);
            saveJsonButton.setToolTipText("Save the tables to a JSON file");
            saveJsonButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    saveJsonButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    saveJsonButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton loadJsonButton = new JButton("Load from JSON");
            loadJsonButton.setFont(new Font("Arial", Font.BOLD, 10));
            loadJsonButton.setBackground(new Color(50, 168, 82));
            loadJsonButton.setForeground(Color.BLACK);
            loadJsonButton.setFocusPainted(false);
            loadJsonButton.setBorderPainted(false);
            loadJsonButton.setContentAreaFilled(true);
            loadJsonButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            loadJsonButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            loadJsonButton.setPreferredSize(buttonSize);
            loadJsonButton.setMaximumSize(buttonSize);
            loadJsonButton.setMinimumSize(buttonSize);
            loadJsonButton.setToolTipText("Load table data from a JSON file");
            loadJsonButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    loadJsonButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    loadJsonButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton clearTableButton = new JButton("Clear Test");
            clearTableButton.setFont(new Font("Arial", Font.BOLD, 10));
            clearTableButton.setBackground(new Color(50, 168, 82));
            clearTableButton.setForeground(Color.BLACK);
            clearTableButton.setFocusPainted(false);
            clearTableButton.setBorderPainted(false);
            clearTableButton.setContentAreaFilled(true);
            clearTableButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            clearTableButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            clearTableButton.setPreferredSize(buttonSize);
            clearTableButton.setMaximumSize(buttonSize);
            clearTableButton.setMinimumSize(buttonSize);
            clearTableButton.setToolTipText("Clear all data from both tables");
            clearTableButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    clearTableButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    clearTableButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton addElementButton = new JButton("Add Element");
            addElementButton.setFont(new Font("Arial", Font.BOLD, 10));
            addElementButton.setBackground(new Color(50, 168, 82));
            addElementButton.setForeground(Color.BLACK);
            addElementButton.setFocusPainted(false);
            addElementButton.setBorderPainted(false);
            addElementButton.setContentAreaFilled(true);
            addElementButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addElementButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            addElementButton.setPreferredSize(buttonSize);
            addElementButton.setMaximumSize(buttonSize);
            addElementButton.setMinimumSize(buttonSize);
            addElementButton.setToolTipText("Add a new element to the element table");
            addElementButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    addElementButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    addElementButton.setBackground(new Color(50, 168, 82));
                }
            });

            JButton moveUpButton2 = new JButton("Move Up");
            moveUpButton2.setFont(new Font("Arial", Font.BOLD, 10));
            moveUpButton2.setBackground(new Color(50, 168, 82));
            moveUpButton2.setForeground(Color.BLACK);
            moveUpButton2.setFocusPainted(false);
            moveUpButton2.setBorderPainted(false);
            moveUpButton2.setContentAreaFilled(true);
            moveUpButton2.setCursor(new Cursor(Cursor.HAND_CURSOR));
            moveUpButton2.setAlignmentX(Component.LEFT_ALIGNMENT);
            moveUpButton2.setPreferredSize(buttonSize);
            moveUpButton2.setMaximumSize(buttonSize);
            moveUpButton2.setMinimumSize(buttonSize);
            moveUpButton2.setToolTipText("Move the selected element up");
            moveUpButton2.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    moveUpButton2.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    moveUpButton2.setBackground(new Color(50, 168, 82));
                }
            });

            JButton moveDownButton2 = new JButton("Move Down");
            moveDownButton2.setFont(new Font("Arial", Font.BOLD, 10));
            moveDownButton2.setBackground(new Color(50, 168, 82));
            moveDownButton2.setForeground(Color.BLACK);
            moveDownButton2.setFocusPainted(false);
            moveDownButton2.setBorderPainted(false);
            moveDownButton2.setContentAreaFilled(true);
            moveDownButton2.setCursor(new Cursor(Cursor.HAND_CURSOR));
            moveDownButton2.setAlignmentX(Component.LEFT_ALIGNMENT);
            moveDownButton2.setPreferredSize(buttonSize);
            moveDownButton2.setMaximumSize(buttonSize);
            moveDownButton2.setMinimumSize(buttonSize);
            moveDownButton2.setToolTipText("Move the selected element down");
            moveDownButton2.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    moveDownButton2.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    moveDownButton2.setBackground(new Color(50, 168, 82));
                }
            });

            JButton deleteElementButton = new JButton("Delete Element");
            deleteElementButton.setFont(new Font("Arial", Font.BOLD, 10));
            deleteElementButton.setBackground(new Color(50, 168, 82));
            deleteElementButton.setForeground(Color.BLACK);
            deleteElementButton.setFocusPainted(false);
            deleteElementButton.setBorderPainted(false);
            deleteElementButton.setContentAreaFilled(true);
            deleteElementButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            deleteElementButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            deleteElementButton.setPreferredSize(buttonSize);
            deleteElementButton.setMaximumSize(buttonSize);
            deleteElementButton.setMinimumSize(buttonSize);
            deleteElementButton.setToolTipText("Delete the selected element");
            deleteElementButton.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    deleteElementButton.setBackground(new Color(40, 138, 72));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    deleteElementButton.setBackground(new Color(50, 168, 82));
                }
            });

            buttonPanel.add(addRowButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(addAboveButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(addBelowButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(deleteRowButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(moveUpButton1);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(moveDownButton1);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(saveJsonButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(loadJsonButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(clearTableButton);
            buttonPanel.add(Box.createVerticalStrut(200));
            buttonPanel.add(addElementButton);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(moveUpButton2);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(moveDownButton2);
            buttonPanel.add(Box.createVerticalStrut(10));
            buttonPanel.add(deleteElementButton);

            frame.add(buttonPanel, BorderLayout.EAST);

            Runnable updateTestStepNumbers1 = () -> {
                int step = 1;
                for (int i = 0; i < table1.getRowCount(); i++) {
                    String testId = String.valueOf(tableModel1.getValueAt(i, 0));
                    if (!testId.isEmpty() && !testId.equals("#")) {
                        step = 1;
                    }
                    tableModel1.setValueAt(String.valueOf(step), i, 1);
                    step++;
                }
            };

            tableModel1.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    if (e.getColumn() == 0) {
                        updateTestStepNumbers1.run();
                    }
                    isTableModified = true;
                }
            });

            addRowButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int stepNumber = table1.getRowCount() + 1;
                    tableModel1.addRow(new Object[]{"", String.valueOf(stepNumber), "", "", "", ""});
                    int newRowIndex = table1.getRowCount() - 1;
                    table1.setRowSelectionInterval(newRowIndex, newRowIndex);
                    table1.scrollRectToVisible(table1.getCellRect(newRowIndex, 0, true));
                    updateTestStepNumbers1.run();
                    isTableModified = true;
                }
            });

            addAboveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    if (table1.getRowCount() == 0) {
                        return;
                    }
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow == 0) {
                        return;
                    }
                    int insertIndex = (selectedRow == -1) ? table1.getRowCount() : selectedRow;
                    int stepNumber = insertIndex + 1;
                    tableModel1.insertRow(insertIndex, new Object[]{"", String.valueOf(stepNumber), "", "", "", ""});
                    table1.setRowSelectionInterval(insertIndex, insertIndex);
                    table1.scrollRectToVisible(table1.getCellRect(insertIndex, 0, true));
                    updateTestStepNumbers1.run();
                    isTableModified = true;
                }
            });

            addBelowButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    if (table1.getRowCount() == 0) {
                        return;
                    }
                    int selectedRow = table1.getSelectedRow();
                    int insertIndex = (selectedRow == -1) ? table1.getRowCount() : selectedRow + 1;
                    int stepNumber = insertIndex + 1;
                    tableModel1.insertRow(insertIndex, new Object[]{"", String.valueOf(stepNumber), "", "", "", ""});
                    table1.setRowSelectionInterval(insertIndex, insertIndex);
                    table1.scrollRectToVisible(table1.getCellRect(insertIndex, 0, true));
                    updateTestStepNumbers1.run();
                    isTableModified = true;
                }
            });

            deleteRowButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to delete", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String testId = String.valueOf(tableModel1.getValueAt(selectedRow, 0));
                    if (!testId.isEmpty() && !testId.equals("#")) {
                        JOptionPane.showMessageDialog(table1, "Cannot delete row with a Test Id", "Deletion Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    tableModel1.removeRow(selectedRow);
                    if (table1.getRowCount() > 0) {
                        int newSelection = Math.min(selectedRow, table1.getRowCount() - 1);
                        table1.setRowSelectionInterval(newSelection, newSelection);
                        table1.scrollRectToVisible(table1.getCellRect(newSelection, 0, true));
                    } else {
                        table1.clearSelection();
                    }
                    updateTestStepNumbers1.run();
                    isTableModified = true;
                }
            });

            moveUpButton1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to move", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selectedRow == 0) {
                        JOptionPane.showMessageDialog(table1, "Cannot move the first row of the table", "Invalid Action", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    int testCaseStart = selectedRow;
                    for (int i = selectedRow; i >= 0; i--) {
                        String testId = String.valueOf(tableModel1.getValueAt(i, 0));
                        if (!testId.isEmpty() && !testId.equals("#")) {
                            testCaseStart = i;
                            break;
                        }
                        if (i == 0) {
                            testCaseStart = 0;
                            break;
                        }
                    }
                    if (selectedRow > testCaseStart) {
                        tableModel1.moveRow(selectedRow, selectedRow, selectedRow - 1);
                        table1.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                        table1.scrollRectToVisible(table1.getCellRect(selectedRow - 1, 0, true));
                        updateTestStepNumbers1.run();
                        isTableModified = true;
                    }
                }
            });

            moveDownButton1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table1.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to move", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selectedRow == table1.getRowCount() - 1) {
                        JOptionPane.showMessageDialog(table1, "Cannot move the last row of the table", "Invalid Action", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    tableModel1.moveRow(selectedRow, selectedRow, selectedRow + 1);
                    table1.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    table1.scrollRectToVisible(table1.getCellRect(selectedRow + 1, 0, true));
                    updateTestStepNumbers1.run();
                    isTableModified = true;
                }
            });

            addElementButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    tableModel2.addRow(new Object[]{"", "", "", ""});
                    int newRowIndex = table2.getRowCount() - 1;
                    table2.setRowSelectionInterval(newRowIndex, newRowIndex);
                    table2.scrollRectToVisible(table2.getCellRect(newRowIndex, 0, true));
                    isTableModified = true;
                }
            });

            moveUpButton2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table2.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to move", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selectedRow == 0) {
                        JOptionPane.showMessageDialog(table2, "Cannot move the first row of the table", "Invalid Action", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String temp = previousElementNames.get(selectedRow);
                    previousElementNames.set(selectedRow, previousElementNames.get(selectedRow - 1));
                    previousElementNames.set(selectedRow - 1, temp);
                    tableModel2.moveRow(selectedRow, selectedRow, selectedRow - 1);
                    table2.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    table2.scrollRectToVisible(table2.getCellRect(selectedRow - 1, 0, true));
                    isTableModified = true;
                }
            });

            moveDownButton2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table2.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to move", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selectedRow == table2.getRowCount() - 1) {
                        JOptionPane.showMessageDialog(table2, "Cannot move the last row of the table", "Invalid Action", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    String temp = previousElementNames.get(selectedRow);
                    previousElementNames.set(selectedRow, previousElementNames.get(selectedRow + 1));
                    previousElementNames.set(selectedRow + 1, temp);
                    tableModel2.moveRow(selectedRow, selectedRow, selectedRow + 1);
                    table2.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
                    table2.scrollRectToVisible(table2.getCellRect(selectedRow + 1, 0, true));
                    isTableModified = true;
                }
            });

            deleteElementButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    int selectedRow = table2.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "Please select a row to delete", "No Row Selected", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (selectedRow < previousElementNames.size()) {
                        previousElementNames.remove(selectedRow);
                    }
                    tableModel2.removeRow(selectedRow);
                    if (table2.getRowCount() > 0) {
                        int newSelection = Math.min(selectedRow, table2.getRowCount() - 1);
                        table2.setRowSelectionInterval(newSelection, newSelection);
                        table2.scrollRectToVisible(table2.getCellRect(newSelection, 0, true));
                    } else {
                        table2.clearSelection();
                    }
                    isTableModified = true;
                }
            });

            saveJsonButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    if (tableModel1.getRowCount() == 0 && tableModel2.getRowCount() == 0) {
                        JOptionPane.showMessageDialog(frame, "No data to save.", "Save Info", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        if (lastUsedFile == null) {
                            showSaveDialog(frame, tableModel1, tableModel2, null);
                        } else {
                            showSaveOptionsDialog(frame, lastUsedFile, tableModel1, tableModel2);
                        }
                    }
                }
            });

            loadJsonButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Load Table from JSON");
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
                    int userSelection = fileChooser.showOpenDialog(frame);
                    if (userSelection != JFileChooser.APPROVE_OPTION) {
                        return;
                    }
                    File file = fileChooser.getSelectedFile();
                    try (FileReader fileReader = new FileReader(file)) {
                        StringBuilder content = new StringBuilder();
                        int ch;
                        while ((ch = fileReader.read()) != -1) {
                            content.append((char) ch);
                        }
                        JSONArray rootArray = new JSONArray(content.toString());
                        tableModel1.setRowCount(0);
                        tableModel2.setRowCount(0);
                        previousElementNames.clear();
                        Set<String> validElementNames = new TreeSet<>();
                        for (int i = 0; i < rootArray.length(); i++) {
                            JSONObject obj = rootArray.getJSONObject(i);
                            if (obj.has("Element_List")) {
                                JSONObject elementList = obj.getJSONObject("Element_List");
                                for (String key : elementList.keySet()) {
                                    validElementNames.add(key);
                                }
                            }
                        }
                        for (int i = 0; i < rootArray.length(); i++) {
                            JSONObject obj = rootArray.getJSONObject(i);
                            if (obj.has("Test_Id")) {
                                String testId = obj.getString("Test_Id");
                                Set<String> keys = new TreeSet<>((k1, k2) -> {
                                    if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                                        return k1.compareTo(k2);
                                    }
                                    int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                                    int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                                    return Integer.compare(n1, n2);
                                });
                                keys.addAll(obj.keySet());
                                keys.remove("Test_Id");
                                for (String key : keys) {
                                    if (key.startsWith("Test_Step ")) {
                                        JSONArray stepArray = obj.getJSONArray(key);
                                        String stepNumber = key.replace("Test_Step ", "");
                                        String testAction = stepArray.length() > 0 ? stepArray.getString(0) : "";
                                        String testElement = stepArray.length() > 1 ? stepArray.getString(1) : "";
                                        if (!testElement.isEmpty() && !validElementNames.contains(testElement)) {
                                            testElement = "";
                                        }
                                        String testData = stepArray.length() > 2 ? stepArray.getString(2) : "";
                                        String description = stepArray.length() > 3 ? stepArray.getString(3) : "";
                                        tableModel1.addRow(new Object[]{testId, stepNumber, testAction, testElement, testData, description});
                                        testId = "";
                                    }
                                }
                            } else if (obj.has("Element_List")) {
                                JSONObject elementList = obj.getJSONObject("Element_List");
                                for (String key : elementList.keySet()) {
                                    JSONArray elementArray = elementList.getJSONArray(key);
                                    String elementName = key;
                                    String automationId = elementArray.length() > 0 ? elementArray.getString(0) : "";
                                    String name = elementArray.length() > 1 ? elementArray.getString(1) : "";
                                    String xpath = elementArray.length() > 2 ? elementArray.getString(2) : "";
                                    tableModel2.addRow(new Object[]{elementName, automationId, name, xpath});
                                    previousElementNames.add(elementName);
                                }
                            }
                        }
                        updateTestStepNumbers1.run();
                        lastUsedFile = file;
                        isTableModified = false;
                        JOptionPane.showMessageDialog(frame, "Table loaded successfully from " + file.getAbsolutePath(), "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Error reading file: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                    } catch (JSONException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid JSON format: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            clearTableButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    stopEditingAndTabOut.run();
                    if (tableModel1.getRowCount() > 0 || tableModel2.getRowCount() > 0) {
                        if (isTableModified) {
                            int confirm = JOptionPane.showConfirmDialog(
                                frame,
                                "Table has unsaved changes. Would you like to save before clearing?",
                                "Unsaved Changes",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );
                            if (confirm == JOptionPane.YES_OPTION) {
                                if (lastUsedFile == null) {
                                    if (showSaveDialog(frame, tableModel1, tableModel2, null)) {
                                        tableModel1.setRowCount(0);
                                        tableModel2.setRowCount(0);
                                        previousElementNames.clear();
                                        lastUsedFile = null;
                                    }
                                } else {
                                    if (showSaveOptionsDialog(frame, lastUsedFile, tableModel1, tableModel2)) {
                                        tableModel1.setRowCount(0);
                                        tableModel2.setRowCount(0);
                                        previousElementNames.clear();
                                        lastUsedFile = null;
                                    }
                                }
                            } else if (confirm == JOptionPane.NO_OPTION) {
                                tableModel1.setRowCount(0);
                                tableModel2.setRowCount(0);
                                previousElementNames.clear();
                                lastUsedFile = null;
                                isTableModified = false;
                            }
                        } else {
                            tableModel1.setRowCount(0);
                            tableModel2.setRowCount(0);
                            previousElementNames.clear();
                            lastUsedFile = null;
                            isTableModified = false;
                        }
                    }
                }
            });

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    stopEditingAndTabOut.run();
                    if (isTableModified) {
                        int confirm = JOptionPane.showConfirmDialog(
                            frame,
                            "You have unsaved changes. Would you like to save before exiting?",
                            "Unsaved Changes",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE
                        );
                        if (confirm == JOptionPane.YES_OPTION) {
                            if (lastUsedFile != null) {
                                if (saveFile(lastUsedFile, frame, tableModel1, tableModel2)) {
                                    frame.dispose(); // Close window after successful save
                                }
                            } else {
                                if (showSaveDialog(frame, tableModel1, tableModel2, null)) {
                                    frame.dispose(); // Close window after successful save
                                }
                            }
                        } else if (confirm == JOptionPane.NO_OPTION) {
                            frame.dispose();
                        }
                        // Cancel keeps the window open
                    } else {
                        frame.dispose();
                    }
                }
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static boolean showSaveOptionsDialog(JFrame frame, File file, DefaultTableModel tableModel1, DefaultTableModel tableModel2) {
        Object[] options = {"Save", "Save As", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            frame,
            "Choose an option to save the table:",
            "Save Options",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        if (choice == 0) {
            return saveFile(file, frame, tableModel1, tableModel2);
        } else if (choice == 1) {
            return showSaveDialog(frame, tableModel1, tableModel2, file);
        }
        return false; // Cancel
    }

    private static boolean showSaveDialog(JFrame frame, DefaultTableModel tableModel1, DefaultTableModel tableModel2, File lastFile) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Table as JSON");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON Files", "json"));
        if (lastFile != null) {
            fileChooser.setSelectedFile(lastFile);
        }
        int userSelection = fileChooser.showSaveDialog(frame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            String filePath = newFile.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".json")) {
                newFile = new File(filePath + ".json");
            }
            if (newFile.exists()) {
                int confirm = JOptionPane.showConfirmDialog(
                    frame,
                    "File already exists. Overwrite?",
                    "Save Options",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    if (saveFile(newFile, frame, tableModel1, tableModel2)) {
                        lastUsedFile = newFile;
                        return true;
                    }
                } else if (confirm == JOptionPane.NO_OPTION) {
                    return showSaveDialog(frame, tableModel1, tableModel2, newFile);
                }
            } else {
                if (saveFile(newFile, frame, tableModel1, tableModel2)) {
                    lastUsedFile = newFile;
                    return true;
                }
            }
        }
        return false; // Cancel or failure
    }
    
    private static boolean saveFile(File file, JFrame frame, DefaultTableModel tableModel1, DefaultTableModel tableModel2) {
        try {
            JSONArray rootArray = new JSONArray();
            String currentTestId = "";
            LinkedHashMap<String, Object> testCaseMap = null;
            int stepCount = 1;

            for (int i = 0; i < tableModel1.getRowCount(); i++) {
                String testId = String.valueOf(tableModel1.getValueAt(i, 0));
                String testAction = String.valueOf(tableModel1.getValueAt(i, 2));
                String testElement = String.valueOf(tableModel1.getValueAt(i, 3));
                String testData = String.valueOf(tableModel1.getValueAt(i, 4));
                String description = String.valueOf(tableModel1.getValueAt(i, 5));

                if (!testId.isEmpty() && !testId.equals("#")) {
                    if (testCaseMap != null) {
                        LinkedHashMap<String, Object> sortedTestCaseMap = new LinkedHashMap<>();
                        sortedTestCaseMap.put("Test_Id", testCaseMap.get("Test_Id"));
                        TreeSet<String> sortedKeys = new TreeSet<>((k1, k2) -> {
                            if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                                return k1.compareTo(k2);
                            }
                            int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                            int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                            return Integer.compare(n1, n2);
                        });
                        sortedKeys.addAll(testCaseMap.keySet());
                        sortedKeys.remove("Test_Id");
                        for (String key : sortedKeys) {
                            sortedTestCaseMap.put(key, testCaseMap.get(key));
                        }
                        rootArray.put(new JSONObject(sortedTestCaseMap));
                    }
                    testCaseMap = new LinkedHashMap<>();
                    testCaseMap.put("Test_Id", testId);
                    currentTestId = testId;
                    stepCount = 1;
                }
                JSONArray stepArray = new JSONArray();
                stepArray.put(testAction);
                stepArray.put(testElement);
                stepArray.put(testData);
                stepArray.put(description);
                if (testCaseMap != null) {
                    testCaseMap.put("Test_Step " + stepCount, stepArray);
                    stepCount++;
                }
            }
            if (testCaseMap != null) {
                LinkedHashMap<String, Object> sortedTestCaseMap = new LinkedHashMap<>();
                sortedTestCaseMap.put("Test_Id", testCaseMap.get("Test_Id"));
                TreeSet<String> sortedKeys = new TreeSet<>((k1, k2) -> {
                    if (!k1.startsWith("Test_Step ") || !k2.startsWith("Test_Step ")) {
                        return k1.compareTo(k2);
                    }
                    int n1 = Integer.parseInt(k1.replace("Test_Step ", ""));
                    int n2 = Integer.parseInt(k2.replace("Test_Step ", ""));
                    return Integer.compare(n1, n2);
                });
                sortedKeys.addAll(testCaseMap.keySet());
                sortedKeys.remove("Test_Id");
                for (String key : sortedKeys) {
                    sortedTestCaseMap.put(key, testCaseMap.get(key));
                }
                rootArray.put(new JSONObject(sortedTestCaseMap));
            }

            if (tableModel2.getRowCount() > 0) {
                LinkedHashMap<String, Object> elementMap = new LinkedHashMap<>();
                JSONObject elementObject = new JSONObject();
                for (int i = 0; i < tableModel2.getRowCount(); i++) {
                    String elementName = String.valueOf(tableModel2.getValueAt(i, 0));
                    String automationId = String.valueOf(tableModel2.getValueAt(i, 1));
                    String name = String.valueOf(tableModel2.getValueAt(i, 2));
                    String xpath = String.valueOf(tableModel2.getValueAt(i, 3));
                    if (!elementName.isEmpty()) {
                        JSONArray elementArray = new JSONArray();
                        elementArray.put(automationId);
                        elementArray.put(name);
                        elementArray.put(xpath);
                        elementObject.put(elementName, elementArray);
                    }
                }
                elementMap.put("Element_List", elementObject);
                rootArray.put(new JSONObject(elementMap));
            }

            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(rootArray.toString(2));
                fileWriter.flush(); // Ensure data is written
                isTableModified = false; // Reset flag only on successful save
                JOptionPane.showMessageDialog(frame, "Table saved successfully to " + file.getAbsolutePath(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (JSONException ex) {
            JOptionPane.showMessageDialog(frame, "Error creating JSON: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}