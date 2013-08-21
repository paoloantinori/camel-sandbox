package posta;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.activemq.camel.component.ActiveMQConfiguration;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;

/**
 * @author <a href="http://christianposta.com/blog">Christian Posta</a>
 */
public class ActiveMQRollbackTest extends CamelTestSupport {

    BrokerService brokerService;

    @Override
    public void setUp() throws Exception {
        System.out.println("Starting broker...");
        BrokerFactoryBean factory = new BrokerFactoryBean();
        factory.setConfig(new ClassPathResource("activemq.xml"));
        factory.afterPropertiesSet();
        brokerService = factory.getBroker();
        brokerService.start();
        brokerService.waitUntilStarted();
        System.out.println("Started!");
        super.setUp();    //To change body of overridden methods use File | Settings | File Templates.
    }


    @After
    public void shutdownBroker() throws Exception {
        brokerService.stop();
        brokerService.waitUntilStopped();
    }

    @Test
    public void testRunner() throws InterruptedException {
        MockEndpoint mock = getMockEndpoint("mock:throwException");
        mock.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getIn();
                Boolean redelivered = message.getHeader("JMSRedelivered", Boolean.class);
                System.out.println("Redelivered? " + redelivered);
                throw new RuntimeException("fail this!");
            }
        });


        template.sendBody("activemq:inbound", "hi");

        System.out.println("wait...");
        Exchange exchange = consumer.receive("activemq:inbound.DLQ", 5 * 1000);
        System.out.println("done waiting...");
        assertNotNull(exchange);
        assertNotNull(exchange.getIn());
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        ActiveMQConfiguration jmsConfiguration = new ActiveMQConfiguration();
        jmsConfiguration.setBrokerURL("tcp://localhost:61616?jms.redeliveryPolicy.maximumRedeliveries=1");
        jmsConfiguration.setTransacted(true);
        jmsConfiguration.setTransactionManager(getTransactionManager(jmsConfiguration.getConnectionFactory()));
        ActiveMQComponent activemq = new ActiveMQComponent(jmsConfiguration);

        CamelContext ctx = super.createCamelContext();
        ctx.addComponent("activemq", activemq);
        return ctx;
    }

    private PlatformTransactionManager getTransactionManager(ConnectionFactory connectionFactory) {
        return new JmsTransactionManager(connectionFactory);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("activemq:inbound").to("mock:throwException");
            }
        };
    }
}
