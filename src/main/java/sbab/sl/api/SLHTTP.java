package sbab.sl.api;

import jakarta.ws.rs.core.UriBuilder;
import org.eclipse.jetty.http.HttpStatus;
import sbab.sl.api.aggregation.Line;
import sbab.sl.api.aggregation.LinesAggregator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

public class SLHTTP {

	public enum TransportMode {
		BUS("BUSTERM"),
		METRO("METROSTN"),
		TRAM("TRAMSTN"),
		TRAIN("RAILWSTN"),
		SHIP("SHIPBER"),
		FERRY("FERRYBER");

		private final String stopCode;

		private TransportMode(final String stopCode) {
			this.stopCode = stopCode;
		}
	}

	// We are using xml because when I tested the api JSON API did not support gzip.
	// This makes it so that a request for StopPoint data sends the following over the network.
	// 852K	content.gzip // GZIP xml
	// 5.1M	content.json // raw json
	// I have not measured but I'm guessing the network is going to be the bottleneck in this and not content parsing.
	private static final String BASE_URI = "https://api.sl.se/api2/linedata.xml";
	private static final String HEADER_CONTENT_ENCODING = "Content-Encoding";

	private final Logger logger;

	/*package*/ SLHTTP() {
		this(Logger.getGlobal());
	}

	public SLHTTP(final Logger logger) {
		this.logger = logger;
	}

	private static HttpRequest gzipDeflateRequest(final URI uri)
	{
		return HttpRequest.newBuilder(uri)
				.header("Accept-Encoding", "gzip, deflate")
				.GET()
				.version(HttpClient.Version.HTTP_2).build();
	}

	private static HttpResponse.BodyHandler<InputStream> bufferedHandler(final int bufferSize)
	{
		final HttpResponse.BodyHandler<InputStream> inputStreamHandler = HttpResponse.BodyHandlers.ofInputStream();
		return HttpResponse.BodyHandlers.buffering(inputStreamHandler, bufferSize);
	}

	/*package*/ URI createStopPointURI(final TransportMode transportMode,
									   final String accessToken)
	{
		final UriBuilder builder = UriBuilder.fromUri(BASE_URI);

		builder.queryParam("key", accessToken);
		builder.queryParam("model", "StopPoint");
	 	builder.queryParam("StopAreaTypeCode", transportMode.stopCode);

		return builder.build();
	}

	/*package*/ URI createJourneyURI(final TransportMode transportMode,
									 final String accessToken)
	{
		final UriBuilder builder = UriBuilder.fromUri(BASE_URI);

		builder.queryParam("key", accessToken);
		builder.queryParam("model", "JourneyPatternPointOnLine");
		builder.queryParam("DefaultTransportModeCode", transportMode.name());

		return builder.build();
	}

	// We need to always drain stream to completion otherwise it cannot be reused.
	// https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpResponse.BodySubscribers.html#ofInputStream()
	private void drainAndCloseStream(final InputStream inputStream)
	{
		try (inputStream) {
			while (inputStream.read() != -1) {}
		} catch (final IOException ioException) {
			// Ignore.
		}
	}

	// Figure out how we should unpack the data.
	/*package*/
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	/*package*/	InputStream wrapStream(final Optional<String> encodingValue,
									   final InputStream stream)
		throws IOException
	{

		final InputStream inputStream;
		if (encodingValue.isPresent()) {
			final String encoding = encodingValue.get();
			switch (encoding.toLowerCase()) {
				case "gzip":
					inputStream = new GZIPInputStream(stream);
					break;
				case "deflate":
					inputStream = new DeflaterInputStream(stream);
					break;
				default:
					// We do not support use of multiple applied compressions at this point.
					throw new IOException("Could not decompress stream encoded with " + encoding);
			}
		} else {
			inputStream = stream;
		}

		return inputStream;
	}

	public Future<Collection<Line>> lineTopListForMode(final HttpClient client,
													   final TransportMode mode,
													   final String authToken)
	{
		final URI journeyURI = createJourneyURI(mode, authToken);
		final URI stopURI = createStopPointURI(mode, authToken);

		final HttpRequest journeyRequest = gzipDeflateRequest(journeyURI);
		final HttpRequest stopRequest = gzipDeflateRequest(stopURI);

		final int bufferSize = 1024 * 16;

		final HttpResponse.BodyHandler<InputStream> journeyInput = bufferedHandler(bufferSize);
		final HttpResponse.BodyHandler<InputStream> stopInput = bufferedHandler(bufferSize);

		final LinesAggregator.DataCollector collector = LinesAggregator.newLongestLinesCollector(mode);
		final LinesAggregator aggregator = LinesAggregator.newDefault();

		final AtomicBoolean shouldComplete = new AtomicBoolean(false);

		// This lambda creates a consumer for the data.
		// As it is hard to test this code due to it being classes that cannot be instantiated and using a lot of
		// parameters from internal function I thought it was best to sacrifice some readability to have a single point
		// of failure.
		final Function<BiConsumer<InputStream, LinesAggregator.DataCollector>, Consumer<HttpResponse<InputStream>>>
				constructorFunction = (collectorBiConsumer) -> response -> {
					final InputStream originalStream = response.body();
					try {
						if (response.statusCode() == HttpStatus.OK_200) {
							final HttpHeaders headers = response.headers();
							final Optional<String> encodingValue = headers.firstValue(HEADER_CONTENT_ENCODING);

							final InputStream xmlStream = wrapStream(encodingValue, originalStream);

							collectorBiConsumer.accept(xmlStream, collector);

							// Complete the collector if all data has been collected.
							if (shouldComplete.getAndSet(true)) {
								collector.complete();
							}
						} else {
							// Cancel future if we fail to collect any data.
							collector.fail();
						}
					} catch (final Exception e) {
						collector.fail();
						logger.log(Level.WARNING, "Could not decode data.", e);
					} finally {
						drainAndCloseStream(originalStream);
					}
				};

		// Collect data from api response.
		client.sendAsync(journeyRequest, journeyInput)
				.thenAccept(constructorFunction.apply(aggregator::collectXMLJourneyData));
		client.sendAsync(stopRequest, stopInput)
				.thenAccept(constructorFunction.apply(aggregator::collectXMLStopData));

		return collector.getFuture();
	}
}
