package sbab;

import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import sbab.sl.api.SLHTTP;
import sbab.toplist.TopList;
import sbab.toplist.TopListAPI;
import sbab.toplist.TopListDataContainer;

import java.net.URI;
import java.util.logging.Logger;

public class ServerMain {

	private static final String PROPERTY_HOST_ADDRESS = "host.address";
	private static final String PROPERTY_HOST_PORT = "host.port";

	public static void main(final String[] args)
			throws Exception
	{
		final String accessToken = System.getProperty(TopListDataContainer.PROPERTY_ACCESS_TOKEN);
		if (accessToken == null) {
			throw new NullPointerException("Access token must be supplied add it using " + TopListDataContainer.PROPERTY_ACCESS_TOKEN + " property.");
		}

		final String address = System.getProperty(PROPERTY_HOST_ADDRESS, "0.0.0.0");
		final int port = Integer.getInteger(PROPERTY_HOST_PORT, 8081);

		// We don't support TLS at this moment.
		final URI baseUri = UriBuilder.fromUri("http://" + address).port(port).build();
		final Logger logger = Logger.getGlobal();

		final ResourceConfig config = new ResourceConfig(
				TopListAPI.class, TopList.class, TopList.JSResources.class
		);
		EncodingFilter.enableFor(config, GZipEncoder.class);

		final Server server = JettyHttpContainerFactory.createServer(baseUri, config);
		final TopListDataContainer topListDataContainer = TopListDataContainer.getGlobal();

		topListDataContainer.requestNewData(SLHTTP.TransportMode.BUS, accessToken);

		server.start();
		logger.info("Server started.");

		// Keep the main thread alive while the server is running.
		server.join();
	}
}
