package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class DatabaseAdministration {
	Logger logger;
	public static int referenceLength = 64;
	public static int ownerLength = 64;

	// =============================
	// ALLGEMEINE DATENBANK-METHODEN
	// =============================

	/**
	 * Constructor which initializes the Class-Logger.
	 *
	 * @author Jotham Weber
	 */
	public DatabaseAdministration() {
		logger = Logger.getLogger(getClass());
	}

	/**
	 * Establishes the connection to the jdbc database.
	 *
	 * @return the connection or null if there was an error.
	 * @author Jotham Weber
	 */
	public Connection getConnection() {
		try {
			logger.info("Verbindung zur Datenbank wird aufgebaut.");
			Class.forName("org.apache.derby.jdbc.ClientDriver");
			Properties properties = new Properties();
			properties.put("user", "user");
			Connection connection = DriverManager.getConnection("jdbc:derby:database;create=true", properties);
			return connection;
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
	}

	/**
	 * Resets the database tables to a standard scenario.
	 *
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public void resetDatabaseTables() throws SQLException {
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

	/**
	 * Provides the number of entries in a database table.
	 *
	 * @param tableName
	 *            specifies the table.
	 * @return the number of entries.
	 * @throws SQLException
	 * @author Jotham Weber
	 */
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

	/**
	 * Deletes a database table.
	 *
	 * @param tableName
	 *            specifies the table.
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public void deleteTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		statement.execute("DROP table " + tableName);
		logger.info("SQL-Statement ausgeführt: DROP table " + tableName);
		statement.close();
		connection.close();
	}

	/**
	 * Clears a database table by deleting every entry.
	 *
	 * @param tableName
	 *            specifies the table.
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	public void clearTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		statement.execute("DELETE FROM " + tableName);
		logger.info("SQL-Statement ausgeführt: DELETE FROM " + tableName);
		statement.close();
		connection.close();
	}

	/**
	 * Checks if a string is an integer value.
	 *
	 * @param s
	 *            string value to be checked.
	 * @return true, if the string value is an integer and false, if it is not.
	 * @author Jotham Weber
	 */
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

	/**
	 * Checks if a string is an numeric value.
	 *
	 * @param s
	 *            string value to be checked.
	 * @return true, if the string value is an numeric value and false, if it is
	 *         not.
	 */
	public static boolean isNumeric(String s) {
		s = s.replace('.', ',');
		Scanner scanner = new Scanner(s);
		if (scanner.hasNextInt())
			return true;
		else if (scanner.hasNextDouble())
			return true;
		else
			return false;
	}

}
