package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import bertelsbank.rest.Account;
import bertelsbank.rest.Transaction;

public class TransactionDataAccess {
	DatabaseAdministration dbAdministration = new DatabaseAdministration();

	Logger logger;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	/**
	 * Constructor which initializes the Class-Logger.
	 *
	 * @author Jotham Weber
	 */
	public TransactionDataAccess() {
		logger = Logger.getLogger(getClass());
	}

	/**
	 * Creates the Transactiontable on the database if it does not exist.
	 *
	 * @throws SQLException
	 * @author Jotham Weber
	 */
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
			String sql = "CREATE table transactionTable (id int not null primary key, senderNumber varchar(4) not null, "
					+ "receiverNumber varchar(4) not null, amount decimal(9,2) not null, reference varchar(" + DatabaseAdministration.referenceLength + ") not null, date timestamp not null)";
			// Tabelle wird erstellt
			statement.execute(sql);
			logger.info("SQL-statement ausgeführt: " + sql);
			statement.close();
			logger.info("Tabelle \"TransactionTable\" wurde erstellt.");
		}
		connection.close();
	}

	/**
	 * Writes a transaction to the database table.
	 *
	 * @param senderNumber
	 *            specifies the sender account.
	 * @param receiverNumber
	 *            specifies the receiver account.
	 * @param amount
	 *            money which will be transferred
	 * @param reference
	 *            text which will be displayed in relation to the transaction
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public void addTransaction(String senderNumber, String receiverNumber, BigDecimal amount, String reference)
			throws SQLException {
		try (Connection connection = dbAdministration.getConnection();
				PreparedStatement preparedStatement = connection
						.prepareStatement("INSERT INTO transactionTable VALUES (?,?,?,?,?,?)")) {
			int id = dbAdministration.getEntryCount("transactionTable") + 1;
			preparedStatement.setInt(1, id);
			preparedStatement.setString(2, senderNumber);
			preparedStatement.setString(3, receiverNumber);
			preparedStatement.setBigDecimal(4, amount);
			preparedStatement.setString(5, reference);
			java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now());
			preparedStatement.setTimestamp(6, timestamp);
			// Write transaction to database
			preparedStatement.execute();
			logger.info("SQL-Statement ausgeführt: INSERT INTO transactionTable VALUES " + id + ", " + senderNumber
					+ ", " + receiverNumber + ", " + amount + ", " + reference + ", " + timestamp);
			logger.info("Transaktion ausgeführt. Sendernr.: " + senderNumber + ", Empfängernr.: " + receiverNumber
					+ ", Betrag: " + amount + ", Referenz: " + reference);
		} catch (SQLException e) {
			logger.error(e);
		}
	}

	/**
	 * Provides the transaction history for a specific account.
	 *
	 * @param accountNumber
	 *            specifies the account.
	 * @return a list with all transactions where the account is either sender
	 *         or receiver.
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public List<Transaction> getTransactionHistory(String accountNumber) throws SQLException {
		List<Transaction> transactionHistoryList = new ArrayList<Transaction>();
		AccountDataAccess daAccount = new AccountDataAccess();

		// Wenn "all" als Kontonummer übergeben wird, soll die gesamte
		// Transaktionshistorie aller Konten zurückgegeben werden
		if (accountNumber.equals("all")) {
			Connection connection = dbAdministration.getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable ORDER BY date desc";
			ResultSet resultSet = statement.executeQuery(sql);
			logger.info("SQL-Statement ausgeführt: " + sql);
			while (resultSet.next()) {
				// Erstellt ein Transactionsobjekt anhand des Datensatzes
				Transaction transaction = new Transaction();
				transaction.setId(resultSet.getInt(1));
				transaction.setSender(daAccount.getAccountByNumber(resultSet.getString(2), false));
				transaction.setReceiver(daAccount.getAccountByNumber(resultSet.getString(3), false));
				transaction.setAmount(resultSet.getBigDecimal(4));
				transaction.setReference(resultSet.getString(5));
				transaction.setTransactionDate(resultSet.getTimestamp(6));
				// Transaktionsobjekt in die Liste hinzufügen
				transactionHistoryList.add(transaction);
			}
			resultSet.close();
			statement.close();
			connection.close();
		} else {
			// Transaktionshistorie für ein bestimmtes Konto zusammenstellen
			Account baseAccount = daAccount.getAccountByNumber(accountNumber, false);
			Connection connection = dbAdministration.getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM transactionTable WHERE sendernumber = '" + accountNumber
					+ "' or receivernumber = '" + accountNumber + "' ORDER BY date desc";
			ResultSet resultSet = statement.executeQuery(sql);
			logger.info("SQL-Statement ausgeführt: " + sql);
			while (resultSet.next()) {
				// Transaktionsobjekt anhand des Datensatzes erstellen
				Transaction transaction = new Transaction();
				transaction.setId(resultSet.getInt(1));
				// Prüfung, ob das entsprechende Konto Sender oder Empfänger ist
				if (resultSet.getString(2).equals(accountNumber)) {
					transaction.setSender(baseAccount);
				} else {
					transaction.setSender(daAccount.getAccountByNumber(resultSet.getString(2), false));
				}
				if (resultSet.getString(3).equals(accountNumber)) {
					transaction.setReceiver(baseAccount);
				} else {
					transaction.setReceiver(daAccount.getAccountByNumber(resultSet.getString(3), false));
				}
				transaction.setAmount(resultSet.getBigDecimal(4));
				transaction.setReference(resultSet.getString(5));
				transaction.setTransactionDate(resultSet.getTimestamp(6));
				// Transaktionsobjekt in die Liste hinzufügen
				transactionHistoryList.add(transaction);
			}
			resultSet.close();
			statement.close();
			connection.close();
		}
		return transactionHistoryList;
	}

	/**
	 * Provides the current balance of a specific account.
	 *
	 * @param accountNumber
	 *            specifies the account.
	 * @return the current balance.
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public BigDecimal getAccountBalance(String accountNumber) throws SQLException {
		BigDecimal balance = BigDecimal.ZERO;
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT amount FROM transactionTable WHERE sendernumber = '" + accountNumber + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		while (resultSet.next()) {
			// Jede Transaktion, die von dem Konto ausging, wird vom Kontostand
			// subtrahiert.
			balance = balance.subtract(resultSet.getBigDecimal(1));
		}
		sql = "SELECT amount FROM transactionTable WHERE receivernumber = '" + accountNumber + "'";
		resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		while (resultSet.next()) {
			// Jede Transaktion an dieses Konto wird zum Kontostand addiert.
			balance = balance.add(resultSet.getBigDecimal(1));
		}
		resultSet.close();
		statement.close();
		connection.close();
		return balance;
	}

	/**
	 * Writes every entry of the transactiontable on the console.
	 *
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	private void showContentTransactionTable() throws SQLException {
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM transactionTable";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
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
