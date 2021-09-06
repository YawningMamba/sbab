package sbab.sl.api;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Objects;

public class JourneyPatternPointOnLine {

	public static JourneyPatternPointOnLine fromXML(final XMLStreamReader streamReader,
													final String endElementName)
			throws XMLStreamException
	{
		boolean isDone = false;

		int eventType;
		int lintNumber = -1;
		int pointNumber = -1;
		while (!isDone &&streamReader.hasNext()) {
			eventType = streamReader.next();
			if (eventType == XMLStreamConstants.START_ELEMENT) {
				switch (streamReader.getLocalName()) {
					case "LineNumber":
						streamReader.next();
						lintNumber = Integer.parseInt(streamReader.getText());
						break;

					case "JourneyPatternPointNumber":
						streamReader.next();
						pointNumber = Integer.parseInt(streamReader.getText());
						break;
				}
			}
			isDone = (eventType == XMLStreamConstants.END_ELEMENT && streamReader.getLocalName().equals(endElementName));
		}

		if (lintNumber == -1 ||pointNumber == -1) {
			throw new XMLStreamException("Incorrect value for journey, [lineNumber=" + lintNumber + ",pointNumber=" + pointNumber + "].");
		}

		return new JourneyPatternPointOnLine(lintNumber, pointNumber);
	}

	private final int lineNumber;
	private final int pointNumber;

	public JourneyPatternPointOnLine(final int lineNumber,
									 final int pointNumber)
	{
		this.lineNumber = lineNumber;
		this.pointNumber = pointNumber;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public int getPointNumber() {
		return pointNumber;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final JourneyPatternPointOnLine point = (JourneyPatternPointOnLine) o;
		return lineNumber == point.lineNumber && pointNumber == point.pointNumber;
	}

	@Override
	public int hashCode() {
		return Objects.hash(lineNumber, pointNumber);
	}
}
