package sbab.sl.api;

import org.junit.Test;

import java.io.*;
import java.util.Optional;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

public class SLHTTPTest {

	private static final String URI_BASE = "https://api.sl.se/api2/linedata.xml?key=";

	@Test
	public void canCreateStopPointURI()
	{
		final String accessToken = "H12346";
		final SLHTTP.TransportMode mode = SLHTTP.TransportMode.BUS;
		final SLHTTP slhttp = new SLHTTP();

		final String shouldBe = URI_BASE + accessToken + "&model=StopPoint&StopAreaTypeCode=BUSTERM";

		assertEquals(shouldBe, slhttp.createStopPointURI(mode, accessToken).toString());
	}

	@Test
	public void canCreateJourneyURI()
	{
		final String accessToken = "12346H";
		final SLHTTP.TransportMode mode = SLHTTP.TransportMode.FERRY;
		final SLHTTP slhttp = new SLHTTP();

		final String shouldBe = URI_BASE + accessToken + "&model=JourneyPatternPointOnLine&DefaultTransportModeCode=FERRY";

		assertEquals(shouldBe, slhttp.createJourneyURI(mode, accessToken).toString());
	}

	@Test
	public void canWrapInputStreamWithDeflate()
			 throws IOException
	 {
		final Optional<String> encoding = Optional.of("deflate");
		final SLHTTP slhttp = new SLHTTP();

		assertTrue("Failed lower check", slhttp.wrapStream(encoding, InputStream.nullInputStream()) instanceof DeflaterInputStream);

		 final Optional<String> upperEncoding = Optional.of("deflate".toUpperCase());
		assertTrue("Failed upper check", slhttp.wrapStream(upperEncoding, InputStream.nullInputStream()) instanceof DeflaterInputStream);
	}

	@Test
	public void canWrapInputStreamWithGZIP()
			throws IOException
	{
		final Optional<String> encoding = Optional.of("gzip");
		final SLHTTP slhttp = new SLHTTP();

		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
		final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(gzipOutputStream);

		outputStreamWriter.write("All my data.");
		outputStreamWriter.flush();

		gzipOutputStream.finish();
		gzipOutputStream.flush();

		final ByteArrayInputStream lowerCheckStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

		assertTrue("Failed lower check", slhttp.wrapStream(encoding, lowerCheckStream) instanceof GZIPInputStream);

		final ByteArrayInputStream upperCheckStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

		final Optional<String> upperEncoding = Optional.of("gzip".toUpperCase());
		assertTrue("Failed upper check", slhttp.wrapStream(upperEncoding, upperCheckStream) instanceof GZIPInputStream);
	}

	@Test
	public void cannotWrapInputStreamWithGZIPAndDeflate()
	{
		try {
			final Optional<String> encoding = Optional.of("deflate, gzip");
			final SLHTTP slhttp = new SLHTTP();

			slhttp.wrapStream(encoding, InputStream.nullInputStream());

			fail("Should not be able to create stream.");
		} catch (final IOException ioException) {

		}
	}
}
