package posta;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 5/14/13
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestDeadLetterWithProcessor extends CamelTestSupport{

    @Test
    public void testFromMailingList() {

        NotifyBuilder notifyBuilder = new NotifyBuilder(context).whenBodiesDone(1).create();
        template.sendBody("jms:queue:Input", "Hello!");

        notifyBuilder.matches(1, TimeUnit.SECONDS);

    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = new JndiRegistry();
        registry.bind("jms", ActiveMQComponent.activeMQComponent("vm://localhost"));
        return registry;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("jms:queue:Failed")
                        .maximumRedeliveries(1)
                        .redeliveryDelay(0));

                onException(Throwable.class)
                        .maximumRedeliveries(0)
                        .handled(true);


                from("jms:queue:Input")
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                System.out.println("Processing msg " +
                                        exchange.getIn().getBody());
                                throw new RuntimeException("Something bad happens here...");
                            }
                        })
                        .to("jms:queue:Output");
            }
        };

    }
}
