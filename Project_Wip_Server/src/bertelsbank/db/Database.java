package bertelsbank.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import bertelsbank.rest.Account;
import bertelsbank.rest.Student;

public class Database {

	public static void main(String[] args) throws SQLException {
		Database main = new Database();
		//main.createAccountTable();
		//main.addAccount();
		//main.showContents();
		//main.deleteTable("account");
	}

	//Ü
	public Database () {
		try {
			createAccountTable();
			addAccount("BANK", "0000");

			showContents();
			//deleteTable("account");
			//clearTable("account");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	//Connection erstellen
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

	//Ü
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

	//Ausgabe des Tabelleninhalts von student
	private void showContents() throws SQLException {
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

	//Ausgabe des Tabelleninhalts von student
	private int getEntryCount() throws SQLException {
		int entryCount = 0;
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM account";
		ResultSet resultSet = statement.executeQuery(sql);
		while (resultSet.next()) {
			entryCount = resultSet.getInt(1);

			System.out.println("Table Entry Count: " + entryCount);
		}
		resultSet.close();
		statement.close();
		connection.close();
		return entryCount;
	}

	//Pr�ft, ob Kontonummer existiert
	private boolean numberExists(String number) throws SQLException {
		int entryCount = 0;
		Connection connection = getConnection();
		Statement statement = connection.createStatement();
		String sql = "SELECT count(*) FROM account where number = '" + number + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		while (resultSet.next()) {
			entryCount = resultSet.getInt(1);

			System.out.println("Table Entry Count: " + entryCount);
		}
		resultSet.close();
		statement.close();
		connection.close();
		if(entryCount == 0){
			return false;
		}else{
			return true;
		}
	}

	//Konto der Tabelle hinzufügen
	public void addAccount(String owner, String number) throws SQLException {
		if(!numberExists(number)){
			System.out.println("Adding account...");
			//Try - Catch mit Resourcen
			try (Connection connection = getConnection();
					PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO account VALUES (?,?,?)")) {
				preparedStatement.setInt(1, getEntryCount() + 1);
				preparedStatement.setString(2, owner);
				preparedStatement.setString(3, number);
				preparedStatement.execute();
			} catch (SQLException e) {
				//Exception loggen, ggf. angemessen reagieren
				e.printStackTrace();
			}
		}else{
			System.out.println("Account not added.");
		}
	}
	//Kontentabelle erstellen
	private void createAccountTable() throws SQLException {
		Connection connection = getConnection();
		//Optionale Prüfung, ob Tabelle bereits besteht
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
			statement.execute("create table account (id int not null, owner varchar(64) not null, number varchar(64) not null)");
			statement.close();
		}

		connection.close();
	}

	private void deleteTable(String tableName) throws SQLException{
		Connection connection = getConnection();
		System.out.println("Deleting table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("drop table " + tableName);
		statement.close();
		connection.close();
	}

	private void clearTable(String tableName) throws SQLException{
		Connection connection = getConnection();
		System.out.println("Clearing table " + tableName + "...");
		Statement statement = connection.createStatement();
		statement.execute("delete from " + tableName);
		statement.close();
		connection.close();
	}

}
