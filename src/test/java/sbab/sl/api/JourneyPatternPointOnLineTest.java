package sbab.sl.api;

import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class JourneyPatternPointOnLineTest {

	@Test
	public void canParseXMLToJourney()
			throws Exception
	{
		final String lineNumber = "1";
		final String stopNumber = "10008";
		final String stopPointXML = "<JourneyPatternPointOnLine xmlns=\"https://api.sl.se/api/pws\">" +
				"<LineNumber>" + lineNumber + "</LineNumber>" +
				"<DirectionCode>1</DirectionCode>" +
				"<JourneyPatternPointNumber>" + stopNumber + "</JourneyPatternPointNumber>" +
				"<LastModifiedUtcDateTime>2012-06-23 00:00:00.000</LastModifiedUtcDateTime>" +
				"<ExistsFromDate>2012-06-23 00:00:00.000</ExistsFromDate> " +
				"</JourneyPatternPointOnLine>";

		final Reader reader = new StringReader(stopPointXML);

		final XMLStreamReader streamReader = XMLInputFactory.newFactory().createXMLStreamReader(reader);

		final JourneyPatternPointOnLine point = JourneyPatternPointOnLine.fromXML(streamReader, "JourneyPatternPointOnLine");

		assertEquals(Integer.parseInt(lineNumber), point.getLineNumber());
		assertEquals(Integer.parseInt(stopNumber), point.getPointNumber());
	}
}
