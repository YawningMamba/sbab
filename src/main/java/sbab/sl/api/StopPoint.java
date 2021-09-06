package sbab.sl.api;

import jakarta.json.bind.annotation.JsonbProperty;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Objects;

public class StopPoint {

	@JsonbProperty("name")
	private final String pointName;
	@JsonbProperty("id")
	private final int pointNumber;

	public static StopPoint fromXML(final XMLStreamReader streamReader,
									final String endElementName)
			throws XMLStreamException
	{
		boolean isDone = false;

		int eventType;
		int pointNumber = -1;
		String pointName = null;
		while (!isDone &&streamReader.hasNext()) {
			eventType = streamReader.next();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				switch (streamReader.getLocalName()) {
					case "StopPointNumber":
						streamReader.next();
						pointNumber = Integer.parseInt(streamReader.getText());
						break;

					case "StopPointName":
						streamReader.next();
						pointName = streamReader.getText();
						break;
				}
			}
			isDone = (eventType == XMLStreamConstants.END_ELEMENT && streamReader.getLocalName().equals(endElementName));
		}

		if (pointName == null ||pointNumber == -1) {
			throw new XMLStreamException("Incorrect value for stop, [pointName=" + pointName + ",pointNumber=" + pointNumber + "].");
		}

		return new StopPoint(pointName, pointNumber);
	}

	public StopPoint(final String pointName,
					 final int pointNumber)
	{
		this.pointName = pointName;
		this.pointNumber = pointNumber;
	}

	public int getPointNumber() {
		return pointNumber;
	}

	public String getPointName() {
		return pointName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StopPoint point = (StopPoint) o;
		return pointNumber == point.pointNumber && Objects.equals(pointName, point.pointName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pointName, pointNumber);
	}
}
