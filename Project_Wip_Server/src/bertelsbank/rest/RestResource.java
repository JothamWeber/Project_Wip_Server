package bertelsbank.rest;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.SimpleLayout;

import com.sun.jersey.spi.resource.Singleton;

import bertelsbank.dataAccess.AccountDataAccess;
import bertelsbank.dataAccess.DatabaseAdministration;
import bertelsbank.dataAccess.TransactionDataAccess;

@Path("/")
@Singleton
public class RestResource {
	AccountDataAccess daAccount = new AccountDataAccess();
	TransactionDataAccess daTransation = new TransactionDataAccess();
	DatabaseAdministration dbAdministration = new DatabaseAdministration();
	Logger logger;
	String serverErrorMessage = "Interner Serverfehler. Bitte versuchen Sie es erneut.";

	/**
	 * The constructor which initializes the Root-Logger and the Class-Logger.
	 *
	 * @author Jotham Weber
	 */
	public RestResource() {
		try {
			logger = Logger.getRootLogger();
			logger.setAdditivity(false);
			SimpleLayout layout = new SimpleLayout();
			FileAppender fileAppender;
			fileAppender = new FileAppender(layout, "logs/ServerLogFile.log", false);
			logger.addAppender(fileAppender);
			logger.setLevel(Level.ALL);
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger = Logger.getLogger(getClass());
		logger.info("Server gestartet.");
	}

	// ==========================
	// ÖFFENTLICHE SCHNITTSTELLEN
	// ==========================

	/**
	 * Returns an account object with all its transactions if the number
	 * provided by the client belongs to an existing account. Otherwise an error
	 * message will be returned.
	 *
	 * @param number
	 *            the account number of the account which should be returned
	 * @return the response to the http-get. In best case it will be the desired
	 *         account object or a http status with error information.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/account/{number}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAccount(@PathParam("number") String number) {

		String errorMessage = "";
		logger.info("Anforderung eines Kontoobjektes mit der Nummer: " + number);

		// Besteht die Kontonummer aus 4 Zahlen?
		if (number.length() != 4 || !dbAdministration.isInteger(number)) {
			errorMessage = "Die Kontonummer muss aus 4 Zahlen bestehen.";
			logger.error(errorMessage + " (" + number + ")");
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		try {
			// Existiert der Account?
			if (!daAccount.numberExists(number)) {
				errorMessage = "Diese Kontonummer existiert nicht.";
				logger.error(errorMessage + " (" + number + ")");
				return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
			} else {
				// Gibt den Account zurück
				return Response.ok(daAccount.getAccountByNumber(number, true)).build();
			}
		} catch (Exception e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		} finally {
			if (errorMessage.equals("")) {
				logger.info("Kontoobjekt wurde übermittelt. (" + number + ")");
			}
		}
	}

	/**
	 * Receives the information for a transaction and checks if the transaction
	 * is viable. If not, an error response will be returned. Otherwise the
	 * transaction will be triggered.
	 *
	 * @param senderNumber
	 *            the account number of the sender.
	 * @param receiverNumber
	 *            the account number of the receiver.
	 * @param amount
	 *            the value which is to send from the sender account to the
	 *            receiver account.
	 * @param reference
	 *            a short text which explains the reason of the transaction.
	 * @return a http response which shows if the transaction was done or not.
	 *         If the transaction was not possible, an error message explains
	 *         the cause.
	 */
	@POST
	@Path("/transaction")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public synchronized Response executeTransaction(@FormParam("senderNumber") String senderNumber,
			@FormParam("receiverNumber") String receiverNumber, @FormParam("amount") BigDecimal amount,
			@FormParam("reference") String reference) {

		String errorMessage = "";
		logger.info("Anforderung einer Transaktionsdurchführung. (" + senderNumber + " an " + receiverNumber + " | "
				+ amount + " | " + reference + ")");

		// Sind alle Werte vorhanden?
		if (senderNumber == null || receiverNumber == null || amount == null || reference == null) {
			errorMessage = "Nicht alle Felder sind gefüllt.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Haben "senderNumber" und "receiverNumber" das richtige Format?
		if (senderNumber.length() != 4 || !dbAdministration.isInteger(senderNumber) || receiverNumber.length() != 4
				|| !dbAdministration.isInteger(receiverNumber)) {
			errorMessage = "Die Kontonummer muss aus 4 Zahlen bestehen.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Unterscheiden sich "senderNumber" und "receiverNumber"?
		if (senderNumber.equals(receiverNumber)) {
			errorMessage = "Das Senderkonto darf nicht das Empfängerkonto sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Ist "amount" größer als 0?
		if (!(amount.compareTo(BigDecimal.ZERO) == 1)) {
			errorMessage = "Der Betrag muss größer als 0 sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Hat "amount" mehr als 2 Nachkommastellen?
		if (amount.scale() > 2) {
			errorMessage = "Der Betrag darf maximal 2 Nachkommastellen haben.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Besteht "reference" aus den erlaubten Zeichen?
		Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(reference);
		if (m.find()) {
			errorMessage = "Die Referenz darf nur folgende Zeichen beinhalten: A-Z, a-z, 0-9, Leerzeichen.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		try {
			// Existiert das Senderkonto?
			if (!daAccount.numberExists(senderNumber)) {
				errorMessage = "Das Senderkonto existiert nicht.";
				logger.error(errorMessage);
				return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
			}

			// Existiert das Empfängerkonto?
			if (!daAccount.numberExists(receiverNumber)) {
				errorMessage = "Das Empfängerkonto existiert nicht.";
				logger.error(errorMessage);
				return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
			}

			// Hat das Empfängerkonto ausreichend Guthaben?
			if (daTransation.getAccountBalance(senderNumber).compareTo(amount) == -1 && !senderNumber.equals("0000")) {
				errorMessage = "Ihr Kontoguthaben reicht für diese Transaktion nicht aus.";
				logger.error(errorMessage);
				return Response.status(Response.Status.PRECONDITION_FAILED).entity(errorMessage).build();
			}
			// Transaktion in Datenbank schreiben
			daTransation.addTransaction(senderNumber, receiverNumber, amount, reference);
			return Response.status(Response.Status.NO_CONTENT).build();

		} catch (Exception e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		}
	}

	// ============
	// VERWALTUNG
	// ============

	/**
	 * Checks if a new account can be created and triggers the execution.
	 *
	 * @param owner
	 *            the name of the person the account will belong to.
	 * @param startBalance
	 *            the amount of money which will be on the account after
	 *            creation.
	 * @return a http response will declare if the creation of the account was
	 *         successful or not.
	 */
	@POST
	@Path("/addAccount")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addAccount(@FormParam("owner") String owner, @FormParam("startBalance") BigDecimal startBalance) {

		String errorMessage = "";
		logger.info("Anforderung des Anlegens eines neuen Kontos.");

		// Sind alle Werte vorhanden?
		if (owner.equals("") || startBalance == null) {
			errorMessage = "Nicht alle Felder sind gefüllt.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Ist "owner" = bank?
		if (owner.toLowerCase().equals("bank")) {
			errorMessage = "Das Konto \"Bank\" ist reserviert.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Ist "startBalance" > 0?
		if (startBalance.compareTo(BigDecimal.ZERO) != 1) {
			errorMessage = "Das Startguthaben muss größer als 0 sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}

		// Hat "startBalance" mehr als 2 Nachkommastellen?
		if (startBalance.scale() > 2) {
			errorMessage = "Der Betrag darf maximal 2 Nachkommastellen haben.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		try {
			// Konto erstellen
			daAccount.addAccount(owner, startBalance); // Logging findet in
														// addAccount statt
			return Response.ok().build();
		} catch (SQLException e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		}
	}

	/**
	 * Provides an array containing every single account which belongs to the Bertelsbank.
	 *
	 * @return
	 */
	@GET
	@Path("/allAccounts")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllAccounts() {

		logger.info("Anforderung einer Auflistung aller Konten.");
		String errorMessage = "";

		try {
			List<Account> accounts = daAccount.getAccounts();
			Account[] accountArray = accounts.toArray(new Account[0]);
			return Response.ok(accountArray).build();
		} catch (Exception e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		} finally {
			if (errorMessage.equals("")) {
				logger.info("Eine Auflistung aller Konten wurde übermittelt.");
			}
		}
	}

	// Liefert alle Transaktionen
	/**
	 * @return
	 */
	@GET
	@Path("/allTransactions")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllTransactions() {

		logger.info("Anforderung einer Auflistung aller Transaktionen.");
		String errorMessage = "";

		try {
			List<Transaction> transactions = daTransation.getTransactionHistory("all");
			Transaction[] transactionArray = transactions.toArray(new Transaction[0]);
			return Response.ok(transactionArray).build();
		} catch (Exception e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		} finally {
			if (errorMessage.equals("")) {
				logger.info("Eine Auflistung aller Transaktionen wurde übermittelt.");
			}
		}
	}

	// Ownernamen aktualisieren
	@POST
	@Path("/updateOwner")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response dereservateNumber(@FormParam("number") String number, @FormParam("owner") String owner) {

		logger.info("Anforderung einer Aktualisierung des Besitzernamens von Konto " + number + ".");
		String errorMessage = "";

		try {
			// Existiert das Konto?
			if (!daAccount.numberExists(number)) {
				errorMessage = "Das Konto existiert nicht.";
				logger.error(errorMessage);
				return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
			}

			// Änderung durchführen
			daAccount.updateOwner(number, owner); // Logging findet in
													// updateOwner statt
			return Response.ok().build();
		} catch (SQLException e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		}
	}

	// Wird aufgerufen, wenn ein neues Konto angelegt werden soll und liefert
	// eine freie Kontonummer
	/**
	 * @return
	 */
	@GET
	@Path("/freeNumber")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public synchronized Response getFreeNumber() {

		String errorMessage = "";
		logger.info("Anforderung einer freien Kontonummer.");
		String freeNumber = "";

		try {
			freeNumber = daAccount.getFreeNumber();
			if (freeNumber.equals("")) {
				errorMessage = "Es gibt keine freien Nummern mehr.";
				return Response.status(Response.Status.NOT_FOUND).entity(errorMessage).build();
			} else {
				return Response.ok(freeNumber).build();
			}
		} catch (SQLException e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		} finally {
			if (errorMessage.equals("")) {
				logger.info("Eine freie Kontonummer wurde übermittelt. (" + freeNumber + ")");
			}
		}
	}

	// Wird aufgerufen, wenn im Dialog "Konto erstellen" der Abbrechen-Button
	// gedrückt wird
	/**
	 * @param number
	 * @return
	 * @throws SQLException
	 */
	@POST
	@Path("/dereservateNumber")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response dereservateNumber(@FormParam("number") String number) {

		logger.info("Anforderung der Freigabe der reservierten Nummer " + number + ".");

		daAccount.reservatedNumbers.remove(number);
		logger.info("Die zuvor angeforderte Kontonummer " + number
				+ " wurde wieder freigegeben, da das Anlegen des Kontos abgebrochen wurde.");
		return Response.ok().build();
	}

	// Gibt den Kontostand eines Kontos zurück. Nur für Testzwecke, wird von
	// den Clients nicht aufgerufen. Daher auch kein Logging.
	/**
	 * @param number
	 * @return
	 */
	@GET
	@Path("/accountBalance/{number}")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getAccountBalance(@PathParam("number") String number) {

		logger.info("Anforderung des Guthabens des Kontos " + number + ".");
		String errorMessage = "";

		try {
			return Response.ok(daTransation.getAccountBalance(number).toString()).build();
		} catch (SQLException e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		} finally {
			if (errorMessage.equals("")) {
				logger.info("Der Kontostand des Kontos " + number + " wurde übermittelt.");
			}
		}
	}

	// Setzt die Datenbanktabellen auf ein Standardszenario mit Bankkonto und 4
	// Kundenkonten zurück. Ebenfalls nur zu Test- und Administrationszwecken.
	// Kein Aufruf durch Clients.
	/**
	 * @return
	 */
	@POST
	@Path("/resetDatabaseTables")
	public Response resetDatabaseTables() {

		logger.info("Anforderung einer Zurücksetzung aller Datenbanktabellen.");
		try {
			dbAdministration.resetDatabaseTables();
			logger.info("Die Datenbanktabellen wurden zurückgesetzt.");
			return Response.ok().build();
		} catch (SQLException e) {
			logger.error(e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(serverErrorMessage).build();
		}

	}

}
