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
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.spi.Policy;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author <a href="http://www.christianposta.com/blog">Christian Posta</a>
 */
public class RequestReplyWithinJmsTxTest extends CamelTestSupport {


    @Test
    public void testInOutTransacted() {
        String response = template.requestBody("activemq:incoming", "message", String.class);
        assertNotNull(response);
        assertEquals("you did it!", response);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                Policy policy = context.getRegistry().lookup("PROPAGATION_REQUIRES_NEW_POLICY", SpringTransactionPolicy.class);
                from("activemq:incoming")
                        .policy(policy)
                        // note the key here is stepping out of the thread to get out of the
                        // current transaction
                        .inOut().to("seda:intermediate")
                        .log("Response: ${body}");

                from("seda:intermediate").to("activemq:reply");

                from("activemq:reply").transform(constant("you did it!"));
            }
        };
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {
        DefaultCamelContext camelContext = (DefaultCamelContext) super.createCamelContext();

        // create activemq component, configured for tx
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://localhost");
        JmsComponent component = ActiveMQComponent.jmsComponentTransacted(factory);

        PlatformTransactionManager txManager = component.getConfiguration().getTransactionManager();
        camelContext.addComponent("activemq", component);

        JndiRegistry registry = (JndiRegistry) ((PropertyPlaceholderDelegateRegistry) camelContext.getRegistry()).getRegistry();

        TransactionTemplate defaultTemplate = new TransactionTemplate(txManager);
        SpringTransactionPolicy propRequired = new SpringTransactionPolicy(defaultTemplate);
        registry.bind("PROPAGATION_REQUIRED_POLICY", propRequired);

        TransactionTemplate newTxTemplate = new TransactionTemplate(txManager);
        newTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        SpringTransactionPolicy propRequiresNew = new SpringTransactionPolicy(newTxTemplate);
        registry.bind("PROPAGATION_REQUIRES_NEW_POLICY", propRequiresNew);

        return camelContext;
    }

}
