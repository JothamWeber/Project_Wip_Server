package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.log4j.Logger;

public class DatabaseAdministration {
	Logger logger;

	// =============================
	// ALLGEMEINE DATENBANK-METHODEN
	// =============================

	public DatabaseAdministration(){
		logger = Logger.getLogger(getClass());
	}

	// Connection erstellen
	public Connection getConnection() {
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

	public void resetDatabaseTables() throws SQLException{
		AccountDataAccess daAccount = new AccountDataAccess();
		TransactionDataAccess daTransaction = new TransactionDataAccess();

		deleteTable("transactiontable");
		deleteTable("account");
		daAccount.createAccountTable();
		daTransaction.createTransactionTable();

		daAccount.addAccount("Nadin", new BigDecimal(10000));
		daAccount.addAccount("Alina", new BigDecimal(10000));
		daAccount.addAccount("Sebastian", new BigDecimal(10000));
		daAccount.addAccount("Jotham", new BigDecimal(10000));
	}

	// Rückgabe der Anzahl der Datensätze in der Datenbanktabelle
	public int getEntryCount(String tableName) throws SQLException {
		int entryCount = 0;
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM " + tableName;
		logger.info("SQL-Statement ausgeführt: " + sql);
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
	public void deleteTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		statement.execute("DROP table " + tableName);
		logger.info("SQL-Statement ausgeführt: DROP table " + tableName);
		statement.close();
		connection.close();
	}

	// Entfernt alle Einträge einer Datenbanktabelle
	public void clearTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		statement.execute("DELETE FROM " + tableName);
		logger.info("SQL-Statement ausgeführt: DELETE FROM " + tableName);
		statement.close();
		connection.close();
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}

}
