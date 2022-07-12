package simpleserver;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import io.swagger.annotations.Api;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

public class SimpleServerWithSwaggerUI extends Application {

    public static class Greeting {
        public String greeting;
        public Integer repeat;
    }

    public static class Database {
        AtomicReference<String> currentGreeting = new AtomicReference<String>("Hola");
    }

    @Api
    @Path("/hello")
    static public class SimpleResource {
        private Database database;

        public SimpleResource(Database database) {
            this.database = database;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getAGreeting() {
            return database.currentGreeting.get();
        }

        @POST
        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.APPLICATION_JSON)
        public String setTheGreeting(Greeting greeting) {
            database.currentGreeting.set(greeting.greeting + " " + greeting.repeat + " times");
            return database.currentGreeting.get();
        }
    }

    @Override
    public Set<Object> getSingletons() {
        // this, unfortunately, presents a warning. The warning seems incorrect - the
        // only way around it is to use a DI framework.
        // It can be disabled with one of these:
        // log4j.logger.org.glassfish.jersey.internal=OFF
        // log4j.logger.org.glassfish.jersey.internal.inject.Providers=ERROR

        //@formatter:off
        return Set.of(
                new SimpleResource(new Database()), 
                new ApiListingResource(),
                new SwaggerSerializers());
        //@formatter:on
    }

    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(9000);
        tomcat.getConnector();

        var context = tomcat.addContext("", null);
        ServletContainer servletContainer = new ServletContainer(
                ResourceConfig.forApplication(new SimpleServerWithSwaggerUI()));
        Tomcat.addServlet(context, "api", servletContainer);
        context.addServletMappingDecoded("/api/*", "api");
        
        Tomcat.addServlet(context, "swagger-ui", new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
                String path = req.getPathInfo();
                path = "/META-INF/resources/webjars/swagger-ui" + path;
                Logger.getAnonymousLogger().log(Level.SEVERE, path);
                getClass().getResourceAsStream(path).transferTo(resp.getOutputStream());
            }
        });
        context.addServletMappingDecoded("/swagger-ui/*", "swagger-ui");

        try {
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
}
