package posta;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.RouteDefinition;
import org.junit.Test;


public class CompetingConsumersDoesNotWork extends CompetingConsumersBase
{
	
	/**
	 * It is recommended to override the method isUseAdviceWith and return true to instruct 
	 * Camel that you are using adviceWith in the unit tests. 
	 * Then in your unit test methods, after you have done the adviceWith you MUST 
	 * start CamelContext by invoke the start method on the context instance.
	 */
	@Override
	public boolean isUseAdviceWith() { return true; }

	/**
	 * Override to determine if CamelContext will be auto started.
	 * Return false and CamelContext will not be auto started (you will have to start it manually)
	 * The use of AdviceWithRoutBuilder and isUseAdviceWith handles starting the context 
	 * at the appropriate time.
	 */
	@Override
	public boolean isUseRouteBuilder() { return false; }
	

	/**
	 * This test throws
	 * org.apache.camel.FailedToStartRouteException: 
	 * Failed to start route CompetingConsumersBase$WordCounter@21aed5f9 
	 * because of Multiple consumers for the same endpoint is not allowed: Endpoint[jms://in]
	 * 
	 * If the context.start() is called before the line:
	 *    context.addRouteDefinitions(wcRoute.getRoutes());
	 * then the test passes.  But even if the context.start() is
	 * called just after this line, the test fails.
	 * 
	 * @throws Exception
	 */
	@Test
	public void jmsQW2Consumers() throws Exception 
	{
		// GIVEN			
		context.addRouteDefinitions(lcRoute.getRoutes()); 
		mockOutPoint(lineCtr, lcOutURI).expectedBodiesReceivedInAnyOrder("1");
//		context.start();  // Starting the context before second route makes this test pass
		context.addRouteDefinitions(wcRoute.getRoutes());
		context.start();  // Starting the context after second route added throws FailedToStartRouteException.
		mockOutPoint(wordCtr, wcOutURI).expectedBodiesReceivedInAnyOrder(wordCount(testMsg1));
//		context.start();
		
		// WHEN
		sendBody(inURI, testMsg1);
		sendBody(inURI, testMsg1);
		
		// THEN
		assertMockEndpointsSatisfied(5, TimeUnit.SECONDS);
	}
	
	/**
	 * Uses adviceWith to mock out-point.  
	 * Starts context.
	 */
	protected MockEndpoint mockOutPoint(final Processor proc, final String outURI) throws Exception 
	{
		List<RouteDefinition> routes = context.getRouteDefinitions();
		if (!routes.isEmpty())
		{
			for (RouteDefinition route:routes)
			{
				if (route.getId().equals(proc.toString()))
				{
					route.adviceWith(context, new AdviceWithRouteBuilder()
					{
						@Override
						public void configure() throws Exception {
							log.info("Mocking " + outURI);
							mockEndpoints(outURI);
						}
					});
					break;
				}
			}
		}
		// We must manually start the context when we are done with all the adviceWith.
//		context.start();
		String mockEndPointURI = "mock:" + outURI.replace("//", ""); 
		return getMockEndpoint(mockEndPointURI);
	}
}
