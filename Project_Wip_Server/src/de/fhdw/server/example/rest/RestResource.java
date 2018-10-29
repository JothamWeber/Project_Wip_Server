package de.fhdw.server.example.rest;

import java.io.File;
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

import de.fhdw.server.example.db.Database;

@Path("/")
@Singleton
public class RestResource {
	Database db = new Database();
	Logger logger = Logger.getLogger(getClass());

	@GET
	@Path("/calc/{num1}/{num2}")
	@Produces({MediaType.APPLICATION_JSON})
	public Response calc(@PathParam("num1") double num1, @PathParam("num2") double num2) {
		double result = num1 + num2;
		logger.info(String.format("Calc: %s + %s = %s", num1, num2, result));
		ResultData resultData = new ResultData();
		resultData.setResult(result);
		return Response.ok(resultData).build();
	}

	@GET
	@Path("/students")
	@Produces({MediaType.APPLICATION_JSON})
	public Response students() {
		List<Student> students = db.getStudents();
		Student[] studentArray = students.toArray(new Student[0]);
		return Response.ok(studentArray).build();

//		Alternative Lösung: Wrapper-Klasse für ArrayList:
//		MyStudentListWrapper studentList = new MyStudentListWrapper(students);
//		return Response.ok(studentList).build();
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
