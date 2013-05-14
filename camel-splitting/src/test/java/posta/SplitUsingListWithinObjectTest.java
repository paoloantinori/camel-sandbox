package posta;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/18/13
 * Time: 10:47 AM
 * To change this template use File | Settings | File Templates.
 */
public class SplitUsingListWithinObjectTest extends CamelTestSupport {

    @Test
    public void testSplit() throws InterruptedException {
        NotifyBuilder notifier = new NotifyBuilder(context).whenDone(1).create();
        MockEndpoint expected = MockEndpoint.resolve(context, "mock:expected");
        expected.expectedMessageCount(4);
        template.sendBody("direct:start", createObjectWithList());

        assertTrue(notifier.matches(2, TimeUnit.SECONDS));
        assertMockEndpointsSatisfied();
    }

    private MyObject createObjectWithList() {
        MyObject object = new MyObject();
        object.items.add("Test1");
        object.items.add("Test2");
        object.items.add("Test3");
        object.items.add("Test4");
        return object;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").split(simple("${body.items}")).to("mock:expected");
            }
        };
    }

    public class MyObject {
        List<String> items = new ArrayList<String>();

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }
    }
}
