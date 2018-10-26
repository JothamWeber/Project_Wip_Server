package de.fhdw.server.example.rest;

import java.io.File;
import java.sql.SQLException;
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

import com.sun.jersey.spi.resource.Singleton;

@Path("/")
@Singleton
public class RestResource {

	@GET
	@Path("/test")
	@Produces({MediaType.APPLICATION_JSON})
	public Response test(){
		Database db = new Database();

		try {
			return Response.ok(db.provideContents()).build();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return Response.ok("FAIL").build();
		}

	}

	@GET
	@Path("/addition/{zahl1}/{zahl2}")
	@Produces({MediaType.APPLICATION_JSON})
	public Response addition(@PathParam("zahl1") double zahl1, @PathParam("zahl2") double zahl2) {

		RestData rd = new RestData();
		Double result = zahl1 + zahl2;
		rd.setInfo(result.toString());
		return Response.ok(rd).build();
	}

	@GET
	@Path("/files")
	@Produces({MediaType.APPLICATION_JSON})
	public Response files() {
		File folder = new File("D:/Ordner");
		File[] filesInFolder = folder.listFiles();
		MyFileWrapper myFileWrapper = new MyFileWrapper(filesInFolder);

		return Response.ok(myFileWrapper).build();
	}

    @GET
    @Path("/hello")
    @Produces({ MediaType.TEXT_PLAIN })
    //Aufruf ohne Parameter
    public Response hello() {
        return Response.ok("Hello World").build();

        // Beispiele für Fehler
        // Fehler 5xx mit eigener Fehlermeldung
        // return Response.serverError().entity("Fehler").build();
        // Fehler 4xx
        // return Response.status(Response.Status.BAD_REQUEST).build();
    }

    @GET
    @Path("/hello/{name}")
    @Produces({ MediaType.TEXT_PLAIN })
    //Aufruf mit Parameter
    public Response helloName(@PathParam("name") String name) {
        return Response.ok("Hello " + name).build();
    }















// =============================
// Für spätere Android Beispiele
// =============================

    @GET
    @Path("/data")
    @Produces({ MediaType.APPLICATION_JSON })
    //Aufruf mit Rückgabe eines JSON Objekts
    public Response getData() {
        RestData rd = new RestData();
        rd.setInfo("Antwort: " + new Random().nextInt(42));
        return Response.ok(rd).build();
    }

    @POST
    @Path("/data")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    //Senden von Daten
    public Response sendData(@FormParam("info") String info) {
        System.out.println(info);
        return Response.ok().build();
    }
}
