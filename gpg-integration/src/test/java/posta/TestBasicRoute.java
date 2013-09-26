package posta;

import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cposta-contractor
 * Date: 4/16/13
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestBasicRoute extends CamelTestSupport {

    String startEndpoint = "direct:start";
    String encryptedFileName = "file-encrypted.gpg";
    private static final String GPG_KEYFILE_LOCATION = "key.gpg";
    private static final String GPG_USERID = "christian.posta@gmail.com";

    @Test
    public void testRoute() {
        NotifyBuilder builder = new NotifyBuilder(context).whenDone(1).create();

        Record record = new Record();
        record.setDisbursementCode("disbursementCode");
        record.setCertificateValue("certValue");
        record.setCertificateNumber("certNumber");
        record.setPin("12344");
        ArrayList<Record> records = new ArrayList<Record>();
        records.add(record);
        records.add(record);
        records.add(record);
        records.add(record);
        template.sendBody(startEndpoint, records);
        builder.matches(5, TimeUnit.SECONDS);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final DataFormat bindy = new BindyCsvDataFormat();

                // todo - expose as rest service, retrieve from DB w mybatis, xfer to FTP
                from(startEndpoint)
                        .marshal(bindy)
                        .to("log:org.apache.camel.encrypt?level=INFO&showAll=true")
                        .marshal().pgp(GPG_KEYFILE_LOCATION, GPG_USERID)
                        .to("file:target/results?fileName=" + encryptedFileName);

            }
        };
    }
}
