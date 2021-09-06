package sbab.sl.api.aggregation;

import sbab.sl.api.JourneyPatternPointOnLine;
import sbab.sl.api.SLHTTP;
import sbab.sl.api.StopPoint;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class LinesAggregator
{
	private final Logger logger;

	private final XMLInputFactory inputFactory;

	public static LinesAggregator newDefault()
	{
		return new LinesAggregator(Logger.getGlobal(), XMLInputFactory.newDefaultFactory());
	}

	public static DataCollector newLongestLinesCollector(final SLHTTP.TransportMode mode)
	{
		final LinkedHashMap<Integer, List<Integer>> journeyMap = new LinkedHashMap<>();
		final Map<Integer, StopPoint> stopPointMap = new HashMap<>();

		return new DataCollector(journeyMap, stopPointMap, (completeJourneyMap, completeStopPointMap) -> {

			final TreeSet<Line> lines = new TreeSet<>((line1, line2) -> {
				final int line1StopPoints = line1.getStopPoints().size();
				final int line2StopPoints = line2.getStopPoints().size();

				return Integer.compare(line1StopPoints, line2StopPoints);
			});

			completeJourneyMap.forEach((lineNumber, stops) -> {
				final NavigableSet<StopPoint> lineStops = stops
						.stream().map(completeStopPointMap::get).filter(Objects::nonNull).collect(
								Collectors.toCollection(() -> new TreeSet<>((stopPoint1, stopPoint2) -> {

						final String stop1Name = stopPoint1.getPointName();
						final int pointNumberCompare = Integer.compare(stopPoint1.getPointNumber(), stopPoint2.getPointNumber());;

						final boolean isSquashed = pointNumberCompare != 0&& stop1Name != null &&stop1Name.equals(stopPoint2.getPointName());

						final int compareValue;
						if (isSquashed) {
							compareValue = 0;
						} else {
							compareValue = pointNumberCompare;
						}

						return compareValue;
					})));

				final Line line = new Line(mode, lineNumber, lineStops);

				lines.add(line);
			});

			return lines.descendingSet();
		});
	}

	public LinesAggregator(final Logger logger,
						   final XMLInputFactory xmlInputFactory)
	{
		this.logger = logger;
		this.inputFactory = xmlInputFactory;
	}


	private void locateObject(final InputStream stream,
							  final String objectIdentifier,
							  final Consumer<XMLStreamReader> consumer)
			throws Exception
	{
		final XMLStreamReader reader = this.inputFactory.createXMLStreamReader(stream);

		int eventType;
		while (reader.hasNext()) {
			eventType = reader.next();
			if (eventType == XMLStreamConstants.START_ELEMENT
					&& reader.getLocalName().equals(objectIdentifier)) {
				consumer.accept(reader);
			}
		}
	}

	public void collectXMLJourneyData(final InputStream stream,
									  final DataCollector collector)
	{
		try {
			final String objectIdentifier = "JourneyPatternPointOnLine";
			locateObject(stream, objectIdentifier, streamReader -> {
				try {
					final JourneyPatternPointOnLine journeyPoint = JourneyPatternPointOnLine.fromXML(streamReader, objectIdentifier);
					collector.addJourneyPoint(journeyPoint);

				} catch (final XMLStreamException streamException) {
					// We try parse next object if this happens.
					logger.log(Level.WARNING, "Could not read journey data on stream", streamException);
				}
			});

		} catch (final Exception anyException) {
			collector.fail();
			logger.log(Level.WARNING, "Was not able to consume journey data.", anyException);
		}
	}

	public void collectXMLStopData(final InputStream stream,
								   final DataCollector collector)
	{
		try {
			final String objectIdentifier = "StopPoint";
			locateObject(stream, objectIdentifier, streamReader -> {
				try {
					final StopPoint stopPoint = StopPoint.fromXML(streamReader, objectIdentifier);
					collector.addStopPoint(stopPoint);

				} catch (final XMLStreamException streamException) {
					// We try next object after this happens.
					logger.log(Level.WARNING, "Could not read stop data on stream", streamException);
				}
			});

		} catch (final Exception anyException) {
			collector.fail();
			logger.log(Level.WARNING, "Was not able to consume line data.", anyException);
		}
	}

	public static class DataCollector {

		private Map<Integer, List<Integer>> journeyMap;
		private Map<Integer, StopPoint> stopPointMap;

		private final BiFunction<Map<Integer, List<Integer>>, Map<Integer, StopPoint>, Collection<Line>> aggregate;
		private final LinesFuture linesFuture;

		/*package*/ DataCollector(final Map<Integer, List<Integer>> journeyMap,
								  final Map<Integer, StopPoint> stopPointMap,
								  final BiFunction<Map<Integer, List<Integer>>, Map<Integer, StopPoint>, Collection<Line>> aggregate)
		{
			this.journeyMap = journeyMap;
			this.stopPointMap = stopPointMap;
			this.aggregate = aggregate;
			this.linesFuture = new LinesFuture();
		}

		public void addJourneyPoint(final JourneyPatternPointOnLine journey)
		{
			final List<Integer> stopIds = journeyMap.computeIfAbsent(journey.getLineNumber(), (i) -> new ArrayList<>());
			stopIds.add(journey.getPointNumber());
		}

		public void addStopPoint(final StopPoint stopPoint)
		{
			stopPointMap.put(stopPoint.getPointNumber(), stopPoint);
		}

		public void complete()
		{
			this.journeyMap = Collections.unmodifiableMap(journeyMap);
			this.stopPointMap = Collections.unmodifiableMap(stopPointMap);

			final Collection<Line> lines = aggregate.apply(journeyMap, stopPointMap);

			linesFuture.complete(lines);
		}

		public void fail()
		{
			linesFuture.cancel(false);
		}

		public Future<Collection<Line>> getFuture() {
			return linesFuture;
		}
	}
}
