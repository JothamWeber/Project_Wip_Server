package bertelsbank.rest;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import com.sun.jersey.spi.resource.Singleton;

import bertelsbank.dataAccess.AccountDataAccess;
import bertelsbank.dataAccess.TransactionDataAccess;

@Path("/")
@Singleton
public class RestResource {
	AccountDataAccess daAccount = new AccountDataAccess();
	TransactionDataAccess daTransation = new TransactionDataAccess();
	Logger logger = Logger.getLogger(getClass());

	// ==========================
	// ÖFFENTLICHE SCHNITTSTELLEN
	// ==========================

	// Liefert ein Konto falls vorhanden.
	/**
	 * @param number
	 * @return
	 */
	@GET
	@Path("/account/{number}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAccount(@PathParam("number") String number) {

		if (number.length() != 4 || !isInteger(number)) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Die Kontonummer muss aus 4 Zahlen bestehen.")
					.build();
		}

		try {
			if (!daAccount.numberExists(number)) {
				return Response.status(Response.Status.NOT_FOUND).entity("Diese Kontonummer existiert nicht.").build();
			} else {
				return Response.ok(daAccount.getAccountByNumber(number, true)).build();
			}
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
		}
	}

	/**
	 * @param senderNumber
	 * @param receiverNumber
	 * @param amount
	 * @param reference
	 * @return
	 */
	@POST
	@Path("/transaction")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response executeTransaction(@FormParam("senderNumber") String senderNumber,
			@FormParam("receiverNumber") String receiverNumber, @FormParam("amount") BigDecimal amount,
			@FormParam("reference") String reference) {

		// Sind alle Werte vorhanden?
		if (senderNumber == null || receiverNumber == null || amount == null || reference == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Nicht alle Felder sind gefüllt.").build();
		}
		// Ist amount größer als 0?
		if (!(amount.compareTo(BigDecimal.ZERO) == 1)) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Der Betrag muss größer als 0 sein.").build();
		}
		// Hat amount mehr als 2 Nachkommastellen?
		if (amount.scale() > 2) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Der Betrag darf maximal 2 Nachkommastellen haben.").build();
		}

		try {
			// Existiert das Empfängerkonto?
			if (!daAccount.numberExists(receiverNumber)) {
				return Response.status(Response.Status.NOT_FOUND).entity("Das Empfängerkonto existiert nicht.").build();
			}
			// Hat das Empfängerkonto ausreichend Guthaben?
			if (daTransation.getAccountBalance(senderNumber).compareTo(amount) == -1 && !senderNumber.equals("0000")) {
				return Response.status(Response.Status.PRECONDITION_FAILED)
						.entity("Ihr Kontoguthaben reicht für diese Transaktion nicht aus.").build();
			}
			// Transaktion in Datenbank schreiben
			daTransation.addTransaction(senderNumber, receiverNumber, amount, reference);
			return Response.status(Response.Status.NO_CONTENT).build(); // ok

		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
		}
	}

	// ============
	// VERWALTUNG
	// ============

	// Neues Konto erstellen
	/**
	 * @param owner
	 * @param amount
	 * @return
	 */
	@POST
	@Path("/addAccount")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addAccount(@FormParam("owner") String owner, @FormParam("amount") BigDecimal amount) {
		if (owner.equals("") || amount == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Nicht alle Felder sind gefüllt.").build();
		}
		if (owner.toLowerCase().equals("bank")) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Das Konto \"Bank\" ist reserviert.").build();
		}
		if (amount.compareTo(BigDecimal.ZERO) != 1) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Das Startguthaben muss größer als 0 sein.")
					.build();
		}
		try {
			daAccount.addAccount(owner, amount);
			return Response.ok().build();
		} catch (SQLException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
		}

	}

	@GET
	@Path("/allTransactions")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getAllTransactions() {
		try {
			List<Transaction> transactions = daTransation.getTransactionHistory("all");
			Transaction[] transactionArray = transactions.toArray(new Transaction[0]);
			return Response.ok(transactionArray).build();
		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
		}
	}

	// Wird aufgerufen, wenn ein neues Konto angelegt werden soll und liefert
	// eine freie Nummer
	@GET
	@Path("/getFreeNumber")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response getFreeNumber() {
		String freeNumber;
		try {
			freeNumber = daAccount.getFreeNumber();
			if (freeNumber.equals("")) {
				return Response.status(Response.Status.NOT_FOUND).entity("Es gibt keine freien Nummern mehr.").build();
			} else {
				return Response.ok(freeNumber).build();
			}
		} catch (SQLException e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
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
	// Aufruf mit Parameter
	public Response dereservateNumber(@FormParam("number") String number) {
		daAccount.reservatedNumbers.remove(number);
		return Response.ok().build();
	}

	// =======================
	// SONSTIGE HILFSMETHODEN
	// =======================

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
