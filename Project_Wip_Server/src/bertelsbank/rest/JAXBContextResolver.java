package bertelsbank.rest;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

// Wird benötigt, wenn Listen von Objekten als JSON zurückgegeben werden sollen

@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

	// Klassen des Datenmodells
	private static final Class<?>[] CLASSES = new Class[] {Transaction.class, Account.class};
	private final JAXBContext context;

	public JAXBContextResolver() throws Exception {
		this.context = new JSONJAXBContext(JSONConfiguration.natural().humanReadableFormatting(true).build(), CLASSES);
	}

	@Override
	public JAXBContext getContext(Class<?> objectType) {
		return context;
	}

}
