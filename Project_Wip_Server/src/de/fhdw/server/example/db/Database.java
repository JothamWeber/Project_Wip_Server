package de.fhdw.server.example.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import de.fhdw.server.example.rest.Student;

public class Database {

	public static void main(String[] args) throws SQLException {
		Database main = new Database();
		main.createTable();
		main.addStudent();
		main.showContents();
	}

	//Ü
	public Database () {
		try {
			createTable();
			addStudent();
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
		String sql = "SELECT * FROM student";
		ResultSet resultSet = statement.executeQuery(sql);
		System.out.println("Table student:");
		while (resultSet.next()) {
			int id = resultSet.getInt(1);
			String name = resultSet.getString(2);

			System.out.println(id + " -- " + name);
		}
		resultSet.close();
		statement.close();
		connection.close();
	}

	//Studenten der Tabelle hinzufügen
	private void addStudent() {
		System.out.println("Adding student...");
		//Try - Catch mit Resourcen
		try (Connection connection = getConnection();
				PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO student VALUES (?,?)")) {
			preparedStatement.setInt(1, 4711);
			preparedStatement.setString(2, "Walter");
			preparedStatement.execute();
		} catch (SQLException e) {
			//Exception loggen, ggf. angemessen reagieren
			e.printStackTrace();
		}
	}

	//Studententabelle erstellen
	private void createTable() throws SQLException {
		Connection connection = getConnection();
		//Optionale Prüfung, ob Tabelle bereits besteht
		ResultSet resultSet = connection.getMetaData().getTables("%", "%", "%", new String[] { "TABLE" });
		boolean shouldCreateTable = true;
		while (resultSet.next() && shouldCreateTable) {
			if (resultSet.getString("TABLE_NAME").equalsIgnoreCase("STUDENT")) {
				shouldCreateTable = false;
			}
		}
		resultSet.close();

		if (shouldCreateTable) {
			System.out.println("Creating table student...");
			Statement statement = connection.createStatement();
			statement.execute("create table student (id int not null, name varchar(64))");
			statement.close();
		}
		connection.close();
	}
}
