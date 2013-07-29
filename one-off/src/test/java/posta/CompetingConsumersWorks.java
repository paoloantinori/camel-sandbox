package posta;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CompetingConsumersWorks extends CompetingConsumersBase
{
	@Override
	public String isMockEndpoints() { return "seda*"; }

	@Test
	public void jmsQW2Consumers() throws Exception 
	{
		// GIVEN		
		context.addRouteDefinitions(lcRoute.getRoutes()); 
		context.addRouteDefinitions(wcRoute.getRoutes()); 

		getMockEndpoint("mock:" + lcOutURI.replace("//", ""))
			.expectedBodiesReceivedInAnyOrder("1");
		getMockEndpoint("mock:" + wcOutURI.replace("//", ""))
			.expectedBodiesReceivedInAnyOrder(wordCount(testMsg1));

		// WHEN
		sendBody(inURI, testMsg1);
		sendBody(inURI, testMsg1);
		
		// THEN
		assertMockEndpointsSatisfied(5, TimeUnit.SECONDS);
	}
}
