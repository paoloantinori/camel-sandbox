import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/15/13
 * Time: 11:04 AM
 * To change this template use File | Settings | File Templates.
 */
public class TestCsv extends CamelTestSupport {


    @Test
    public void testDriveCsv() {
        System.out.println("Whatever");

        NotifyBuilder notifier = new NotifyBuilder(context).whenDone(1).create();

        File csvFile = new File(getClass().getResource("/sample.csv").getFile());
        template.sendBody("direct:incoming", csvFile);

        assertTrue(notifier.matches(5, TimeUnit.SECONDS));

        // assert the payload
        MockEndpoint mockEndpoint = MockEndpoint.resolve(context, "mock:result");
        List list = (List) mockEndpoint.getExchanges().get(0).getIn().getBody();
        assertEquals(13, list.size());

    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:incoming").
                        unmarshal().csv().to("mock:result");

            }
        };

    }
}
