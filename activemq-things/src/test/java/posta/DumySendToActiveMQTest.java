/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package posta;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

import javax.jms.JMSException;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class DumySendToActiveMQTest extends CamelTestSupport {

    @Test
    public void testFoo() throws InterruptedException, JMSException {
        template.sendBody("jms:incoming", new TestMessageObject("hi"));
        TimeUnit.SECONDS.sleep(1);
        Exchange exchange = consumer.receive("jms:outgoing");
        JmsMessage obj = exchange.getIn(JmsMessage.class);

        System.out.println("Type: "  + obj.getClass());
        Object objectMessage =  obj.getBody();
        System.out.println("Type part 2: " + objectMessage.getClass());
//        assertTrue(obj instanceof TestMessageObject);
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext rc = super.createCamelContext();
        rc.addComponent("jms", JmsComponent.jmsComponent(new ActiveMQConnectionFactory("tcp://localhost:61616")));
        return rc;
    }

}
