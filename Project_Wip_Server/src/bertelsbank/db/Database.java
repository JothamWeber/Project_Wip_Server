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

import bertelsbank.rest.Student;

public class Database {

	public static void main(String[] args) throws SQLException {
		Database main = new Database();
		//main.createAccountTable();
		//main.addAccount();
		main.showContents();
	}

	//Ü
	public Database () {
		try {
			createAccountTable();
			addAccount();
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
	public List<Student> getStudents() {
		List<Student> students = new ArrayList<>();

		try {
			Connection connection = getConnection();
			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM student";
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				int id = resultSet.getInt(1);
				String name = resultSet.getString(2);
				Student student = new Student();
				student.setId(id);
				student.setName(name);
				students.add(student);
			}
			resultSet.close();
			statement.close();
			connection.close();
			return students;
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

			System.out.println(id + " -- " + owner);
		}
		resultSet.close();
		statement.close();
		connection.close();
	}
	
	//Konto der Tabelle hinzufügen
		private void addAccount() {
			System.out.println("Adding account...");
			//Try - Catch mit Resourcen
			try (Connection connection = getConnection();
					PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO account VALUES (?,?)")) {
				preparedStatement.setInt(1, 1000);
				preparedStatement.setString(2, "BANK");
				preparedStatement.execute();
			} catch (SQLException e) {
				//Exception loggen, ggf. angemessen reagieren
				e.printStackTrace();
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
			statement.execute("create table account (id int not null, owner varchar(64))");
			statement.close();
		}
		connection.close();
	}
				
}
