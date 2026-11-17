import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class SimpleBankingSystem extends JFrame {

    private static final String DB_URL = "jdbc:sqlite:banking.db";

    private static final Color ORANGE = new Color(244, 119, 35);
    private static final Color ORANGE_DARK = new Color(220, 94, 20);
    private static final Color NAVY = new Color(34, 42, 53);
    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color TEXT = new Color(47, 56, 69);
    private static final Color MUTED = new Color(112, 122, 137);
    private static final Color BORDER = new Color(225, 229, 235);
    private static final Color GREEN = new Color(37, 151, 104);
    private static final Color RED = new Color(207, 83, 65);

    private JTextField accountNumberField;
    private JTextField nameField;
    private JTextField balanceField;
    private JTextField amountField;

    private JLabel accountNumberLabel;
    private JLabel nameLabel;
    private JLabel currentBalanceLabel;
    private JLabel statusLabel;

    private JComboBox<String> accountSelector;
    private DefaultTableModel model;
    private JTable transactionTable;

    private String selectedAccountNumber;
    private boolean updatingAccountSelector;

    public SimpleBankingSystem() {
        setTitle("Orange Bank | Simple Banking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 700));
        setSize(1120, 860);
        setLocationRelativeTo(null);

        initializeDatabase();
        createGUI();
        refreshAccounts();
    }

    // ---------------- DATABASE ----------------

    private Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "SQLite JDBC driver not found. Check that the JDBC JAR is in the lib folder and included in the classpath.",
                e
            );
        }

        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void initializeDatabase() {
        String accountsSql =
            "CREATE TABLE IF NOT EXISTS accounts (" +
            "account_number TEXT PRIMARY KEY, " +
            "holder_name TEXT NOT NULL, " +
            "balance REAL NOT NULL CHECK(balance >= 0), " +
            "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";

        String transactionsSql =
            "CREATE TABLE IF NOT EXISTS transactions (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "account_number TEXT NOT NULL, " +
            "transaction_type TEXT NOT NULL, " +
            "amount REAL NOT NULL, " +
            "balance_after REAL NOT NULL, " +
            "transaction_date TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(account_number) REFERENCES accounts(account_number))";

        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute(accountsSql);
            statement.execute(transactionsSql);
        } catch (SQLException e) {
            showError("Database initialization failed:\n" + e.getMessage());
        }
    }

    // ---------------- USER INTERFACE ----------------

    private void createGUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(20, 22, 14, 22));

        root.add(createHeader(), BorderLayout.NORTH);

        JPanel dashboard = new JPanel(new BorderLayout(0, 15));
        dashboard.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.add(createAccountManagementCard());
        cards.add(createAccountOverviewCard());

        JPanel historyCard = createTransactionHistoryCard();

        JSplitPane splitPane = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, cards, historyCard
        );
        splitPane.setResizeWeight(0.58);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);

        dashboard.add(splitPane, BorderLayout.CENTER);
        root.add(dashboard, BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(19, 24, 19, 24));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ORANGE BANK");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(ORANGE);

        JLabel subtitle = new JLabel("SIMPLE BANKING MANAGEMENT SYSTEM");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(205, 212, 222));
        subtitle.setBorder(new EmptyBorder(5, 1, 0, 0));

        brand.add(title);
        brand.add(subtitle);

        statusLabel = new JLabel("●  DATABASE CONNECTED");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusLabel.setForeground(new Color(125, 224, 164));

        header.add(brand, BorderLayout.WEST);
        header.add(statusLabel, BorderLayout.EAST);
        return header;
    }

    private JPanel createAccountManagementCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 12));

        card.add(sectionHeading(
            "Account Management",
            "Create a new account or perform a transaction"
        ), BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);

        accountNumberField = createField();
        nameField = createField();
        balanceField = createField();
        amountField = createField();

        addField(fields, c, "Account Number", accountNumberField);
        addField(fields, c, "Account Holder Name", nameField);
        addField(fields, c, "Initial Balance (₹)", balanceField);
        addField(fields, c, "Transaction Amount (₹)", amountField);

        JPanel actions = new JPanel(new GridLayout(2, 2, 9, 9));
        actions.setOpaque(false);

        JButton createButton = createButton("Create Account", ORANGE);
        JButton depositButton = createButton("Deposit Money", GREEN);
        JButton withdrawButton = createButton("Withdraw Money", RED);
        JButton clearButton = createButton("Clear Fields", NAVY);

        actions.add(createButton);
        actions.add(depositButton);
        actions.add(withdrawButton);
        actions.add(clearButton);

        createButton.addActionListener(e -> createAccount());
        depositButton.addActionListener(e -> depositMoney());
        withdrawButton.addActionListener(e -> withdrawMoney());
        clearButton.addActionListener(e -> clearInputFields());

        card.add(fields, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createAccountOverviewCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 12));

        card.add(sectionHeading(
            "Account Overview",
            "Select an account to view its details"
        ), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel selectLabel = createLabel("SELECT EXISTING ACCOUNT");
        selectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        accountSelector = new JComboBox<>();
        accountSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        accountSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
        accountSelector.setPreferredSize(new Dimension(250, 39));
        accountSelector.setBackground(Color.WHITE);
        accountSelector.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loadButton = createButton("Load Selected Account", ORANGE);
        loadButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel balancePanel = new JPanel();
        balancePanel.setBackground(new Color(255, 244, 235));
        balancePanel.setBorder(new EmptyBorder(16, 17, 16, 17));
        balancePanel.setLayout(new BoxLayout(balancePanel, BoxLayout.Y_AXIS));
        balancePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        balancePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel balanceCaption = new JLabel("AVAILABLE BALANCE");
        balanceCaption.setFont(new Font("Segoe UI", Font.BOLD, 11));
        balanceCaption.setForeground(MUTED);

        currentBalanceLabel = new JLabel("₹0.00");
        currentBalanceLabel.setFont(new Font("Segoe UI", Font.BOLD, 30));
        currentBalanceLabel.setForeground(ORANGE_DARK);
        currentBalanceLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        balancePanel.add(balanceCaption);
        balancePanel.add(currentBalanceLabel);

        accountNumberLabel = new JLabel("Account Number: —");
        nameLabel = new JLabel("Account Holder: —");
        styleDetailLabel(accountNumberLabel);
        styleDetailLabel(nameLabel);

        JButton checkButton = createButton("Check Balance", NAVY);
        JButton refreshButton = createButton("Refresh Accounts", new Color(94, 105, 121));

        JPanel secondaryActions = new JPanel(new GridLayout(1, 2, 9, 0));
        secondaryActions.setOpaque(false);
        secondaryActions.add(checkButton);
        secondaryActions.add(refreshButton);

        body.add(selectLabel);
        body.add(Box.createVerticalStrut(7));
        body.add(accountSelector);
        body.add(Box.createVerticalStrut(9));
        body.add(loadButton);
        body.add(Box.createVerticalStrut(17));
        body.add(balancePanel);
        body.add(Box.createVerticalStrut(13));
        body.add(accountNumberLabel);
        body.add(Box.createVerticalStrut(7));
        body.add(nameLabel);
        body.add(Box.createVerticalStrut(15));
        body.add(secondaryActions);

        loadButton.addActionListener(e -> {
            String number = (String) accountSelector.getSelectedItem();
            if (number == null) {
                showError("There are no accounts to load. Create an account first.");
            } else {
                loadAccount(number);
            }
        });

        checkButton.addActionListener(e -> showBalance());
        refreshButton.addActionListener(e -> refreshAccounts());

        accountSelector.addActionListener(e -> {
            if (!updatingAccountSelector) {
                String number = (String) accountSelector.getSelectedItem();
                if (number != null) {
                    accountNumberField.setText(number);
                }
            }
        });

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTransactionHistoryCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 12));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JLabel title = new JLabel("TRANSACTION HISTORY");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(NAVY);

        JLabel hint = new JLabel("Transactions for the selected account");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(MUTED);

        heading.add(title, BorderLayout.WEST);
        heading.add(hint, BorderLayout.EAST);

        String[] columns = {
            "Transaction Type", "Amount", "Balance After", "Date"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(model);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        transactionTable.setRowHeight(30);
        transactionTable.setForeground(TEXT);
        transactionTable.setBackground(Color.WHITE);
        transactionTable.setGridColor(BORDER);
        transactionTable.setSelectionBackground(new Color(255, 232, 213));
        transactionTable.setSelectionForeground(TEXT);
        transactionTable.setFillsViewportHeight(true);
        transactionTable.setAutoCreateRowSorter(true);

        transactionTable.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 12)
        );
        transactionTable.getTableHeader().setBackground(new Color(255, 239, 225));
        transactionTable.getTableHeader().setForeground(NAVY);
        transactionTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        transactionTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(heading, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        JLabel label = new JLabel(
            "ORANGE BANK  |  SIMPLE BANKING MANAGEMENT SYSTEM"
        );
        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        label.setForeground(MUTED);

        footer.add(label, BorderLayout.WEST);
        return footer;
    }

    private JPanel createCard() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            new EmptyBorder(17, 18, 17, 18)
        ));
        return panel;
    }

    private JPanel sectionHeading(String titleText, String subtitleText) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 19));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private void addField(
        JPanel panel, GridBagConstraints c, String labelText, JTextField field
    ) {
        JLabel label = createLabel(labelText);
        c.gridy++;
        c.insets = new Insets(5, 0, 2, 0);
        panel.add(label, c);

        c.gridy++;
        c.insets = new Insets(2, 0, 5, 0);
        panel.add(field, c);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(75, 86, 102));
        return label;
    }

    private void styleDetailLabel(JLabel label) {
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT);
    }

    private JTextField createField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(220, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(211, 218, 227)),
            new EmptyBorder(7, 10, 7, 10)
        ));
        return field;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 39));
        return button;
    }

    // ---------------- ACCOUNT OPERATIONS ----------------

    private void createAccount() {
        String number = accountNumberField.getText().trim();
        String name = nameField.getText().trim();

        if (number.isEmpty() || name.isEmpty()) {
            showError("Please enter an account number and account holder name.");
            return;
        }

        if (number.length() > 40) {
            showError("Account number must be 40 characters or fewer.");
            return;
        }

        if (!isValidMoney(balanceField.getText())) {
            showError("Enter a valid initial balance (zero or greater).");
            return;
        }

        double openingBalance = Double.parseDouble(balanceField.getText().trim());

        String insertAccount =
            "INSERT INTO accounts(account_number, holder_name, balance) VALUES(?, ?, ?)";
        String insertTransaction =
            "INSERT INTO transactions(account_number, transaction_type, amount, balance_after) " +
            "VALUES(?, ?, ?, ?)";

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement accountStatement =
                     connection.prepareStatement(insertAccount)) {
                accountStatement.setString(1, number);
                accountStatement.setString(2, name);
                accountStatement.setDouble(3, openingBalance);
                accountStatement.executeUpdate();
            }

            if (openingBalance > 0) {
                try (PreparedStatement transactionStatement =
                         connection.prepareStatement(insertTransaction)) {
                    transactionStatement.setString(1, number);
                    transactionStatement.setString(2, "Account Opening");
                    transactionStatement.setDouble(3, openingBalance);
                    transactionStatement.setDouble(4, openingBalance);
                    transactionStatement.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            showError("Could not create account. The account number may already exist.\n\n"
                + e.getMessage());
            return;
        }

        refreshAccounts();
        selectAccountInCombo(number);
        loadAccount(number);
        nameField.setText("");
        balanceField.setText("");

        JOptionPane.showMessageDialog(
            this, "Account created successfully!", "Success",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void loadAccount(String number) {
        if (number == null || number.trim().isEmpty()) {
            showError("Please select or enter an account number.");
            return;
        }

        String sql =
            "SELECT account_number, holder_name, balance FROM accounts WHERE account_number = ?";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    selectedAccountNumber = null;
                    clearAccountDetails();
                    showError("Account not found.");
                    return;
                }

                selectedAccountNumber = result.getString("account_number");
                accountNumberField.setText(selectedAccountNumber);
                accountNumberLabel.setText("Account Number:  " + selectedAccountNumber);
                nameLabel.setText("Account Holder:  " + result.getString("holder_name"));
                currentBalanceLabel.setText(formatCurrency(result.getDouble("balance")));
            }

            loadTransactionHistory(number);
        } catch (SQLException e) {
            showError("Unable to load account:\n" + e.getMessage());
        }
    }

    private void depositMoney() {
        performTransaction(true);
    }

    private void withdrawMoney() {
        performTransaction(false);
    }

    private void performTransaction(boolean deposit) {
        if (!isAccountSelected()) return;

        if (!isValidMoney(amountField.getText()) ||
            Double.parseDouble(amountField.getText().trim()) <= 0) {
            showError("Enter a valid transaction amount greater than zero.");
            return;
        }

        double amount = Double.parseDouble(amountField.getText().trim());

        String updateSql = deposit
            ? "UPDATE accounts SET balance = balance + ? WHERE account_number = ?"
            : "UPDATE accounts SET balance = balance - ? WHERE account_number = ? AND balance >= ?";

        String transactionSql =
            "INSERT INTO transactions(account_number, transaction_type, amount, balance_after) " +
            "VALUES(?, ?, ?, ?)";

        double newBalance;

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);

            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setDouble(1, amount);
                update.setString(2, selectedAccountNumber);
                if (!deposit) update.setDouble(3, amount);

                int changed = update.executeUpdate();
                if (changed == 0) {
                    connection.rollback();
                    showError(deposit ? "Account not found." : "Insufficient balance.");
                    return;
                }
            }

            newBalance = getBalance(connection, selectedAccountNumber);

            try (PreparedStatement transaction = connection.prepareStatement(transactionSql)) {
                transaction.setString(1, selectedAccountNumber);
                transaction.setString(2, deposit ? "Deposit" : "Withdrawal");
                transaction.setDouble(3, amount);
                transaction.setDouble(4, newBalance);
                transaction.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            showError("Transaction failed:\n" + e.getMessage());
            return;
        }

        amountField.setText("");
        loadAccount(selectedAccountNumber);

        JOptionPane.showMessageDialog(
            this,
            deposit ? "Money deposited successfully!" : "Money withdrawn successfully!",
            "Transaction Successful",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private double getBalance(Connection connection, String number) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return result.getDouble("balance");
            }
        }
        throw new SQLException("Account not found.");
    }

    private void showBalance() {
        if (!isAccountSelected()) return;

        try (Connection connection = connect()) {
            double balance = getBalance(connection, selectedAccountNumber);
            currentBalanceLabel.setText(formatCurrency(balance));
            JOptionPane.showMessageDialog(
                this,
                "Account Number: " + selectedAccountNumber +
                "\nAvailable Balance: " + formatCurrency(balance),
                "Account Balance",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (SQLException e) {
            showError("Unable to retrieve balance:\n" + e.getMessage());
        }
    }

    // ---------------- TRANSACTION HISTORY ----------------

    private void loadTransactionHistory(String number) {
        model.setRowCount(0);

        String sql =
            "SELECT transaction_type, amount, balance_after, transaction_date " +
            "FROM transactions WHERE account_number = ? ORDER BY id DESC";

        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, number);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    model.addRow(new Object[] {
                        result.getString("transaction_type"),
                        formatCurrency(result.getDouble("amount")),
                        formatCurrency(result.getDouble("balance_after")),
                        result.getString("transaction_date")
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load transaction history:\n" + e.getMessage());
        }
    }

    // ---------------- ACCOUNT LIST AND HELPERS ----------------

    private void refreshAccounts() {
        String previouslySelected = selectedAccountNumber;
        updatingAccountSelector = true;
        accountSelector.removeAllItems();

        String sql = "SELECT account_number FROM accounts ORDER BY account_number";

        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                accountSelector.addItem(result.getString("account_number"));
            }
        } catch (SQLException e) {
            updatingAccountSelector = false;
            showError("Unable to refresh accounts:\n" + e.getMessage());
            return;
        }

        if (previouslySelected != null) {
            accountSelector.setSelectedItem(previouslySelected);
        }
        updatingAccountSelector = false;
    }

    private void selectAccountInCombo(String number) {
        updatingAccountSelector = true;
        accountSelector.setSelectedItem(number);
        updatingAccountSelector = false;
    }

    private boolean isAccountSelected() {
        if (selectedAccountNumber == null) {
            String typedNumber = accountNumberField.getText().trim();
            if (!typedNumber.isEmpty()) {
                loadAccount(typedNumber);
            }
        }

        if (selectedAccountNumber == null) {
            showError("Please create or load an account first.");
            return false;
        }
        return true;
    }

    private boolean isValidMoney(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            double amount = Double.parseDouble(value.trim());
            return Double.isFinite(amount) && amount >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatCurrency(double amount) {
        return String.format(java.util.Locale.US, "₹%,.2f", amount);
    }

    private void clearInputFields() {
        accountNumberField.setText("");
        nameField.setText("");
        balanceField.setText("");
        amountField.setText("");
    }

    private void clearAccountDetails() {
        accountNumberLabel.setText("Account Number:  —");
        nameLabel.setText("Account Holder:  —");
        currentBalanceLabel.setText("₹0.00");
        model.setRowCount(0);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this, message, "Orange Bank",
            JOptionPane.ERROR_MESSAGE
        );
    }

    // ---------------- MAIN ----------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use Swing's default look and feel if the system one is unavailable.
            }

            SimpleBankingSystem application = new SimpleBankingSystem();
            application.setVisible(true);
        });
    }
}
