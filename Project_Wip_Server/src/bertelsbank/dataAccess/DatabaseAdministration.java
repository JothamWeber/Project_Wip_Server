package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseAdministration {

	// =============================
	// ALLGEMEINE DATENBANK-METHODEN
	// =============================

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
		System.out.println("Deleting table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("drop table " + tableName);
		statement.close();
		connection.close();
	}

	// Entfernt alle Einträge einer Datenbanktabelle
	public void clearTable(String tableName) throws SQLException {
		Connection connection = getConnection();
		System.out.println("Clearing table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("delete from " + tableName);
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
