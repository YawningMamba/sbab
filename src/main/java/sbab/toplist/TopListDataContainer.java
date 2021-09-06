package sbab.toplist;

import sbab.sl.api.SLHTTP;
import sbab.sl.api.aggregation.Line;

import java.net.http.HttpClient;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TopListDataContainer {

	public static final String PROPERTY_ACCESS_TOKEN = "top.list.data.access.token";

	private static final AtomicReference<TopListDataContainer> GLOBAL = new AtomicReference<>();

	private final Map<SLHTTP.TransportMode, Future<Collection<Line>>> incompleteData;
	private final Map<SLHTTP.TransportMode, Collection<Line>> completeData;
	private final Map<SLHTTP.TransportMode, AtomicLong> lastTimeRequested;

	private final Logger logger;
	private final HttpClient client;

	public static TopListDataContainer getGlobal()
	{
		final TopListDataContainer global = GLOBAL.get();

		final TopListDataContainer container;
		if (global == null) {

			final Logger logger = Logger.getGlobal();
			final HttpClient client = HttpClient.newHttpClient();
			final Map<SLHTTP.TransportMode, Future<Collection<Line>>> incompleteMap = new EnumMap<>(SLHTTP.TransportMode.class);
			final Map<SLHTTP.TransportMode, Collection<Line>> completeMap = new EnumMap<>(SLHTTP.TransportMode.class);
			final Map<SLHTTP.TransportMode, AtomicLong> lastTimeRequested = new EnumMap<>(SLHTTP.TransportMode.class);

			final TopListDataContainer topListDataContainer = new TopListDataContainer(logger, client, incompleteMap, completeMap, lastTimeRequested);

			if (GLOBAL.compareAndSet(null, topListDataContainer)) {
				container = topListDataContainer;
			} else {
				container = GLOBAL.get();
			}
		} else {
			container = global;
		}

		return container;
	}

	private TopListDataContainer(final Logger logger,
								 final HttpClient client,
								 final Map<SLHTTP.TransportMode, Future<Collection<Line>>> incompleteData,
								 final Map<SLHTTP.TransportMode, Collection<Line>> completeData,
								 final Map<SLHTTP.TransportMode, AtomicLong> lastTimeRequested)
	{
		this.logger = logger;
		this.client = client;
		this.incompleteData = incompleteData;
		this.completeData = completeData;
		this.lastTimeRequested = lastTimeRequested;
	}

	public Collection<Line> getTopList(final SLHTTP.TransportMode mode)
	{
		final Future<Collection<Line>> topList = incompleteData.get(mode);

		final Collection<Line> resultTopList;
		if (topList != null && topList.isDone()) {
			if (topList.isCancelled()) {
				resultTopList = completeData.getOrDefault(mode, Collections.emptyList());
			} else {
				// Using the local variable to handle the exception case so that we can keep the final collection
				// for the if statement. This will make it so that we get compiler error if we forget to set the
				// data in any of the cases. Simple human error removed by compiler.
				Collection<Line> aList;
				try {
					// We should never freeze here due to that we check isDone before but just
					// in case something is wrong with the future we don't want to freeze the server.
					aList = topList.get(100, TimeUnit.MILLISECONDS);
					completeData.put(mode, aList);

				} catch (final Exception e) {
					logger.log(Level.WARNING, "Could not store completed top list data. ", e);
					aList = completeData.getOrDefault(mode, Collections.emptyList());
				}

				resultTopList = aList;
			}
		} else {
			resultTopList = completeData.getOrDefault(mode, Collections.emptyList());
		}

		return resultTopList;
	}

	public void requestNewData(final SLHTTP.TransportMode mode,
							   final String accessToken)
	{
		// This should not happen but lets not crash if it does.
		if (accessToken != null) {

			final AtomicLong lastTimeAtomic = lastTimeRequested.computeIfAbsent(mode, (aMode) -> new AtomicLong(0));
			final long lastTime = lastTimeAtomic.get();
			final long systemTime = System.currentTimeMillis();

			// Just to not spam API server and lock to often.
			if ((systemTime - lastTime) > 1000 &&lastTimeAtomic.compareAndSet(lastTime, systemTime)) {

				final Future<Collection<Line>> request = incompleteData.get(mode);
				if (request == null ||request.isDone()) {
					synchronized (incompleteData) {
						incompleteData.compute(mode, (currentMode, aRequest) -> {
							if (aRequest == null||aRequest.isDone()) {
								final SLHTTP slhttp = new SLHTTP(logger);
								return slhttp.lineTopListForMode(client, mode, accessToken);
							} else {
								return aRequest;
							}
						});
					}
				}
			}
		} else {
			logger.log(Level.WARNING, "No access token was supplied when trying to access data API.", new NullPointerException());
		}
	}
}
