package bertelsbank.rest;

import java.io.Console;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.eclipse.jetty.http.HttpBuffers;

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

	@POST
	@Path("/transaction")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response executeTransaction(@FormParam("senderNumber") String senderNumber,
			@FormParam("receiverNumber") String receiverNumber, @FormParam("amount") BigDecimal amount,
			@FormParam("reference") String reference) {

		//Ist Eingabe eine Zahl?
		if (senderNumber == null || receiverNumber == null || amount == null || reference == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Nicht alle Felder sind gefüllt.").build();
		}
		if (!(amount.compareTo(BigDecimal.ZERO) == 1)) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Der Betrag muss größer als 0 sein.").build();
		}

		try {
			if (!daAccount.numberExists(receiverNumber)) {
				return Response.status(Response.Status.NOT_FOUND).entity("Das Empfängerkonto existiert nicht.").build();
			}
			if(daTransation.getAccountBalance(senderNumber).compareTo(amount) == -1 && !senderNumber.equals("0000")){
				return Response.status(Response.Status.PRECONDITION_FAILED).entity("Ihr Kontoguthaben reicht für diese Transaktion nicht aus.").build();
			}

			daTransation.addTransaction(senderNumber, receiverNumber, amount, reference);
			return Response.ok().build(); // 204??

		} catch (Exception e) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Interner Serverfehler. Bitte versuchen Sie es erneut.").build();
		}
	}
	
	

	// ============
	// VERWALTUNG
	// ============

	// Neues Konto erstellen
	@POST
	@Path("/addAccount")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response addAccount(@FormParam("owner") String owner, @FormParam("amount") BigDecimal amount)
			throws SQLException {
		if (owner.toLowerCase().equals("bank")) {
			return Response.status(Response.Status.BAD_REQUEST).entity("Das Konto \"Bank\" ist reserviert.").build();
		}
		return Response.ok(daAccount.addAccount(owner, amount)).build();
	}

	// Wird aufgerufen, wenn ein neues Konto angelegt werden soll und liefert
	// eine freie Nummer
	@GET
	@Path("/getFreeNumber")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response getFreeNumber() throws SQLException {
		String freeNumber = daAccount.getFreeNumber();
		if (freeNumber.equals("")) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			return Response.ok(freeNumber).build();
		}
	}

	// Wird aufgerufen, wenn im Dialog "Konto erstellen" der Abbrechen-Button
	// gedrückt wird
	@GET
	@Path("/dereservateNumber/{number}")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response dereservateNumber(@PathParam("number") String number) throws SQLException {
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
