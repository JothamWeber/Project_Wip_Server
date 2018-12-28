package bertelsbank.dataAccess;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import bertelsbank.rest.Account;

public class AccountDataAccess {
	TransactionDataAccess daTransaction = new TransactionDataAccess();
	DatabaseAdministration dbAdministration = new DatabaseAdministration();
	Connection connection = dbAdministration.getConnection();
	public List<String> reservedNumbers = new ArrayList<String>(); // enthält
																	// alle
																	// reservierten
																	// Kontonummern
	boolean bankAccountExists = true;
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	Logger logger;

	/**
	 * Constructor which initializes the Class-Logger.
	 *
	 * @author Jotham Weber
	 */
	public AccountDataAccess() {
		logger = Logger.getLogger(getClass());
	}

	/**
	 * Creates the account table on the database if it does not exist. The bank
	 * account will be added after creation of the table.
	 *
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
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
			String sql = "CREATE table account (id int not null primary key, owner varchar("
					+ DatabaseAdministration.ownerLength + ") not null, number varchar(4) not null)";
			// Tabelle wird erstellt
			statement.execute(sql);
			statement.close();
			logger.info("SQL-Statement ausgeführt: " + sql);
			logger.info("Tabelle \"Account\" wurde erstellt.");
			bankAccountExists = false;
			// Das Bank-Konto wird angelegt
			addAccount("0000", "BANK", BigDecimal.ZERO);
			bankAccountExists = true;
		}
		connection.close();
	}

	/**
	 * Adds an account with an owner and a start balance to the database table
	 * "Account". The start balance will be provided from the bank account.
	 *
	 * @param accountNumber
	 *            specifies the account.
	 * @param owner
	 *            the person owning the account.
	 * @param startBalance
	 *            the amount of money which will be transferred to the account
	 *            immediately after creation.
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public void addAccount(String accountNumber, String owner, BigDecimal startBalance) throws SQLException {

		try (Connection connection = dbAdministration.getConnection();
				PreparedStatement preparedStatement = connection
						.prepareStatement("INSERT INTO account VALUES (?,?,?)")) {
			int id = dbAdministration.getEntryCount("account") + 1;
			preparedStatement.setInt(1, id);
			preparedStatement.setString(2, owner);
			preparedStatement.setString(3, accountNumber);
			// Datensatz in die Datenbanktabelle schreiben
			preparedStatement.execute();
			logger.info("SQL-Statement ausgeführt: " + "INSERT INTO account VALUES (" + id + ", " + owner + ", "
					+ accountNumber + ")");
			logger.info("Neues Konto angelegt. Besitzer: " + owner + ", Kontonr.: " + accountNumber + ", Startkapital: "
					+ startBalance);
			if (startBalance.compareTo(BigDecimal.ZERO) == 1) {
				// Wenn das angeforderte Startguthaben > 0 ist, wird es dem
				// Konto überwiesen
				daTransaction.addTransaction("0000", accountNumber, startBalance, "STARTGUTHABEN");
			}
			// Die reservierte Nummer gehört jetzt zu einem Konto, daher
			// kann die Reservierung aufgehoben werden
			reservedNumbers.remove(accountNumber);
		} catch (SQLException e) {
			logger.error(e);
		}
	}

	/**
	 * Returns an account object belonging to the given number.
	 *
	 * @param number
	 *            specifies the account.
	 * @param attachTransactions
	 *            if this is true, the transaction history for this account is
	 *            attached to the account object.
	 * @return the account object
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public Account getAccountByNumber(String number, boolean attachTransactions) throws SQLException {
		Account account = new Account();
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		// Datensatz von der Datenbank erhalten
		String sql = "SELECT * FROM account WHERE  number = '" + number + "'";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		if (resultSet.next()) {
			// Aus dem Datensatz ein Kontoobjekt erstellen
			int id = resultSet.getInt(1);
			String owner = resultSet.getString(2);
			account.setId(id);
			account.setOwner(owner);
			account.setNumber(number);
			if (attachTransactions) {
				// Transaktionen an das Kontoobjekt hängen
				account.setTransactions(daTransaction.getTransactionHistory(number));
			}
		}
		resultSet.close();
		statement.close();
		connection.close();
		return account;
	}

	/**
	 * Every account existing on the account database table is put into a list.
	 * The list is returned.
	 *
	 * @return list containing account objects.
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public List<Account> getAccounts() throws SQLException {
		List<Account> accounts = new ArrayList<>();
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		// Alle Datensätze der Account-Tabelle abrufen
		String sql = "SELECT * FROM account";
		ResultSet resultSet = statement.executeQuery(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		while (resultSet.next()) {
			// Datensätze in Kontoobjekte umwandeln
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
	}

	/**
	 * Replaces the current owner name of an account in the database by a new
	 * name.
	 *
	 * @param number
	 *            specifies the account.
	 * @param owner
	 *            the new owner name.
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public void updateOwner(String number, String owner) throws SQLException {
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		String sql = "UPDATE account SET owner = '" + owner + "' WHERE number = '" + number + "'";
		// Der Datensatz wird auf der Datenbank aktualisiert
		statement.executeUpdate(sql);
		logger.info("SQL-Statement ausgeführt: " + sql);
		logger.info("Der Besitzer des Kontos " + number + " wurde durch \"" + owner + "\" ersetzt.");
		statement.close();
		connection.close();
	}

	/**
	 * The next free number for an account is determined, reserved and returned.
	 *
	 * @return if the bank account does not exist yet, the number returned is
	 *         0000. Otherwise it is a number between 1000 and 9999, which does
	 *         not belong to an existing account. If there is no more free
	 *         number, an empty string will be returned.
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public String getFreeNumber() throws SQLException {
		String freeNumber = "";
		if (bankAccountExists) {
			// Angefangen bei 1000 werden die Nummern auf Verfügbarkeit geprüft
			for (int i = 1000; i <= 9999; i++) {
				String iString = String.valueOf(i);
				// Prüfung, ob die Nummer schon einem Konto zugeordnet oder
				// reserviert ist
				if (!numberExists(iString) && !reservedNumbers.contains(iString)) {
					freeNumber = iString;
					break;
				}
			}
		} else {
			freeNumber = "0000";
		}
		// Reservierung der Nummer
		reservedNumbers.add(freeNumber);
		return freeNumber;
	}

	/**
	 * Checks if a number is the account number of an existing account.
	 *
	 * @param number
	 *            account number belonging to the account which is to be
	 *            checked.
	 * @return true, if there is an account with this number and false, if there
	 *         is no account belonging to this number.
	 * @throws SQLException
	 *             if the database access fails or the sql statement cannot be
	 *             executed.
	 * @author Jotham Weber
	 */
	public boolean numberExists(String number) throws SQLException {
		int entryCount = 0;
		Connection connection = dbAdministration.getConnection();
		Statement statement = connection.createStatement();
		// Auf der Datenbank wird abgefragt, wie viele Datensätze diese
		// Kontonummer haben. Der Wert kann 0 oder 1 sein.
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

	/**
	 * Writes every entry of the account table on the console.
	 *
	 * @throws SQLException
	 * @author Jotham Weber
	 */
	private void showContentAccountTable() throws SQLException {
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
