package posta;
import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.processor.interceptor.DefaultTraceFormatter;
import org.apache.camel.processor.interceptor.Tracer;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;


public class CompetingConsumersBase extends CamelTestSupport
{
	String inURI = "jms:in";
	String wcOutURI = "seda://wc.out";
	String lcOutURI = "seda://lc.out";
	
	String testMsg1 = "This is a test message.";
	
	LineCounter lineCtr = new LineCounter();
	WordCounter wordCtr = new WordCounter();
	
	class LineCounter implements Processor
	{
		@Override
		public void process(Exchange exchange) throws Exception {
			Message msg = exchange.getIn();
			msg.setBody(StringUtils.split(msg.getBody(String.class), 
					System.getProperty("path.separator")).length);
		}
	}
	
	class WordCounter implements Processor
	{
		@Override
		public void process(Exchange exchange) throws Exception {
			Message msg = exchange.getIn();
			msg.setBody(StringUtils.split(msg.getBody(String.class)).length);
			;
		}
	}
	
	class SUT extends RoutesDefinition
	{
		public SUT(final Processor processor, final String inURI, final String outURI) throws Exception
		{
			RouteBuilder rb = new RouteBuilder() {
				@Override
				public void configure() throws Exception 
				{
					from(inURI)
					.id(processor.toString())
					.process(processor)					
					.log("Processed message with "+processor.toString()+": ${body}")
					.to(outURI)
					;
				}
			};
			rb.configure();
			route(rb.getRouteCollection().getRoutes().get(0));  
		}	
	}

	SUT lcRoute, wcRoute;
	
	protected CamelContext createCamelContext() throws Exception {
	    CamelContext camelContext = super.createCamelContext();
	    String url = "vm://test-broker?broker.persistent=false&broker.useJmx=false";
	    ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
	    camelContext.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));
	    return camelContext;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.camel.test.junit4.CamelTestSupport#setUp()
	 */	
	@Before
	@Override
	public void setUp() throws Exception
	{
		super.setUp();
		log.info("Context Name: "+context.getName());
		setupTracing();
		lcRoute = new SUT(lineCtr, inURI, lcOutURI);
		wcRoute = new SUT(wordCtr, inURI, wcOutURI);
	}

	/**
	 * Configure Tracer (eg. setting max characters in output, etc.
	 */
	private void setupTracing() 
	{
		DefaultTraceFormatter formatter = new DefaultTraceFormatter();
		formatter.setMaxChars(300);
		Tracer tracer = new Tracer();
		tracer.setFormatter(formatter);
		context.addInterceptStrategy(tracer);
		context.setTracing(true);
	}
	

	/**
	 * Word Count 
	 * @param line String from which to count words.
	 * @return number of words in the line as a String
	 */
	protected String wordCount(String line) {
		return line.split(" ").length + "";
	}

}
