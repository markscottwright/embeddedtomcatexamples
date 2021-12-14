package simpleserver;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class SimpleDIServer extends ResourceConfig {

    public static class Database {

    }

    @Path("/hello")
    static public class SimpleResource {
        private Database database;

        @Inject
        public SimpleResource(Database database) {
            this.database = database;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String getAGreeting() {
            return "this is the simplest thing: " + database;
        }
    }

    public SimpleDIServer() {
        registerClasses(SimpleResource.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new Database()).to(Database.class);
            }
        });
    }

    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(9000);
        tomcat.getConnector();

        var context = tomcat.addContext("", null);
        Tomcat.addServlet(context, "api", new ServletContainer(new SimpleDIServer()));
        context.addServletMappingDecoded("/api/*", "api");

        try {
            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            e.printStackTrace();
        }
    }
}
