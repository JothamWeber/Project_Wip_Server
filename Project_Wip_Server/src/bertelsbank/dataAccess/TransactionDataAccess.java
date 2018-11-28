package bertelsbank.dataAccess;

import java.io.IOException;
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
import org.apache.log4j.Logger;
import org.eclipse.jetty.jndi.java.javaNameParser;

import bertelsbank.rest.Account;
import bertelsbank.rest.Transaction;

public class TransactionDataAccess {
	DatabaseAdministration dbAdministration = new DatabaseAdministration();

	Logger logger;
	public List<String> reservatedNumbers = new ArrayList<String>();
	boolean bankAccountExists = true;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	public TransactionDataAccess() {
		logger = Logger.getLogger(getClass());
	}

	// Transaktionstabelle erstellen
	public void createTransactionTable() throws SQLException {
		Connection connection = dbAdministration.getConnection();
		// Prüfung, ob Tabelle bereits besteht
		ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
		boolean shouldCreateTable = true;
		while (resultSet.next() && shouldCreateTable) {
			if (resultSet.getString("TABLE_NAME").equalsIgnoreCase("TRANSACTIONTABLE")) {
				shouldCreateTable = false;
			}
		}
		resultSet.close();

		if (shouldCreateTable) {
			Statement statement = connection.createStatement();
			statement.execute(
					"create table transactionTable (id int not null primary key, senderNumber varchar(4) not null, "
							+ "receiverNumber varchar(4) not null, amount decimal(20,2) not null, reference varchar(64) not null, date timestamp not null)");
			statement.close();

			logger.info("Tabelle \"TransactionTable\" wurde erstellt.");
		}

		connection.close();
	}

	// Konto der Tabelle hinzufügen
	public void addTransaction(String senderNumber, String receiverNumber, BigDecimal amount, String reference)
			throws SQLException {
		try (Connection connection = dbAdministration.getConnection();
				PreparedStatement preparedStatement = connection
						.prepareStatement("INSERT INTO transactionTable VALUES (?,?,?,?,?,?)")) {
			preparedStatement.setInt(1, dbAdministration.getEntryCount("transactionTable") + 1);
			preparedStatement.setString(2, senderNumber);
			preparedStatement.setString(3, receiverNumber);
			preparedStatement.setBigDecimal(4, amount);
			preparedStatement.setString(5, reference);
			preparedStatement.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
			preparedStatement.execute();
			logger.info("Transaktion ausgeführt. Sendernr.: " + senderNumber + ", Empfängernr.: " + receiverNumber
					+ ", Betrag: " + amount + ", Referenz: " + reference);
		} catch (SQLException e) {
			logger.error(e);
			e.printStackTrace();
		}
	}

	public List<Transaction> getTransactionHistory(String accountNumber) throws SQLException {
		List<Transaction> transactionHistoryList = new ArrayList<Transaction>();
		AccountDataAccess daAccount = new AccountDataAccess();

		// Wenn "all" als Kontonummer übergeben wird, soll die gesamte
		// Transaktionshistorie aller Konten zurückgegeben werden
		if (accountNumber.equals("all")) {
			Connection connection = dbAdministration.getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable order by date desc";
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
			Connection connection = dbAdministration.getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable where sendernumber = '" + accountNumber
					+ "' or receivernumber = '" + accountNumber + "' order by date desc";
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

	public BigDecimal getAccountBalance(String accountNumber) throws SQLException {
		BigDecimal balance = BigDecimal.ZERO;

		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT amount FROM transactionTable where sendernumber = '" + accountNumber + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		while (resultSet.next()) {
			balance = balance.subtract(resultSet.getBigDecimal(1));
		}

		sql = "SELECT amount FROM transactionTable where receivernumber = '" + accountNumber + "'";
		resultSet = statement.executeQuery(sql);
		while (resultSet.next()) {
			balance = balance.add(resultSet.getBigDecimal(1));
		}

		resultSet.close();
		statement.close();
		connection.close();

		return balance;
	}

	// Ausgabe des Tabelleninhalts von transactiontable
	private void showContentsTransactionTable() throws SQLException {
		Connection connection = dbAdministration.getConnection();
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
