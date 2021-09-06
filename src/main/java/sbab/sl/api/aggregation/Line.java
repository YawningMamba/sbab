package sbab.sl.api.aggregation;

import jakarta.json.bind.annotation.JsonbProperty;
import sbab.sl.api.SLHTTP;
import sbab.sl.api.StopPoint;

import java.util.NavigableSet;

public class Line
{
	@JsonbProperty("transport")
	private final SLHTTP.TransportMode mode;
	@JsonbProperty("line")
	private final int lineNumber;
	@JsonbProperty("stops")
	private final NavigableSet<StopPoint> stopPoints;

	public Line(final SLHTTP.TransportMode mode,
				final int lineNumber,
				final NavigableSet<StopPoint> stopPoints)
	{
		this.mode = mode;
		this.lineNumber = lineNumber;
		this.stopPoints = stopPoints;
	}

	public SLHTTP.TransportMode getMode() {
		return mode;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public NavigableSet<StopPoint> getStopPoints() {
		return stopPoints;
	}
}
