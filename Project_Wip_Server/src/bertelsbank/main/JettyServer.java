package bertelsbank.main;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class JettyServer {

	public static void main(String[] args) throws Exception {
		Server server = new Server(9998);

		// JERSEY
		ResourceConfig resourceConfig = new PackagesResourceConfig("bertelsbank.rest");
		ServletContextHandler sh = new ServletContextHandler();
		sh.setContextPath("/rest");
		sh.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
        server.setHandler(sh);
		server.start();
	}
}
