package sbab.sl.api.aggregation;

import org.junit.Test;
import sbab.sl.api.JourneyPatternPointOnLine;
import sbab.sl.api.SLHTTP;
import sbab.sl.api.StopPoint;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

public class LinesAggregatorTest {

	@Test
	public void canPrioritizeLinesBasedOnLength()
			throws InterruptedException, ExecutionException
	{
		final LinesAggregator.DataCollector collector = LinesAggregator.newLongestLinesCollector(SLHTTP.TransportMode.BUS);

		final Future<Collection<Line>> lines = collector.getFuture();

		final StopPoint myStopPoint = new StopPoint("Hello Stop", 2);
		final StopPoint mySecondStopPoint = new StopPoint("Hello Stop second", 4);
		final StopPoint myThirdStopPoint = new StopPoint("Hello Stop third", 5);
		final StopPoint myForthStopPoint = new StopPoint("Hello Stop forth", 6);

		final StopPoint myDupeStopPoint = new StopPoint("Hello Stop second", 7);

		final StopPoint myNotFoundStopPoint = new StopPoint("Hello Stop not found", 8);

		final int line10id = 10;

		collector.addJourneyPoint(new JourneyPatternPointOnLine(line10id, myStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line10id, mySecondStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line10id, myThirdStopPoint.getPointNumber()));

		final int line20id = 20;

		collector.addJourneyPoint(new JourneyPatternPointOnLine(line20id, myStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line20id, mySecondStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line20id, myThirdStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line20id, myForthStopPoint.getPointNumber()));

		collector.addJourneyPoint(new JourneyPatternPointOnLine(line20id, myDupeStopPoint.getPointNumber()));

		final int line30id = 30;

		collector.addJourneyPoint(new JourneyPatternPointOnLine(line30id, myStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line30id, mySecondStopPoint.getPointNumber()));
		collector.addJourneyPoint(new JourneyPatternPointOnLine(line30id, myNotFoundStopPoint.getPointNumber()));

		collector.addStopPoint(myStopPoint);
		collector.addStopPoint(mySecondStopPoint);
		collector.addStopPoint(myThirdStopPoint);
		collector.addStopPoint(myForthStopPoint);

		collector.addStopPoint(myDupeStopPoint);

		collector.complete();

		assertTrue(lines.isDone());

		final Collection<Line> linesCollection = lines.get();
		final Iterator<Line> lineIterator = linesCollection.iterator();

		final Line line = lineIterator.next();
		assertEquals(line20id, line.getLineNumber());

		final NavigableSet<StopPoint> stopPoints = line.getStopPoints();

		assertEquals(4, stopPoints.size());
		assertTrue("Missing first", stopPoints.contains(myStopPoint));
		assertTrue("Missing other", stopPoints.contains(mySecondStopPoint));
		assertTrue("Missing third", stopPoints.contains(myThirdStopPoint));
		assertTrue("Missing forth", stopPoints.contains(myForthStopPoint));

		assertEquals(line10id, lineIterator.next().getLineNumber());
		assertEquals(line30id, lineIterator.next().getLineNumber());
	}
}
