package bertelsbank.rest;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;

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

@Path("/")
@Singleton
public class RestResource {
	AccountDataAccess daAccount = new AccountDataAccess();
	Logger logger = Logger.getLogger(getClass());

	@GET
	@Path("/addAccount/{owner}/{amount}")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response addAccount(@PathParam("owner") String owner, @PathParam("amount") BigDecimal amount)
			throws SQLException {
		if (owner.equals("BANK")) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		return Response.ok(daAccount.addAccount(owner)).build();
	}

	@GET
	@Path("/account/{number}")
	@Produces({ MediaType.APPLICATION_JSON })
	// Aufruf mit Parameter
	public Response addAccount(@PathParam("number") String number) throws SQLException {

		return Response.ok(daAccount.getAccountByNumber(number)).build();
	}

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

	@GET
	@Path("/dereservateNumber/{number}")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response dereservateNumber(@PathParam("number") String number) throws SQLException {
		daAccount.reservatedNumbers.remove(number);
		return Response.ok().build();
	}

	@GET
	@Path("/addTransaction/{senderNumber}/{receiverNumber}/{amount}/{reference}")
	@Produces({ MediaType.TEXT_PLAIN })
	// Aufruf mit Parameter
	public Response dereservateNumber(@PathParam("senderNumber") String senderNumber, @PathParam("receiverNumber")
	String receiverNumber, @PathParam("amount") BigDecimal amount, @PathParam("reference") String reference) throws SQLException {
		daAccount.addTransaction(senderNumber, receiverNumber, amount, reference);
		return Response.ok().build();
	}

}
