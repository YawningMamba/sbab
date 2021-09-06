package sbab.toplist;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import sbab.sl.api.SLHTTP;
import sbab.sl.api.aggregation.Line;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/top-list/api/{mode}")
public class TopListAPI {

    public CacheControl createCacheControl()
    {
        final Calendar calendar = Calendar.getInstance();

        final Instant now = calendar.toInstant();

        // External API updates at 2 am every day.
        calendar.set(Calendar.HOUR_OF_DAY, 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);

        final Instant tomorrow = calendar.toInstant();

        final Duration duration = Duration.between(now, tomorrow);

        final long seconds = duration.toSeconds();

        final CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) seconds);

        return cacheControl;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response topListMode(final @DefaultValue("BUS") @PathParam("mode") String mode,
                                final @DefaultValue("10") @QueryParam("maxLines") int maxLines)
    {
        SLHTTP.TransportMode transportMode;
        try {
            transportMode = SLHTTP.TransportMode.valueOf(mode.toUpperCase());
        } catch (final IllegalArgumentException notFound) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final TopListDataContainer container = TopListDataContainer.getGlobal();
        final Collection<Line> lines = container.getTopList(transportMode);

        final String accessToken = System.getProperty(TopListDataContainer.PROPERTY_ACCESS_TOKEN);
        container.requestNewData(transportMode, accessToken);

        final Response response;
        if (lines.isEmpty()) {
            response = Response.noContent().build();
        } else {
            final List<Line> resultLines = lines.stream().limit(maxLines).collect(Collectors.toList());
            final Jsonb jsonb = JsonbBuilder.create();

            final CacheControl cacheControl = createCacheControl();

            response = Response.ok(jsonb.toJson(resultLines)).cacheControl(cacheControl).build();
        }

        return response;
    }
}
