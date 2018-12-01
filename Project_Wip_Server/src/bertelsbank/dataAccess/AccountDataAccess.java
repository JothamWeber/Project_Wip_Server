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

import javax.ws.rs.GET;

import org.apache.derby.client.am.DateTime;
import org.apache.derby.client.am.Decimal;
import org.apache.derby.iapi.services.io.NewByteArrayInputStream;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.session.JDBCSessionIdManager.DatabaseAdaptor;

import bertelsbank.rest.Account;
import bertelsbank.rest.Transaction;
import javafx.beans.value.WeakChangeListener;

public class AccountDataAccess {
	TransactionDataAccess daTransaction = new TransactionDataAccess();
	DatabaseAdministration dbAdministration = new DatabaseAdministration();

	Connection connection = dbAdministration.getConnection();
	public List<String> reservatedNumbers = new ArrayList<String>();
	boolean bankAccountExists = true;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	Logger logger;

	public AccountDataAccess() {
		logger = Logger.getLogger(getClass());
	}

	// Kontentabelle erstellen
	public void createAccountTable() throws SQLException {
		Connection connection = dbAdministration.getConnection();
		// Prüfung, ob Tabelle bereits besteht
		ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
		boolean shouldCreateTable = true;
		while (resultSet.next() && shouldCreateTable) {
			if (resultSet.getString("TABLE_NAME").equalsIgnoreCase("ACCOUNT")) {
				shouldCreateTable = false;
			}
		}
		resultSet.close();

		if (shouldCreateTable) {
			Statement statement = connection.createStatement();
			String sql = "CREATE table account (id int not null primary key, owner varchar(64) not null, number varchar(4) not null)";
			statement.execute(sql);
			statement.close();
			logger.info("SQL-Statement ausgeführt: " + sql);
			logger.info("Tabelle \"Account\" wurde erstellt.");
			bankAccountExists = false;
			addAccount("BANK", BigDecimal.ZERO);
			bankAccountExists = true;
		}

		connection.close();
	}

	// Konto der Tabelle hinzufÃ¼gen
	public void addAccount(String owner, BigDecimal startBalance) throws SQLException {

		String accountNumber = getFreeNumber();
		if (!accountNumber.equals("")) {
			try (Connection connection = dbAdministration.getConnection();
					PreparedStatement preparedStatement = connection
							.prepareStatement("INSERT INTO account VALUES (?,?,?)")) {
				int id = dbAdministration.getEntryCount("account") + 1;
				preparedStatement.setInt(1, id);
				preparedStatement.setString(2, owner);
				preparedStatement.setString(3, accountNumber);
				preparedStatement.execute();
				logger.info("SQL-Statement ausgeführt: " + "INSERT INTO account VALUES (" + id + ", " + owner + ", "
						+ accountNumber + ")");
				logger.info("Neues Konto angelegt. Besitzer: " + owner + ", Kontonr.: " + accountNumber
						+ ", Startkapital: " + startBalance);
				if (startBalance.compareTo(BigDecimal.ZERO) == 1) {
					daTransaction.addTransaction("0000", accountNumber, startBalance, "STARTGUTHABEN");
				}
				reservatedNumbers.remove(accountNumber);
			} catch (SQLException e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
	}

	// Rückgabe eines Kontos
	public Account getAccountByNumber(String number, boolean attachTransactions) throws SQLException {
		Account account = new Account();
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM account WHERE  number = '" + number + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		if (resultSet.next()) {
			int id = resultSet.getInt(1);
			String owner = resultSet.getString(2);
			account.setId(id);
			account.setOwner(owner);
			account.setNumber(number);
			if (attachTransactions) {
				account.setTransactions(daTransaction.getTransactionHistory(number));
			}
		}
		resultSet.close();
		statement.close();
		connection.close();
		return account;
	}

	// Rückgabe einer Liste aller Konten
	public List<Account> getAccounts() {
		List<Account> accounts = new ArrayList<>();
		try {
			Connection connection = dbAdministration.getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM account";
			ResultSet resultSet = statement.executeQuery(sql);
			logger.info("SQL-Statement ausgeführt: " + sql);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				String owner = resultSet.getString(2);
				String number = resultSet.getString(3);
				Account account = new Account();
				account.setId(id);
				account.setOwner(owner);
				account.setNumber(number);
				account.setTransactions(daTransaction.getTransactionHistory(number));
				accounts.add(account);
			}
			resultSet.close();
			statement.close();
			connection.close();
			return accounts;
		} catch (SQLException e) {
			logger.error(e);
			e.printStackTrace();
			return null;
		}
	}

	// Aktualisiert den Namen des Owners
	public void updateOwner(String number, String owner) throws SQLException {
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "UPDATE account SET owner = '" + owner + "' WHERE number = '" + number + "'";
		statement.executeUpdate(sql);
		logger.info("SQL-Statement ausgeführt :" + sql);
		logger.info("Der Besitzer des Kontos " + number + " wurde durch \"" + owner + "\" ersetzt.");
		statement.close();
		connection.close();
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
	public boolean numberExists(String number) throws SQLException {
		int entryCount = 0;
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM account WHERE number = '" + number + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		if (resultSet.next()) {
			entryCount = resultSet.getInt(1);
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

	// Ausgabe des Tabelleninhalts von account. Nur zu Testzwecken. Wird nicht
	// vom Client aufgerufen.
	private void showContentsAccountTable() throws SQLException {
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM account";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
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

}
