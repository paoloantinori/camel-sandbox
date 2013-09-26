import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import posta.ModelClass;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/15/13
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestBindy extends CamelTestSupport {

    
    @Test
    public void testBindy() {
        NotifyBuilder notifier = new NotifyBuilder(context).whenDone(1).create();

        File csvFile = new File(getClass().getResource("/sample.csv").getFile());
        template.sendBody("direct:incoming", csvFile);

        assertTrue(notifier.matches(5, TimeUnit.SECONDS));

        // assert the payload
        MockEndpoint mockEndpoint = MockEndpoint.resolve(context, "mock:result");
        List<ModelClass> list = (List<ModelClass>) mockEndpoint.getExchanges().get(0).getIn().getBody();
        assertEquals(13, list.size());
        ModelClass modelClass = list.get(0);
        assertEquals("99USD", modelClass.getAmount());
        assertEquals("test comment", modelClass.getComment());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        final DataFormat bindy = new BindyCsvDataFormat();
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:incoming").unmarshal(bindy).to("mock:result");
            }
        };


    }
}
