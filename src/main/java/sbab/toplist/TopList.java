package sbab.toplist;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

@Path("v1/top-list")
public class TopList {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String topList() throws IOException {
        final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("top-list/index.html");
        return new String(inputStream.readAllBytes());
    }

    @Path("v1/top-list/res/{resource}")
    public static class JSResources {

        private static final Set<String> AVAILABLE_RESOURCES = new TreeSet<>();

        static {
            AVAILABLE_RESOURCES.add("react.production.min.js");
            AVAILABLE_RESOURCES.add("react-dom.production.min.js");
            AVAILABLE_RESOURCES.add("top-list.js");
            AVAILABLE_RESOURCES.add("style.css");
        }

        @GET
        public Response resource(final @PathParam("resource") String resource)
        {
            final Response response;
            if (AVAILABLE_RESOURCES.contains(resource)) {

                final String contentType;
                if (resource.endsWith(".css")) {
                    contentType = "text/css";
                } else {
                    contentType = "application/javascript";
                }

                final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("top-list/res/" + resource);
                if (inputStream != null) {
                    final InputStream resourceStream = new BufferedInputStream(inputStream);
                    response = Response.ok(resourceStream, contentType).build();
                } else {
                    response = Response.noContent().build();
                }
            } else {
                response = Response.status(Response.Status.NOT_FOUND).build();
            }

            return response;
        }
    }
}
