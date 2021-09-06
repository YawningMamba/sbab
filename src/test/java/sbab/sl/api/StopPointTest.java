package sbab.sl.api;

import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class StopPointTest {

	@Test
	public void canParseXMLToStopPoint()
			throws Exception
	{
		final String stopPointName = "T-Centralen";
		final String stopPointNumber = "1051";
		final String stopPointXML = "<StopPoint xmlns=\"https://api.sl.se/api/pws\">" +
				" <StopPointNumber>" + stopPointNumber + "</StopPointNumber>" +
				" <StopPointName>" + stopPointName + "</StopPointName>" +
				" <StopAreaNumber>1051</StopAreaNumber> " +
				"<LocationNorthingCoordinate>59.3313179695028</LocationNorthingCoordinate>" +
				" <LocationEastingCoordinate>18.0616773959365</LocationEastingCoordinate>" +
				" <ZoneShortName>A</ZoneShortName>" +
				" <StopAreaTypeCode>METROSTN</StopAreaTypeCode>" +
				" <LastModifiedUtcDateTime>2014-06-03 00:00:00.000</LastModifiedUtcDateTime> " +
				"<ExistsFromDate>2014-06-03 00:00:00.000</ExistsFromDate>" +
				" </StopPoint>";

		final Reader reader = new StringReader(stopPointXML);

		final XMLStreamReader streamReader = XMLInputFactory.newFactory().createXMLStreamReader(reader);

		final StopPoint point = StopPoint.fromXML(streamReader, "StopPoint");

		assertEquals(stopPointName, point.getPointName());
		assertEquals(Integer.parseInt(stopPointNumber), point.getPointNumber());
	}

}
