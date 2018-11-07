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
import org.eclipse.jetty.jndi.java.javaNameParser;

import bertelsbank.rest.Account;
import bertelsbank.rest.Transaction;

public class TransactionDataAccess {
	public List<String> reservatedNumbers = new ArrayList<String>();
	boolean bankAccountExists = true;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	// Konstruktor - ruft Methoden auf, die die Datenbank inkl. Tabellen
	// initialisiert
	public TransactionDataAccess() {
		try {
			// deleteTable("transactiontable");

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
					+ "receiverNumber varchar(4) not null, amount decimal(20,2) not null, reference varchar(64) not null, date timestamp not null)");
			statement.close();
		}

		connection.close();
	}

	// Konto der Tabelle hinzufügen
	public void addTransaction(String senderNumber, String receiverNumber, BigDecimal amount, String reference)
			throws SQLException {
		System.out.println("Adding transaction...");
		try (Connection connection = getConnection();
				PreparedStatement preparedStatement = connection
						.prepareStatement("INSERT INTO transactionTable VALUES (?,?,?,?,?,?)")) {
			preparedStatement.setInt(1, getEntryCount("transactionTable") + 1);
			preparedStatement.setString(2, senderNumber);
			preparedStatement.setString(3, receiverNumber);
			preparedStatement.setBigDecimal(4, amount);
			preparedStatement.setString(5, reference);
			preparedStatement.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
			preparedStatement.execute();
			showContentsTransactionTable();
		} catch (SQLException e) {
			// Exception loggen, ggf. angemessen reagieren
			e.printStackTrace();
		}
	}

	public List<Transaction> getTransactionHistory(String accountNumber) throws SQLException {
		List<Transaction> transactionHistoryList = new ArrayList<Transaction>();
		AccountDataAccess daAccount = new AccountDataAccess();

		// Wenn keine Kontonummer übergeben wird, soll die gesamte
		// Transaktionshistorie aller Konten zurückgegeben werden
		if (accountNumber == null || accountNumber.equals("")) {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable";
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				Transaction transaction = new Transaction();
				transaction.setId(resultSet.getInt(1));
				transaction.setSender(daAccount.getAccountByNumber(resultSet.getString(2), false));
				transaction.setReceiver(daAccount.getAccountByNumber(resultSet.getString(3), false));
				transaction.setAmount(resultSet.getBigDecimal(4));
				transaction.setReference(resultSet.getString(5));
				transaction.setTransactionDate(resultSet.getTimestamp(6));
				transactionHistoryList.add(transaction);
			}
			resultSet.close();
			statement.close();
			connection.close();
		} else {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable";
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				Transaction transaction = new Transaction();
				transaction.setId(resultSet.getInt(1));
				transaction.setSender(daAccount.getAccountByNumber(resultSet.getString(2), false));
				transaction.setReceiver(daAccount.getAccountByNumber(resultSet.getString(3), false));
				transaction.setAmount(resultSet.getBigDecimal(4));
				transaction.setReference(resultSet.getString(5));
				transaction.setTransactionDate(resultSet.getTimestamp(6));
				transactionHistoryList.add(transaction);
			}
			resultSet.close();
			statement.close();
			connection.close();
		}
		return transactionHistoryList;
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
			String amount = resultSet.getString(4);
			String reference = resultSet.getString(5);
			String date = resultSet.getString(6);

			System.out.println(id + " | " + senderNumber + " | " + receiverNumber + " | " + amount + " | " + reference
					+ " | " + date);
		}
		resultSet.close();
		statement.close();
		connection.close();
	}

}
