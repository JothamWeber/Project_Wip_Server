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
	 * The constructor which initializes the Class-Logger.
	 *
	 * @author Jotham Weber
	 */
	public RestResource() {
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
	 * @return the response to the HTTP-get. In best case it will be the desired
	 *         account object or a HTTP status with error information.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/account/{number}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAccount(@PathParam("number") String number) {

		String errorMessage = "";
		logger.info("Anforderung eines Kontoobjektes mit der Nummer: " + number);

		// Besteht die Kontonummer aus 4 Zahlen?
		if (number.length() != 4 || !DatabaseAdministration.isInteger(number)) {
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
	 * @return a HTTP response which shows if the transaction was done or not.
	 *         If the transaction was not possible, an error message explains
	 *         the cause.
	 */
	@POST
	@Path("/transaction")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public synchronized Response executeTransaction(@FormParam("senderNumber") String senderNumber,
			@FormParam("receiverNumber") String receiverNumber, @FormParam("amount") String amount,
			@FormParam("reference") String reference) {

		String errorMessage = "";
		logger.info("Anforderung einer Transaktionsdurchführung. (" + senderNumber + " an " + receiverNumber + " | "
				+ amount + " | " + reference + ")");
		// Sind alle Werte vorhanden?
		if (senderNumber == null || receiverNumber == null || amount == null || reference.equals("")) {
			errorMessage = "Nicht alle Felder sind gefüllt.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Haben "senderNumber" und "receiverNumber" das richtige Format?
		if (senderNumber.length() != 4 || !DatabaseAdministration.isInteger(senderNumber)
				|| receiverNumber.length() != 4 || !DatabaseAdministration.isInteger(receiverNumber)) {
			errorMessage = "Die Kontonummer muss aus 4 Zahlen bestehen.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Beginnen "senderNumber" und "receiverNumber" mit einer 1?
		if (senderNumber.startsWith("0") || receiverNumber.startsWith("0")) {
			errorMessage = "Die Kontonummern müssen mit einer 1 beginnen.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Unterscheiden sich "senderNumber" und "receiverNumber"?
		if (senderNumber.equals(receiverNumber)) {
			errorMessage = "Das Senderkonto darf nicht das Empfängerkonto sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "amount" ein numerischer Wert?
		BigDecimal amountBigDecimal;
		if(DatabaseAdministration.isNumeric(amount)){
			amountBigDecimal = new BigDecimal(amount);
		} else {
			errorMessage = "Der Betrag hat das falsche Format.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "amount" größer als 0?
		if (!(amountBigDecimal.compareTo(BigDecimal.ZERO) == 1)) {
			errorMessage = "Der Betrag muss größer als 0 sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "amount" kleiner als 999.999.999,99?
		if (amountBigDecimal.compareTo(new BigDecimal(999999999.99)) == 1) {
			errorMessage = "Der Betrag darf nicht größer als 999.999.999,99 sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Hat "amount" mehr als 2 Nachkommastellen?
		if (amountBigDecimal.scale() > 2) {
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
		// Überschreitet "reference" die erlaubte Länge?
		if (reference.length() > DatabaseAdministration.referenceLength) {
			errorMessage = "Der Verwendungszweck darf max. " + DatabaseAdministration.referenceLength
					+ " Zeichen beinhalten.";
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
			if (daTransation.getAccountBalance(senderNumber).compareTo(amountBigDecimal) == -1 && !senderNumber.equals("0000")) {
				errorMessage = "Ihr Kontoguthaben reicht für diese Transaktion nicht aus.";
				logger.error(errorMessage);
				return Response.status(Response.Status.PRECONDITION_FAILED).entity(errorMessage).build();
			}
			// Transaktion in Datenbank schreiben
			daTransation.addTransaction(senderNumber, receiverNumber, amountBigDecimal, reference);
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
	 *            the amount of money which will be transferred to the account
	 *            immediately after creation.
	 * @return a HTTP response will declare if the creation of the account was
	 *         successful or not.
	 */
	@POST
	@Path("/addAccount")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addAccount(@FormParam("owner") String owner, @FormParam("startBalance") String startBalance) {

		String errorMessage = "";
		logger.info("Anforderung des Anlegens eines neuen Kontos.");

		// Sind alle Werte vorhanden?
		if (owner.equals("") || startBalance.equals("")) {
			errorMessage = "Nicht alle Felder sind gefüllt.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Besteht "reference" aus den erlaubten Zeichen?
		Pattern p = Pattern.compile("[^a-z ]", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(owner);
		if (m.find()) {
			errorMessage = "Der Besitzer darf nur folgende Zeichen beinhalten: A-Z, a-z, Leerzeichen.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "owner" = bank?
		if (owner.toLowerCase().equals("bank")) {
			errorMessage = "Das Konto \"Bank\" ist reserviert.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Entspricht "owner" der Zeichenbegrenzung?
		if (owner.length() > DatabaseAdministration.ownerLength) {
			errorMessage = "Der Besitzername darf max. " + DatabaseAdministration.ownerLength + "Zeichen beinhalten.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "startBalance" ein numerischer Wert?
		BigDecimal startBalanceBigDecimal;
		if (DatabaseAdministration.isNumeric(startBalance)) {
			startBalanceBigDecimal = new BigDecimal(startBalance);
		} else {
			errorMessage = "Der Betrag hat das falsche Format.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Ist "startBalance" > 0?
		if (startBalanceBigDecimal.compareTo(BigDecimal.ZERO) != 1) {
			errorMessage = "Das Startguthaben muss größer als 0 sein.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		// Hat "startBalance" mehr als 2 Nachkommastellen?
		if (startBalanceBigDecimal.scale() > 2) {
			errorMessage = "Der Betrag darf maximal 2 Nachkommastellen haben.";
			logger.error(errorMessage);
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		}
		try {
			// Konto erstellen
			daAccount.addAccount(owner, startBalanceBigDecimal); // Logging in
																	// addAccount
			return Response.ok().build();
		} catch (SQLException e) {
			logger.error(e);
			errorMessage = serverErrorMessage;
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorMessage).build();
		}
	}

	/**
	 * Provides a collection of all accounts which belongs to the Bertelsbank.
	 *
	 * @return array containing all account objects with the respective
	 *         transactions.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/allAccounts")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllAccounts() {

		logger.info("Anforderung einer Auflistung aller Konten.");
		String errorMessage = "";

		try {
			// Laden aller Accountobjecte
			List<Account> accounts = daAccount.getAccounts();
			// Umwandlung in Array, um als Json senden zu können
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

	/**
	 * Provides all transactions executed within the Bertelsbank.
	 *
	 * @return array containing all Transaction objects.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/allTransactions")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllTransactions() {

		logger.info("Anforderung einer Auflistung aller Transaktionen.");
		String errorMessage = "";

		try {
			// Laden aller Transaktionsobjekte
			List<Transaction> transactions = daTransation.getTransactionHistory("all");
			// Umwandlung in Array, um als Json senden zu können
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

	/**
	 * Receives form-URL-encoded parameters to update the owner name of a
	 * specific account.
	 *
	 * @param number
	 *            specifies the account which is to be updated.
	 * @param owner
	 *            provides the new owner name which will replace the former
	 *            owner name.
	 * @return HTTP response which returns the status of the task.
	 * @author Jotham Weber
	 */
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

	/**
	 * When a new account is to be created this method provides a free number
	 * which can be used as the account number.
	 *
	 * @return HTTP response containing the free number as a string (plain text)
	 *         or the HTTP error message.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/freeNumber")
	@Produces({ MediaType.TEXT_PLAIN })
	public synchronized Response getFreeNumber() {

		String errorMessage = "";
		logger.info("Anforderung einer freien Kontonummer.");
		String freeNumber = "";

		try {
			freeNumber = daAccount.getFreeNumber();
			if (freeNumber.equals("")) {
				// In diesem Fall sind alle Nummern von 1000 bis 9999 bereits
				// vergeben.
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

	/**
	 * The method "getFreeNumber" provides a free number to create a new
	 * account. If the creation is canceled and the new account will not be
	 * created, the number will again be made usable for the next new account.
	 *
	 * @param number
	 *            contains the number which is to be declared as free again.
	 * @return HTTP response with HTTP statuscode 200.
	 * @author Jotham Weber
	 */
	@POST
	@Path("/recallNumberReservation")
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response recallNumberReservation(@FormParam("number") String number) {

		logger.info("Anforderung der Freigabe der reservierten Nummer " + number + ".");
		// Die entsprechende Nummer wird aus einer Liste aller reservierten
		// Nummern entfernt.
		daAccount.reservedNumbers.remove(number);
		logger.info("Die zuvor angeforderte Kontonummer " + number
				+ " wurde wieder freigegeben, da das Anlegen des Kontos abgebrochen wurde.");
		return Response.ok().build();
	}

	/**
	 * Provides the balance for a specific account.
	 *
	 * @param number
	 *            identifies the account.
	 * @return HTTP return containing the calculated account balance or an error
	 *         message.
	 * @author Jotham Weber
	 */
	@GET
	@Path("/accountBalance/{number}")
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getAccountBalance(@PathParam("number") String number) {

		logger.info("Anforderung des Guthabens des Kontos " + number + ".");
		String errorMessage = "";
		try {
			// Das aktuelle Guthaben wird abgefragt und übermittelt
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

	/**
	 * Resets the database tables to a standard scenario with a bank account and
	 * 4 customer accounts. Each one of the customer accounts will start with a
	 * balance of 10,000.
	 *
	 * @return HTTP response.
	 * @author Jotham Weber
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
