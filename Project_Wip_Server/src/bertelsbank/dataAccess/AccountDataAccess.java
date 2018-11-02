package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.derby.client.am.DateTime;
import org.apache.derby.client.am.Decimal;
import org.apache.derby.iapi.services.io.NewByteArrayInputStream;

import bertelsbank.rest.Account;
import bertelsbank.rest.Transaction;

public class AccountDataAccess {
	public List<String> reservatedNumbers = new ArrayList<String>();
	boolean bankAccountExists = true;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	// Konstruktor - ruft Methoden auf, die die Datenbank inkl. Tabellen
	// initialisiert
	public AccountDataAccess() {
		try {
			// deleteTable("account");
			 deleteTable("transactiontable");
			// clearTable("account");

			createAccountTable();
			showContentsAccountTable();

			createTransactionTable();
			showContentsTransactionTable();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// =============================
	// ALLGEMEINE DATENBANK-METHODEN
	// =============================

	// Connection erstellen
	private Connection getConnection() {
		try {
			Class.forName("org.apache.derby.jdbc.ClientDriver");

			Properties properties = new Properties();
			properties.put("user", "user");

			Connection connection = DriverManager.getConnection("jdbc:derby:database;create=true", properties);
			return connection;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Ausgabe des Tabelleninhalts von student
	private int getEntryCount(String tableName) throws SQLException {
		int entryCount = 0;
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM " + tableName;
		ResultSet resultSet = statement.executeQuery(sql);
		if (resultSet.next()) {
			entryCount = resultSet.getInt(1);
		}
		resultSet.close();
		statement.close();
		connection.close();
		return entryCount;
	}

	// Löscht eine Tabelle der Datenbank
	private void deleteTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		System.out.println("Deleting table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("drop table " + tableName);
		statement.close();
		connection.close();
	}

	// Entfernt alle Einträge einer Datenbanktabelle
	private void clearTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		System.out.println("Clearing table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("delete from " + tableName);
		statement.close();
		connection.close();
	}

	// =============================
	// DB-TABELLE ACCOUNT
	// =============================

	// Kontentabelle erstellen
	public void createAccountTable() throws SQLException {
		Connection connection = getConnection();
		// Optionale PrÃ¼fung, ob Tabelle bereits besteht
		ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
		boolean shouldCreateTable = true;
		while (resultSet.next() && shouldCreateTable) {
			if (resultSet.getString("TABLE_NAME").equalsIgnoreCase("ACCOUNT")) {
				shouldCreateTable = false;
			}
		}
		resultSet.close();

		if (shouldCreateTable) {
			System.out.println("Creating table account...");
			Statement statement = connection.createStatement();
			statement.execute(
					"create table account (id int not null, owner varchar(64) not null, number varchar(4) not null)");
			statement.close();
			bankAccountExists = false;
			addAccount("BANK");
			bankAccountExists = true;
		}

		connection.close();
	}

	// Konto der Tabelle hinzufÃ¼gen
	public String addAccount(String owner) throws SQLException {
		String status = "";
		String accountNumber = getFreeNumber();
		if (!accountNumber.equals("")) {
			System.out.println("Adding account...");
			try (Connection connection = getConnection();
					PreparedStatement preparedStatement = connection
							.prepareStatement("INSERT INTO account VALUES (?,?,?)")) {
				preparedStatement.setInt(1, getEntryCount("account") + 1);
				preparedStatement.setString(2, owner);
				preparedStatement.setString(3, accountNumber);
				preparedStatement.execute();
				status = "Folgendes Konto wurde erfolgreich erstellt:" + "\nKontonummer: " + accountNumber
						+ "\nInhaber: " + owner + "\nStartguthaben: ";
				reservatedNumbers.remove(accountNumber);
				showContentsAccountTable();
			} catch (SQLException e) {
				// Exception loggen, ggf. angemessen reagieren
				e.printStackTrace();
				status = "Es gab ein Problem beim Erstellen des Kontos. Bitte versuchen Sie es erneut.";
				System.out.println(status);
			}
		}
		return status;
	}

	// Rückgabe eines Kontos
	public Account getAccountByNumber(String number) {
		Account account = new Account();
		try {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM account where number = '" + number + "'";
			ResultSet resultSet = statement.executeQuery(sql);
			if (resultSet.next()) {
				int id = resultSet.getInt(1);
				String owner = resultSet.getString(2);
				account.setId(id);
				account.setOwner(owner);
				account.setNumber(number);
				// Transaktionsliste fehlt noch
			}
			resultSet.close();
			statement.close();
			connection.close();
			return account;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Rückgabe einer Liste aller Konten
	public List<Account> getAccounts() {
		List<Account> accounts = new ArrayList<>();

		try {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM account";
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				String owner = resultSet.getString(2);
				String number = resultSet.getString(3);
				Account account = new Account();
				account.setId(id);
				account.setOwner(owner);
				account.setNumber(number);
				accounts.add(account);
			}
			resultSet.close();
			statement.close();
			connection.close();
			return accounts;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Ermittelt die nächste freie Kontonummer
	public String getFreeNumber() throws SQLException {
		String freeNumber = "";
		if (bankAccountExists) {
			for (int i = 1000; i <= 9999; i++) {
				String iString = String.valueOf(i);
				if (!numberExists(iString) && !reservatedNumbers.contains(iString)) {
					freeNumber = iString;
					break;
				}
			}
		} else {
			freeNumber = "0000";
		}
		reservatedNumbers.add(freeNumber);

		return freeNumber;
	}

	// Prüft, ob Kontonummer existiert
	private boolean numberExists(String number) throws SQLException {
		int entryCount = 0;
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM account where number = '" + number + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		if (resultSet.next()) {
			entryCount = resultSet.getInt(1);
			// System.out.println("Table Entry Count: " + entryCount);
		}
		resultSet.close();
		statement.close();
		connection.close();
		if (entryCount == 0) {
			return false;
		} else {
			return true;
		}
	}

	// Ausgabe des Tabelleninhalts von account
	private void showContentsAccountTable() throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM account";
		ResultSet resultSet = statement.executeQuery(sql);
		System.out.println("Table account:");
		while (resultSet.next()) {
			int id = resultSet.getInt(1);
			String owner = resultSet.getString(2);
			String number = resultSet.getString(3);

			System.out.println(id + " | " + owner + " | " + number);
		}
		resultSet.close();
		statement.close();
		connection.close();
	}

	// =============================
	// DB-TABELLE TRANSACTION
	// =============================

	// Transaktionstabelle erstellen
	public void createTransactionTable() throws SQLException {
		Connection connection = getConnection();
		// Optionale Prüfung, ob Tabelle bereits besteht
		ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
		boolean shouldCreateTable = true;
		while (resultSet.next() && shouldCreateTable) {
			if (resultSet.getString("TABLE_NAME").equalsIgnoreCase("TRANSACTIONTABLE")) {
				shouldCreateTable = false;
			}
		}
		resultSet.close();

		if (shouldCreateTable) {
			System.out.println("Creating table transaction...");
			Statement statement = connection.createStatement();
			statement.execute("create table transactionTable (id int not null, senderNumber varchar(4) not null, "
					+ "receiverNumber varchar(4) not null, amount decimal(20,2) not null, reference varchar(64) not null, date varchar(64) not null)");
			statement.close();
		}

		connection.close();
	}

	// Konto der Tabelle hinzufügen
	public void addTransaction(String senderNumber, String receiverNumber, BigDecimal amount, String reference) throws SQLException {
		System.out.println("Adding transaction...");
		try (Connection connection = getConnection();
				PreparedStatement preparedStatement = connection
						.prepareStatement("INSERT INTO transactionTable VALUES (?,?,?,?,?,?)")) {
			preparedStatement.setInt(1, getEntryCount("transactionTable") + 1);
			preparedStatement.setString(2, senderNumber);
			preparedStatement.setString(3, receiverNumber);
			preparedStatement.setBigDecimal(4, amount);
			preparedStatement.setString(5, reference);
			preparedStatement.setString(6, simpleDateFormat.format(new java.util.Date()));
			preparedStatement.execute();
			showContentsTransactionTable();
		} catch (SQLException e) {
			// Exception loggen, ggf. angemessen reagieren
			e.printStackTrace();
		}
	}

	// Ausgabe des Tabelleninhalts von transactiontable
	private void showContentsTransactionTable() throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM transactionTable";
		ResultSet resultSet = statement.executeQuery(sql);
		System.out.println("Table transaction:");
		while (resultSet.next()) {
			int id = resultSet.getInt(1);
			String senderNumber = resultSet.getString(2);
			String receiverNumber = resultSet.getString(3);
			String amount = resultSet.getString(4);        // !!!!
			String reference = resultSet.getString(5);
			String date = resultSet.getString(6);

			System.out.println(id + " | " + senderNumber + " | " + receiverNumber + " | " + amount + " | " + reference + " | " + date);
		}
		resultSet.close();
		statement.close();
		connection.close();
	}

}
