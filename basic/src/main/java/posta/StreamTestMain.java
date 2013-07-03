package posta;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

/**
 * @author <a href="http://christianposta.com/blog">Christian Posta</a>
 */
public class StreamTestMain {
    public static void main(String[] args) throws Exception {
        DefaultCamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("stream:in?promptMessage=Enter something:").marshal().gzip().to("stream:out");
            }
        });

        context.start();
        Thread.sleep(60 * 1000);
        context.stop();
    }
}
